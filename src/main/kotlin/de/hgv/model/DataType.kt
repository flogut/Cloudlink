package de.hgv.model

enum class DataType {
    HEIGHT, TEMPERATURE, LONGITUDE, LATITUDE;

    companion object {
        fun get(string: String?): DataType? = when (string) {
            "height" -> HEIGHT
            "temperature" -> TEMPERATURE
            "longitude" -> LONGITUDE
            "latitude" -> LATITUDE
            else -> null
        }

    }
}