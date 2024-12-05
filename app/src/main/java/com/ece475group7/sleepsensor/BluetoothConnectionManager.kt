package com.ece475group7.sleepsensor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.ece475group7.sleepsensor.BluetoothManager
import java.util.UUID

class BluetoothConnectionManager(private val context: Context) {

    private var onDeviceConnected: ((String) -> Unit)? = null
    private var onDeviceDisconnected: (() -> Unit)? = null

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    fun connectToDevice(device: BluetoothDevice, onDeviceConnected: (String) -> Unit, onDeviceDisconnected: () -> Unit) {
        if (!verifyBluetoothPermissions()) {
            return
        }
        this.onDeviceConnected = onDeviceConnected
        this.onDeviceDisconnected = onDeviceDisconnected
        // Connect to the device
        BluetoothManager.mBluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    fun disconnectFromDevice() {
        if (!verifyBluetoothPermissions()) {
            return
        }
        BluetoothManager.mBluetoothGatt?.disconnect()
        BluetoothManager.mBluetoothGatt = null
        onDeviceDisconnected?.invoke()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (!verifyBluetoothPermissions()) {
                return
            }
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                onDeviceConnected?.invoke(gatt?.device?.name ?: "Unknown Device")
                enableUartNotifications()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                onDeviceDisconnected?.invoke()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic?.uuid == BluetoothManager.UART_TX_CHARACTERISTIC_UUID) {
                val data = characteristic.value
                // Handle the received data
                Log.d("BluetoothConnectionManager", "Received data: ${data?.toString(Charsets.UTF_8)}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun enableUartNotifications() {
        if (!verifyBluetoothPermissions()) {
            return
        }
        val service = BluetoothManager.mBluetoothGatt?.getService(BluetoothManager.UART_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(BluetoothManager.UART_RX_CHARACTERISTIC_UUID)
        BluetoothManager.mBluetoothGatt?.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        BluetoothManager.mBluetoothGatt?.writeDescriptor(descriptor)
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    fun readUartData() {
        if (!verifyBluetoothPermissions()) {
            return
        }
        val service = BluetoothManager.mBluetoothGatt?.getService(BluetoothManager.UART_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(BluetoothManager.UART_TX_CHARACTERISTIC_UUID)
        BluetoothManager.mBluetoothGatt?.readCharacteristic(characteristic)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun verifyBluetoothPermissions() : Boolean {
        if (
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                1
            )

            return !(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
        } else {
            return true
        }
    }
}