package io.projectreactor.bot

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * @author Simon Baslé
 */
@RestController
class PingController {

    @GetMapping("/", produces = [(MediaType.TEXT_PLAIN_VALUE)])
    fun home() = "UP"

}
