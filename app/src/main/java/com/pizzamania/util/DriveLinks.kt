package com.pizzamania.util

/**
 * Accepts normal Google Drive share links like:
 *   https://drive.google.com/file/d/<ID>/view?usp=sharing
 *   https://drive.google.com/open?id=<ID>
 * and turns them into a direct image URL:
 *   https://drive.google.com/uc?export=view&id=<ID>
 *
 * If it's not a Drive URL, returns the original string.
 */
fun toDirectImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val trimmed = url.trim()
    if (!trimmed.contains("drive.google.com")) return trimmed

    val idFromD = Regex("""/d/([a-zA-Z0-9_-]+)""").find(trimmed)?.groupValues?.get(1)
    val idFromQuery = Regex("""[?&]id=([a-zA-Z0-9_-]+)""").find(trimmed)?.groupValues?.get(1)
    val id = idFromD ?: idFromQuery

    return if (id != null) "https://drive.google.com/uc?export=view&id=$id" else trimmed
}
