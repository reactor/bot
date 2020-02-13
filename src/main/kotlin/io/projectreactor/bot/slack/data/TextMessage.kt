package io.projectreactor.bot.slack.data

import com.fasterxml.jackson.databind.node.ArrayNode

data class TextMessage(val text: String? = null, val blocks: ArrayNode? = null, val attachments: List<Attachment>? = null)