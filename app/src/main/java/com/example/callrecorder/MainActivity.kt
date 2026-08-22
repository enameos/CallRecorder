package com.example.callrecorder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnRecord: MaterialButton
    private lateinit var tvStatus: android.widget.TextView
    private lateinit var tvEmpty: android.widget.TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: RecordingsAdapter

    private var isRecording = false

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val recording = intent?.getBooleanExtra(RecordingService.EXTRA_IS_RECORDING, false) ?: false
            updateRecordingUi(recording)
            if (!recording) refreshList()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micGranted = results[Manifest.permission.RECORD_AUDIO] == true
        if (micGranted) {
            showSpeakerReminderThenStart()
        } else {
            Toast.makeText(this, "Microphone permission is required to record", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        btnRecord = findViewById(R.id.btnRecord)
        tvStatus = findViewById(R.id.tvStatus)
        tvEmpty = findViewById(R.id.tvEmpty)
        recycler = findViewById(R.id.recyclerRecordings)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = RecordingsAdapter(
            context = this,
            items = mutableListOf(),
            onDelete = { rec ->
                RecordingsRepository.delete(this, rec)
                refreshList()
            },
            onShare = { rec -> shareRecording(rec) }
        )
        recycler.adapter = adapter

        btnRecord.setOnClickListener {
            if (isRecording) stopRecording() else requestPermissionsThenStart()
        }

        isRecording = RecordingService.isCurrentlyRecording(this)
        updateRecordingUi(isRecording)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(stateReceiver, IntentFilter(RecordingService.ACTION_STATE_CHANGED))
        refreshList()
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stateReceiver)
        adapter.releasePlayer()
    }

    private fun requestPermissionsThenStart() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val notGranted = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) {
            showSpeakerReminderThenStart()
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun showSpeakerReminderThenStart() {
        AlertDialog.Builder(this)
            .setTitle(R.string.speaker_reminder_title)
            .setMessage(getString(R.string.speaker_reminder_body) + "\n\n" + getString(R.string.consent_reminder))
            .setPositiveButton(R.string.start_recording) { _, _ -> startRecording() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun startRecording() {
        val intent = Intent(this, RecordingService::class.java).setAction(RecordingService.ACTION_START)
        ContextCompat.startForegroundService(this, intent)
    }

    private f
