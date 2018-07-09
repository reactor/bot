package io.projectreactor.bot.github.data

data class GitRef(val label: String,
                  val ref: String,
                  val sha: String)