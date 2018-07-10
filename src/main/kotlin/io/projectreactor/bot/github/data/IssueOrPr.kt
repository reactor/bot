package io.projectreactor.bot.github.data

import com.fasterxml.jackson.annotation.JsonAlias
import java.time.LocalDateTime


sealed class IssueOrPr {
    abstract val number: Int
    abstract val state: String
    abstract val title: String
}

data class Issue(var html_url: String,
                 override val number: Int,
                 override val state: String,
                 override val title: String,
                 @JsonAlias("user") val author: User) : IssueOrPr()

data class PullRequest(val html_url: String,
                       override val number: Int,
                       override val state: String,
                       override val title: String,
                       @JsonAlias("user") val author: User,
                       val merged: Boolean = false,
                       val merged_at: LocalDateTime? = null,
                       val merged_by: User? = null,
                       val author_association: String,
                       val base: GitRef? = null) : IssueOrPr()
