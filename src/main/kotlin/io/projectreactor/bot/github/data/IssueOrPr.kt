/*
 * Copyright (c) 2018-2021 VMware Inc. or its affiliates, All Rights Reserved.
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

import com.fasterxml.jackson.annotation.JsonAlias
import java.time.LocalDateTime


sealed class IssueOrPr {
    abstract val number: Int
    abstract val state: String
    abstract val title: String
}

data class Issue(var html_url: String,
                 override val number: Int,
                 override val state: String,
                 override val title: String,
                 val labels: List<Label>,
                 @JsonAlias("user") val author: User) : IssueOrPr()

data class PullRequest(val html_url: String,
                       override val number: Int,
                       override val state: String,
                       override val title: String,
                       @JsonAlias("user") val author: User,
                       val merged: Boolean = false,
                       val merged_at: LocalDateTime? = null,
                       val merged_by: User? = null,
                       val author_association: String,
                       val base: GitRef? = null) : IssueOrPr()
