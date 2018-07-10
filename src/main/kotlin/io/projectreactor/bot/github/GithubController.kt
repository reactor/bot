package io.projectreactor.bot.github

import io.projectreactor.bot.config.GitHubProperties
import io.projectreactor.bot.github.data.CommentEvent
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
import reactor.core.publisher.toMono
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
        val repo = fastTrackService.findRepo(event.repository) ?:
                return ResponseEntity.noContent().build<String>().toMono()

        if (event.action == "labeled" && event.label?.name == repo.watchedLabel) {
            return fastTrackService.fastTrack(event, repo)
                    .timeout(Duration.ofSeconds(4))
        }

        if (event.action == "unlabeled" && event.label?.name == repo.watchedLabel) {
            return fastTrackService.unfastTrack(event, repo)
                    .timeout(Duration.ofSeconds(4))
        }

        if (event.action == "closed" && event.pull_request.merged && event.pull_request.base?.ref != "master" ) {
            return Mono.zip(
                    issueService.label(repo.forwardLabel, event.pull_request, repo),
                    issueService.comment("@${event.pull_request.author.login} this PR seems to have been merged on a maintenance branch, please ensure the change is merge-forwarded to `master` :bow:",
                                event.pull_request, repo)
            )
                    .map { ResponseEntity.ok(it?.toString() ?: "") }
        }

        return ResponseEntity.noContent().build<String>().toMono()
    }

    @PostMapping("/gh/comment", consumes = [(MediaType.APPLICATION_JSON_VALUE)])
    fun commentHook(@RequestBody event: CommentEvent): Mono<ResponseEntity<String>> {
        val repo = fastTrackService.findRepo(event.repository)
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

}