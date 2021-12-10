/*
 * Copyright (c) 2017-2021 VMware Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.projectreactor.bot.github.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
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
                GitRef("reactor:main", "main","c2111862dd0087c1da9be181c4e0284d74a8f858")
        )

        val pr = objectMapper?.readValue<PullRequest>(json)

        Assertions.assertThat(pr).isEqualTo(expected)
    }

    @Test
    fun deserializesNoBase() {
        val json = this.javaClass::class.java.getResource("/prNoBase.json").readText()
        val expected = PullRequest(
                "https://github.com/reactor/reactor-core/pull/886", 886,
                "open", "Another proofreading pass", User("Buzzardo"),
                false, null,null,
                "CONTRIBUTOR",
                base = null
        )

        val pr = objectMapper?.readValue<PullRequest>(json)

        Assertions.assertThat(pr).isEqualTo(expected)
    }
}