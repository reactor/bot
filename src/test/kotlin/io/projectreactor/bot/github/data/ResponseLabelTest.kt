package io.projectreactor.bot.github.data;

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.junit4.SpringRunner

/**
 * @author Simon Baslé
 */
@RunWith(SpringRunner::class)
@JsonTest
class ResponseLabelTest {

    @Autowired
    private var objectMapper: ObjectMapper? = null

    @Test
    fun deserializesArray() {
        val json = this.javaClass::class.java.getResource("/responseLabel.json")
                .readText()

        val expected = listOf(ResponseLabel(208045946,
                "https://api.github.com/repos/octocat/Hello-World/labels/bug",
                "bug",
                "Houston, we have a problem",
                "f29513", true))

        val pr = objectMapper?.readValue<List<ResponseLabel>>(json)

        Assertions.assertThat(pr).isEqualTo(expected)
    }
}
