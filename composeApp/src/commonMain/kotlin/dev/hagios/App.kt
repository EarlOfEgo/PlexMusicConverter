package dev.hagios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import dev.hagios.plex.Album
import dev.hagios.plex.PlexRepository
import dev.hagios.settings.SettingsRepository
import dev.hagios.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.java.KoinJavaComponent.getKoin
import java.net.URL

@Composable
@Preview
fun App() {
    var showSettings by remember { mutableStateOf(false) }
    if (showSettings) {
        SettingsScreen { showSettings = false }
    }

    MaterialTheme {
        var convert by remember { mutableStateOf(false) }
        val tracksToConvert by remember { mutableStateOf(mutableMapOf<Album, List<Track>>()) }
        val repository: PlexRepository = getKoin().get()
        val settingsRepository: SettingsRepository = getKoin().get()
        val artists = repository.getMusicCollection().collectAsState(emptyList()).value
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { showSettings = true }) {
                Text("Settings")
            }
            val convertingList = remember { mutableStateListOf<Album>() }
            val collapsedState = remember(artists) { artists.map { true }.toMutableStateList() }
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).weight(6f)) {
                LazyColumn(modifier = Modifier.weight(1f).background(MaterialTheme.colors.background)) {
                    artists.forEachIndexed { index, artist ->
                        val collapsed = collapsedState[index]
                        item(key = "header_$index") {
                            Column {
                                Row(
                                    modifier = Modifier.clickable { collapsedState[index] = !collapsed },
                                    verticalAlignment = CenterVertically
                                ) {
                                    artist.thumb?.let { thumb ->
                                        AsyncImage(
                                            load = { loadImageBitmap(createImageUrl(thumb, settingsRepository)) },
                                            painterFor = { remember { BitmapPainter(it) } },
                                            contentDescription = "$artist logo",
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                    Spacer(Modifier.size(4.dp))
                                    Column {
                                        Text(
                                            text = artist.name,
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.h6
                                        )
                                        Text(
                                            text = "${artist.albums.size} Albums",
                                            style = MaterialTheme.typography.subtitle1
                                        )
                                    }
                                }
                                if (!collapsed) {
                                    artist.albums.forEach { album ->
                                        Row(
                                            modifier = Modifier.padding(start = 4.dp)
                                                .clickable { convertingList.add(album) }) {
                                            album.thumb?.let { thumb ->
                                                AsyncImage(
                                                    load = { loadImageBitmap(createImageUrl(thumb, settingsRepository)) },
                                                    painterFor = { remember { BitmapPainter(it) } },
                                                    contentDescription = "$album logo",
                                                    contentScale = ContentScale.FillWidth,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                            Text(modifier = Modifier.padding(start = 8.dp), text = album.title)
                                        }
                                    }
                                }
                            }
                            Divider()
                        }
                    }
                }
                Spacer(modifier = Modifier.fillMaxHeight().width(4.dp))
                LazyColumn(modifier = Modifier.weight(1f).background(MaterialTheme.colors.background)) {
                    items(convertingList) { album ->
                        Row(modifier = Modifier.padding(start = 4.dp).clickable { convertingList.remove(album) }) {
                            album.thumb?.let { thumb ->
                                AsyncImage(
                                    load = { loadImageBitmap(createImageUrl(thumb, settingsRepository)) },
                                    painterFor = { remember { BitmapPainter(it) } },
                                    contentDescription = "$album logo",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(modifier = Modifier.padding(start = 8.dp), text = album.title)
                        }
                        val tracks = repository.getTracksForAlbum(album.id).collectAsState(emptyList()).value
                        tracksToConvert[album] = tracks
                        tracks.forEach { track ->
                            Row {
                                Text(text = track.title, modifier = Modifier.padding(start = 4.dp).weight(2f))
                                Text(text = track.format, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(convertingList.isNotEmpty()) {
                LaunchedEffect(convert) {
                    withContext(Dispatchers.IO) {
                        tracksToConvert.forEach { (album, tracksToConvert) ->
                            val artist = artists.first { it.albums.contains(album) }
                            repository.downloadSongs(artist.name, album, tracksToConvert)
                        }
                    }
                }
                Row(modifier = Modifier.weight(1f)) {
                    Button(onClick = { convert = !convert }) {
                        Text("Convert")
                    }
                }
            }
        }
    }
}

private fun createImageUrl(thumb: String, settingsRepository: SettingsRepository): String {
    return "http://${settingsRepository.getPlexServerUrl()}${thumb}?X-Plex-Token=${settingsRepository.getPlexToken()}"
}


@Composable
fun <T> AsyncImage(
    load: suspend () -> T,
    painterFor: @Composable (T) -> Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val image: T? by produceState<T?>(null) {
        value = withContext(Dispatchers.IO) {
            try {
                load()
            } catch (e: Exception) {
                // instead of printing to console, you can also write this to log,
                // or show some error placeholder
                e.printStackTrace()
                null
            }
        }
    }

    if (image != null) {
        Image(
            painter = painterFor(image!!),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}


fun loadImageBitmap(url: String): ImageBitmap =
    URL(url).openStream().buffered().use(::loadImageBitmap)
