package com.ece475group7.sleepsensor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.ece475group7.sleepsensor.data.Datasource
import com.ece475group7.sleepsensor.model.SleepSensor
import com.ece475group7.sleepsensor.ui.theme.SleepSensorTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SleepSensorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SleepApp()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview
@Composable
fun SleepApp() {
    val context = LocalContext.current
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var deviceConnectedToName by remember {mutableStateOf<String?>(null)}
    val connectionManager = remember { BluetoothConnectionManager(context) }
    val layoutDirection = LocalLayoutDirection.current
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isDropdownExpanded = !isDropdownExpanded }
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            ) {
                // text on the left side, right side be a button that opens a dropdown
                if (isDropdownExpanded) {
                    if (deviceConnectedToName == null) {
                        Text("Select a device to connect to")
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("$deviceConnectedToName connected")
                    }
                } else {
                    Text(
                        text = deviceConnectedToName ?: "[No device connected]"
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = run {
                        if (deviceConnectedToName != null) "Disconnect"
                        else if (isDropdownExpanded) "Hide Devices"
                        else "Find Devices"
                    },
                    textDecoration = TextDecoration.Underline
                )
            }

        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            if (isDropdownExpanded && deviceConnectedToName == null) {
                BluetoothConnectionScreen(
                    connectionManager = connectionManager,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    onDeviceConnected = { deviceName ->
                        deviceConnectedToName = deviceName
                        isDropdownExpanded = false
                    },
                    onDeviceDisconnected = {
                        deviceConnectedToName = null
                    }
                )
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        SensorsList(sensorList = Datasource().loadSensors())
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BluetoothConnectionScreen(
    connectionManager : BluetoothConnectionManager,
    modifier: Modifier = Modifier,
    onDeviceConnected: (String) -> Unit,
    onDeviceDisconnected: () -> Unit
) {
    val context = LocalContext.current
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    var discoveredDevices by remember { mutableStateOf(listOf<BluetoothDevice>()) }
    var isConnecting by remember { mutableStateOf(false) }

    if (
        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
    ) { return }

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action: String? = intent!!.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    discoveredDevices += device
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)
        bluetoothAdapter?.startDiscovery()

        onDispose {
            context.unregisterReceiver(receiver)
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    Column {
        LazyColumn(modifier = modifier) {
            items(discoveredDevices) { device ->
                device.name?.let {
                    Text(`
                        text = it,
                        modifier = Modifier.padding(vertical = 8.dp).clickable {
                            isConnecting = true
                            connectionManager.connectToDevice(device, onDeviceConnected, onDeviceDisconnected)
                        }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clickable {
                    discoveredDevices = listOf()
                    bluetoothAdapter?.startDiscovery()
                },
        ) {
            Text(
                text = "Discovered ${discoveredDevices.size} devices (${discoveredDevices.count { it.name != null }} named)",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 12.sp
            )
        }
    }
}


@Composable
fun SensorsList(sensorList: List<SleepSensor>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {

        items(sensorList) {sensor ->
            SensorCard(
                sensor = sensor,
                modifier = Modifier.padding(8.dp)
            )

        }
    }
}

@Composable
fun SensorCard(sensor: SleepSensor, modifier: Modifier = Modifier) {
    Button(
        modifier = modifier,
        onClick = { /*TODO*/ }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),

            ) {
            Icon(
                painter = painterResource(sensor.sensorIconResourceId),
                contentDescription = null
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(sensor.sensorNameResourceId),
            )
        }
    }
}

//@Preview
@Composable
fun PreviewSensorCard() {
    SensorCard(
        sensor = SleepSensor(R.string.light_sensor_name, R.drawable.wb_sunny_24px)
    )
}