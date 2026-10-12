package com.rfyt.app

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object YtScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    /**
     * Search YouTube videos directly by scraping youtube.com/results HTML and initialData
     */
    suspend fun searchVideos(query: String): List<YtVideoItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<YtVideoItem>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encoded"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()

            // Extract ytInitialData
            val pattern = Pattern.compile("var ytInitialData = (\\{.*?\\});</script>", Pattern.DOTALL)
            val matcher = pattern.matcher(html)

            var jsonStr = ""
            if (matcher.find()) {
                jsonStr = matcher.group(1) ?: ""
            } else {
                val altPattern = Pattern.compile("window\\[\"ytInitialData\"\\] = (\\{.*?\\});</script>", Pattern.DOTALL)
                val altMatcher = altPattern.matcher(html)
                if (altMatcher.find()) {
                    jsonStr = altMatcher.group(1) ?: ""
                }
            }

            if (jsonStr.isNotEmpty()) {
                val root = JsonParser.parseString(jsonStr).asJsonObject
                val contents = root.getAsJsonObject("contents")
                    ?.getAsJsonObject("twoColumnSearchResultsRenderer")
                    ?.getAsJsonObject("primaryContents")
                    ?.getAsJsonObject("sectionListRenderer")
                    ?.getAsJsonArray("contents")

                contents?.forEach { section ->
                    val itemSection = section.asJsonObject.getAsJsonObject("itemSectionRenderer")
                    val items = itemSection?.getAsJsonArray("contents")
                    items?.forEach { itm ->
                        val videoRenderer = itm.asJsonObject.getAsJsonObject("videoRenderer")
                        if (videoRenderer != null) {
                            val videoId = videoRenderer.get("videoId")?.asString ?: ""
                            val title = videoRenderer.getAsJsonObject("title")
                                ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString
                                ?: "Video YouTube"
                            val channelName = videoRenderer.getAsJsonObject("ownerText")
                                ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString
                                ?: "YouTube Creator"
                            val duration = videoRenderer.getAsJsonObject("lengthText")
                                ?.get("simpleText")?.asString
                            val viewCount = videoRenderer.getAsJsonObject("viewCountText")
                                ?.get("simpleText")?.asString
                            val publishedTime = videoRenderer.getAsJsonObject("publishedTimeText")
                                ?.get("simpleText")?.asString

                            val thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                            if (videoId.isNotEmpty()) {
                                list.add(
                                    YtVideoItem(
                                        id = videoId,
                                        title = title,
                                        author = channelName,
                                        thumbnail = thumbUrl,
                                        duration = duration,
                                        views = viewCount,
                                        publishedTime = publishedTime
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: If scraping gets throttled, use Invidious public instance fallback
        if (list.isEmpty()) {
            list.addAll(fetchFromInvidious(query))
        }

        return@withContext list
    }

    /**
     * Fallback to public Invidious YouTube mirror scraper
     */
    private fun fetchFromInvidious(query: String): List<YtVideoItem> {
        val list = mutableListOf<YtVideoItem>()
        val instances = listOf("https://vid.puffyan.us", "https://inv.nadeko.net", "https://invidious.nerdvpn.de")
        
        for (inst in instances) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val req = Request.Builder()
                    .url("$inst/api/v1/search?q=$encoded&type=video")
                    .header("User-Agent", USER_AGENT)
                    .build()

                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val arr = JsonParser.parseString(res.body?.string() ?: "[]").asJsonArray
                    arr.forEach { el ->
                        val obj = el.asJsonObject
                        val vId = obj.get("videoId")?.asString ?: ""
                        if (vId.isNotEmpty()) {
                            list.add(
                                YtVideoItem(
                                    id = vId,
                                    title = obj.get("title")?.asString ?: "",
                                    author = obj.get("author")?.asString ?: "Creator",
                                    thumbnail = "https://i.ytimg.com/vi/$vId/hqdefault.jpg",
                                    duration = "${obj.get("lengthSeconds")?.asInt?.div(60) ?: 0}:${obj.get("lengthSeconds")?.asInt?.rem(60) ?: 0}",
                                    views = "${obj.get("viewCount")?.asLong ?: 0} views"
                                )
                            )
                        }
                    }
                    if (list.isNotEmpty()) break
                }
            } catch (ignored: Exception) {}
        }
        return list
    }
}
