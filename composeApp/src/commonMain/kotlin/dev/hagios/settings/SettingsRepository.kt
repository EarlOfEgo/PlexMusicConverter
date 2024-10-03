package dev.hagios.settings

private const val PLEX_SERVER_URL = "plexServerUrl"
private const val PLEX_TOKEN = "plexToken"

class SettingsRepository(
    private val settingsStorage: SettingsStorage
) {

    fun setPlexServerUrl(url: String) {
        settingsStorage.storeString(PLEX_SERVER_URL, url)
    }

    fun getPlexServerUrl(): String? {
        return settingsStorage.getString(PLEX_SERVER_URL)
    }

    fun getPlexToken(): String? {
        return settingsStorage.getString(PLEX_TOKEN)
    }

    fun setPlexToken(serverToken: String) {
        settingsStorage.storeString(PLEX_TOKEN, serverToken)
    }
}