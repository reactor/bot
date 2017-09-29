package io.projectreactor.bot.github

import io.projectreactor.bot.github.data.PrUpdate
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
    fun prHook(@RequestBody body: PrUpdate): Mono<ResponseEntity<Unit>> {
        return fastTrackService.process(body)
                .map { ResponseEntity.status(it).build<Unit>() }
    }

}