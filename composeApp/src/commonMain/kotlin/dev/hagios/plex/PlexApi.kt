package dev.hagios.plex

import dev.hagios.*
import dev.hagios.settings.SettingsRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PlexApi(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository
) {
    suspend fun getAllMusicDirectories(): List<Location> {

        try {
            val response: Response<MediaContainer> = httpClient.get("/library/sections/").body()
            return response.MediaContainer.Directory.filter { it.agent == "tv.plex.agents.music" }
                .flatMap { it.Location }
        } catch (e: Exception) {
            println(e)
            return emptyList()
        }
    }

    suspend fun getAlbumsForArtist(libraryId: Int, artist: Artist): List<Album> {
        val albums = httpClient.get("/library/sections/$libraryId/all?artist.id=${artist.id}&type=9")
            .body<Response<MediaContainerMusic>>()
        return albums.MediaContainer.Metadata.mapNotNull {
            it.ratingKey?.let { it1 ->
                Album(
                    title = it.title,
                    id = it1,
                    thumb = it.thumb,
                    artist = artist.name
                )
            }
        }
    }

    suspend fun getAllTracksForAlbum(albumId: String): List<Track> {
        val tracks = httpClient.get("/library/metadata/$albumId/children?").body<Response<TrackMediaContainer>>()
//        println(tracks)
        return tracks.MediaContainer.Metadata?.flatMap { track ->
            track.Media.mapNotNull {
                track.ratingKey?.let { it1 ->
                    Track(
                        title = track.title, ratingKey = it1, format = it.audioCodec, partKey = it.Part.first().key
                    )
                }
            } ?: emptyList()
        } ?: emptyList()
    }

    suspend fun getMusicCollection(id: Int): MediaContainerMusic {
        try {
            val response: Response<MediaContainerMusic> = httpClient.get("/library/sections/$id/all").body()
            return response.MediaContainer
        } catch (e: Exception) {
            println(e)
            throw e
        }
    }

    suspend fun downloadSong(pathKey: String, remaining: (Int) -> Unit): File {
        val file = withContext(Dispatchers.IO) {
            File.createTempFile("files", "index")
        }
        return httpClient.prepareGet({
            url {
                protocol = URLProtocol.HTTP
                host = settingsRepository.getPlexServerUrl() ?: "localhost"
                settingsRepository.getPlexToken()?.let { token -> parameters.append("X-Plex-Token", token) }
                path(pathKey)
                parameters.append("download", "1")
            }
        }).execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.body()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    file.appendBytes(bytes)
                    remaining((file.length().toDouble() / (httpResponse.contentLength() ?: 1).toDouble() * 100).toInt())
                }
            }
            file
        }
    }
}