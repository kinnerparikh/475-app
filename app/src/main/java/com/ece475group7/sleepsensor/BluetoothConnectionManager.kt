package com.ece475group7.sleepsensor

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.ece475group7.sleepsensor.BluetoothManager

class BluetoothConnectionManager(private val context: Context) {

    private var onDeviceConnected: ((String) -> Unit)? = null
    private var onDeviceDisconnected: (() -> Unit)? = null

    fun connectToDevice(device: BluetoothDevice, onDeviceConnected: (String) -> Unit, onDeviceDisconnected: () -> Unit) {
        this.onDeviceConnected = onDeviceConnected
        this.onDeviceDisconnected = onDeviceDisconnected
        // Connect to the device
        BluetoothManager.mBluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnectFromDevice() {
        BluetoothManager.mBluetoothGatt?.disconnect()
        BluetoothManager.mBluetoothGatt = null
        onDeviceDisconnected?.invoke()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
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

    private fun enableUartNotifications() {
        val service = BluetoothManager.mBluetoothGatt?.getService(BluetoothManager.UART_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(BluetoothManager.UART_RX_CHARACTERISTIC_UUID)
        BluetoothManager.mBluetoothGatt?.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic?.getDescriptor(BluetoothManager.CLIENT_CHARACTERISTIC_CONFIG_UUID)
    }
}