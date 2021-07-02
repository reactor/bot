/*
 * Copyright (c) 2018-2021 VMware Inc. or its affiliates, All Rights Reserved.
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

import io.projectreactor.bot.config.GitHubProperties.Repo
import io.projectreactor.bot.github.data.IssueOrPr
import io.projectreactor.bot.github.data.Repository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * @author Simon Baslé
 */
@Service
@EnableCaching
class IssueService(@Qualifier("githubClient") val client: WebClient) {

    companion object {
        val LOG = LoggerFactory.getLogger(IssueService::class.java)
    }

    @Cacheable("triageCapable")
    fun triageCapable(fullyQualifiedGithubRepoName: String, label: String): Mono<Boolean> {
        return client.get()
                .uri("/repos/$fullyQualifiedGithubRepoName/labels/$label")
                .retrieve()
                .toBodilessEntity()
                .map { true }
                .onErrorReturn(false)
                .cache(Duration.ofHours(1))
    }

    fun label(label: String, issue: IssueOrPr, repo: Repo): Mono<String> {
        val owner = "${repo.org}/${repo.repo}"
        val number = issue.number
        return label(label, number, owner)
    }

    fun label(label: String, number: Int, fullyQualifiedGithubRepoName: String): Mono<String> {
        return client.post()
                .uri("/repos/$fullyQualifiedGithubRepoName/issues/$number/labels")
                .bodyValue("[\"$label\"]")
                .retrieve()
                .bodyToMono<String>()
                .doFirst { LOG.debug("Applying label $label to ${fullyQualifiedGithubRepoName}#${number}") }
                .doOnNext { LOG.trace("Label details: $it") }
                .doOnError {
                    if (it is WebClientResponseException) {
                        LOG.error("GitHub error applying label: ${it.message}" +
                                "\n${it.responseBodyAsString}")
                    }
                    else
                        LOG.error("GitHub error applying label: ${it.message}")
                }
                .doOnSuccess { LOG.debug("Done: Applying label") }
    }

    fun comment(comment: String, issue: IssueOrPr, repo: Repo): Mono<String>
        = comment(comment, issue, "${repo.org}/${repo.repo}")

    fun comment(comment: String, issue: IssueOrPr, repoFullName: String): Mono<String> {
        val number = issue.number
        return client.post()
                .uri("/repos/$repoFullName/issues/$number/comments")
                .bodyValue("{\"body\": \"$comment\"}")
                .retrieve()
                .bodyToMono<String>()
                .doFirst { LOG.debug("Commenting on ${repoFullName}#${issue.number}") }
                .doOnError {
                    if (it is WebClientResponseException) {
                        LOG.error("GitHub error commenting: ${it.message}" +
                                "\n${it.responseBodyAsString}")
                    }
                    else
                        LOG.error("GitHub error commenting: ${it.message}")
                }
                .doOnSuccess { LOG.debug("Done commenting") }
    }

    fun triage(repo: Repository, number: Int, label: String?): Mono<String> {
        if (label == null) {
            return Mono.just("")
        }
        val fullyQualifiedGithubRepoName = repo.full_name
        return triageCapable(fullyQualifiedGithubRepoName, label)
                .flatMap { isTriageCapable ->
                    if (isTriageCapable) {
                        LOG.debug("Marking $fullyQualifiedGithubRepoName#$number for triage")
                        return@flatMap label(label, number, fullyQualifiedGithubRepoName)
                                .doOnError {
                                    if (it is WebClientResponseException) {
                                        LOG.error("GitHub error commenting: ${it.message}" +
                                                "\n${it.responseBodyAsString}")
                                    } else
                                        LOG.error("GitHub error marking for triage: ${it.message}")
                                }
                                .doOnSuccess { if (it == null) LOG.debug("No triage label to apply") else LOG.debug("Successfully marked for triage with label $label")}
                    }
                    else {
                        LOG.debug("Marking for triage not supported for $fullyQualifiedGithubRepoName with label $label")
                        return@flatMap Mono.just("")
                    }
                }
    }
}