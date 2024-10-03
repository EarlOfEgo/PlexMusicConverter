package dev.hagios.settings

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val settingsRepository = getKoin().get<SettingsRepository>()
    var serverUrl by remember { mutableStateOf(settingsRepository.getPlexServerUrl() ?: "") }
    var serverToken by remember { mutableStateOf(settingsRepository.getPlexToken() ?: "") }
    Window(
        onCloseRequest = onClose,
        title = "Settings",
    ) {
        MaterialTheme {
            Column(
                modifier = Modifier.fillMaxWidth().scrollable(rememberScrollState(), orientation = Orientation.Vertical)
            ) {
                OutlinedTextField(value = serverUrl, onValueChange = { serverUrl = it }, label = { Text("Plex Server URL") })
                OutlinedTextField(value = serverToken, onValueChange = { serverToken = it }, label = { Text("Plex Server Token") })
                Button(onClick = {
                    settingsRepository.setPlexServerUrl(serverUrl)
                    settingsRepository.setPlexToken(serverToken)
                    onClose()
                }) {
                    Text("Save")
                }
            }
        }
    }
}