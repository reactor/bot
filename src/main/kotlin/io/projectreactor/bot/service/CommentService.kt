/*
 * Copyright (c) 2018-2020 VMware Inc. or its affiliates, All Rights Reserved.
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

import io.projectreactor.bot.config.GitHubProperties
import io.projectreactor.bot.config.GitHubProperties.Repo
import io.projectreactor.bot.github.data.CommentEvent
import io.projectreactor.bot.github.data.Organization
import io.projectreactor.bot.github.data.PrUpdate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

/**
 * Parses commands from github
 * @author Simon Baslé
 */
@Service
class CommentService(val ghProps: GitHubProperties,
                     val slackBot: SlackBot,
                     val fastTrackService: FastTrackService,
                     @Qualifier("githubClient") val client: WebClient,
                     helpService: HelpService) {

    companion object {
        val LOG = LoggerFactory.getLogger(CommentService::class.java)

        const val LABEL_PREFIX = "label " //unimplemented
        const val ASSIGN_PREFIX = "assign me" //unimplemented
        const val FAST_TRACK_PREFIX = "fast track"
    }

    init {
        helpService.addHelp(HelpCategory.GITHUB, FAST_TRACK_PREFIX, "Fast track the PR (mark as approved and notifies in Slack), with an optional <message>. Delete the comment to remove the approval.")
    }

    fun parseCommand(command: String, event: CommentEvent, repo: Repo): Mono<ResponseEntity<String>> {
        when {
            command.startsWith(LABEL_PREFIX) -> return label(command.removePrefix(LABEL_PREFIX), event, repo)
            command.startsWith(ASSIGN_PREFIX) -> return assignToAuthor(event, repo)
            command.startsWith(FAST_TRACK_PREFIX) -> {
                val msg = command
                        .removePrefix(FAST_TRACK_PREFIX)
                        .trimStart()

                val deleted = event.action == "deleted"

                val prEvent = PrUpdate(
                        if (deleted) "unlabeled" else "labeled",
                        event.issue.number,
                        event.issue,
                        event.repository,
                        null,
                        event.comment.user)

                return if (deleted)
                    fastTrackService.unfastTrack(prEvent, repo)
                else
                    fastTrackService.fastTrack(prEvent, repo, msg)
            }
            else -> return ResponseEntity.noContent().build<String>().toMono()
        }
    }

    protected fun label(label: String, event: CommentEvent, repo: Repo): Mono<ResponseEntity<String>> {
        //TODO implement setting a label via comment
        return ResponseEntity.noContent().build<String>().toMono()
    }

    protected fun assignToAuthor(event: CommentEvent, repo: Repo): Mono<ResponseEntity<String>> {
        //TODO implement assigning to author via comment
        return ResponseEntity.noContent().build<String>().toMono()
    }
}