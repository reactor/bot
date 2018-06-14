package io.projectreactor.bot.github

import io.projectreactor.bot.config.GitHubProperties
import io.projectreactor.bot.github.data.CommentEvent
import io.projectreactor.bot.github.data.PrUpdate
import io.projectreactor.bot.service.CommentService
import io.projectreactor.bot.service.FastTrackService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * @author Simon Baslé
 */
@RestController
class GithubController(val fastTrackService: FastTrackService,
                       val commentService: CommentService,
                       val ghProps: GitHubProperties) {

    @PostMapping("/gh/pr", consumes = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    fun prHook(@RequestBody event: PrUpdate): Mono<ServerResponse> {
        val repo = fastTrackService.findRepo(event.repository) ?:
                return ServerResponse.noContent().build()

        if (event.action == "labeled" && event.label?.name == repo.watchedLabel) {
            return fastTrackService.fastTrack(event, repo)
                    .timeout(Duration.ofSeconds(4))
        }

        if (event.action == "unlabeled" && event.label?.name == repo.watchedLabel) {
            return fastTrackService.unfastTrack(event, repo)
                    .timeout(Duration.ofSeconds(4))
        }

        return ServerResponse.noContent().build()
    }

    @PostMapping("/gh/comment", consumes = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    fun commentHook(@RequestBody event: CommentEvent): Mono<ServerResponse> {
        val repo = fastTrackService.findRepo(event.repository)
                ?: return ServerResponse.noContent().build()

        val prefix = "@${ghProps.botUsername} "
        //ignore own comments and comments not starting with a mention of the bot
        if (event.comment.user.login == ghProps.botUsername
                || !event.comment.body.startsWith(prefix))
            return ServerResponse.noContent().build()

        return commentService.parseCommand(event.comment.body.removePrefix(prefix),
                event, repo)
                .timeout(Duration.ofSeconds(4))

        return ServerResponse.noContent().build()
    }

}