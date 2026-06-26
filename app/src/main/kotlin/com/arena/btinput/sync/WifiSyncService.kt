package com.arena.btinput.sync

import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * Phone-to-Phone Local WiFi Sync (same WiFi network only)
 *
 * - One phone starts as **Host** (shows its local IP)
 * - Other phones connect by entering the host IP
 *
 * Commands:
 *   "SWITCH:PPSSPP"
 *   "SWITCH:GTA"
 *   "SWITCH:FPS"
 *   "SWITCH:CUSTOM"
 *   "LOAD_PROFILE:MyProfile"
 */
class WifiSyncService {

    companion object {
        private const val TAG = "WifiSyncService"
        const val PORT = 45678
    }

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null

    private var hosting = false
    private var connected = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onCommandReceived: ((String) -> Unit)? = null
    var onStatusChanged: ((connected: Boolean, hosting: Boolean, ip: String?) -> Unit)? = null

    fun startHosting() {
        if (hosting) return

        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                hosting = true
                val localIp = getLocalIpAddress()
                Log.d(TAG, "Hosting on $localIp:$PORT")

                onStatusChanged?.invoke(false, true, localIp)

                while (hosting) {
                    val socket = serverSocket!!.accept()
                    clientSocket = socket
                    connected = true
                    onStatusChanged?.invoke(true, true, localIp)

                    handleClient(socket)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Hosting error", e)
                stop()
            }
        }
    }

    fun connectToHost(hostIp: String) {
        scope.launch {
            try {
                val socket = Socket(hostIp, PORT)
                clientSocket = socket
                connected = true
                onStatusChanged?.invoke(true, false, hostIp)

                sendRaw("CONNECT")
                handleClient(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to $hostIp", e)
                onStatusChanged?.invoke(false, false, null)
            }
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                var line: String?
                while (socket.isConnected && reader.readLine().also { line = it } != null) {
                    line?.let { cmd ->
                        Log.d(TAG, "Received: $cmd")
                        onCommandReceived?.invoke(cmd)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client handler error", e)
            } finally {
                connected = false
                onStatusChanged?.invoke(false, hosting, getLocalIpAddress())
            }
        }
    }

    fun sendCommand(command: String) {
        scope.launch {
            try {
                clientSocket?.let { sock ->
                    if (sock.isConnected) {
                        PrintWriter(sock.getOutputStream(), true).println(command)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Send failed", e)
            }
        }
    }

    private fun sendRaw(msg: String) {
        try {
            clientSocket?.let {
                PrintWriter(it.getOutputStream(), true).println(msg)
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        hosting = false
        connected = false

        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}

        clientSocket = null
        serverSocket = null

        onStatusChanged?.invoke(false, false, null)
    }

    fun isConnected() = connected
    fun isHosting() = hosting

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}