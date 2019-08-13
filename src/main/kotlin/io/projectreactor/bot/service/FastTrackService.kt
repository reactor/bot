package io.projectreactor.bot.service

import io.projectreactor.bot.config.GitHubProperties
import io.projectreactor.bot.config.GitHubProperties.Repo
import io.projectreactor.bot.github.data.PrUpdate
import io.projectreactor.bot.github.data.PullRequest
import io.projectreactor.bot.github.data.Repository
import io.projectreactor.bot.github.data.ResponseReview
import io.projectreactor.bot.slack.data.Attachment
import io.projectreactor.bot.slack.data.Field
import io.projectreactor.bot.slack.data.TextMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URLEncoder

/**
 * @author Simon Baslé
 */
@Service
class FastTrackService(val ghProps: GitHubProperties,
                       val slackBot: SlackBot,
                       @Qualifier("githubClient") val client: WebClient) {

    companion object {
        val LOG = LoggerFactory.getLogger(FastTrackService::class.java)

        val EVENT_FAST_TRACK = "Fast Track"
        val EVENT_FAST_TRACK_CANCELLED = "Fast Track Cancelled"
        val EVENT_FAST_TRACK_REVIEWED = "Fast Track Reviewed"
    }

    protected fun getBotReviews(event: PrUpdate, repo: Repo, all: Boolean = false): Flux<ResponseReview> {
        LOG.debug("Getting bot reviews")
        return client.get()
                .uri("/repos/${repo.org}/${repo.repo}/pulls/${event.number}/reviews")
                .retrieve()
                .bodyToMono<Array<ResponseReview>>()
                .flatMapMany { Flux.fromArray(it) }
                .doOnNext { LOG.trace("Got review $it") }
                .filter { it.user.login == ghProps.botUsername && (all || it.state == "APPROVED") }
                .doOnNext { LOG.debug("Found bot review $it") }
    }

    protected fun dismissBotReviews(event: PrUpdate, repo: Repo): Mono<ClientResponse> {
        return getBotReviews(event, repo, true)
                .concatMapDelayError( { review ->
                    if (review.state == "PENDING")
                        client.delete()
                                .uri("/repos/${repo.org}/${repo.repo}/pulls/${event.number}/reviews/${review.id}")
                                .exchange()
                                .doFirst { LOG.debug("Deleting PENDING bot review ${review.html_url}") }
                    else
                        client.put()
                                .uri("/repos/${repo.org}/${repo.repo}/pulls/${event.number}/reviews/${review.id}/dismissals")
                                .body("{\"message\": \"Fast-track cancelled by @${event.sender.login}\"}")
                                .exchange()
                                .doFirst { LOG.debug("Dismissing bot review ${review.html_url}") }
                },5)
                .ignoreElements()
    }

    protected fun cancelFastTrack(event: PrUpdate, repo: Repo): Mono<ResponseEntity<String>> {
        val pr = event.pull_request
        val sender = event.sender.login

        val senderId = repo.maintainers[sender]
        val senderNotif = if (senderId == null) sender else "<@$senderId>"

        LOG.debug("Cancelling fast track of ${repo.org}/${repo.repo}#${pr.number}")

        return dismissBotReviews(event, repo)
                .then(msgCancelFastTrack(pr, senderNotif, sender).toMono())
                .doOnError { LOG.error("GitHub error during $EVENT_FAST_TRACK_CANCELLED", it) }
                .onErrorResume { msgGithubError(pr, EVENT_FAST_TRACK_CANCELLED, it).toMono() }
                .flatMap { slackBot.sendMessage(it) }
                .doOnSuccess { LOG.debug("Done: Cancelling fast track of ${repo.org}/${repo.repo}#${pr.number}") }
    }

    protected fun reviewedFastTrack(event: PrUpdate, repo: Repo): Mono<ResponseEntity<String>> {
        val pr = event.pull_request
        val sender = event.sender.login
        val merger = event.pull_request.merged_by?.login ?: "unknown"

        val senderId = repo.maintainers[sender]
        val senderNotif = if (senderId == null) sender else "<@$senderId>"

        val mergerId = repo.maintainers[merger]
        val mergerNotif = if (mergerId == null) merger else "<@$mergerId>"

        LOG.debug("Reviewing fast track of ${repo.org}/${repo.repo}#${pr.number}")

        return dismissBotReviews(event, repo)
                .then(msgReviewedFastTrack(pr, mergerNotif, merger, senderNotif, sender).toMono())
                .doOnError { LOG.error("GitHub error during $EVENT_FAST_TRACK_REVIEWED", it) }
                .onErrorResume { msgGithubError(pr, EVENT_FAST_TRACK_REVIEWED, it).toMono() }
                .flatMap { slackBot.sendMessage(it) }
                .doOnSuccess { LOG.debug("Done: Reviewing fast track of ${repo.org}/${repo.repo}#${pr.number}") }
    }

    protected fun msgFastTrackNotPossible(pr: PullRequest, repo: Repo,
                                          senderMention: String,
                                          senderRaw: String,
                                          authorRaw: String): TextMessage {
        val rich = Attachment(
                fallback = "Cannot fast-track PR #${pr.number}, see ${pr.html_url}",
                color = "danger",
                pretext = ":boom: $senderMention I cannot fast-track approve for user '$authorRaw'" +
                        " as he/she is not in my list of maintainers." +
                        "\nPlease do a formal <${pr.html_url}/files|PR review> instead.",
                title = "PR #${pr.number} \"${pr.title}\"",
                title_link = pr.html_url,
                fields = listOf(
                        Field("Event", EVENT_FAST_TRACK, true),
                        Field("Bot Action", "Rejected", true)
                )
        )

        return TextMessage(null, listOf(rich))
    }

    protected fun msgFastTrackApproved(pr: PullRequest, repo: Repo,
                                       review: ResponseReview,
                                       senderMention: String,
                                       otherMention: String,
                                       senderRaw: String): TextMessage {
        val issueTitle = URLEncoder.encode("Post-facto review of #${pr.number}", "utf-8")
        val issueBody = URLEncoder.encode("I saw issues while reviewing #${pr.number}, " +
                "which was fast-tracked by @$senderRaw", "utf-8")
        val issueUrl = pr.html_url.replace("pull/${pr.number}",
                "issues/new?title=$issueTitle&body=$issueBody")

        val reason = Attachment(
                fallback = "PR #${pr.number} fast-tracked by $senderRaw, see ${pr.html_url}",
                color = "warning",
                pretext = ":mag_right: $otherMention :warning: please look at this " +
                        "<${pr.html_url}|PR> that was fast-tracked by $senderMention",
                title = "PR #${pr.number} \"${pr.title}\"",
                title_link = pr.html_url,
                fields = listOf(
                        Field("Event", EVENT_FAST_TRACK, true),
                        Field("Bot Action", "Auto-Approved PR", true),
                        Field("If You Where @Mentioned before warning sign", " - <${pr.html_url}/files|Review code>" +
                                " even if it was merged and remove label `${repo.watchedLabel}` once done." +
                                "\n - <$issueUrl|Create an issue> if you see any problem with the merged code.", false)
                ),
                footer = "PR Review by bot: ${review.html_url}"
        )
        return TextMessage(null, listOf(reason))
    }

    protected fun msgCancelFastTrack(pr: PullRequest, senderMention: String, senderRaw: String)
            : TextMessage {
        val message = Attachment(
                fallback = "PR #${pr.number} fast-track cancelled by $senderRaw, see ${pr.html_url}",
                color = "good",
                pretext = ":white_check_mark: Looks like $senderMention cancelled fast-track of " +
                        "PR <${pr.html_url}|#${pr.number}> before it was merged :+1:",
                title = "PR #${pr.number} \"${pr.title}\"",
                title_link = pr.html_url,
                fields = listOf(
                        Field("Event", EVENT_FAST_TRACK_CANCELLED, true),
                        Field("Bot Action", "Removed Auto-Approve", true)
                ),
                footer = "PR was not merged and label was removed by @$senderRaw"
        )
        return TextMessage(null, listOf(message))
    }

    protected fun msgReviewedFastTrack(pr: PullRequest,
                                       mergerMention: String, mergerRaw: String,
                                       senderMention: String, senderRaw: String)
            : TextMessage {
        val message = Attachment(
                fallback = "PR #${pr.number} fast-track from $mergerRaw approved by $senderRaw, see ${pr.html_url}",
                color = "good",
                pretext = ":white_check_mark: Looks like $senderMention reviewed after fast-track of " +
                        "PR <${pr.html_url}|#${pr.number}> by $mergerMention :+1:",
                title = "PR #${pr.number} \"${pr.title}\"",
                title_link = pr.html_url,
                fields = listOf(
                        Field("Event", EVENT_FAST_TRACK_REVIEWED, true),
                        Field("Bot Action", "Removed Auto-Approve", true)
                ),
                footer = "PR was merged by @$mergerRaw and label was removed by @$senderRaw"
        )
        return TextMessage(null, listOf(message))
    }

    protected fun msgGithubError(pr: PullRequest, eventType: String, error: Throwable) : TextMessage {
        val errorDetail = if (error !is WebClientResponseException) error.toString()
        else "$error\n${error.responseBodyAsString}"

        val message = Attachment(
                fallback = "Error during $eventType while using GitHub API",
                color = "danger",
                pretext = ":boom: Something went wrong when interacting with GitHub",
                title = "PR #${pr.number} \"${pr.title}\"",
                title_link = pr.html_url,
                fields = listOf(
                        Field("Event", eventType, true),
                        Field("Bot Action", "Logged GitHub error", true),
                        Field("Error Details", errorDetail)
                )
        )
        return TextMessage(null, listOf(message))
    }

    fun findRepo(repo: Repository) : Repo? {
        return ghProps.repos.values
                .stream()
                //check the pr is on a relevant repo
                .filter { repo.full_name == "${it.org}/${it.repo}" }
                .findFirst()
                .orElse(null)
    }

    fun fastTrack(event: PrUpdate, repo: Repo, msg: String? = null): Mono<ResponseEntity<String>> {
        val pr = event.pull_request
        val author = pr.author.login
        val sender = event.sender.login
        val senderId = repo.maintainers[sender]

        if (sender == ghProps.botUsername) {
            //ignore fast track labels put by the bot itself
            LOG.trace("Fast track label set by bot, ignored")
            return ResponseEntity.noContent().build<String>().toMono()
        }

        val senderNotify = if (senderId == null) sender else "<@$senderId>"
        val otherNotify = repo.maintainers
                .filter { it.key != sender }
                .map { "<@${it.value}>" }
                .joinToString(", ")

        if (!repo.maintainers.containsKey(author)) {
            LOG.debug("Fast track of ${repo.org}/${repo.repo}#${pr.number}: not a maintainer")

            return slackBot.sendMessage(
                    msgFastTrackNotPossible(pr, repo, senderNotify, sender, author)
            )
        }

        LOG.debug("Fast track of ${repo.org}/${repo.repo}#${pr.number}")

        val reviewPayload = if (!msg.isNullOrBlank())
            "{\"body\": \"Fast-track requested by @${event.sender.login} " +
                    "with message: $msg\", \"event\": \"APPROVE\"}"
        else
            "{\"body\": \"Fast-track requested by @${event.sender.login}\", " +
                    "\"event\": \"APPROVE\"}"

        val reviewUri = "/repos/${repo.org}/${repo.repo}/pulls/${event.number}/reviews"

        LOG.trace("$reviewUri\n$reviewPayload")

        return getBotReviews(event, repo, true)
                .switchIfEmpty(client.post()
                        .uri(reviewUri)
                        .body(reviewPayload)
                        .retrieve()
                        .bodyToFlux<ResponseReview>()
                        .doFirst { LOG.debug("No current review, creating one") }
                )
                .single()
                .doOnNext { LOG.trace("Bot Review: $it") }
                .map { msgFastTrackApproved(pr, repo, it, senderNotify, otherNotify, sender) }
                .doOnError {
                    if (it is WebClientResponseException) {
                        LOG.error("GitHub error during $EVENT_FAST_TRACK: ${it.message}" +
                                "\n${it.responseBodyAsString}")
                    }
                    else
                        LOG.error("GitHub error during $EVENT_FAST_TRACK: ${it.message}")
                }
                .onErrorResume { msgGithubError(pr, EVENT_FAST_TRACK, it).toMono() }
                .flatMap { slackBot.sendMessage(it) }
                .doOnSuccess { LOG.debug("Done: Fast track of ${repo.org}/${repo.repo}#${pr.number}") }
    }

    fun unfastTrack(event: PrUpdate, repo: Repo) : Mono<ResponseEntity<String>> =
            if (ghProps.noCancel || event.pull_request.merged_by != null)
                reviewedFastTrack(event, repo)
            else
                cancelFastTrack(event, repo)
}