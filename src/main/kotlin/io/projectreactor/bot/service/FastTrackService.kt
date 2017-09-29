package io.projectreactor.bot.service

import io.projectreactor.bot.config.GitHubProperties
import io.projectreactor.bot.config.GitHubProperties.Repo
import io.projectreactor.bot.github.data.PrUpdate
import io.projectreactor.bot.github.data.Repository
import io.projectreactor.bot.slack.data.Attachment
import io.projectreactor.bot.slack.data.Field
import io.projectreactor.bot.slack.data.TextMessage
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

/**
 * @author Simon Baslé
 */
@Service
class FastTrackService(val ghProps: GitHubProperties, val slackBot: SlackBot) {

    fun fastTrack(event: PrUpdate, repo: Repo): Mono<HttpStatus> {
        val pr = event.pull_request
        val author = pr.author.login
        val sender = event.sender.login

        if (!repo.maintainers.containsKey(author)) {
            val senderId = repo.maintainers[sender]
            val notif = if (senderId == null) sender else "<@$senderId>"

            return slackBot.sendMessage(
                    TextMessage("$notif I cannot fast-track approve for user" +
                            " $author as he/she is not in my list of maintainers." +
                            " Please do a formal PR review instead.", null))
                    .map { it.statusCode() }
        }

        val toNotify = repo.maintainers
                .map { if (it.key == sender) "@${it.key}" else "<@${it.value}>" }
                .joinToString(", ")

        val reason = Attachment(
                fallback = "Please, ${repo.maintainers.keys}, look at fast tracked PR ${pr.html_url}",
                color = event.label?.color ?: "warning",
                pretext = ":warning: $toNotify, please look at this PR:",
                title = pr.title,
                title_link = pr.html_url,
                fields = listOf(
                        Field("Reason", "Fast Tracked\n${repo.watchedLabel}", true),
                        Field("Fast Tracked By", sender, true),
                        Field("Recommended Action", "Review code even if it" +
                                "was merged and remove ${repo.watchedLabel} once done" +
                                "\nor create an issue if something is up", false)
                )
        )

        return Mono.just(TextMessage(null, listOf(reason)))
                .flatMap { slackBot.sendMessage(it) }
                .map { it.statusCode() }
    }

    fun findRepo(repo: Repository) : Repo? {
        return ghProps.repos.values
                .stream()
                //check the pr is on a relevant repo
                .filter { repo.full_name == "${it.org}/${it.repo}" }
                .findFirst()
                .orElse(null)
    }

    fun process(event: PrUpdate) : Mono<HttpStatus> {
        val repo = findRepo(event.repository) ?: return HttpStatus.NOT_MODIFIED.toMono()

        if (event.action == "labelled" && event.label?.name == repo.watchedLabel) {
            return fastTrack(event, repo)
        }

        return HttpStatus.NOT_MODIFIED.toMono()
    }
}