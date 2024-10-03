package dev.hagios.di

import dev.hagios.plex.PlexApi
import dev.hagios.plex.PlexRepository
import dev.hagios.settings.SettingsRepository
import dev.hagios.settings.SettingsStorage
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun dataModule() = module {

    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
    single {
        val settingsRepository = get<SettingsRepository>()
        HttpClient {
            install(ContentNegotiation) {
                json(get(), contentType = ContentType.Application.Json)
            }
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.ALL
            }
            defaultRequest {
                extracted(settingsRepository)
            }
        }
    }

    single {

    }

    singleOf(::PlexApi)
    singleOf(::SettingsStorage)
    factoryOf(::PlexRepository)
    factoryOf(::SettingsRepository)
}

fun DefaultRequest.DefaultRequestBuilder.extracted(settingsRepository: SettingsRepository) {
    url {
        protocol = URLProtocol.HTTP
        host = settingsRepository.getPlexServerUrl() ?: "localhost"
        settingsRepository.getPlexToken()?.let { token -> parameters.append("X-Plex-Token", token) }
    }
}