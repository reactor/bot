package io.projectreactor.bot.service;


import org.springframework.stereotype.Service;

/**
 * @author Simon Baslé
 */
@Service
class HelpService {

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

	fun dumpHelpPlaintext(): String {
		var helpMessage = "The following commands are known to the bot in the different channels:\n"
		helpByCategory.forEach { cat ->
			helpMessage += "${cat.key}:\n"
			cat.value.forEach {
				helpMessage += " - ${it.key}: ${it.value}\n"
			}
		}
		return helpMessage
	}
}

enum class HelpCategory {
	GITHUB,
	SLACK,
	MISC
}