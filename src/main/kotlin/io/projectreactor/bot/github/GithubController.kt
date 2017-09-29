package io.projectreactor.bot.github

import io.projectreactor.bot.service.FastTrackService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * @author Simon Baslé
 */
@RestController
class GithubController(val fastTrackService: FastTrackService) {

    @PostMapping("/gh/pr", consumes = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    fun onPrUpdate(@RequestBody body: PrUpdate): Mono<ResponseEntity<Unit>> {
        val org = body.org
        val repo = body.repo
        val labels = body.labels
        val prId = body.id

        return fastTrackService.fastTrack(prId, org, repo, labels)
    }

}