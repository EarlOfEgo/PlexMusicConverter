package dev.hagios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import dev.hagios.plex.Album
import dev.hagios.plex.Artist
import dev.hagios.plex.PlexRepository
import dev.hagios.settings.SettingsRepository
import dev.hagios.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.java.KoinJavaComponent.getKoin
import java.net.URL
import kotlin.io.path.pathString

@Composable
@Preview
fun App() {
    var showSettings by remember { mutableStateOf(false) }
    if (showSettings) {
        SettingsScreen { showSettings = false }
    }

    var convert by remember { mutableStateOf(false) }
    val tracksToConvert by remember { mutableStateOf(mutableMapOf<Album, List<Track>>()) }
    val convertingList = remember { mutableStateListOf<Album>() }
    val repository: PlexRepository = getKoin().get()
    val settingsRepository: SettingsRepository = getKoin().get()
    val artists = repository.getMusicCollection().collectAsState(emptyList()).value

    if (convert) {
        ConvertingTracks(tracksToConvert, settingsRepository, repository) {
            convert = false
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Plex Music Converter") }, actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                })
            },
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                Column(
                    Modifier.fillMaxSize().padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp).weight(6f)) {
                        LazyColumn(modifier = Modifier.weight(1f).background(MaterialTheme.colors.background)) {
                            artists.forEachIndexed { index, artist ->
                                item(key = "header_$index") {
                                    ArtistItem(artists, index, artist, settingsRepository, { convertingList.add(it) })
                                }
                            }
                        }
                        Spacer(modifier = Modifier.fillMaxHeight().width(4.dp))
                        AnimatedVisibility(convertingList.isNotEmpty(), modifier = Modifier.weight(1f)) {
                            Row {
                                Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
                                LazyColumn(modifier = Modifier.background(MaterialTheme.colors.background)) {
                                    items(convertingList) { album ->
                                        val tracks =
                                            repository.getTracksForAlbum(album.id).collectAsState(emptyList()).value
                                        tracksToConvert[album] = tracks
                                        ConvertingItem(convertingList, album, settingsRepository, tracks)
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                }
                AnimatedVisibility(convertingList.isNotEmpty(), modifier = Modifier.align(Alignment.BottomEnd)) {
                    ExtendedFloatingActionButton(
                        onClick = { convert = !convert },
                        modifier = Modifier.padding(16.dp),
                        text = { Text("Convert") }
                    )
                }
            }
        }
    }
}

enum class ConversionStatus {
    DOWNLOADING,
    CONVERTING,
    DONE
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun ConvertingTracks(
    tracksToConvert: MutableMap<Album, List<Track>>,
    settingsRepository: SettingsRepository,
    repository: PlexRepository,
    onClose: () -> Unit
) {
    Window(onCloseRequest = onClose, title = "Converting") {
        MaterialTheme {
            Scaffold {
                Column(Modifier.padding(16.dp)) {
                    Text("Converting")
                    tracksToConvert.forEach { (album, tracks) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            AlbumImage(album, settingsRepository)
                            Text(modifier = Modifier.padding(start = 8.dp), text = album.title)
                        }

                        tracks.forEachIndexed { index, track ->
                            Row(verticalAlignment = CenterVertically, modifier = Modifier.height(IntrinsicSize.Min)) {
                                Text(
                                    text = "${index + 1} - ${track.title}",
                                    modifier = Modifier.padding(start = 4.dp).weight(1f)
                                )
                                var progress by remember { mutableStateOf(0F) }
                                var conversionStatus by remember { mutableStateOf(ConversionStatus.DOWNLOADING) }
                                LaunchedEffect(Unit) {
                                    withContext(Dispatchers.IO.limitedParallelism(1)) {
                                        val downloadedFile = repository.downloadTrack(track) {
                                            progress = it.toFloat() / 100
                                        }
                                        conversionStatus = ConversionStatus.CONVERTING
                                        try {
                                            val file = repository.convertToFlac(downloadedFile, track, album)
                                            repository.tag(
                                                filePath = file.pathString,
                                                artist = album.artist,
                                                album = album.title,
                                                title = track.title,
                                                "$index"
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        conversionStatus = ConversionStatus.DONE
                                    }
                                }
                                Text(
                                    text = when (conversionStatus) {
                                        ConversionStatus.DOWNLOADING -> "Downloading"
                                        ConversionStatus.CONVERTING -> "Converting"
                                        ConversionStatus.DONE -> "Done"
                                    }
                                )
                                Spacer(Modifier.width(2.dp))
                                when (conversionStatus) {
                                    ConversionStatus.DOWNLOADING -> LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(vertical = 1.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )

                                    ConversionStatus.CONVERTING -> LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(vertical = 1.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )

                                    ConversionStatus.DONE -> LinearProgressIndicator(
                                        progress = 1f,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(vertical = 1.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}


@Composable
private fun ConvertingItem(
    convertingList: SnapshotStateList<Album>,
    album: Album,
    settingsRepository: SettingsRepository,
    tracks: List<Track>
) {
    Row(modifier = Modifier.fillMaxWidth().padding(4.dp).clickable { convertingList.remove(album) }) {
        AlbumImage(album, settingsRepository)
        Text(modifier = Modifier.padding(start = 8.dp), text = album.title)
    }

    tracks.forEachIndexed { index, track ->
        Row {
            Text(text = "${index + 1} - ${track.title}", modifier = Modifier.padding(start = 4.dp).weight(1f))
            Text(text = track.format)
        }
    }
}

@Composable
private fun ArtistItem(
    artists: List<Artist>,
    index: Int,
    artist: Artist,
    settingsRepository: SettingsRepository,
    addToConvertingList: (Album) -> Unit
) {
    val collapsedState = remember(artists) { artists.map { true }.toMutableStateList() }
    val collapsed = collapsedState[index]
    Column {
        Row(
            modifier = Modifier.clickable { collapsedState[index] = !collapsed }.padding(16.dp),
            verticalAlignment = CenterVertically
        ) {
            artist.thumb?.let { thumb ->
                AsyncImage(
                    load = { loadImageBitmap(createImageUrl(thumb, settingsRepository)) },
                    painterFor = { remember { BitmapPainter(it) } },
                    contentDescription = "$artist logo",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.size(64.dp).clip(CircleShape)
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
                    modifier = Modifier.fillMaxWidth().clickable { addToConvertingList(album) }
                        .padding(start = 32.dp, top = 2.dp, bottom = 2.dp)
                ) {
                    AlbumImage(album, settingsRepository)
                    Text(modifier = Modifier.padding(start = 8.dp), text = album.title)
                }
                Divider()
            }
        }
    }
    Divider()
}

@Composable
private fun AlbumImage(album: Album, settingsRepository: SettingsRepository) {
    album.thumb?.let { thumb ->
        AsyncImage(
            load = { loadImageBitmap(createImageUrl(thumb, settingsRepository)) },
            painterFor = { remember { BitmapPainter(it) } },
            contentDescription = "$album logo",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
        )
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
