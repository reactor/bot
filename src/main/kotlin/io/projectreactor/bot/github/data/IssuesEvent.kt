package io.projectreactor.bot.github.data

data class IssuesEvent(val action: String,
                       val issue: Issue,
                       val repository: Repository)