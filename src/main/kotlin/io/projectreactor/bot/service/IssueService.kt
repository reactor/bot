package io.projectreactor.bot.service

import io.projectreactor.bot.config.GitHubProperties.Repo
import io.projectreactor.bot.github.data.IssueOrPr
import io.projectreactor.bot.github.data.ResponseLabel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

/**
 * @author Simon Baslé
 */
@Service
class IssueService(@Qualifier("githubClient") val client: WebClient) {

    companion object {
        val LOG = LoggerFactory.getLogger(IssueService::class.java)
    }

    fun label(label: String, issue: IssueOrPr, repo: Repo): Mono<ResponseEntity<ResponseLabel>> {
        val owner = "${repo.org}/${repo.repo}"
        val number = issue.number
        return client.post()
                .uri("/repos/$owner/issues/$number/labels")
                .syncBody("[\"$label\"]")
                .retrieve()
                .bodyToMono<List<ResponseLabel>>()
                .doOnSubscribe { LOG.debug("Applying label $label to ${repo.org}/${repo.repo}#${issue.number}") }
                .doOnNext { FastTrackService.LOG.trace("Label details: $it") }
                .doOnError {
                    if (it is WebClientResponseException) {
                        FastTrackService.LOG.error("GitHub error applying label: ${it.message}" +
                                "\n${it.responseBodyAsString}")
                    }
                    else
                        FastTrackService.LOG.error("GitHub error applying label: ${it.message}")
                }
                .doOnSuccess { FastTrackService.LOG.debug("Done: Applying label") }
                .map { ResponseEntity.ok(it[0]) }
    }

    fun comment(comment: String, issue: IssueOrPr, repo: Repo): Mono<ResponseEntity<String>> {
        val owner = "${repo.org}/${repo.repo}"
        val number = issue.number
        return client.post()
                .uri("/repos/$owner/issues/$number/comments")
                .syncBody("{\"body\": \"$comment\"}")
                .retrieve()
                .bodyToMono<String>()
                .doOnSubscribe { LOG.debug("Commenting on ${repo.org}/${repo.repo}#${issue.number}") }
                .doOnError {
                    if (it is WebClientResponseException) {
                        FastTrackService.LOG.error("GitHub error commenting: ${it.message}" +
                                "\n${it.responseBodyAsString}")
                    }
                    else
                        FastTrackService.LOG.error("GitHub error commenting: ${it.message}")
                }
                .doOnSuccess { FastTrackService.LOG.debug("Done commenting") }
                .map { ResponseEntity.ok(it) }
    }
}