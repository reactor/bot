package io.projectreactor.bot.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles("test")
class GitHubPropertiesTest {

    @Autowired
    private var config: GitHubProperties? = null

    @Test
    fun hasMergeHintRepoList() {
        assertThat(config?.mergeHintRepos)
                .contains("org/example1", "org/example2")
    }

    @Test
    fun smokeTest() {
        assertThat(config?.botUsername).isEqualTo("fakebot")
    }

}