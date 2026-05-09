package com.example.vibecheck_dev.data.repository_impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import com.example.vibecheck_dev.data.source.p2p.SocketManager
import com.example.vibecheck_dev.data.source.p2p.WifiDirectReceiver
import com.example.vibecheck_dev.domain.repository.P2pRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class P2pRepositoryImpl(
    private val context: Context
) : P2pRepository {

    private val manager: WifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: WifiP2pManager.Channel = manager.initialize(context, context.mainLooper, null)
    private var receiver: WifiDirectReceiver? = null

    private val socketManager = SocketManager()

    override val incomingMessages: SharedFlow<String> = socketManager.incomingMessages

    private val _peersList = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    override val peersList: StateFlow<List<WifiP2pDevice>> = _peersList.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    override val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    private val _isWifiP2pEnabled = MutableStateFlow(false)
    override val isWifiP2pEnabled: StateFlow<Boolean> = _isWifiP2pEnabled.asStateFlow()

    init {
        registerReceiver()
    }

    private fun registerReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        receiver = WifiDirectReceiver(
            manager = manager,
            channel = channel,
            onWifiStateChanged = { isEnabled ->
                _isWifiP2pEnabled.value = isEnabled
            },
            onPeersChanged = {
                manager.requestPeers(channel) { peerList ->
                    _peersList.value = peerList.deviceList.toList()
                }
            },
            onConnectionChanged = { isConnected ->
                if (isConnected) {
                    manager.requestConnectionInfo(channel) { info ->
                        _connectionInfo.value = info
                        if (info.groupFormed && info.isGroupOwner) {
                            socketManager.startServer()
                        } else if (info.groupFormed) {
                            socketManager.startClient(info.groupOwnerAddress.hostAddress)
                        }
                    }
                } else {
                    // JIKA SISTEM WIFI MENDETEKSI PUTUS
                    forceDisconnectUI()
                }
            }
        )
        context.registerReceiver(receiver, intentFilter)
    }

    // Fungsi tambahan untuk membersihkan State UI saat ada yang diskonek
    private fun forceDisconnectUI() {
        Log.d("P2P_LOG", "Memaksa Reset Status UI karena Diskonek")
        _connectionInfo.value = null
        _peersList.value = emptyList()
        socketManager.closeConnections()
    }

    override fun sendMessage(message: String) {
        socketManager.sendMessage(message)
    }

    override fun startDiscovery() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d("P2P_LOG", "Pencarian dimulai") }
            override fun onFailure(reasonCode: Int) { Log.d("P2P_LOG", "Gagal mencari: $reasonCode") }
        })
    }

    override fun stopDiscovery() {
        manager.stopPeerDiscovery(channel, null)
    }

    override fun connectToDevice(deviceAddress: String) {
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
            this.groupOwnerIntent = 15
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d("P2P_LOG", "Berhasil mengirim permintaan koneksi") }
            override fun onFailure(reason: Int) { Log.d("P2P_LOG", "Gagal menyambung: $reason") }
        })
    }

    override fun disconnect() {
        // Hancurkan grup Wi-Fi Direct agar HP Kamera juga langsung sadar!
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("P2P_LOG", "Grup Wi-Fi dihancurkan dengan sukses (Abort Ditekan)")
                forceDisconnectUI()
            }
            override fun onFailure(reason: Int) {
                Log.d("P2P_LOG", "Gagal menghancurkan grup, tapi tetap reset UI")
                forceDisconnectUI()
            }
        })
    }
}