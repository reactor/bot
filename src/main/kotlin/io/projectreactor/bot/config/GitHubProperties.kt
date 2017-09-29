package io.projectreactor.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @author Simon Baslé
 */
@ConfigurationProperties("github")
class GitHubProperties {

    val repos: MutableMap<String, Repo> = mutableMapOf()

    class Repo {
        var org: String = "INVALID"
        var repo: String = "INVALID"
        var watchedLabel: String = "INVALID"
        var maintainers: MutableMap<String, String> = mutableMapOf()
    }
}