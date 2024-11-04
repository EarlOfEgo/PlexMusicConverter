package dev.hagios

import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(
    val MediaContainer: T,
)



data class Track(
//    val id: String,
    val title: String,
    val ratingKey: String,
    val format: String,
    val partKey: String
)

@Serializable
data class MediaContainerMusic(
    val Metadata: List<Metadata>,
    val allowSync: Boolean,
    val art: String,
    val content: String,
    val identifier: String,
    val librarySectionID: Int,
    val librarySectionTitle: String,
    val librarySectionUUID: String,
    val mediaTagPrefix: String,
    val mediaTagVersion: Int,
    val nocache: Boolean,
    val size: Int,
    val thumb: String,
    val title1: String,
    val title2: String,
    val viewGroup: String
)

@Serializable
data class Metadata(
    val Country: List<Country>?,
    val Genre: List<Genre>?,
    val addedAt: Int?,
    val art: String?,
    val guid: String?,
    val index: Int?,
    val key: String?,
    val lastViewedAt: Int?,
    val ratingKey: String?,
    val skipCount: Int?,
    val summary: String?,
    val thumb: String?,
    val title: String,
    val titleSort: String?,
    val type: String?,
    val updatedAt: Int?,
    val viewCount: Int?
)

@Serializable
data class TrackMediaContainer(
    val Metadata: List<TrackMetadata>?,
    val allowSync: Boolean,
    val art: String,
    val grandparentRatingKey: Int,
    val grandparentThumb: String,
    val grandparentTitle: String,
    val identifier: String,
    val key: String,
    val librarySectionID: Int,
    val librarySectionTitle: String,
    val librarySectionUUID: String,
    val mediaTagPrefix: String,
    val mediaTagVersion: Int,
    val nocache: Boolean,
    val parentIndex: Int,
    val parentTitle: String,
    val parentYear: Int,
    val size: Int,
    val thumb: String,
    val title1: String,
    val title2: String,
    val viewGroup: String
)

@Serializable
data class TrackMetadata(
    val Media: List<Media>,
    val addedAt: Int,
    val duration: Int,
    val grandparentGuid: String,
    val grandparentKey: String,
    val grandparentRatingKey: String,
    val grandparentThumb: String,
    val grandparentTitle: String,
    val guid: String,
    val index: Int,
    val key: String,
    val parentGuid: String,
    val parentIndex: Int,
    val parentKey: String,
    val parentRatingKey: String,
    val parentStudio: String,
    val parentThumb: String,
    val parentTitle: String,
    val parentYear: Int,
    val ratingCount: Int?,
    val ratingKey: String?,
    val summary: String,
    val thumb: String,
    val title: String,
    val titleSort: String?,
    val type: String,
    val updatedAt: Int?
)

@Serializable
data class Media(
    val Part: List<Part>,
    val audioChannels: Int,
    val audioCodec: String,
    val bitrate: Int,
    val container: String,
    val duration: Int,
    val id: Int
)

@Serializable
data class Part(
    val container: String,
    val duration: Int,
    val `file`: String,
    val hasThumbnail: String?,
    val id: Int,
    val key: String,
    val size: Int
)

@Serializable
data class Country(
    val tag: String
)

@Serializable
data class Genre(
    val tag: String
)

@Serializable
data class MediaContainer(
    val Directory: List<Directory>,
    val allowSync: Boolean,
    val size: Int,
    val title1: String
)

@Serializable
data class Directory(
    val Location: List<Location>,
    val agent: String,
    val allowSync: Boolean,
    val art: String,
    val composite: String,
    val content: Boolean,
    val contentChangedAt: Int,
    val createdAt: Int,
    val directory: Boolean,
    val filters: Boolean,
    val hidden: Int,
    val key: String,
    val language: String,
    val refreshing: Boolean,
    val scannedAt: Int,
    val scanner: String,
    val thumb: String,
    val title: String,
    val type: String,
    val updatedAt: Int?,
    val uuid: String
)

@Serializable
data class Location(
    val id: Int,
    val path: String
)
