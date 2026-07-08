package com.carputer.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SensorEvent(
    val event: String,
    val data: SensorData
)

@Serializable
data class SensorData(
    val speed: Int = 0,
    val rpm: Int = 0,
    val throttle: Int = 0,
    val map: Int = 0,
    val coolant: Int = 0,
    val oil: Int = 0,
    val ambient: Int = 0,
    val intake: Int = 0,
    val driverDoor: Boolean = false,
    val passengerDoor: Boolean = false,
    val rearLeftDoor: Boolean = false,
    val rearRightDoor: Boolean = false,
    val trunk: Boolean = false,
    val hood: Boolean = false,
    val fuel: Int = 0,
    val oilPressure: Int = 0,
    val brakeFluid: Int = 0,
    val battery: Int = 0,
)
