package com.mangalix.sources.mangalek

data class MangaItem(val title: String, val url: String, val coverUrl: String? = null, val latestChapter: String? = null)
data class MangaDetails(
    val title: String, val url: String, val coverUrl: String? = null,
    val description: String? = null, val genres: List<String> = emptyList(), val status: String? = null
)
data class MangaChapter(val title: String, val url: String, val number: Float? = null)
