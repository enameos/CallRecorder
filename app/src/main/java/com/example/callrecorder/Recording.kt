package com.example.callrecorder

import android.net.Uri

data class Recording(
    val displayName: String,
    val uri: Uri,
    val dateAddedMillis: Long,
    val sizeBytes: Long
)
