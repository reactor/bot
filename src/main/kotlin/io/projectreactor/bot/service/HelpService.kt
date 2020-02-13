package io.projectreactor.bot.service;


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import org.springframework.stereotype.Service

/**
 * @author Simon Baslé
 */
@Service
class HelpService(private val jsonMapper: ObjectMapper) {

	val helpByCategory = mutableMapOf<HelpCategory, MutableMap<String, String>>()

	fun addHelp(category: HelpCategory, command: String, description: String) =
		helpByCategory.computeIfAbsent(category) { mutableMapOf() }
				.put(command, description)

	fun dumpHelpMarkdown(): String {
		var helpMessage = "The following commands are known to the bot in the different channels:\n"
		helpByCategory.forEach { cat ->
			helpMessage += "### ${cat.key}:\n"
			cat.value.forEach {
				helpMessage += " - `${it.key}`; ${it.value}\n"
			}
		}
		return helpMessage
	}

	fun dumpHelpSlackBlocksJson(): ArrayNode {
		val blocks = jsonMapper.createArrayNode()

		blocks.addObject()
				.put("type", "context")
				.put("text", "The Reactor Bot watches the following *MEDIUM* for the given `commands`:")

		helpByCategory.forEach { cat ->
			val section = blocks.addObject()
					.put("type", "section")
					.putObject("text")

			section.put("type", "mrkdwn")

			var text = ":mag_right: *${cat.key}*"
			cat.value.forEach {
				text += "\n\t - `${it.key}` : _${it.value}_"
			}
			section.put("text", text)
		}
		return blocks
	}
}

enum class HelpCategory {
	GITHUB,
	SLACK,
	MISC
}