package app.sender

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/** Sender workflow: select a real API-returned Receiver first, then compose and send its notice. */
class SenderActivity : ComponentActivity() {
    private lateinit var statusChip: Chip
    private lateinit var statusText: TextView
    private lateinit var targetSummary: TextView
    private lateinit var receiverList: LinearLayout
    private lateinit var refreshButton: MaterialButton
    private lateinit var composerCard: MaterialCardView
    private lateinit var titleInput: TextInputEditText
    private lateinit var bodyInput: TextInputEditText
    private lateinit var sendButton: MaterialButton
    private lateinit var resultText: TextView
    private val receiverCards = mutableMapOf<String, MaterialCardView>()
    private var selectedReceiver: LiveReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContentView(buildScreen()); renderConfiguredState(); lockComposer() }

    private fun buildScreen(): View {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F6F8F7")) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(28), dp(24), dp(22)); setBackgroundColor(Color.parseColor("#0E5D5A")) }
        header.addView(label("NOTICEFLOW / SENDER", Color.parseColor("#CCF2E8"), 12f, Typeface.BOLD))
        header.addView(label("Choose. Write. Send.", Color.WHITE, 28f, Typeface.BOLD).apply { setPadding(0, dp(8), 0, 0) })
        header.addView(label("First choose a registered device from the live backend. Then compose and deliver its notice.", Color.parseColor("#E9FAF5"), 14f, Typeface.NORMAL).apply { setPadding(0, dp(6), 0, 0) })
        page.addView(header)

        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(32)) }
        statusChip = Chip(this).apply { isClickable = false; chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#D8F3EE")); setTextColor(Color.parseColor("#0E5D5A")) }
        body.addView(statusChip)
        statusText = label("", Color.parseColor("#526168"), 14f, Typeface.NORMAL).apply { setPadding(0, dp(10), 0, dp(14)) }
        body.addView(statusText)

        body.addView(targetPanel(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14) })
        composerCard = composerPanel()
        body.addView(composerCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14) })
        resultText = label("Select a real Receiver to begin composing a notice.", Color.parseColor("#526168"), 14f, Typeface.NORMAL).apply { setPadding(dp(4), dp(4), dp(4), 0) }
        body.addView(resultText)
        body.addView(label("BACKEND: ${SenderBackendClient.endpointLabel()}", Color.parseColor("#76858A"), 11f, Typeface.NORMAL).apply { setPadding(dp(4), dp(18), dp(4), 0) })
        page.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return page
    }

    private fun targetPanel(): MaterialCardView = card().apply {
        addView(LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(label("STEP 1 · DELIVERY TARGET", Color.parseColor("#0E5D5A"), 12f, Typeface.BOLD))
            addView(label("These are real enabled Receiver records returned by the backend. Tap one to select it.", Color.parseColor("#526168"), 14f, Typeface.NORMAL).apply { setPadding(0, dp(7), 0, dp(12)) })
            targetSummary = label("No receiver selected", Color.parseColor("#8A5A13"), 14f, Typeface.BOLD).apply { setPadding(0, 0, 0, dp(10)) }
            addView(targetSummary)
            receiverList = LinearLayout(this@SenderActivity).apply { orientation = LinearLayout.VERTICAL }
            addView(receiverList)
            refreshButton = MaterialButton(this@SenderActivity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply { text = "Load registered receivers"; setOnClickListener { loadReceivers() } }
            addView(refreshButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        })
    }

    private fun composerPanel(): MaterialCardView = card().apply {
        addView(LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(label("STEP 2 · WRITE NOTICE", Color.parseColor("#0E5D5A"), 12f, Typeface.BOLD))
            addView(label("Notice fields unlock after you choose a live Receiver above.", Color.parseColor("#526168"), 14f, Typeface.NORMAL).apply { setPadding(0, dp(7), 0, dp(12)) })
            val titleLayout = TextInputLayout(this@SenderActivity).apply { hint = "Notice title"; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE; counterMaxLength = 140; isCounterEnabled = true }
            titleInput = TextInputEditText(this@SenderActivity).apply { setSingleLine(); maxLines = 1 }
            titleLayout.addView(titleInput); addView(titleLayout)
            val bodyLayout = TextInputLayout(this@SenderActivity).apply { hint = "Notice message"; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE; counterMaxLength = 4000; isCounterEnabled = true }
            bodyInput = TextInputEditText(this@SenderActivity).apply { minLines = 4; gravity = Gravity.TOP; maxLines = 7 }
            bodyLayout.addView(bodyInput); addView(bodyLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
            sendButton = MaterialButton(this@SenderActivity).apply { text = "Send to selected receiver"; setOnClickListener { sendNotice() } }
            addView(sendButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(16) })
        })
    }

    private fun renderConfiguredState() {
        if (SenderBackendClient.isConfigured()) { statusChip.text = "Ready to load"; statusText.text = "Load registered Receivers from the connected backend." } else { statusChip.text = "Backend URL needed"; statusText.text = "This build needs its reachable HTTPS backend URL before it can load live devices." }
    }

    private fun loadReceivers() = lifecycleScope.launch {
        refreshButton.isEnabled = false; statusChip.text = "Loading live devices"; statusText.text = "Reading enabled Receiver records from the backend…"
        runCatching { SenderBackendClient.loadReceivers() }.onSuccess { receivers ->
            selectedReceiver = null; receiverCards.clear(); receiverList.removeAllViews(); lockComposer()
            receivers.forEach { receiver -> receiverList.addView(receiverCard(receiver), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }) }
            statusChip.text = if (receivers.isEmpty()) "No enabled Receivers" else "${receivers.size} registered Receiver(s)"
            statusText.text = if (receivers.isEmpty()) "No enabled Receiver records exist yet. Connect a real Receiver app first." else "Tap a device card to choose the exact Receiver for this notice."
        }.onFailure { error -> statusChip.text = "Could not load devices"; statusText.text = error.message ?: "Backend request did not complete." }.also { refreshButton.isEnabled = true }
    }

    private fun receiverCard(receiver: LiveReceiver): MaterialCardView = MaterialCardView(this).apply {
        radius = dp(14).toFloat(); cardElevation = 0f; setCardBackgroundColor(Color.parseColor("#F9FCFB")); strokeColor = Color.parseColor("#B8CBC5"); strokeWidth = dp(1); isClickable = true; isFocusable = true
        addView(LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(label(receiver.label, Color.parseColor("#172B30"), 17f, Typeface.BOLD))
            addView(label("Receiver ID: ${receiver.receiverId}", Color.parseColor("#526168"), 12f, Typeface.NORMAL).apply { setPadding(0, dp(5), 0, 0) })
            addView(label(receiver.lastSeenAt?.let { "Last seen: $it" } ?: "Registered device", Color.parseColor("#0E5D5A"), 12f, Typeface.BOLD).apply { setPadding(0, dp(5), 0, 0) })
        })
        setOnClickListener { selectReceiver(receiver) }
        receiverCards[receiver.receiverId] = this
    }

    private fun selectReceiver(receiver: LiveReceiver) {
        selectedReceiver = receiver
        receiverCards.forEach { (id, card) ->
            val selected = id == receiver.receiverId
            card.setCardBackgroundColor(Color.parseColor(if (selected) "#D8F3EE" else "#F9FCFB"))
            card.strokeColor = Color.parseColor(if (selected) "#0E5D5A" else "#B8CBC5")
            card.strokeWidth = dp(if (selected) 2 else 1)
        }
        targetSummary.setTextColor(Color.parseColor("#0E5D5A")); targetSummary.text = "Selected: ${receiver.label}"
        unlockComposer()
        statusChip.text = "Target selected"; statusText.text = "Now write the notice for ${receiver.label}."
        resultText.text = "Target locked to receiver ID ${receiver.receiverId}."
    }

    private fun lockComposer() {
        composerCard.alpha = 0.58f; titleInput.isEnabled = false; bodyInput.isEnabled = false; sendButton.isEnabled = false
        if (::targetSummary.isInitialized) { targetSummary.text = "No receiver selected"; targetSummary.setTextColor(Color.parseColor("#8A5A13")) }
    }
    private fun unlockComposer() { composerCard.alpha = 1f; titleInput.isEnabled = true; bodyInput.isEnabled = true; sendButton.isEnabled = true }

    private fun sendNotice() = lifecycleScope.launch {
        val target = selectedReceiver ?: run { resultText.text = "Choose a live Receiver first."; return@launch }
        val title = titleInput.text?.toString()?.trim().orEmpty(); val body = bodyInput.text?.toString()?.trim().orEmpty()
        when { title.isEmpty() -> { titleInput.error = "Enter a title"; return@launch }; body.isEmpty() -> { bodyInput.error = "Enter a message"; return@launch } }
        sendButton.isEnabled = false; statusChip.text = "Sending"; statusText.text = "Sending through the backend and Firebase Cloud Messaging…"
        runCatching { SenderBackendClient.sendTestNotice(target.receiverId, title, body) }.onSuccess { messageId ->
            statusChip.text = "Sent to FCM"; statusText.text = "The backend accepted the notice for ${target.label}."
            resultText.text = "Notice accepted for ${target.label}. FCM message ID: $messageId"; titleInput.setText(""); bodyInput.setText("")
        }.onFailure { error -> statusChip.text = "Send did not complete"; statusText.text = error.message ?: "The backend could not send the notice."; resultText.text = "No notice was sent." }.also { sendButton.isEnabled = true }
    }

    private fun card() = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = dp(1).toFloat(); setCardBackgroundColor(Color.WHITE); strokeColor = Color.parseColor("#E0E7E4"); strokeWidth = dp(1) }
    private fun label(text: String, color: Int, size: Float, style: Int) = TextView(this).apply { this.text = text; setTextColor(color); textSize = size; typeface = Typeface.create("sans", style) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
