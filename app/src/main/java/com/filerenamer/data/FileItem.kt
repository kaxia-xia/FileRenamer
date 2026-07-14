package com.filerenamer.data

import android.net.Uri

data class FileItem(
    val uri: Uri,
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    var isSelected: Boolean = false
)
