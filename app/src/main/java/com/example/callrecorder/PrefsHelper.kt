package com.example.callrecorder

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import androidx.preference.PreferenceManager

object PrefsHelper {

    const val KEY_FORMAT = "pref_format"
    const val KEY_SOURCE = "pref_source"
    const val KEY_STORAGE = "pref_storage"
    const val KEY_CUSTOM_FOLDER_URI = "pref_custom_folder_uri"
    const val KEY_BITRATE = "pref_bitrate"

    const val FORMAT_M4A = "m4a"
    const val FORMAT_3GP = "3gp"

    const val SOURCE_VOICE_COMM = "voice_communication"
    const val SOURCE_MIC = "mic"

    const val STORAGE_APP_PRIVATE = "app_private"
    const val STORAGE_CUSTOM = "custom_folder"

    private fun prefs(context: Context) =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun format(context: Context): String =
        prefs(context).getString(KEY_FORMAT, FORMAT_M4A) ?: FORMAT_M4A

    fun audioSource(context: Context): Int {
        val v = prefs(context).getString(KEY_SOURCE, SOURCE_VOICE_COMM)
        return if (v == SOURCE_MIC) MediaRecorder.AudioSource.MIC
        else MediaRecorder.AudioSource.VOICE_COMMUNICATION
    }

    fun storageMode(context: Context): String =
        prefs(context).getString(KEY_STORAGE, STORAGE_APP_PRIVATE) ?: STORAGE_APP_PRIVATE

    fun customFolderUri(context: Context): Uri? {
        val s = prefs(context).getString(KEY_CUSTOM_FOLDER_URI, null) ?: return null
        return Uri.parse(s)
    }

    fun setCustomFolderUri(context: Context, uri: Uri) {
        prefs(context).edit()
            .putString(KEY_CUSTOM_FOLDER_URI, uri.toString())
            .putString(KEY_STORAGE, STORAGE_CUSTOM)
            .apply()
    }

    fun bitrate(context: Context): Int {
        val v = prefs(context).getString(KEY_BITRATE, "64000") ?: "64000"
        return v.toIntOrNull() ?: 64000
    }
}
