package io.projectreactor.bot.service

import io.projectreactor.bot.config.GitHubProperties
import io.projectreactor.bot.slack.Attachment
import io.projectreactor.bot.slack.Field
import io.projectreactor.bot.slack.TextMessage
import org.springframework.http.HttpStatus.*
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.stream.Collectors

/**
 * @author Simon Baslé
 */
@Service
class FastTrackService(val ghProps: GitHubProperties, val slackBot: SlackBot) {

    fun fastTrack(prId: String, repository: String, organization: String, labels: Set<String>)
            : Mono<ResponseEntity<Unit>> {
        val repo = ghProps.repos.values
                .stream()
                //check the pr is on a relevant repo
                .filter { it.org == organization && it.repo == repository
                        && labels.contains(it.watchedLabel) }
                .findFirst()
                .orElse(null)

        return Mono.justOrEmpty(repo)
                .map({
                    val maintainerNotif = it.maintainers.values.stream()
                            .map { "<@$it>" }
                            .collect(Collectors.joining(", "))

                    val reason = Attachment(
                            fallback = "Please, ${it.maintainers.keys}, look at fast tracked PR $organization/$repository#$prId",
                            color = "warning",
                            pretext = ":warning: $maintainerNotif, please look at this PR:",
                            title = "$organization/$repository#$prId",
                            title_link = "http://github.com/$organization/$repository/issues/$prId",
                            fields = listOf(
                                    Field("Reason", "Fast Tracked", true),
                                    Field("Labels", labels.stream().collect(Collectors.joining(", ")), true),
                                    Field("Recommended Action", "Review code even though it was merged and remove ${repo.watchedLabel} once done" +
                                            "\nor create an issue if something is up", false)
                            )
                    )

                    TextMessage(null, listOf(reason))
                })
                .flatMap { slackBot.sendMessage(it) }
                .map { it.statusCode() }
                .defaultIfEmpty(ACCEPTED)
                .map { status(it).build<Unit>() }
    }
}