package io.projectreactor.bot.config

import io.projectreactor.bot.config.SlackProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

/**
 * @author Simon Baslé
 */
@Configuration
class BotConfiguration {

    @Bean
    fun slackClient(props: SlackProperties): WebClient {
        return WebClient.builder()
                .baseUrl(props.incomingWebhook)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic ${props.botToken}")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
    }

}