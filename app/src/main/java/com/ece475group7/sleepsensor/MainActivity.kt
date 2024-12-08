package com.ece475group7.sleepsensor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ece475group7.sleepsensor.data.Datasource
import com.ece475group7.sleepsensor.model.SleepSensor
import com.ece475group7.sleepsensor.ui.theme.SleepSensorTheme

// Bluetooth permissions
const val REQUEST_BLUETOOTH_PERMISSIONS = 1

class MainActivity : ComponentActivity() {

    private val sensorDataViewModel: SensorDataViewModel by viewModels()

    // Register an ActivityResultLauncher to handle permission requests
    private val requestBluetoothPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Callback for the results of the permissions request
            val deniedPermissions = permissions.filterValues { !it }
            if (deniedPermissions.isEmpty()) {
                // All permissions are granted
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
            } else {
                // Some or all permissions were denied
                Toast.makeText(this, "Bluetooth permissions are required!", Toast.LENGTH_LONG).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure Bluetooth permissions
        ensureBluetoothPermissions()

        // Initialize the BluetoothConnectionManager with the callback
        val connectionManager = BluetoothConnectionManager(this) { sensorName, value ->
            sensorDataViewModel.updateSensorValue(sensorName, value)
        }

        setContent {
            SleepSensorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SleepApp(sensorDataViewModel, connectionManager)
                }
            }
        }
    }

    private fun ensureBluetoothPermissions() {
        // List of permissions needed
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )

        // Add new permissions for Android 12 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        }

        // Check if permissions are already granted
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            // Launch the permission request dialog
            requestBluetoothPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun SleepApp(sensorDataViewModel: SensorDataViewModel, connectionManager: BluetoothConnectionManager) {
    val context = LocalContext.current
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var deviceConnectedToName by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (deviceConnectedToName != null) {
                        // Disconnect if currently connected
                        connectionManager.disconnectFromDevice()
                        deviceConnectedToName = null
                    } else {
                        isDropdownExpanded = !isDropdownExpanded
                    }
                }
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            ) {
                if (isDropdownExpanded) {
                    if (deviceConnectedToName == null) {
                        Text("Select a device to connect to")
                        Spacer(modifier = Modifier.width(8.dp))
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
                    text = when {
                        deviceConnectedToName != null -> "Disconnect"
                        isDropdownExpanded -> "Hide Devices"
                        else -> "Find Devices"
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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


        val sensorValues = sensorDataViewModel.sensorValues
        LazyColumn(modifier = Modifier.padding(8.dp)) {
            item {
                // Header row with keys
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    sensorValues.keys.forEach { key ->
                        Text(
                            text = key,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
            item {
                // Values row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    sensorValues.values.forEach { value ->
                        Text(
                            text = value,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Existing sensors list (static data)
        SensorsList(sensorList = Datasource().loadSensors())
    }
}


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BluetoothConnectionScreen(
    connectionManager: BluetoothConnectionManager,
    modifier: Modifier = Modifier,
    onDeviceConnected: (String) -> Unit,
    onDeviceDisconnected: () -> Unit
) {
    val context = LocalContext.current
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    var discoveredDevices by remember { mutableStateOf(listOf<ScanResult>()) }
    var isScanning by remember { mutableStateOf(true) }

    if (
        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
    ) {
        Log.e("BluetoothConnectionScreen", "Bluetooth permissions not granted.")
        return
    }

    val scanner = bluetoothAdapter?.bluetoothLeScanner

    val scanCallback = remember {
        object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let {
                    val alreadyInList = discoveredDevices.any { d -> d.device.address == it.device.address }
                    if (!alreadyInList) {
                        Log.d("BluetoothConnectionScreen", "Discovered device: ${it.device.name} - ${it.device.address}")
                        discoveredDevices = discoveredDevices + it
                    }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { result ->
                    val alreadyInList = discoveredDevices.any { d -> d.device.address == result.device.address }
                    if (!alreadyInList) {
                        Log.d("BluetoothConnectionScreen", "Batch discovered device: ${result.device.name} - ${result.device.address}")
                        discoveredDevices = discoveredDevices + result
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.e("BluetoothConnectionScreen", "Scan failed with error code: $errorCode")
            }
        }
    }

    DisposableEffect(Unit) {
        if (bluetoothAdapter != null && scanner != null) {
            Log.d("BluetoothConnectionScreen", "Starting BLE scan.")
            scanner.startScan(scanCallback)
            isScanning = true
        } else {
            Log.e("BluetoothConnectionScreen", "Bluetooth adapter or scanner is null.")
        }

        onDispose {
            if (scanner != null && scanCallback != null) {
                Log.d("BluetoothConnectionScreen", "Stopping BLE scan.")
                scanner.stopScan(scanCallback)
                isScanning = false
            }
        }
    }

    Column {
        LazyColumn(modifier = modifier) {
            items(discoveredDevices) { scanResult ->
                val device = scanResult.device
                val deviceName = device.name ?: "Unnamed Device"
                Text(
                    text = "$deviceName (${device.address})",
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable {
                            Log.d("BluetoothConnectionScreen", "Device clicked: $deviceName - ${device.address}")
                            scanner?.stopScan(scanCallback)
                            isScanning = false
                            connectionManager.connectToDevice(device, onDeviceConnected, onDeviceDisconnected)
                        }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clickable {
                    Log.d("BluetoothConnectionScreen", "Restarting BLE scan.")
                    discoveredDevices = listOf()
                    scanner?.startScan(scanCallback)
                    isScanning = true
                },
        ) {
            Text(
                text = "Discovered ${discoveredDevices.size} devices",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 12.sp
            )
        }
    }
}



@Composable
fun SensorsList(sensorList: List<SleepSensor>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(sensorList) { sensor ->
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
                .fillMaxWidth()
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
