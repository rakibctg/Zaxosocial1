package com.zaxo.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.zaxo.data.local.DeviceEntity
import com.zaxo.data.local.ZaxoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class DeviceRegistrationManager(private val context: Context) {
    private val db = ZaxoDatabase.getDatabase(context)
    private val deviceDao = db.deviceDao()
    private val prefs = context.getSharedPreferences("zaxo_device_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun registerDeviceOnStartup() {
        scope.launch {
            try {
                // Get or generate a persistent unique Device ID
                var deviceId = prefs.getString("device_id", null)
                if (deviceId == null) {
                    deviceId = UUID.randomUUID().toString()
                    prefs.edit().putString("device_id", deviceId).apply()
                }

                // Collect device details
                val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                val model = Build.MODEL
                val deviceName = "$manufacturer $model"
                val osVersion = Build.VERSION.RELEASE
                val appVersion = "6.0.0"

                Log.d("DeviceRegistrationManager", "Registering Device: ID=$deviceId, Name=$deviceName, OS=$osVersion, App=$appVersion")

                // Update isCurrent for other devices to false, insert or update current device to true
                val allDevices = deviceDao.getActiveDevicesList()
                
                // Set all other devices' isCurrent status to false
                allDevices.forEach { device ->
                    if (device.id != deviceId && device.isCurrent) {
                        deviceDao.insertDevice(device.copy(isCurrent = false))
                    }
                }

                // Insert/Update current device
                val currentDevice = DeviceEntity(
                    id = deviceId,
                    name = "$deviceName (This Device)",
                    location = "San Francisco, CA", // Default, simulating city IP lookup
                    lastActive = System.currentTimeMillis(),
                    isCurrent = true
                )
                deviceDao.insertDevice(currentDevice)

                // Ensure a couple of sample active sessions exist for simulation
                if (deviceDao.getActiveDevicesList().size < 2) {
                    deviceDao.insertDevice(
                        DeviceEntity(
                            id = "macbook_pro_session",
                            name = "MacBook Pro 16\"",
                            location = "Chicago, IL",
                            lastActive = System.currentTimeMillis() - 3600000 * 2,
                            isCurrent = false
                        )
                    )
                    deviceDao.insertDevice(
                        DeviceEntity(
                            id = "iphone_15_session",
                            name = "iPhone 15 Pro",
                            location = "London, UK",
                            lastActive = System.currentTimeMillis() - 3600000 * 24,
                            isCurrent = false
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e("DeviceRegistrationManager", "Failed to register device", e)
            }
        }
    }

    fun getDeviceId(): String {
        return prefs.getString("device_id", "current_device_id_zaxo") ?: "current_device_id_zaxo"
    }
}
