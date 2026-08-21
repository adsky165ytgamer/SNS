package app.sender

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local, user-visible record of notices accepted by the live backend. */
class SenderHistory(context: Context) {
    private val preferences = context.getSharedPreferences("sender_history", Context.MODE_PRIVATE)

    fun hasCompletedOnboarding(): Boolean = preferences.getBoolean("onboarding_complete", false)

    fun completeOnboarding() = preferences.edit().putBoolean("onboarding_complete", true).apply()

    fun resetOnboarding() = preferences.edit().putBoolean("onboarding_complete", false).apply()

    fun items(): List<DeliveryRecord> {
        val raw = preferences.getString("delivery_history", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val receiverId = item.optString("receiverId").trim()
                    val receiverName = item.optString("receiverName").trim()
                    val title = item.optString("title").trim()
                    val body = item.optString("body").trim()
                    val messageId = item.optString("messageId").trim()
                    if (receiverId.isNotBlank() && title.isNotBlank() && body.isNotBlank()) {
                        add(DeliveryRecord(receiverId, receiverName, title, body, messageId, item.optLong("sentAt", 0L)))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun record(receiver: LiveReceiver, title: String, body: String, messageId: String) {
        val next = listOf(DeliveryRecord(receiver.receiverId, receiver.label, title.trim(), body.trim(), messageId, System.currentTimeMillis())) + items()
        val array = JSONArray()
        next.distinctBy { "${it.receiverId}\u0000${it.title}\u0000${it.body}" }
            .take(MAX_HISTORY)
            .forEach { record ->
                array.put(JSONObject()
                    .put("receiverId", record.receiverId)
                    .put("receiverName", record.receiverName)
                    .put("title", record.title)
                    .put("body", record.body)
                    .put("messageId", record.messageId)
                    .put("sentAt", record.sentAt))
            }
        preferences.edit().putString("delivery_history", array.toString()).apply()
    }

    fun clear() = preferences.edit().remove("delivery_history").apply()

    companion object {
        private const val MAX_HISTORY = 30
    }
}

data class DeliveryRecord(
    val receiverId: String,
    val receiverName: String,
    val title: String,
    val body: String,
    val messageId: String,
    val sentAt: Long,
)

