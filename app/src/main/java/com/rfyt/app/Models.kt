package com.rfyt.app

data class YtVideoItem(
    val id: String,
    val title: String,
    val author: String,
    val authorAvatar: String? = null,
    val thumbnail: String,
    val duration: String? = null,
    val views: String? = null,
    val publishedTime: String? = null
)
