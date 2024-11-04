package dev.hagios.settings

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val settingsRepository = getKoin().get<SettingsRepository>()
    var serverUrl by remember { mutableStateOf(settingsRepository.getPlexServerUrl() ?: "") }
    var serverToken by remember { mutableStateOf(settingsRepository.getPlexToken() ?: "") }
    var targetPath by remember { mutableStateOf(settingsRepository.getTargetPath() ?: "") }
    Window(
        onCloseRequest = onClose,
        title = "Settings",
    ) {
        MaterialTheme {
            Scaffold(
                topBar = {
                    TopAppBar(title = { Text("Settings") })
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                        .scrollable(rememberScrollState(), orientation = Orientation.Vertical)
                ) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Plex Server URL") })
                    OutlinedTextField(
                        value = serverToken,
                        onValueChange = { serverToken = it },
                        label = { Text("Plex Server Token") })
                    OutlinedTextField(
                        value = targetPath,
                        onValueChange = { targetPath = it },
                        label = { Text("Target output path") },
                    )
                    Button(onClick = {
                        settingsRepository.setPlexServerUrl(serverUrl)
                        settingsRepository.setPlexToken(serverToken)
                        settingsRepository.setTargetPath(targetPath)
                        onClose()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}