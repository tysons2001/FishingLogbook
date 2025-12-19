package com.tyson.fishinglogbook.weather

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherSnapshot(
    val temperatureC: Double?,
    val pressureHpa: Double?
)

object OpenMeteoClient {

    /**
     * Fetch current temperature_2m (°C) and pressure_msl (hPa) for the given coordinates.
     * Uses Open-Meteo "forecast" endpoint with current variables.
     */
    fun fetchCurrent(lat: Double, lon: Double): WeatherSnapshot? {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,pressure_msl" +
                "&timezone=auto"
        )

        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) return null

            val root = JSONObject(body)
            val current = root.optJSONObject("current") ?: return null

            val temp = if (current.has("temperature_2m")) current.optDouble("temperature_2m") else Double.NaN
            val press = if (current.has("pressure_msl")) current.optDouble("pressure_msl") else Double.NaN

            WeatherSnapshot(
                temperatureC = if (temp.isNaN()) null else temp,
                pressureHpa = if (press.isNaN()) null else press
            )
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
