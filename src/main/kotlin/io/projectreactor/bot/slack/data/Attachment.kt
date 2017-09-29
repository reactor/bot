package io.projectreactor.bot.slack.data

data class Attachment(
        val fallback: String, //Required plain-text summary of the attachment
        val title: String, //eg. "Slack API Documentation"
        val title_link: String? = null, //eg. "https://api.slack.com/"
        val color: String? = null, //eg. "#36a64f"
        val pretext: String? = null, //Optional text that appears above the attachment block
        val author_name: String? = null, //eg. "Bobby Tables"
        val author_link: String? = null, //eg. "http://flickr.com/bobby/"
        val author_icon: String? = null, //eg. "http://flickr.com/icons/bobby.jpg"
        val text: String? = null, //Optional text that appears within the attachment
        val image_url: String? = null, //eg "http://my-website.com/path/to/image.jpg"
        val thumb_url: String? = null, //eg. "http://example.com/path/to/thumb.png"
        val footer: String? = null, //eg. "Slack API"
        val footer_icon: String? = null, //eg. "https://platform.slack-edge.com/img/default_application_icon.png"
        val ts: Int? = null, //timestamp to reference if message relates to specific instant
        val fields: List<Field>? = null)