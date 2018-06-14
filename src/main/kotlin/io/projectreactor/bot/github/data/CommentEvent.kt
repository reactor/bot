package io.projectreactor.bot.github.data

data class CommentEvent(val action: String,
                        val issue: PullRequest,
                        val comment: Comment,
                        val repository: Repository)

data class Comment(val id: Int,
                   val user: User,
                   val body: String)