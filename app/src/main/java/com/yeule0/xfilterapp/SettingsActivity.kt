package com.yeule0.xfilterapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var editFlags: EditText
    private lateinit var editWords: EditText
    private lateinit var switchFilterAds: SwitchMaterial
    private lateinit var switchIrcMode: SwitchMaterial
    private lateinit var buttonSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Find Views
        editFlags = findViewById(R.id.editFlags)
        editWords = findViewById(R.id.editWords)
        switchFilterAds = findViewById(R.id.switchFilterAds)
        switchIrcMode = findViewById(R.id.switchIrcMode)
        buttonSave = findViewById(R.id.buttonSave)

        // Load existing settings
        loadSettings()

        // Save Button Listener
        buttonSave.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun loadSettings() {
        editFlags.setText(SettingsManager.getFlagsToHide(this).joinToString(", "))
        editWords.setText(SettingsManager.getWordsToHide(this).joinToString(", "))
        switchFilterAds.isChecked = SettingsManager.getFilterAds(this)
        switchIrcMode.isChecked = SettingsManager.getIrcMode(this)
    }

    private fun saveSettings() {
        val flags = editFlags.text.toString().split(',')
            .map { it.trim() }.filter { it.isNotEmpty() }
        val words = editWords.text.toString().split(',')
            .map { it.trim() }.filter { it.isNotEmpty() }
        val filterAds = switchFilterAds.isChecked
        val ircMode = switchIrcMode.isChecked

        // Save using updated SettingsManager method
        SettingsManager.saveSettings(this, flags, words, filterAds, ircMode)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}