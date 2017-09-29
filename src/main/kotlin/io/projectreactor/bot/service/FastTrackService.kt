package io.projectreactor.bot.service

import io.projectreactor.bot.config.GitHubProperties
import io.projectreactor.bot.config.GitHubProperties.Repo
import io.projectreactor.bot.github.data.PrUpdate
import io.projectreactor.bot.github.data.PullRequest
import io.projectreactor.bot.github.data.Repository
import io.projectreactor.bot.slack.data.Attachment
import io.projectreactor.bot.slack.data.Field
import io.projectreactor.bot.slack.data.TextMessage
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

/**
 * @author Simon Baslé
 */
@Service
class FastTrackService(val ghProps: GitHubProperties, val slackBot: SlackBot) {

    fun cancelFastTrack(event: PrUpdate, repo: Repo): Mono<ServerResponse> {
        val pr = event.pull_request
        val sender = event.sender.login

        val senderId = repo.maintainers[sender]
        val senderNotif = if (senderId == null) sender else "<@$senderId>"

        //TODO find review by bot and remove it
        return slackBot.sendMessage(
                msgCancelFastTrack(pr, senderNotif, sender))
    }

    fun fastTrack(event: PrUpdate, repo: Repo): Mono<ServerResponse> {
        val pr = event.pull_request
        val author = pr.author.login
        val sender = event.sender.login
        val senderId = repo.maintainers[sender]

        val senderNotify = if (senderId == null) sender else "<@$senderId>"
        val otherNotify = repo.maintainers
                .filter { it.key != sender }
                .map { "<@${it.value}>" }
                .joinToString(", ")

        if (!repo.maintainers.containsKey(author)) {
            return slackBot.sendMessage(
                    msgFastTrackNotPossible(pr, repo, senderNotify, sender, author)
            )
        }

        //TODO do the actual Approve
        return slackBot.sendMessage(
                msgFastTrackApproved(pr, repo, senderNotify, otherNotify, sender))
    }

    protected fun msgFastTrackNotPossible(pr: PullRequest, repo: Repo,
                                          senderMention: String,
                                          senderRaw: String,
                                          authorRaw: String): TextMessage {
        val rich = Attachment(
                fallback = "$senderRaw I cannot fast-track approve PRs from $authorRaw, see ${pr.html_url}",
                color = "danger",
                pretext = ":boom: $senderMention I cannot fast-track approve for user '$authorRaw'" +
                        " as he/she is not in my list of maintainers." +
                        "\nPlease do a formal PR review instead.",
                title = pr.title,
                title_link = pr.html_url,
                fields = listOf(
                        Field("Trigger", "Fast Track", true),
                        Field("Label", repo.watchedLabel, true),
                        Field("By", senderRaw, true),
                        Field("Bot Action", "Rejected", true)
                )
        )

        return TextMessage(null, listOf(rich))
    }

    protected fun msgFastTrackApproved(pr: PullRequest, repo: Repo,
                                       senderMention: String,
                                       otherMention: String,
                                       senderRaw: String): TextMessage {
        val reason = Attachment(
                fallback = "Please, ${repo.maintainers.keys}, look at PR ${pr.html_url}, " +
                        "fast-tracked by $senderRaw",
                color = "warning",
                pretext = ":warning: :mag_right: $otherMention please look at this PR that " +
                        "was fast-tracked by $senderMention",
                title = pr.title,
                title_link = pr.html_url,
                fields = listOf(
                        Field("Trigger", "Fast Track", true),
                        Field("Label", repo.watchedLabel, true),
                        Field("By", senderRaw, true),
                        Field("Bot Action", "Auto-Approved PR", true),
                        Field("If You Where @Mentioned", " - Review code even if it " +
                                "was merged and remove label `${repo.watchedLabel}` once done." +
                                "\n - Create an issue if you see any problem with the merged code.", false)
                )
        )
        return TextMessage(null, listOf(reason))
    }

    protected fun msgCancelFastTrack(pr: PullRequest, senderMention: String, senderRaw: String)
            : TextMessage {
        val message = Attachment(
                fallback = "Looks like $senderRaw cancelled fast-track of PR ${pr.html_url} before it was merged.",
                color = "good",
                pretext = ":white_check_mark: Looks like $senderMention cancelled fast-track of PR #${pr.number} before it was merged :+1:",
                title = pr.title,
                title_link = pr.html_url,
                fields = listOf(
                        Field("Trigger", "Fast Track", true),
                        Field("Bot Action", "Removed Auto-Approve", true)
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

    fun process(event: PrUpdate) : Mono<ServerResponse> {
        val repo = findRepo(event.repository) ?: return ServerResponse.noContent().build()

        if (event.action == "labeled" && event.label?.name == repo.watchedLabel) {
            return fastTrack(event, repo)
        }

        if (event.action == "unlabeled" && event.label?.name == repo.watchedLabel
                && event.pull_request.merged_by == null) {
            return cancelFastTrack(event, repo)
        }

        return ServerResponse.noContent().build()
    }
}