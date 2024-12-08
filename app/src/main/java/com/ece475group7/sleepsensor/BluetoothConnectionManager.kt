// file: BluetoothConnectionManager.kt
package com.ece475group7.sleepsensor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import java.util.UUID

class BluetoothConnectionManager(
    private val context: Context,
    private val onSensorDataReceived: (String, String) -> Unit
) {

    private var onDeviceConnected: ((String) -> Unit)? = null
    private var onDeviceDisconnected: (() -> Unit)? = null

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    fun connectToDevice(device: BluetoothDevice, onDeviceConnected: (String) -> Unit, onDeviceDisconnected: () -> Unit) {
        if (!verifyBluetoothPermissions()) {
            Log.e("BluetoothConnectionManager", "Permissions not granted. Cannot connect to device.")
            return
        }
        this.onDeviceConnected = onDeviceConnected
        this.onDeviceDisconnected = onDeviceDisconnected
        // Connect to the device
        Log.d("BluetoothConnectionManager", "Attempting to connect to device: ${device.name} - ${device.address}")
        BluetoothManager.mBluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    fun disconnectFromDevice() {
        if (!verifyBluetoothPermissions()) {
            Log.e("BluetoothConnectionManager", "Permissions not granted. Cannot disconnect from device.")
            return
        }
        BluetoothManager.mBluetoothGatt?.disconnect()
        BluetoothManager.mBluetoothGatt = null
        Log.d("BluetoothConnectionManager", "Disconnected from device.")
        onDeviceDisconnected?.invoke()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (!verifyBluetoothPermissions()) {
                Log.e("BluetoothConnectionManager", "Permissions not granted during connection state change.")
                return
            }

            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    Log.d("BluetoothConnectionManager", "Connected to GATT server, starting service discovery.")
                    gatt?.discoverServices()
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    Log.d("BluetoothConnectionManager", "Disconnected from GATT server.")
                    onDeviceDisconnected?.invoke()
                }
                else -> {
                    Log.d("BluetoothConnectionManager", "Connection state changed to $newState.")
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluetoothConnectionManager", "Services discovered successfully.")
                enableUartNotifications()
                onDeviceConnected?.invoke(gatt?.device?.name ?: "Unknown Device")
            } else {
                Log.w("BluetoothConnectionManager", "Service discovery failed with status: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic?.uuid == BluetoothManager.UART_TX_CHARACTERISTIC_UUID) {
                val data = characteristic.value
                val dataStr = data?.toString(Charsets.UTF_8).orEmpty().trim()
                Log.d("BluetoothConnectionManager", "Received data: $dataStr")

                // If empty, do nothing
                if (dataStr.isEmpty()) return

                // Parse JSON if it's not empty
                try {
                    val jsonObj = JSONObject(dataStr)
                    val keys = jsonObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = jsonObj.get(key).toString()
                        onSensorDataReceived(key, value)
                    }
                } catch (e: Exception) {
                    Log.e("BluetoothConnectionManager", "JSON parsing error: ${e.message}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun enableUartNotifications() {
        if (!verifyBluetoothPermissions()) {
            Log.e("BluetoothConnectionManager", "Permissions not granted while enabling notifications.")
            return
        }
        val service = BluetoothManager.mBluetoothGatt?.getService(BluetoothManager.UART_SERVICE_UUID)
        if (service == null) {
            Log.e("BluetoothConnectionManager", "UART Service not found.")
            return
        }
        val characteristic = service.getCharacteristic(BluetoothManager.UART_TX_CHARACTERISTIC_UUID)
        if (characteristic != null) {
            val notificationSet = BluetoothManager.mBluetoothGatt?.setCharacteristicNotification(characteristic, true)
            Log.d("BluetoothConnectionManager", "Set characteristic notification: $notificationSet")

            val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val descriptorWrite = BluetoothManager.mBluetoothGatt?.writeDescriptor(descriptor)
                Log.d("BluetoothConnectionManager", "Descriptor write initiated: $descriptorWrite")
            } else {
                Log.e("BluetoothConnectionManager", "Descriptor not found for UART TX characteristic.")
            }
        } else {
            Log.e("BluetoothConnectionManager", "UART TX Characteristic not found, cannot enable notifications.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun verifyBluetoothPermissions() : Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    }
}