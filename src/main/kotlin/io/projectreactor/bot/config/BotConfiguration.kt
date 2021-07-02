/*
 * Copyright (c) 2017-2018 VMware Inc. or its affiliates, All Rights Reserved.
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
                .defaultHeader(HttpHeaders.AUTHORIZATION, "token ${props.token}")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
    }

}