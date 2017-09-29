package io.projectreactor.bot.slack

data class Field(
        val title: String,
        val value: String,
        val short: Boolean? = null)