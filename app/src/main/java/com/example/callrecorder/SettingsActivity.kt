package com.example.callrecorder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.settingsToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction()
            .replace(R.id.settingsContainer, SettingsFragment())
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private val folderPicker = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->
            if (uri != null) {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                PrefsHelper.setCustomFolderUri(requireContext(), uri)
                updateFolderSummary()
                (findPreference<ListPreference>(PrefsHelper.KEY_STORAGE))?.value =
                    PrefsHelper.STORAGE_CUSTOM
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>("pref_choose_folder")?.setOnPreferenceClickListener {
                folderPicker.launch(null)
                true
            }
            updateFolderSummary()
        }

        private fun updateFolderSummary() {
            val pref = findPreference<Preference>("pref_choose_folder") ?: return
            val uri = PrefsHelper.customFolderUri(requireContext())
            pref.summary = if (uri != null) {
                DocumentFile.fromTreeUri(requireContext(), uri)?.name ?: uri.toString()
            } else {
                "No folder selected yet"
            }
        }
    }
}
