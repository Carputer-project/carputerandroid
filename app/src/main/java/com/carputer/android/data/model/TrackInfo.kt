package com.carputer.android.data.model

data class TrackInfo(
    val filePath: String,
    val fileName: String,
    val dirName: String,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0,
    val hasArtwork: Boolean = false,
)
