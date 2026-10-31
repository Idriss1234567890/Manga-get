package com.mangalix.sources.mangalek

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Public-page parser for mangalik.net.
 * It deliberately does not bypass CAPTCHA, Cloudflare, login walls, or access controls.
 */
class MangalekSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    companion object {
        const val NAME = "مانجا ليك"
        const val BASE_URL = "https://mangalik.net"
    }

    private fun absolute(url: String): String =
        if (url.startsWith("http")) url else BASE_URL.trimEnd('/') + "/" + url.trimStart('/')

    private fun fetch(url: String): Document {
        val request = Request.Builder()
            .url(absolute(url))
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36")
            .header("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")
            .header("Referer", BASE_URL)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code}")
            val doc = Jsoup.parse(response.body?.string().orEmpty(), response.request.url.toString())
            if (doc.title().contains("Just a moment", true) ||
                doc.select("form#challenge-form, #cf-challenge-running").isNotEmpty()) {
                throw java.io.IOException("Cloudflare verification required; access was not bypassed.")
            }
            return doc
        }
    }

    fun getPopular(page: Int = 1): List<MangaItem> {
        val url = if (page <= 1) "$BASE_URL/manga/" else "$BASE_URL/manga/page/$page/"
        return parseCards(fetch(url))
    }

    fun search(query: String, page: Int = 1): List<MangaItem> {
        val q = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        return parseCards(fetch("$BASE_URL/?s=$q&post_type=wp-manga"))
    }

    private fun parseCards(doc: Document): List<MangaItem> {
        val result = linkedMapOf<String, MangaItem>()
        val selectors = listOf(".c-tabs-item__content", ".page-item-detail", ".manga", ".c-item")
        for (selector in selectors) {
            for (card in doc.select(selector)) {
                val a = card.selectFirst("a[href*=/manga/]") ?: continue
                val href = a.absUrl("href")
                if (href.isBlank() || href.contains("/manga-genre/")) continue
                val title = listOfNotNull(
                    card.selectFirst(".post-title"), card.selectFirst(".item-title"),
                    card.selectFirst("h3"), card.selectFirst("h4"), a
                ).map { it.text().trim() }.firstOrNull { it.isNotBlank() } ?: continue
                val img = card.selectFirst("img")
                val cover = img?.let {
                    listOf(it.attr("data-src"), it.attr("data-lazy-src"), it.absUrl("src"))
                        .firstOrNull(String::isNotBlank)
                }
                val latest = card.selectFirst(".latest-chap, .chapter")?.text()?.trim()
                result.putIfAbsent(href, MangaItem(title, href, cover, latest))
            }
            if (result.isNotEmpty()) break
        }
        return result.values.toList()
    }

    fun getDetails(mangaUrl: String): MangaDetails {
        val doc = fetch(mangaUrl)
        val title = doc.selectFirst(".post-title h1, .post-title, h1")?.text()?.trim()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content") ?: doc.title()
        val img = doc.selectFirst(".profile-manga img")
        val cover = img?.let { it.absUrl("src").ifBlank { it.attr("data-src") } }
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
        val description = doc.selectFirst(".summary__content, .summary-content, .manga-summary")?.text()?.trim()
            ?: doc.selectFirst("meta[name=description]")?.attr("content")
        val genres = doc.select(".genres-content a, .mg_genres a, a[href*=/manga-genre/]")
            .map { it.text().trim() }.filter(String::isNotBlank).distinct()
        val rawStatus = doc.selectFirst(".post-status")?.text().orEmpty()
        val status = when {
            rawStatus.contains("OnGoing", true) -> "مستمر"
            rawStatus.contains("Completed", true) -> "مكتمل"
            rawStatus.isBlank() -> null
            else -> rawStatus
        }
        return MangaDetails(title, absolute(mangaUrl), cover, description, genres, status)
    }

    fun getChapters(mangaUrl: String): List<MangaChapter> {
        val doc = fetch(mangaUrl)
        val result = linkedMapOf<String, MangaChapter>()
        val selectors = ".listing-chapters_wrap a[href], .main.version-chap a[href], " +
            ".wp-manga-chapter a[href], .chapter-item a[href], .list-chapter a[href]"
        for (a in doc.select(selectors)) {
            val href = a.absUrl("href")
            val title = a.text().trim()
            if (href.isBlank() || title.isBlank()) continue
            val number = Regex("""(?:chapter|الفصل)\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
                .find(title)?.groupValues?.getOrNull(1)?.toFloatOrNull()
            result.putIfAbsent(href, MangaChapter(title, href, number))
        }
        return result.values.toList()
    }

    fun getPageImages(chapterUrl: String): List<String> {
        val doc = fetch(chapterUrl)
        return doc.select(".reading-content img.wp-manga-chapter-img, .reading-content .page-break img, img.wp-manga-chapter-img")
            .mapNotNull { img ->
                val value = listOf(
                    img.attr("data-src"), img.attr("data-lazy-src"),
                    img.attr("data-original"), img.attr("src")
                ).firstOrNull(String::isNotBlank) ?: return@mapNotNull null
                if (value.startsWith("http")) value else java.net.URI(doc.location()).resolve(value).toString()
            }
            .filter { it.startsWith("http") }
            .distinct()
    }
}
