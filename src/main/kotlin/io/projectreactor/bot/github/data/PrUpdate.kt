package io.projectreactor.bot.github.data

data class PrUpdate(val action: String,
                    val number: Int,
                    val pull_request: PullRequest,
                    val repository: Repository,
                    val organization: Organization,
                    val label: Label? = null,
                    val sender: User)