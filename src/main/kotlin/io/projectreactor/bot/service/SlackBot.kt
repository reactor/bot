package io.projectreactor.bot.service

import io.projectreactor.bot.slack.data.TextMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * @author Simon Baslé
 */
@Service
class SlackBot(@Qualifier("slackClient") private val client: WebClient) {

    companion object {
        val LOG = LoggerFactory.getLogger(SlackBot::class.java)
    }

    fun sendMessage(message: TextMessage, linkNames: Boolean = true) : Mono<ResponseEntity<String>> {
        return if (linkNames)
            client.post()
                    .attribute("link_names", 1)
                    .syncBody(message)
                    .exchange()
                    .doOnError { LOG.error("Error during SlackBot phase", it) }
                    .flatMap { it.toEntity(String::class.java) }
        else
            client.post()
                    .syncBody(message)
                    .exchange()
                    .doOnError { LOG.error("Error during SlackBot phase", it) }
                    .flatMap { it.toEntity(String::class.java) }
    }

}