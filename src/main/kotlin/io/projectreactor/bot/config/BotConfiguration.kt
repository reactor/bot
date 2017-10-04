package io.projectreactor.bot.config

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

    @Bean
    fun githubClient(props: GitHubProperties): WebClient {
        return WebClient.builder()
                .baseUrl("https://api.github.com/")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic ${props.token}")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
    }

}