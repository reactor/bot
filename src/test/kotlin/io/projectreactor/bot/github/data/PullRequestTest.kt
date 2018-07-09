package io.projectreactor.bot.github.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@JsonTest
class PullRequestTest {

    @Autowired
    private var objectMapper: ObjectMapper? = null

    @Test
    fun deserializes() {
        val json = this.javaClass::class.java.getResource("/pr.json").readText()
        val expected = PullRequest(
                "https://github.com/reactor/reactor-core/pull/886", 886,
                "open", "Another proofreading pass", User("Buzzardo"),
                false, null,null,
                "CONTRIBUTOR",
                GitRef("reactor:master", "master","c2111862dd0087c1da9be181c4e0284d74a8f858")
        )

        val pr = objectMapper?.readValue<PullRequest>(json)

        Assertions.assertThat(pr).isEqualTo(expected)
    }
}