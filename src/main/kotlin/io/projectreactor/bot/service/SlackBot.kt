/*
 * Copyright (c) 2017-2021 VMware Inc. or its affiliates, All Rights Reserved.
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
                    .retrieve()
                    .toEntity(String::class.java)
                    .doOnError { LOG.error("Error during SlackBot phase", it) }
        else
            client.post()
                    .bodyValue(message)
                    .retrieve()
                    .toEntity(String::class.java)
                    .doOnError { LOG.error("Error during SlackBot phase", it) }
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
                .retrieve()
                .toEntity(String::class.java)
                .doOnNext {
                    if (it.statusCode.isError || it.body?.contains("\"ok\":false") != false) {
                        LOG.warn("error while sending ephemeral message: $it")
                        LOG.debug("ephemeral message payload was >>>$body<<<")
                    }
                }
                .doOnError { LOG.warn("error while sending ephemeral message", it) }
    }

}