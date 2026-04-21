package com.example.vibecheck_dev.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Duration

class VibeServer {
    // State flow untuk mengupdate IP ke UI secara real-time
    val serverIpState = MutableStateFlow("")
    private var server: NettyApplicationEngine? = null

    fun startServer(ipAddress: String) {
        if (server != null) return // Jangan nyalakan dobel

        serverIpState.value = ipAddress

        server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                // Endpoint WebSocket
                webSocket("/ws") {
                    println("LOG: Remote Terhubung!")
                    send("Halo dari Host Camera!")

                    // Di sini nanti lu taruh logika WebRTC (Kirim Offer SDP)
                    // ...

                    // Menerima perintah dari HP Remote (Mendengarkan Shutter/Zoom)
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                println("LOG: Terima dari Remote -> $text")

                                if (text.contains("SHUTTER")) {
                                    // Panggil fungsi CameraX takePicture()
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }.start(wait = false)
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
        serverIpState.value = ""
    }
}