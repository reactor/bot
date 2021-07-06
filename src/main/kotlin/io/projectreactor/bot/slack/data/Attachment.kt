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

package io.projectreactor.bot.slack.data

data class Attachment(
        val fallback: String, //Required plain-text summary of the attachment
        val title: String, //eg. "Slack API Documentation"
        val title_link: String? = null, //eg. "https://api.slack.com/"
        val color: String? = null, //eg. "#36a64f"
        val pretext: String? = null, //Optional text that appears above the attachment block
        val author_name: String? = null, //eg. "Bobby Tables"
        val author_link: String? = null, //eg. "https://flickr.com/bobby/"
        val author_icon: String? = null, //eg. "https://flickr.com/icons/bobby.jpg"
        val text: String? = null, //Optional text that appears within the attachment
        val image_url: String? = null, //eg "https://my-website.com/path/to/image.jpg"
        val thumb_url: String? = null, //eg. "https://example.com/path/to/thumb.png"
        val footer: String? = null, //eg. "Slack API"
        val footer_icon: String? = null, //eg. "https://platform.slack-edge.com/img/default_application_icon.png"
        val ts: Int? = null, //timestamp to reference if message relates to specific instant
        val fields: List<Field>? = null)