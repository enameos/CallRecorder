package com.example.callrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    private var recorder: MediaRecorder? = null
    private var outputPfd: ParcelFileDescriptor? = null
    private var isRecording = false
    private var recordingStartTime = 0L

    companion object {
        const val ACTION_START = "com.example.callrecorder.action.START"
        const val ACTION_STOP = "com.example.callrecorder.action.STOP"
        const val ACTION_STATE_CHANGED = "com.example.callrecorder.action.STATE_CHANGED"
        const val EXTRA_IS_RECORDING = "extra_is_recording"

        const val CHANNEL_ID = "recording_channel"
        const val NOTIF_ID = 1001

        fun isCurrentlyRecording(context: Context): Boolean = instance?.isRecording ?: false

        private var instance: RecordingService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    private fun startRecording() {
        if (isRecording) return

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        val fileName = buildFileName()

        try {
            val newRecorder = createRecorder()
            when (PrefsHelper.storageMode(this)) {
                PrefsHelper.STORAGE_CUSTOM -> configureCustomOutput(newRecorder, fileName)
                else -> configureAppPrivateOutput(newRecorder, fileName)
            }

            newRecorder.prepare()
            newRecorder.start()

            recorder = newRecorder
            isRecording = true
            recordingStartTime = System.currentTimeMillis()

            startForeground(NOTIF_ID, buildNotification())
            broadcastState(true)
        } catch (e: Exception) {
            cleanupAfterFailure()
            broadcastState(false)
        }
    }

    private fun stopRecording() {
        if (!isRecording) {
            stopSelfSafely()
            return
        }
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
        } finally {
            recorder = null
            outputPfd?.close()
            outputPfd = null
            isRecording = false

            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_NORMAL
        }
        broadcastState(false)
        stopSelfSafely()
    }

    private fun stopSelfSafely() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanupAfterFailure() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        outputPfd?.close()
        outputPfd = null
        isRecording = false
        stopSelfSafely()
    }

    private fun createRecorder(): MediaRecorder {
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        r.setAudioSource(PrefsHelper.audioSource(this))

        val format = PrefsHelper.format(this)
        if (format == PrefsHelper.FORMAT_3GP) {
            r.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        } else {
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(PrefsHelper.bitrate(this))
            r.setAudioSamplingRate(44100)
        }
        return r
    }

    private fun buildFileName(): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val ext = PrefsHelper.format(this)
        return "call_$stamp.$ext"
    }

    private fun configureAppPrivateOutput(r: MediaRecorder, fileName: String) {
        val dir = RecordingsRepository.appPrivateDir(this)
        val file = File(dir, fileName)
        r.setOutputFile(file.absolutePath)
    }

    private fun configureCustomOutput(r: Media
