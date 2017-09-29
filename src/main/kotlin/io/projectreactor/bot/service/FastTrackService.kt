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

        val senderId = repo.maintainers[sender]
        val senderNotif = if (senderId == null) sender else "<@$senderId>"

        if (!repo.maintainers.containsKey(author)) {
            val notif = if (senderId == null) sender else "<@$senderId>"

            return slackBot.sendMessage(
                    TextMessage("$notif I cannot fast-track approve for user" +
                            " $author as he/she is not in my list of maintainers." +
                            " Please do a formal PR review instead.", null))
                    .map { it.statusCode() }
        }

        val toNotify = repo.maintainers
                .filter { it.key != sender }
                .map { "<@${it.value}>" }
                .joinToString(", ")

        val reason = Attachment(
                fallback = "Please, ${repo.maintainers.keys}, look at PR ${pr.html_url}, " +
                        "fast-tracked by $sender",
                color = event.label?.color ?: "warning",
                pretext = ":warning: $toNotify please look at this PR that was fast-tracked by $senderNotif",
                title = pr.title,
                title_link = pr.html_url,
                fields = listOf(
                        Field("Trigger", "Fast Track", true),
                        Field("Label", repo.watchedLabel, true),
                        Field("By", sender, true),
                        Field("Bot Action", "Auto-Approved PR", true),
                        Field("If You Where @Mentioned", " - Review code even if it " +
                                "was merged and remove label `${repo.watchedLabel}` once done." +
                                "\n - Create an issue if you see any problem with the merged code.", false)
                )
        )

        //TODO actual approve

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
        val repo = findRepo(event.repository) ?: return HttpStatus.NO_CONTENT.toMono()

        if (event.action == "labeled" && event.label?.name == repo.watchedLabel) {
            return fastTrack(event, repo)
        }

        return HttpStatus.NO_CONTENT.toMono()
    }
}