package io.projectreactor.bot.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GithubControllerCompanionObjectTest {

    @Test
    fun needForwardMergeWithMergeUser() {
        val maintainers = setOf("rick", "summer", "morty", "beth")
        val result = GithubController.maintainersToPing("jerry", "rick", maintainers)

        assertThat(result).isEqualTo("@rick")
    }

    @Test
    fun needForwardMergeAuthorIsMaintainer() {
        val maintainers = setOf("rick", "summer", "morty", "beth")
        val result = GithubController.maintainersToPing("morty", null, maintainers)

        assertThat(result).isEqualTo("@morty")
    }

    @Test
    fun needForwardMergeAuthorIsNotMaintainer() {
        val maintainers = setOf("rick", "summer", "morty", "beth")
        val result = GithubController.maintainersToPing("jerry", null, maintainers)

        assertThat(result).isEqualTo("@rick, @summer, @morty, @beth")
    }

}