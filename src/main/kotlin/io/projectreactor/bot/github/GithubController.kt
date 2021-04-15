package io.projectreactor.bot.github

import io.projectreactor.bot.config.GitHubProperties
import io.projectreactor.bot.github.data.CommentEvent
import io.projectreactor.bot.github.data.IssuesEvent
import io.projectreactor.bot.github.data.PrUpdate
import io.projectreactor.bot.service.CommentService
import io.projectreactor.bot.service.FastTrackService
import io.projectreactor.bot.service.IssueService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Duration

/**
 * @author Simon Baslé
 */
@RestController
class GithubController(val fastTrackService: FastTrackService,
                       val commentService: CommentService,
                       val issueService: IssueService,
                       val ghProps: GitHubProperties) {

    @PostMapping("/gh/pr", consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun prHook(@RequestBody event: PrUpdate): Mono<ResponseEntity<String>> {
        //we distinguish the fast track case from the issue merged case by processing closed first
        if (event.action == "closed") {
            if (event.pull_request.merged && event.pull_request.base != null
                    && event.pull_request.base.ref.endsWith(".x")) {

                val mergeHintEnabled = ghProps.mergeHintRepos.contains(event.repository.full_name)
                if (mergeHintEnabled) {

                    val repo = fastTrackService.findExactRepoConfig(event.repository)
                    val repoMaintainers = repo?.maintainers?.keys ?: emptySet<String>()

                    val maintainersToPing = maintainersToPing(
                            event.pull_request.author.login,
                            event.pull_request.merged_by?.login,
                            repoMaintainers)

                    return issueService.comment("$maintainersToPing this PR seems to have been merged on a maintenance branch, please ensure the change is merge-forwarded to intermediate maintenance branches and up to `main` :bow:",
                            event.pull_request, event.repository.full_name)
                            .map { ResponseEntity.ok(it?.toString() ?: "") }
                }
            }
        }
        else {
            val repo = fastTrackService.findExactRepoConfig(event.repository)
                    ?: return ResponseEntity.noContent().build<String>().toMono()

            if (event.action == "labeled" && event.label?.name == repo.watchedLabel) {
                return fastTrackService.fastTrack(event, repo)
                        .timeout(Duration.ofSeconds(4))
            }

            if (event.action == "unlabeled" && event.label?.name == repo.watchedLabel) {
                return fastTrackService.unfastTrack(event, repo)
                        .timeout(Duration.ofSeconds(4))
            }
        }

        return ResponseEntity.noContent().build<String>().toMono()
    }

    @PostMapping("/gh/comment", consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun commentHook(@RequestBody event: CommentEvent): Mono<ResponseEntity<String>> {
        val repo = fastTrackService.findExactRepoConfig(event.repository)
                ?: return ResponseEntity.noContent().build<String>().toMono()

        val prefix = "@${ghProps.botUsername} "
        //ignore own comments and comments not starting with a mention of the bot
        if (event.comment.user.login == ghProps.botUsername
                || !event.comment.body.startsWith(prefix))
            return ResponseEntity.noContent().build<String>().toMono()

        return commentService.parseCommand(event.comment.body.removePrefix(prefix),
                event, repo)
                .timeout(Duration.ofSeconds(4))
    }

    @PostMapping("gh/issue", consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun issueHook(@RequestBody issueEvent: IssuesEvent): Mono<ResponseEntity<String>> {
        //we need to fallback on synthetic repo to enable blanket triage labelling
        //note that projects which don't even have the common label won't be notified
        val repoProp = fastTrackService.findRepoConfigOrCommonConfig(issueEvent.repository) ?:
                return ResponseEntity.noContent().build<String>().toMono()

        if (issueEvent.action.contains("opened")) {
            if (issueEvent.issue.labels.isNullOrEmpty()) {
                return issueService.triage(issueEvent.repository, issueEvent.issue.number, repoProp.triageLabel)
                        .map { ResponseEntity.ok(it?.toString() ?: "") }
            }
        }
        return ResponseEntity.noContent().build<String>().toMono()
    }

    companion object {

        fun maintainersToPing(author: String, merger: String?, maintainers: Set<String>): String =
                when {
                    merger != null -> "@${merger}"
                    maintainers.contains(author) -> "@$author"
                    maintainers.isEmpty() -> "maintainers,"
                    else -> maintainers.joinToString { "@$it" }
                }
    }

}