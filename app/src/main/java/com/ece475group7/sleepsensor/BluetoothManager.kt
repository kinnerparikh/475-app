package com.ece475group7.sleepsensor

import android.bluetooth.BluetoothGatt
import java.util.UUID

object BluetoothManager {
    var mBluetoothGatt: BluetoothGatt? = null

    val UART_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val UART_TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val UART_RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
}