package io.projectreactor.bot.github.data

/**
 * @author Simon Baslé
 */
data class ResponseReview(val id: Int, val body: String, val state: String,
                          val commit_id: String, val html_url: String, val user: User)