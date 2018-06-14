package io.projectreactor.bot.service

import io.projectreactor.bot.config.GitHubProperties
import io.projectreactor.bot.config.GitHubProperties.Repo
import io.projectreactor.bot.github.data.CommentEvent
import io.projectreactor.bot.github.data.Organization
import io.projectreactor.bot.github.data.PrUpdate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

/**
 * @author Simon Baslé
 */
@Service
class CommentService(val ghProps: GitHubProperties,
                     val slackBot: SlackBot,
                     val fastTrackService: FastTrackService,
                     @Qualifier("githubClient") val client: WebClient) {

    companion object {
        val LOG = LoggerFactory.getLogger(CommentService::class.java)

        val LABEL_PREFIX = "label "
        val ASSIGN_PREFIX = "assign me"
        val FAST_TRACK_PREFIX = "fast track"
    }

    fun parseCommand(command: String, event: CommentEvent, repo: Repo): Mono<ServerResponse> {
        if (command.startsWith(LABEL_PREFIX)) {
            return label(command.removePrefix(LABEL_PREFIX), event, repo)
        }
        else if (command.startsWith(ASSIGN_PREFIX)) {
            return assignToAuthor(event, repo)
        }
        else if (command.startsWith(FAST_TRACK_PREFIX)) {
            val msg = command
                    .removePrefix(FAST_TRACK_PREFIX)
                    .trimStart()

            val prEvent = PrUpdate("labeled", event.issue.number,
                    event.issue,
                    event.repository,
                    Organization(repo.org),
                    null,
                    event.comment.user)

            return fastTrackService.fastTrack(prEvent, repo, msg)
        }
        else return ServerResponse.noContent().build()
    }

    protected fun label(label: String, event: CommentEvent, repo: Repo): Mono<ServerResponse> {
        return ServerResponse.noContent().build()
    }

    protected fun assignToAuthor(event: CommentEvent, repo: Repo): Mono<ServerResponse> {
        return ServerResponse.noContent().build()
    }
}