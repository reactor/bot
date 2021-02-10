package io.projectreactor.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @author Simon Baslé
 */
@ConfigurationProperties("github")
class GitHubProperties {

    var botUsername: String? = "INVALID"
    var token: String? = "INVALID"
    var noCancel: Boolean = false

    val mergeHintRepos: MutableList<String> = mutableListOf()

    val repos: MutableMap<String, Repo> = mutableMapOf()

    class Repo {
        var org: String = "INVALID"
        var repo: String = "INVALID"
        var watchedLabel: String = "INVALIDLABEL"
        var triageLabel: String? = "INVALIDLABEL"
        /**
         * Map of maintainer information: keys are github handles with '@', values
         * are Slack UIDs.
         */
        var maintainers: MutableMap<String, String> = mutableMapOf()
    }
}