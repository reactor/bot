package io.projectreactor.bot.github

data class PrUpdate(val id:String, val org: String,
                    val repo: String, val labels: Set<String>)