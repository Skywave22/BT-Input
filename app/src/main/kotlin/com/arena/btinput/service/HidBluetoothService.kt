package com.arena.btinput.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HidBluetoothService : Service() {

    companion object {
        private const val TAG = "HidBluetoothService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "hid_service_channel"

        const val REPORT_ID_KEYBOARD = 1.toByte()
        const val REPORT_ID_MOUSE = 2.toByte()
        const val REPORT_ID_GAMEPAD = 3.toByte()

        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2

        private const val HID_SUBCLASS_COMBO: Byte = 0xC0.toByte()

        private val HID_REPORT_DESCRIPTOR = byteArrayOf(
            // MOUSE
            0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x02.toByte(), 0xA1.toByte(), 0x01.toByte(),
            0x85.toByte(), REPORT_ID_MOUSE,
            0x09.toByte(), 0x01.toByte(), 0xA1.toByte(), 0x00.toByte(),
            0x05.toByte(), 0x09.toByte(), 0x19.toByte(), 0x01.toByte(), 0x29.toByte(), 0x03.toByte(),
            0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(), 0x95.toByte(), 0x03.toByte(),
            0x75.toByte(), 0x01.toByte(), 0x81.toByte(), 0x02.toByte(),
            0x95.toByte(), 0x01.toByte(), 0x75.toByte(), 0x05.toByte(), 0x81.toByte(), 0x03.toByte(),
            0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x30.toByte(), 0x09.toByte(), 0x31.toByte(),
            0x15.toByte(), 0x81.toByte(), 0x25.toByte(), 0x7F.toByte(), 0x75.toByte(), 0x08.toByte(),
            0x95.toByte(), 0x02.toByte(), 0x81.toByte(), 0x06.toByte(),
            0x09.toByte(), 0x38.toByte(), 0x15.toByte(), 0x81.toByte(), 0x25.toByte(), 0x7F.toByte(),
            0x75.toByte(), 0x08.toByte(), 0x95.toByte(), 0x01.toByte(), 0x81.toByte(), 0x06.toByte(),
            0xC0.toByte(), 0xC0.toByte(),

            // KEYBOARD
            0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x06.toByte(), 0xA1.toByte(), 0x01.toByte(),
            0x85.toByte(), REPORT_ID_KEYBOARD,
            0x05.toByte(), 0x07.toByte(), 0x19.toByte(), 0xE0.toByte(), 0x29.toByte(), 0xE7.toByte(),
            0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(), 0x75.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x08.toByte(), 0x81.toByte(), 0x02.toByte(),
            0x95.toByte(), 0x01.toByte(), 0x75.toByte(), 0x08.toByte(), 0x81.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x05.toByte(), 0x75.toByte(), 0x01.toByte(), 0x05.toByte(), 0x08.toByte(),
            0x19.toByte(), 0x01.toByte(), 0x29.toByte(), 0x05.toByte(), 0x91.toByte(), 0x02.toByte(),
            0x95.toByte(), 0x01.toByte(), 0x75.toByte(), 0x03.toByte(), 0x91.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x06.toByte(), 0x75.toByte(), 0x08.toByte(), 0x15.toByte(), 0x00.toByte(),
            0x25.toByte(), 0x65.toByte(), 0x05.toByte(), 0x07.toByte(), 0x19.toByte(), 0x00.toByte(),
            0x29.toByte(), 0x65.toByte(), 0x81.toByte(), 0x00.toByte(),
            0xC0.toByte(),

            // GAMEPAD
            0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x05.toByte(), 0xA1.toByte(), 0x01.toByte(),
            0x85.toByte(), REPORT_ID_GAMEPAD,
            0x05.toByte(), 0x09.toByte(), 0x19.toByte(), 0x01.toByte(), 0x29.toByte(), 0x10.toByte(),
            0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(), 0x75.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x10.toByte(), 0x81.toByte(), 0x02.toByte(),
            0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x30.toByte(), 0x09.toByte(), 0x31.toByte(),
            0x15.toByte(), 0x81.toByte(), 0x25.toByte(), 0x7F.toByte(), 0x75.toByte(), 0x08.toByte(),
            0x95.toByte(), 0x02.toByte(), 0x81.toByte(), 0x02.toByte(),
            0x09.toByte(), 0x32.toByte(), 0x09.toByte(), 0x35.toByte(), 0x81.toByte(), 0x02.toByte(),
            0x09.toByte(), 0x39.toByte(), 0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x07.toByte(),
            0x35.toByte(), 0x00.toByte(), 0x46.toByte(), 0x3B.toByte(), 0x01.toByte(), 0x65.toByte(),
            0x14.toByte(), 0x75.toByte(), 0x04.toByte(), 0x95.toByte(), 0x01.toByte(), 0x81.toByte(), 0x42.toByte(),
            0x75.toByte(), 0x04.toByte(), 0x95.toByte(), 0x01.toByte(), 0x81.toByte(), 0x03.toByte(),
            0x09.toByte(), 0x33.toByte(), 0x09.toByte(), 0x34.toByte(), 0x15.toByte(), 0x00.toByte(),
            0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), 0x75.toByte(), 0x08.toByte(), 0x95.toByte(), 0x02.toByte(),
            0x81.toByte(), 0x02.toByte(),
            0xC0.toByte()
        )
    }

    private val _connectionState = MutableStateFlow(STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var registeredApp = false

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val binder = LocalBinder()

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "App registered: $registered")
            registeredApp = registered
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.d(TAG, "HID state: ${device.address} = $state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectedDevice.value = device
                    _connectionState.value = STATE_CONNECTED
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (_connectedDevice.value == device) {
                        _connectedDevice.value = null
                        _connectionState.value = STATE_DISCONNECTED
                    }
                }
                BluetoothProfile.STATE_CONNECTING -> _connectionState.value = STATE_CONNECTING
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            hidDevice?.replyReport(device, type, id, ByteArray(0))
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): HidBluetoothService = this@HidBluetoothService
    }

    override fun onCreate() {
        super.onCreate()
        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bm.adapter
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("HID Service running"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerHidDevice()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        unregisterHidDevice()
    }

    private fun registerHidDevice() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "Bluetooth not enabled")
            return
        }

        bluetoothAdapter?.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = proxy as BluetoothHidDevice

                    val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                        "BT Input Controller",
                        "Android Mouse + Keyboard + Gamepad",
                        "Arena.ai",
                        HID_SUBCLASS_COMBO,
                        HID_REPORT_DESCRIPTOR
                    )

                    val inQos = BluetoothHidDeviceAppQosSettings(BluetoothHidDeviceAppQosSettings.MAX, 800, 9, 0, 1125, BluetoothHidDeviceAppQosSettings.MAX)
                    val outQos = BluetoothHidDeviceAppQosSettings(BluetoothHidDeviceAppQosSettings.MAX, 9, 800, 0, 1125, BluetoothHidDeviceAppQosSettings.MAX)

                    val ok = hidDevice?.registerApp(null, sdpSettings, inQos, outQos, hidCallback) ?: false
                    Log.d(TAG, "registerApp: $ok")
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                hidDevice = null
                registeredApp = false
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    private fun unregisterHidDevice() {
        hidDevice?.let {
            if (registeredApp) it.unregisterApp()
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
        }
        hidDevice = null
        registeredApp = false
    }

    fun sendMouseReport(dx: Byte, dy: Byte, buttons: Byte, wheel: Byte = 0) {
        val dev = _connectedDevice.value ?: return
        val hid = hidDevice ?: return
        if (!registeredApp) return
        try { hid.sendReport(dev, REPORT_ID_MOUSE.toInt(), byteArrayOf(buttons, dx, dy, wheel)) } catch (e: Exception) { Log.e(TAG, "mouse", e) }
    }

    fun sendKeyboardReport(modifiers: Byte, vararg keyCodes: Byte) {
        val dev = _connectedDevice.value ?: return
        val hid = hidDevice ?: return
        if (!registeredApp) return
        val keys = ByteArray(6) { 0 }
        keyCodes.take(6).forEachIndexed { i, k -> keys[i] = k }
        val report = byteArrayOf(modifiers, 0, keys[0], keys[1], keys[2], keys[3], keys[4], keys[5])
        try { hid.sendReport(dev, REPORT_ID_KEYBOARD.toInt(), report) } catch (e: Exception) { Log.e(TAG, "kbd", e) }
    }

    fun sendGamepadReport(buttons: Int, leftX: Byte, leftY: Byte, rightZ: Byte = 0, rightRz: Byte = 0, hat: Byte = 0x08, rx: Byte = 0, ry: Byte = 0) {
        val dev = _connectedDevice.value ?: return
        val hid = hidDevice ?: return
        if (!registeredApp) return
        val bL = (buttons and 0xFF).toByte()
        val bH = ((buttons shr 8) and 0xFF).toByte()
        val report = byteArrayOf(bL, bH, leftX, leftY, rightZ, rightRz, hat, 0, rx, ry)
        try { hid.sendReport(dev, REPORT_ID_GAMEPAD.toInt(), report) } catch (e: Exception) { Log.e(TAG, "gamepad", e) }
    }

    fun sendReleaseAll() {
        val dev = _connectedDevice.value ?: return
        val hid = hidDevice ?: return
        try { hid.sendReport(dev, REPORT_ID_MOUSE.toInt(), byteArrayOf(0, 0, 0, 0)) } catch (_: Exception) {}
        try { hid.sendReport(dev, REPORT_ID_KEYBOARD.toInt(), byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)) } catch (_: Exception) {}
        try { hid.sendReport(dev, REPORT_ID_GAMEPAD.toInt(), ByteArray(10)) } catch (_: Exception) {}
    }

    fun isConnected(): Boolean = _connectionState.value == STATE_CONNECTED

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "HID Controller", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BT Input HID")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}