package dev.hagios.settings

private const val PLEX_SERVER_URL = "plexServerUrl"
private const val PLEX_TOKEN = "plexToken"
private const val TARGET_PATH = "targetPath"

class SettingsRepository(
    private val settingsStorage: SettingsStorage
) {

    fun setPlexServerUrl(url: String) {
        settingsStorage.storeString(PLEX_SERVER_URL, url)
    }

    fun getPlexServerUrl(): String? = settingsStorage.getString(PLEX_SERVER_URL)

    fun getPlexToken(): String? = settingsStorage.getString(PLEX_TOKEN)


    fun setPlexToken(serverToken: String) {
        settingsStorage.storeString(PLEX_TOKEN, serverToken)
    }

    fun getTargetPath(): String? = settingsStorage.getString(TARGET_PATH)

    fun setTargetPath(targetPath: String) {
        settingsStorage.storeString(TARGET_PATH, targetPath)
    }

}