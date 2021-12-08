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

package io.projectreactor.bot.service

import io.projectreactor.bot.config.GitHubProperties
import io.projectreactor.bot.config.GitHubProperties.Repo
import io.projectreactor.bot.github.data.Repository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * @author Simon Baslé
 */
@Service
class RepoConfigService(val ghProps: GitHubProperties) {

    companion object {
        val LOG = LoggerFactory.getLogger(RepoConfigService::class.java)
    }

    fun findExactRepoConfig(repo: Repository) : Repo? = ghProps.repos.values
            .stream()
            //check the pr is on a relevant repo
            .filter { repo.full_name == "${it.org}/${it.repo}" }
            .findFirst()
            .orElse(null)

    fun findRepoConfigOrCommonConfig(repo: Repository) : Repo? = ghProps.repos.values
            .stream()
            //check the pr is on a relevant repo
            .filter { repo.full_name == "${it.org}/${it.repo}" }
            .findFirst()
            //we need to fallback on synthetic repo to enable blanket hint for PRs merged on maintenance branch
            .orElseGet { emulateRepoFromCommonConfig(repo) }

    fun emulateRepoFromCommonConfig(repo: Repository) : Repo? = ghProps.repos.values
            .stream()
            //check the pr matches a "catchall" repo
            .filter { it.repo == "*" && repo.full_name.startsWith("${it.org}/") }
            .findAny()
            .map { genericRepo ->
                val repoName = repo.name
                val syntheticRepo = Repo()
                with(syntheticRepo) {
                    org = genericRepo.org
                    this.repo = repoName
                    maintainers = genericRepo.maintainers
                    triageLabel = genericRepo.triageLabel
                    watchedLabel = genericRepo.watchedLabel
                }
                syntheticRepo
            }
            .orElse(null)

}