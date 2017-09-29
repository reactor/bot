package io.projectreactor.bot.service

import io.projectreactor.bot.slack.TextMessage
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * @author Simon Baslé
 */
@Service
class SlackBot(@Qualifier("slackClient") private val client: WebClient) {

    fun sendMessage(message: TextMessage, linkNames: Boolean = true) : Mono<ClientResponse> {
        return if (linkNames)
            client.post()
                    .attribute("link_names", 1)
                    .syncBody(message)
                    .exchange()
        else
            client.post()
                    .syncBody(message)
                    .exchange()
    }

}