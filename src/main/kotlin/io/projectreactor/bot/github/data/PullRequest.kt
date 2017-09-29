package io.projectreactor.bot.github.data

import com.fasterxml.jackson.annotation.JsonAlias
import java.time.LocalDateTime

data class PullRequest(val html_url: String,
                       val number: Int,
                       val state: String,
                       val title: String,
                       @JsonAlias("user") val author: User,
                       val merged_at: LocalDateTime? = null,
                       val merged_by: User? = null,
                       val author_association: String)
