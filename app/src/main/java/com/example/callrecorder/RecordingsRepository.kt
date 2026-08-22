package com.example.callrecorder

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

object RecordingsRepository {

    fun appPrivateDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "CallRecordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun list(context: Context): List<Recording> {
        return when (PrefsHelper.storageMode(context)) {
            PrefsHelper.STORAGE_CUSTOM -> listCustomFolder(context)
            else -> listAppPrivate(context)
        }
    }

    private fun listAppPrivate(context: Context): List<Recording> {
        val dir = appPrivateDir(context)
        val files = dir.listFiles() ?: emptyArray()
        return files
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
            .map {
                Recording(
                    displayName = it.name,
                    uri = Uri.fromFile(it),
                    dateAddedMillis = it.lastModified(),
                    sizeBytes = it.length()
                )
            }
    }

    private fun listCustomFolder(context: Context): List<Recording> {
        val folderUri = PrefsHelper.customFolderUri(context) ?: return emptyList()
        val dir = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
        return dir.listFiles()
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
            .map {
                Recording(
                    displayName = it.name ?: "recording",
                    uri = it.uri,
                    dateAddedMillis = it.lastModified(),
                    sizeBytes = it.length()
                )
            }
    }

    fun delete(context: Context, recording: Recording): Boolean {
        return when (PrefsHelper.storageMode(context)) {
            PrefsHelper.STORAGE_CUSTOM -> {
                val folderUri = PrefsHelper.customFolderUri(context) ?: return false
                val dir = DocumentFile.fromTreeUri(context, folderUri) ?: return false
                dir.findFile(recording.displayName)?.delete() ?: false
            }
            else -> {
                val path = recording.uri.path ?: return false
                File(path).delete()
            }
        }
    }
}
