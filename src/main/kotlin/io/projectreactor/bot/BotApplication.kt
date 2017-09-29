package io.projectreactor.bot

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class BotApplication

fun main(args: Array<String>) {
    SpringApplication.run(BotApplication::class.java, *args)

}
