package io.projectreactor.bot.github.data

/**
 * @author Simon Baslé
 */
data class ResponseLabel(val id: Int, val url: String, val name: String,
                         val description: String, val color: String, val default: Boolean)