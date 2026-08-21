package app.sender

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class LiveReceiver(val receiverId: String, val label: String, val enabled: Boolean, val lastSeenAt: String?)

object SenderBackendClient {
    private val baseUrl get() = BuildConfig.BACKEND_BASE_URL
    fun isConfigured() = baseUrl.startsWith("https://") && !baseUrl.contains("replace-with-your-backend")
    fun endpointLabel() = if (isConfigured()) baseUrl else "Backend URL not configured"

    suspend fun loadReceivers(): List<LiveReceiver> = withContext(Dispatchers.IO) {
        val payload = request("GET", "/api/v1/receivers", null)
        val values = payload.getJSONArray("receivers")
        buildList {
            for (index in 0 until values.length()) {
                val value = values.getJSONObject(index)
                val receiverId = value.getString("receiverId")
                val name = value.optString("name").trim()
                add(LiveReceiver(receiverId, if (name.isNotBlank()) name else receiverId, value.optBoolean("enabled", true), value.optString("lastSeenAt").ifBlank { null }))
            }
        }.filter { it.enabled }
    }

    suspend fun sendTestNotice(receiverId: String, title: String, body: String): String = withContext(Dispatchers.IO) {
        request("POST", "/api/v1/test-notice", JSONObject().put("receiverId", receiverId).put("title", title).put("body", body).put("type", "TEST")).getString("messageId")
    }

    private fun request(method: String, path: String, body: JSONObject?): JSONObject {
        check(isConfigured()) { "A reachable HTTPS backend URL has not been configured." }
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method; connection.connectTimeout = 15_000; connection.readTimeout = 15_000
            if (body != null) { connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/json"); connection.outputStream.use { it.write(body.toString().toByteArray()) } }
            val source = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = JSONObject(source.bufferedReader().use { it.readText() })
            if (connection.responseCode !in 200..299 || !response.optBoolean("success", false)) throw IOException(response.optString("error", "Backend request failed"))
            response
        } finally { connection.disconnect() }
    }
}
