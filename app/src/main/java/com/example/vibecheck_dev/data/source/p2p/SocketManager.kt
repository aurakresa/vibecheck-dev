package com.example.vibecheck_dev.data.source.p2p

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class SocketManager {
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private var listenJob: Job? = null
    private val _incomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 50)
    val incomingMessages = _incomingMessages.asSharedFlow()

    private val PORT = 8888

    fun startServer() {
        closeConnections() // Bersihkan sisa koneksi lama
        listenJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d("SOCKET_LOG", "Server siap di Port $PORT...")
                socket = serverSocket?.accept()
                Log.d("SOCKET_LOG", "Remote masuk!")
                setupStreams(socket!!)
            } catch (e: Exception) {
                Log.e("SOCKET_LOG", "Server Error: ${e.message}")
            }
        }
    }

    fun startClient(serverIp: String) {
        closeConnections() // Bersihkan sisa koneksi lama
        listenJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = Socket()
                Log.d("SOCKET_LOG", "Mengetuk IP: $serverIp")
                socket?.connect(InetSocketAddress(serverIp, PORT), 5000)
                Log.d("SOCKET_LOG", "Berhasil terhubung!")
                setupStreams(socket!!)
            } catch (e: Exception) {
                Log.e("SOCKET_LOG", "Client Error: ${e.message}")
            }
        }
    }

    private suspend fun setupStreams(activeSocket: Socket) {
        writer = PrintWriter(activeSocket.outputStream, true)
        reader = BufferedReader(InputStreamReader(activeSocket.inputStream))

        // Terus membaca selama socket belum ditutup secara fisik
        while (currentCoroutineContext().isActive && activeSocket.isConnected && !activeSocket.isClosed) {
            try {
                val message = reader?.readLine()
                if (message != null) {
                    Log.d("SOCKET_LOG", "Terima: $message")
                    _incomingMessages.tryEmit(message)
                } else {
                    Log.d("SOCKET_LOG", "Koneksi diputus oleh perangkat lain (Stream Null).")
                    break
                }
            } catch (e: Exception) {
                Log.e("SOCKET_LOG", "Stream error / terputus: ${e.message}")
                break
            }
        }
    }

    fun sendMessage(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (writer != null) {
                    writer?.println(message)
                    Log.d("SOCKET_LOG", "Kirim: $message")
                } else {
                    Log.e("SOCKET_LOG", "Gagal kirim, Writer (Mulut Socket) null/terputus!")
                }
            } catch (e: Exception) {
                Log.e("SOCKET_LOG", "Error kirim: ${e.message}")
            }
        }
    }

    fun closeConnections() {
        listenJob?.cancel()

        // TRIK ANTI-BUG: Simpan koneksi lama ke variabel sementara
        // Ini mencegah Coroutine Penutup salah mematikan socket yang baru dibuat!
        val oldSocket = socket
        val oldServer = serverSocket
        val oldReader = reader
        val oldWriter = writer

        // Kosongkan variabel utama agar siap dipakai koneksi baru detik itu juga
        socket = null
        serverSocket = null
        reader = null
        writer = null

        // Tutup koneksi lama di background tanpa mengganggu yang baru
        CoroutineScope(Dispatchers.IO).launch {
            try {
                oldReader?.close()
                oldWriter?.close()
                oldSocket?.close()
                oldServer?.close()
                Log.d("SOCKET_LOG", "Pembersihan jalur lama selesai dengan aman.")
            } catch (e: Exception) {
                Log.e("SOCKET_LOG", "Error saat membuang socket lama: ${e.message}")
            }
        }
    }

}