package app.sender

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class LiveReceiver(
    val receiverId: String,
    val label: String,
    val enabled: Boolean,
    val lastSeenAt: String?,
    val ownerUid: String,
)

/** Direct Firebase client. No HTTP/Fastify middle layer is used. */
object DirectFirebaseStore {
    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    suspend fun loadReceivers(): List<LiveReceiver> {
        return firestore.collection("receivers")
            .whereEqualTo("enabled", true)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                val receiverId = document.getString("receiverId")?.trim().orEmpty()
                if (receiverId.isBlank()) return@mapNotNull null
                val name = document.getString("name")?.trim().orEmpty()
                LiveReceiver(
                    receiverId = receiverId,
                    label = name.ifBlank { "Receiver ${receiverId.take(8)}" },
                    enabled = document.getBoolean("enabled") != false,
                    lastSeenAt = document.getTimestamp("lastSeenAt")?.toDate()?.toString(),
                    ownerUid = document.getString("ownerUid")?.trim().orEmpty(),
                )
            }
            .filter { it.ownerUid.isNotBlank() }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun createNotice(senderUid: String, target: LiveReceiver, title: String, body: String, type: String): String {
        val noticeId = UUID.randomUUID().toString()
        firestore.collection("receivers").document(target.receiverId).collection("notices").document(noticeId).set(
            mapOf(
                "noticeId" to noticeId,
                "senderUid" to senderUid,
                "receiverId" to target.receiverId,
                "title" to title,
                "body" to body,
                "type" to type.uppercase(),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        return noticeId
    }
}
