package dev.hagios.plex

import com.github.manevolent.ffmpeg4j.AudioFormat
import com.github.manevolent.ffmpeg4j.FFmpegIO
import com.github.manevolent.ffmpeg4j.source.AudioSourceSubstream
import com.github.manevolent.ffmpeg4j.transcoder.Transcoder
import dev.hagios.Track
import dev.hagios.settings.SettingsRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class PlexRepository(
    private val plexService: PlexApi,
    private val settingsRepository: SettingsRepository
) {

    fun getMusicCollection() = flow {
        val artists = runBlocking {
            plexService.getAllMusicDirectories().map {
                async(start = CoroutineStart.LAZY) {
                    val info = plexService.getMusicCollection(it.id)
                    info.Metadata.map { metadata ->
                        metadata.ratingKey?.let { it1 ->
                            Artist(
                                it1,
                                name = metadata.title,
                                libraryId = info.librarySectionID,
                                albums = emptyList(),
                                thumb = metadata.thumb
                            )
                        }
                    }
                }
            }.map { it.await() }.toList().flatten().filterNotNull().map { artist ->
                async(start = CoroutineStart.LAZY) {
                    val albums = plexService.getAlbumsForArtist(libraryId = artist.libraryId, artist = artist)
                    artist.copy(albums = albums)
                }
            }.map { it.await() }.toList()
        }
        emit(artists)
    }.flowOn(Dispatchers.IO)

    fun getTracksForAlbum(albumId: String) = flow {
        emit(plexService.getAllTracksForAlbum(albumId))
    }

    suspend fun downloadTrack(track: Track, progress: (Int) -> Unit): File {
        return plexService.downloadSong(track.partKey, {
            progress(it)
        })
    }

    fun convertToFlac(file: File, track: Track, album: Album): Path {
        val targetFolder = settingsRepository.getTargetPath() ?: "."
        val folder = "$targetFolder/${album.title}/"
        File(folder).mkdirs()
        return convert(file, track, folder)
    }

    private fun convert(songFile: File, track: Track, folder: String): Path {
        val options: MutableMap<String, String> = HashMap()
        options["strict"] = "experimental"
        val tempFile: Path = Files.createFile(Path.of("$folder${track.title}.mp3"))
        val targetStream =
            FFmpegIO.openChannel(Files.newByteChannel(tempFile, StandardOpenOption.WRITE)).asOutput().open("mp3")
        FFmpegIO.openInputStream(songFile.inputStream()).open("flac")
            .use { sourceStream ->
                sourceStream.registerStreams()
                val mediaSourceSubstream = sourceStream.substreams[0] as AudioSourceSubstream
                val audioFormat: AudioFormat = mediaSourceSubstream.format

                targetStream.registerAudioSubstream("libmp3lame", audioFormat, options)
                Transcoder.convert(sourceStream, targetStream, Double.MAX_VALUE)
            }
        return tempFile
    }

    fun tag(filePath: String, artist: String, album: String, title: String, number: String) {
        AudioFileIO.read(File(filePath)).run {
            tag.setField(FieldKey.ALBUM, album)
            tag.setField(FieldKey.TITLE, title)
            tag.setField(FieldKey.ARTIST, artist)
            tag.setField(FieldKey.ALBUM, album)
            tag.setField(FieldKey.TRACK, number)
            commit()
        }
    }
}

data class Artist(
    val id: String,
    val name: String,
    val libraryId: Int,
    val thumb: String?,
    val albums: List<Album>
)

data class Album(
    val id: String,
    val title: String,
    val thumb: String?,
    val artist: String
)