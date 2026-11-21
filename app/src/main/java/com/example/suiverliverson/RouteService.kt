package com.example.suiverliverson

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

suspend fun fetchRoadRoute(
    origin: GeoPoint,
    pickup: GeoPoint,
    destination: GeoPoint
): List<GeoPoint> = withContext(Dispatchers.IO) {
    val coordinates = listOf(origin, pickup, destination)
        .joinToString(";") { "${it.longitude},${it.latitude}" }
    val url = URL(
        "https://router.project-osrm.org/route/v1/driving/$coordinates?overview=full&geometries=polyline"
    )
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 10_000
        setRequestProperty("User-Agent", "FastDeliveryApp/1.0")
    }

    try {
        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        check(stream != null) { "OpenStreetMap routing returned no response ($responseCode)." }
        val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        check(responseCode in 200..299) { "OpenStreetMap routing failed ($responseCode)." }

        val response = JSONObject(responseText)
        check(response.optString("code") == "Ok") { "No road route was returned." }
        val encodedPolyline = response.getJSONArray("routes")
            .getJSONObject(0)
            .getString("geometry")
        decodePolyline(encodedPolyline)
    } finally {
        connection.disconnect()
    }
}

private fun decodePolyline(encoded: String): List<GeoPoint> {
    val points = mutableListOf<GeoPoint>()
    var index = 0
    var latitude = 0
    var longitude = 0

    while (index < encoded.length) {
        var result = 0
        var shift = 0
        var byte: Int
        do {
            byte = encoded[index++].code - 63
            result = result or ((byte and 0x1f) shl shift)
            shift += 5
        } while (byte >= 0x20)
        latitude += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

        result = 0
        shift = 0
        do {
            byte = encoded[index++].code - 63
            result = result or ((byte and 0x1f) shl shift)
            shift += 5
        } while (byte >= 0x20)
        longitude += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

        points += GeoPoint(latitude / 100_000.0, longitude / 100_000.0)
    }

    return points
}
