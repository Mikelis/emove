package com.emove.emoveapplication

data class Contract(private val path: String, private val rate: Int? = null) {

    companion object {
//        const val URI_EVENTLISTENER = "suunto://MDS/EventListener"

        const val SCHEME_PREFIX = "suunto://"

        private const val DEFAULT_RATE = 13 // or get rates from infoResponse

        private const val ANGULAR_VELOCITY_PATH = "Meas/Gyro"
        private const val ANGULAR_VELOCITY_INFO_PATH = "Meas/Gyro/Info"
        private const val LINEAR_ACCELERATION_PATH = "Meas/Acc"
        private const val LINEAR_INFO_PATH = "Meas/Acc/Info"

//        const val BATTERY_PATH_GET = "System/Energy/Level"
//        // WTF IMU?
//        const val IMU6_PATH = "Meas/IMU6"
//        const val IMU9_PATH = "Meas/IMU9"

        fun angularVelocity(rate: Int = DEFAULT_RATE) = Contract(ANGULAR_VELOCITY_PATH, rate)
        fun angularVelocityInfo() = Contract(ANGULAR_VELOCITY_INFO_PATH)

        fun linearAcceleration(rate: Int = DEFAULT_RATE) = Contract(LINEAR_ACCELERATION_PATH, rate)
        fun linearAccelerationInfo() = Contract(LINEAR_INFO_PATH)
    }

    override fun toString(): String {
        val slashRate = if (rate == null) "" else "/$rate"
        return "$SCHEME_PREFIX$path$slashRate"
    }
}