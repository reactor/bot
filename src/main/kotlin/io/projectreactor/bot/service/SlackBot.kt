package io.projectreactor.bot.service

import io.projectreactor.bot.config.SlackProperties
import io.projectreactor.bot.slack.data.TextMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

/**
 * @author Simon Baslé
 */
@Service
class SlackBot(@Qualifier("slackClient") private val client: WebClient, private val props: SlackProperties) {

    companion object {
        val LOG = LoggerFactory.getLogger(SlackBot::class.java)
    }

    fun sendMessage(message: TextMessage, linkNames: Boolean = true) : Mono<ResponseEntity<String>> {
        return if (linkNames)
            client.post()
                    .attribute("link_names", 1)
                    .bodyValue(message)
                    .exchange()
                    .doOnError { LOG.error("Error during SlackBot phase", it) }
                    .flatMap { it.toEntity(String::class.java) }
        else
            client.post()
                    .bodyValue(message)
                    .exchange()
                    .doOnError { LOG.error("Error during SlackBot phase", it) }
                    .flatMap { it.toEntity(String::class.java) }
    }

    fun sendEphemeralMessage(channel: String, user: String, message: TextMessage) : Mono<ResponseEntity<String>> {
        //avoid sending messages to the bot itself
        if (user == props.botId) return ResponseEntity.noContent().build<String>().toMono()

        val body = mutableMapOf<String, Any>("channel" to channel, "user" to user)
        if (message.text != null) body["text"] = message.text
        if (message.attachments != null) body["attachments"] = message.attachments
        if (message.blocks != null) body["blocks"] = message.blocks

        return client.post()
                .uri("https://slack.com/api/chat.postEphemeral")
                .headers { it.setBearerAuth(props.botToken) }
                .bodyValue(body)
                .exchange()
                .flatMap { it.toEntity(String::class.java) }
                .doOnNext {
                    if (it.statusCode.isError || it.body?.contains("\"ok\":false") != false) {
                        LOG.warn("error while sending ephemeral message: $it")
                        LOG.debug("ephemeral message payload was >>>$body<<<")
                    }
                }
                .doOnError { LOG.warn("error while sending ephemeral message", it) }
    }

}