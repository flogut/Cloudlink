package de.hgv.model

enum class DataType {
    HEIGHT, TEMPERATURE, LONGITUDE, LATITUDE, PRESSURE, DUST, VOLTAGE, INTERNAL_TEMPERATURE;

    companion object {
        fun get(string: String?): DataType? = when (string) {
            "height" -> HEIGHT
            "temperature" -> TEMPERATURE
            "longitude" -> LONGITUDE
            "latitude" -> LATITUDE
            "pressure" -> PRESSURE
            "dust" -> DUST
            "voltage" -> VOLTAGE
            "internal_temperature" -> INTERNAL_TEMPERATURE
            else -> null
        }

    }
}