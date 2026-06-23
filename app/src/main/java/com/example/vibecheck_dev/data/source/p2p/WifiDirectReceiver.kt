package com.example.vibecheck_dev.data.source.p2p

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager

class WifiDirectReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val onWifiStateChanged: (Boolean) -> Unit,
    private val onPeersChanged: () -> Unit,
    // UBAH INI: Tambahkan Boolean untuk tahu apakah nyambung atau putus
    private val onConnectionChanged: (Boolean) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // Mengecek apakah Wi-Fi user nyala atau mati
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                onWifiStateChanged(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
            }
            // Terpanggil ketika HP menemukan perangkat lain di udara
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                onPeersChanged()
            }
            // Terpanggil ketika proses jabat tangan (pairing) berhasil/gagal
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                // Lempar status True (nyambung) atau False (putus)
                onConnectionChanged(networkInfo?.isConnected == true)
            }
        }
    }
}