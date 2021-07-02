/*
 * Copyright (c) 2020 VMware Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.projectreactor.bot.slack

import com.fasterxml.jackson.databind.JsonNode
import io.projectreactor.bot.config.SlackProperties
import io.projectreactor.bot.service.HelpCategory
import io.projectreactor.bot.service.HelpService
import io.projectreactor.bot.service.SlackBot
import io.projectreactor.bot.slack.data.TextMessage
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.util.Logger
import reactor.util.Loggers

/**
 * @author Simon Baslé
 */
@RestController
class SlackController(val slackBot: SlackBot, private val props: SlackProperties, private val helpService: HelpService) {

    companion object {
        val LOG: Logger = Loggers.getLogger(SlackController::class.java)
        const val COMMAND_HELP = "help"
    }

    init {
        helpService.addHelp(HelpCategory.SLACK, COMMAND_HELP, "Mention the bot with `@bot help` to get this help message")
    }

    @PostMapping("/slack/events", consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun commandHook(@RequestBody node: JsonNode): Mono<ResponseEntity<String>> {
        if (node.get("type")?.asText() == "url_verification") {
            if (node.hasNonNull("challenge")) {
                LOG.info("Challenge from Slack")
                return ResponseEntity
                        .status(200)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("" + node["challenge"])
                        .toMono()
            }
            return ResponseEntity.badRequest().build<String>().toMono()
        }

        if (node.get("type")?.asText() == "event_callback") {
            val event = node.get("event")
            val eventType = event.get("type").asText()

            if (event.get("user")?.asText() == props.botId) {
                //ignore messages that are from the bot itself
                return ResponseEntity.ok().build<String>().toMono()
            }

            if (eventType == "app_mention" || (eventType == "message" && event.get("channel_type")?.asText() == "im")) {
                val user = event.get("user").asText()
                val channel = event.get("channel").asText()
                val text = event.get("text").asText()

                if (text.contains("help")) {
                    slackBot.sendEphemeralMessage(channel, user, TextMessage(text = helpService.dumpHelpMarkdown(), blocks = helpService.dumpHelpSlackBlocksJson()))
                            .subscribe()
                }
                return ResponseEntity.ok().build<String>().toMono()
            }
        }

        return ResponseEntity.badRequest().build<String>().toMono()
    }
}