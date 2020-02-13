package io.projectreactor.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @author Simon Baslé
 */
@ConfigurationProperties("slack")
class SlackProperties {

    var incomingWebhook: String = "INVALID"
    var botToken: String = "INVALID"
    var botId: String = "INVALID"

}