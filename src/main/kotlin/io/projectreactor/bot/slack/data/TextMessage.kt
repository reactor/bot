package io.projectreactor.bot.slack.data

import com.fasterxml.jackson.databind.JsonNode

data class TextMessage(val text: String?, val blocks: JsonNode? = null, val attachments: List<Attachment>? = null)