package app.sender

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class LiveReceiver(val receiverId: String, val label: String, val enabled: Boolean, val lastSeenAt: String?)

object SenderBackendClient {
    private val baseUrl get() = BuildConfig.BACKEND_BASE_URL.trimEnd('/')

    fun isConfigured() = baseUrl.startsWith("https://") &&
        !baseUrl.contains("your-permanent-backend") &&
        !baseUrl.contains("replace-with-your-backend")

    fun endpointLabel() = if (isConfigured()) baseUrl else "Backend URL not configured"

    suspend fun loadReceivers(bearerToken: String): List<LiveReceiver> = withContext(Dispatchers.IO) {
        val payload = request("GET", "/api/v1/receivers", null, bearerToken)
        val values = payload.optJSONArray("receivers") ?: throw IOException("Backend returned no receiver list.")
        buildList {
            for (index in 0 until values.length()) {
                val value = values.optJSONObject(index) ?: continue
                val receiverId = value.optString("receiverId").trim()
                if (receiverId.isBlank()) continue
                val name = value.optString("name").trim()
                add(LiveReceiver(
                    receiverId,
                    if (name.isNotBlank()) name else "Receiver ${receiverId.take(8)}",
                    value.optBoolean("enabled", true),
                    value.optString("lastSeenAt").ifBlank { null }
                ))
            }
        }.filter { it.enabled }
    }

    suspend fun sendTestNotice(receiverId: String, title: String, body: String, bearerToken: String): String = withContext(Dispatchers.IO) {
        request("POST", "/api/v1/test-notice", JSONObject()
            .put("receiverId", receiverId)
            .put("title", title)
            .put("body", body)
            .put("type", "TEST"), bearerToken)
            .optString("messageId")
            .ifBlank { "accepted" }
    }

    private fun request(method: String, path: String, body: JSONObject?, bearerToken: String): JSONObject {
        check(isConfigured()) { "Set BACKEND_BASE_URL to the permanent HTTPS backend URL before live testing." }
        val connection = try {
            (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NoticeFlow-Android/0.1")
                setRequestProperty("Authorization", "Bearer $bearerToken")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
        } catch (error: Exception) {
            throw IOException("Could not reach NoticeFlow backend: ${error.message ?: error.javaClass.simpleName}", error)
        }

        return try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty().trim()
            val response = parseResponse(raw, status)
            if (status !in 200..299 || !response.optBoolean("success", false)) {
                val errorCode = response.optString("error").takeIf { it.isNotBlank() } ?: "HTTP_$status"
                throw IOException(friendlyError(errorCode, status))
            }
            response
        } catch (error: SocketTimeoutException) {
            throw IOException("NoticeFlow backend timed out. Check the phone connection and permanent HTTPS URL.", error)
        } catch (error: IOException) {
            throw error
        } catch (error: Exception) {
            throw IOException("NoticeFlow backend request failed: ${error.message ?: error.javaClass.simpleName}", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(raw: String, status: Int): JSONObject {
        if (raw.isBlank()) throw IOException("NoticeFlow backend returned an empty response (HTTP $status).")
        return runCatching { JSONObject(raw) }.getOrElse {
            val contentHint = if (raw.startsWith("<!doctype", ignoreCase = true) || raw.startsWith("<html", ignoreCase = true)) "an HTML page" else "an invalid response"
            throw IOException("NoticeFlow backend returned $contentHint instead of JSON (HTTP $status). Check the permanent HTTPS URL.", it)
        }
    }

    private fun friendlyError(code: String, status: Int): String = when (code) {
        "RECEIVER_NOT_FOUND" -> "That Receiver is no longer registered. Refresh the device list."
        "RECEIVER_DISABLED" -> "That Receiver is disabled. Refresh the device list."
        "INVALID_FCM_TOKEN" -> "The Receiver push token is no longer valid. Reconnect the Receiver app."
        "FCM_SEND_FAILED" -> "Firebase could not deliver the notice. Check the Receiver connection and try again."
        "AUTH_REQUIRED" -> "Sign in with Google before using the sender."
        "INVALID_REQUEST_BODY", "VALIDATION_ERROR" -> "The backend rejected the notice details. Check the fields and try again."
        else -> "Backend request failed with $code (HTTP $status)."
    }
}
