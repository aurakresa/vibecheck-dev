package com.example.vibecheck_dev.domain.repository

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface P2pRepository {
    val peersList: StateFlow<List<WifiP2pDevice>>
    val connectionInfo: StateFlow<WifiP2pInfo?>
    val isWifiP2pEnabled: StateFlow<Boolean>

    // --- TAMBAHKAN DUA BARIS INI ---
    val incomingMessages: SharedFlow<String> // Telinga untuk UI
    fun sendMessage(message: String) // Mulut untuk UI
    // -------------------------------

    fun startDiscovery()
    fun stopDiscovery()
    fun connectToDevice(deviceAddress: String)
    fun disconnect()
}