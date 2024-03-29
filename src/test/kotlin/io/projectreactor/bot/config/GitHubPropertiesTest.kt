/*
 * Copyright (c) 2021 VMware Inc. or its affiliates, All Rights Reserved.
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

package io.projectreactor.bot.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.junit4.SpringRunner

@ExtendWith(SpringExtension::class)
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