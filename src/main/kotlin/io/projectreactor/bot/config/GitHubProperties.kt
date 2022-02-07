/*
 * Copyright (c) 2017-2022 VMware Inc. or its affiliates, All Rights Reserved.
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

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @author Simon Baslé
 */
@ConfigurationProperties("github")
class GitHubProperties {

    var botUsername: String? = "INVALID"
    var token: String? = "INVALID"
    var noCancel: Boolean = false

    var hookSecret: String? = "INVALID"

    val mergeHintRepos: MutableList<String> = mutableListOf()

    val repos: MutableMap<String, Repo> = mutableMapOf()

    class Repo {
        var org: String = "INVALID"
        var repo: String = "INVALID"
        var watchedLabel: String = "INVALIDLABEL"
        var triageLabel: String? = "INVALIDLABEL"
        /**
         * Map of maintainer information: keys are github handles with '@', values
         * are Slack UIDs.
         */
        var maintainers: MutableMap<String, String> = mutableMapOf()
    }
}