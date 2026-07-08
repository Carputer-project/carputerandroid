package com.carputer.android.data.model

data class DtcCode(
    val code: Int,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

data class DtcHistoryEntry(
    val code: Int,
    val description: String = "",
    val timestamp: String = "",
)
