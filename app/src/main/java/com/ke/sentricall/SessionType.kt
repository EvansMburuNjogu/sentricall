package com.ke.sentricall

enum class SessionType(
    val id: String,
    val title: String,
    val description: String
) {
    LISTEN_AUDIO(
        id = "listen_audio",
        title = "Listen to Audio",
        description = "Guard listens to live calls or surroundings for fraud signals."
    ),
    RECORD_SCREEN(
        id = "record_screen",
        title = "Record Screen",
        description = "Guard watches your on-screen activity for suspicious prompts."
    ),
    UPLOAD_MEDIA(
        id = "upload_media",
        title = "Upload Audio / Image",
        description = "Upload recordings or screenshots for a one-off AI scan."
    ),
    WEBSITE_LINK(
        id = "website_link",
        title = "Website Link",
        description = "Paste a link and Guard checks if it looks risky."
    );

    companion object {
        fun fromId(id: String?): SessionType? =
            values().firstOrNull { it.id == id }
    }
}