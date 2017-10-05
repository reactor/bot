package io.projectreactor.bot.github

import io.projectreactor.bot.github.data.PrUpdate
import io.projectreactor.bot.service.FastTrackService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

/**
 * @author Simon Baslé
 */
@RestController
class GithubController(val fastTrackService: FastTrackService) {

    @PostMapping("/gh/pr",
            consumes = arrayOf(MediaType.APPLICATION_JSON_VALUE),
            produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    fun prHook(@RequestBody event: PrUpdate): Mono<ServerResponse> {
        val repo = fastTrackService.findRepo(event.repository) ?:
                return ServerResponse.noContent().build()

        if (event.action == "labeled" && event.label?.name == repo.watchedLabel) {
            return fastTrackService.fastTrack(event, repo)
        }

        if (event.action == "unlabeled" && event.label?.name == repo.watchedLabel) {
            return fastTrackService.unfastTrack(event, repo)
        }

        return ServerResponse.noContent().build()

    }

}