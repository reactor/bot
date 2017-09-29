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
class PrUpdateTest {

    @Autowired
    private var objectMapper: ObjectMapper? = null

    @Test
    fun deserializes() {
        val prJson = this.javaClass::class.java.getResource("/pr.json")
                .readText()
        val json = this.javaClass::class.java.getResource("/prEvent.json")
                .readText()
                .replace("\"<PR_HERE>\"", prJson)

        val expectedPr = PullRequest(
                "https://github.com/reactor/reactor-core/pull/886", 886,
                "open", "Another proofreading pass", User("Buzzardo"),
                null,null,
                "CONTRIBUTOR")
        val expectedRepo = Repository("reactor-core", "reactor/reactor-core")
        val expectedOrg = Organization("reactor")
        val expectedSender = User("simonbasle")
        val expectedLabel = Label("PR-fast-track", "f49b42")
        val expected = PrUpdate("unlabeled", 886, expectedPr,
                expectedRepo, expectedOrg, expectedLabel, expectedSender)

        val pr = objectMapper?.readValue<PrUpdate>(json)

        Assertions.assertThat(pr).isEqualTo(expected)
    }
}