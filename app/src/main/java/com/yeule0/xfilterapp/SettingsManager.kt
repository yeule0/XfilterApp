package com.yeule0.xfilterapp

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {

    private const val PREFS_NAME = "XFilterPrefs"
    private const val KEY_FLAGS = "flagsToHide"
    private const val KEY_WORDS = "wordsToHide"
    private const val KEY_FILTER_ADS = "filterAds"
    private const val KEY_IRC_MODE = "ircMode"

    // --- Defaults ---
    private const val DEFAULT_FILTER_ADS = true
    private const val DEFAULT_IRC_MODE = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Getters ---
    fun getFlagsToHide(context: Context): List<String> {
        val savedString = getPrefs(context).getString(KEY_FLAGS, "") ?: ""
        return savedString.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
    fun getWordsToHide(context: Context): List<String> {
        val savedString = getPrefs(context).getString(KEY_WORDS, "") ?: ""
        return savedString.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
    fun getFilterAds(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FILTER_ADS, DEFAULT_FILTER_ADS)
    }
    fun getIrcMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IRC_MODE, DEFAULT_IRC_MODE)
    }

    // Setters
    fun saveSettings(
        context: Context,
        flags: List<String>,
        words: List<String>,
        filterAds: Boolean,
        ircMode: Boolean
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_FLAGS, flags.joinToString(","))
            putString(KEY_WORDS, words.joinToString(","))
            putBoolean(KEY_FILTER_ADS, filterAds)
            putBoolean(KEY_IRC_MODE, ircMode)
            apply()
        }
    }

    // Data structure
    data class FilterSettings(
        val flagsToHide: List<String>,
        val wordsToHide: List<String>,
        val filterAds: Boolean,
        val ircMode: Boolean
    )

    fun getAllSettings(context: Context): FilterSettings {
        return FilterSettings(
            flagsToHide = getFlagsToHide(context),
            wordsToHide = getWordsToHide(context),
            filterAds = getFilterAds(context),
            ircMode = getIrcMode(context)
        )
    }
}