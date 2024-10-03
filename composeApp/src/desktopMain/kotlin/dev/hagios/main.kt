package dev.hagios

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.manevolent.ffmpeg4j.AudioFormat
import com.github.manevolent.ffmpeg4j.FFmpegIO
import com.github.manevolent.ffmpeg4j.source.AudioSourceSubstream
import com.github.manevolent.ffmpeg4j.transcoder.Transcoder
import dev.hagios.di.dataModule
import org.koin.core.context.GlobalContext.startKoin
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files

fun main() = application {
    startKoin {
        modules(dataModule())
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "musicsync",
    ) {
        App()
    }
}