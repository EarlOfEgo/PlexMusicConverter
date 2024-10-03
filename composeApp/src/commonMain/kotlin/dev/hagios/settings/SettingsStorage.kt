package dev.hagios.settings

import java.util.prefs.Preferences

class SettingsStorage {
        private val preferences: Preferences = Preferences.userRoot().node(this::class.java.name)

        fun storeString(key: String, value: String) = preferences.put(key, value)
        fun getString(key: String, defaultValue: String? = null): String? = preferences.get(key, defaultValue)
        fun deleteString(key: String) = preferences.remove(key)
}