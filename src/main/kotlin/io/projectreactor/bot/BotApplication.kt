package io.projectreactor.bot

import io.projectreactor.bot.config.GitHubProperties
import io.projectreactor.bot.config.SlackProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

@SpringBootApplication
@EnableConfigurationProperties(SlackProperties::class, GitHubProperties::class)
class BotApplication

fun main(args: Array<String>) {
    SpringApplication.run(BotApplication::class.java, *args)

}
