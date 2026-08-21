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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import app.sender.auth.AuthenticatedIdentity
import app.sender.auth.FirebaseBootstrap
import app.sender.auth.GoogleAuthSession
import kotlinx.coroutines.Job
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
    private lateinit var authSummary: TextView
    private lateinit var authButton: MaterialButton
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private val receiverCards = mutableMapOf<String, MaterialCardView>()
    private var selectedReceiver: LiveReceiver? = null
    private val authSession by lazy { GoogleAuthSession(this) }
    private var authIdentity: AuthenticatedIdentity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.isNavigationBarContrastEnforced = false
        setContentView(buildScreen())
        renderConfiguredState()
        lockComposer()
        lifecycleScope.launch {
            refreshAuth()
            if (SenderBackendClient.isConfigured() && authIdentity != null) loadReceivers()
        }
    }

    private fun buildScreen(): View {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F6F8F7")) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(28), dp(24), dp(22)); setBackgroundColor(Color.parseColor("#0E5D5A")) }
        header.addView(label("NOTICEFLOW / SENDER", Color.parseColor("#CCF2E8"), 12f, Typeface.BOLD))
        header.addView(label("A calmer way to send school notices.", Color.WHITE, 28f, Typeface.BOLD).apply { setPadding(0, dp(10), 0, 0) })
        header.addView(label("Secure your account, choose one live Receiver, write clearly, and deliver with confidence.", Color.parseColor("#E9FAF5"), 15f, Typeface.NORMAL).apply { setPadding(0, dp(8), 0, 0) })
        page.addView(header)

        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(32)) }
        body.addView(label("1  ACCOUNT     2  DELIVERY TARGET     3  NOTICE", Color.parseColor("#0E5D5A"), 11f, Typeface.BOLD).apply { setPadding(dp(4), 0, 0, dp(12)) })
        statusChip = Chip(this).apply { isClickable = false; chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#D8F3EE")); setTextColor(Color.parseColor("#0E5D5A")) }
        body.addView(statusChip)
        statusText = label("", Color.parseColor("#526168"), 14f, Typeface.NORMAL).apply { setPadding(dp(2), dp(10), dp(2), dp(14)) }
        body.addView(statusText)
        body.addView(authPanel(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14) })

        body.addView(targetPanel(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14) })
        composerCard = composerPanel()
        body.addView(composerCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14) })
        resultText = label("Select a real Receiver to begin composing a notice.", Color.parseColor("#526168"), 14f, Typeface.NORMAL).apply { setPadding(dp(4), dp(4), dp(4), 0) }
        body.addView(resultText)
        body.addView(label("BACKEND: ${SenderBackendClient.endpointLabel()}", Color.parseColor("#76858A"), 11f, Typeface.NORMAL).apply { setPadding(dp(4), dp(18), dp(4), 0) })
        page.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        ViewCompat.setOnApplyWindowInsetsListener(page) { _, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            header.setPadding(dp(24) + safe.left, dp(28) + safe.top, dp(24) + safe.right, dp(22))
            body.setPadding(dp(20) + safe.left, dp(20), dp(20) + safe.right, dp(32) + safe.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(page)
        body.post {
            for (index in 0 until body.childCount) {
                val child = body.getChildAt(index)
                child.alpha = 0f
                child.translationY = dp(12).toFloat()
                child.animate().alpha(1f).translationY(0f).setStartDelay(index * 45L).setDuration(260L).start()
            }
        }
        return page
    }

    private fun authPanel(): MaterialCardView = card().apply {
        addView(LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(label("SENDER ACCOUNT", Color.parseColor("#0E5D5A"), 12f, Typeface.BOLD))
            authSummary = label("Sign in with the Email/Password account enabled in school-notics.", Color.parseColor("#526168"), 14f, Typeface.NORMAL).apply { setPadding(0, dp(7), 0, dp(10)) }
            addView(authSummary)

            val emailLayout = TextInputLayout(this@SenderActivity).apply {
                hint = "Sender email"
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            }
            emailInput = TextInputEditText(this@SenderActivity).apply {
                setSingleLine()
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            emailLayout.addView(emailInput)
            addView(emailLayout)

            val passwordLayout = TextInputLayout(this@SenderActivity).apply {
                hint = "Password"
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            }
            passwordInput = TextInputEditText(this@SenderActivity).apply {
                setSingleLine()
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordLayout.addView(passwordInput)
            addView(passwordLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })

            authButton = MaterialButton(this@SenderActivity).apply {
                text = "Sign in securely"
                setOnClickListener { signInWithEmail() }
            }
            addView(authButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(14) })

            val secondary = LinearLayout(this@SenderActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            secondary.addView(MaterialButton(this@SenderActivity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "Create account"
                setOnClickListener { createAccount() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            secondary.addView(MaterialButton(this@SenderActivity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "Reset password"
                setOnClickListener { resetPassword() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
            addView(secondary, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
        })
    }

    private fun targetPanel(): MaterialCardView = card().apply {
        addView(LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(label("STEP 2  ·  DELIVERY TARGET", Color.parseColor("#0E5D5A"), 12f, Typeface.BOLD))
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
            addView(label("STEP 3  ·  WRITE AND DELIVER", Color.parseColor("#0E5D5A"), 12f, Typeface.BOLD))
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

    private fun authenticate(): Job = signInWithEmail()

    private fun signInWithEmail(): Job = lifecycleScope.launch {
        setAuthBusy(true, "Signing in…")
        runCatching { authSession.signInWithEmail(emailInput.text?.toString().orEmpty(), passwordInput.text?.toString().orEmpty()) }
            .onSuccess { completeAuthentication(it) }
            .onFailure { showAuthError(it) }
            .also { setAuthBusy(false, if (authIdentity == null) "Sign in securely" else "Sign out") }
    }

    private fun createAccount(): Job = lifecycleScope.launch {
        setAuthBusy(true, "Creating account…")
        runCatching { authSession.createEmailAccount(emailInput.text?.toString().orEmpty(), passwordInput.text?.toString().orEmpty()) }
            .onSuccess { completeAuthentication(it) }
            .onFailure { showAuthError(it) }
            .also { setAuthBusy(false, if (authIdentity == null) "Sign in securely" else "Sign out") }
    }

    private fun resetPassword(): Job = lifecycleScope.launch {
        setAuthBusy(true, "Sending reset email…")
        runCatching { authSession.sendPasswordReset(emailInput.text?.toString().orEmpty()) }
            .onSuccess {
                authSummary.text = "Password reset email sent. Check the inbox for ${emailInput.text}."
                statusChip.text = "Reset email sent"
                statusText.text = "Set a new password, then return here to sign in."
            }
            .onFailure { showAuthError(it) }
            .also { setAuthBusy(false, "Sign in securely") }
    }

    private fun completeAuthentication(identity: AuthenticatedIdentity) {
        authIdentity = identity
        renderAuth(identity)
        statusChip.text = "Authenticated"
        statusText.text = "${identity.authMethod} is active. Sender is ready to load live Receivers."
        if (SenderBackendClient.isConfigured()) loadReceivers()
    }

    private fun showAuthError(error: Throwable) {
        val message = error.message ?: "Firebase authentication did not complete."
        authSummary.text = message
        statusChip.text = "Authentication failed"
        statusText.text = "Check the email and password, then try again."
    }

    private fun setAuthBusy(busy: Boolean, label: String) {
        authButton.isEnabled = !busy
        authButton.text = label
        if (::emailInput.isInitialized) emailInput.isEnabled = !busy
        if (::passwordInput.isInitialized) passwordInput.isEnabled = !busy
    }

    private fun signOut(): Job = lifecycleScope.launch {
        authSession.signOut()
        authIdentity = null
        authSummary.text = "Not authenticated. Sign in with the Sender Email/Password account."
        authButton.text = "Sign in securely"
        authButton.setOnClickListener { signInWithEmail() }
        lockComposer()
        receiverList.removeAllViews()
        statusChip.text = "Signed out"
        statusText.text = "Sign in to load live Receiver devices."
    }

    private suspend fun refreshAuth() {
        runCatching { authSession.current() }.onSuccess {
            authIdentity = it
            if (it != null && ::authSummary.isInitialized) renderAuth(it)
        }
    }

    private fun renderAuth(identity: AuthenticatedIdentity) {
        authSummary.text = "${identity.authMethod}: ${identity.email ?: identity.displayName ?: "device identity"}."
        authButton.text = "Sign out"
        authButton.setOnClickListener { signOut() }
    }

    private fun renderConfiguredState() {
        if (SenderBackendClient.isConfigured()) { statusChip.text = "Account first"; statusText.text = "Sign in with the Sender account, then load live Receivers." } else { statusChip.text = "Backend URL needed"; statusText.text = "This build needs its reachable HTTPS backend URL before it can load live devices." }
    }

    private fun loadReceivers() = lifecycleScope.launch {
        refreshButton.isEnabled = false
        refreshButton.text = "Refreshing…"
        statusChip.text = "Loading live devices"
        statusText.text = "Reading enabled Receiver records from the backend…"
        refreshAuth()
        if (authIdentity == null) {
            statusChip.text = "Account first"
            statusText.text = "Sign in with the Sender Email/Password account, then load live Receiver devices."
            refreshButton.isEnabled = true
            refreshButton.text = "Load registered receivers"
            return@launch
        }
        runCatching { SenderBackendClient.loadReceivers(authIdentity!!.idToken) }.onSuccess { receivers ->
            selectedReceiver = null; receiverCards.clear(); receiverList.removeAllViews(); lockComposer()
            receivers.forEach { receiver -> receiverList.addView(receiverCard(receiver), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }) }
            statusChip.text = if (receivers.isEmpty()) "No enabled Receivers" else "${receivers.size} registered Receiver(s)"
            statusText.text = if (receivers.isEmpty()) "No enabled Receiver records exist yet. Connect a real Receiver app first." else "Tap a device card to choose the exact Receiver for this notice."
        }.onFailure { error ->
            statusChip.text = "Could not load devices"
            statusText.text = error.message ?: "Backend request did not complete."
        }.also {
            refreshButton.isEnabled = true
            refreshButton.text = "Refresh live receivers"
        }
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
        composerCard.animate().alpha(1f).translationY(0f).setDuration(220L).start()
        statusChip.text = "Target selected"; statusText.text = "Now write the notice for ${receiver.label}."
        resultText.text = "Target locked to receiver ID ${receiver.receiverId}."
    }

    private fun lockComposer() {
        composerCard.alpha = 0.58f; composerCard.translationY = dp(6).toFloat(); titleInput.isEnabled = false; bodyInput.isEnabled = false; sendButton.isEnabled = false
        if (::targetSummary.isInitialized) { targetSummary.text = "No receiver selected"; targetSummary.setTextColor(Color.parseColor("#8A5A13")) }
    }
    private fun unlockComposer() { composerCard.alpha = 1f; titleInput.isEnabled = true; bodyInput.isEnabled = true; sendButton.isEnabled = true }

    private fun sendNotice() = lifecycleScope.launch {
        val target = selectedReceiver ?: run { resultText.text = "Choose a live Receiver first."; return@launch }
        val title = titleInput.text?.toString()?.trim().orEmpty(); val body = bodyInput.text?.toString()?.trim().orEmpty()
        when { title.isEmpty() -> { titleInput.error = "Enter a title"; return@launch }; body.isEmpty() -> { bodyInput.error = "Enter a message"; return@launch } }
        sendButton.isEnabled = false
        sendButton.text = "Sending…"
        statusChip.text = "Sending"
        statusText.text = "Sending through the backend and Firebase Cloud Messaging…"
        val token = authIdentity?.idToken ?: run {
            authenticate()
            sendButton.isEnabled = true
            sendButton.text = "Send to selected receiver"
            return@launch
        }
        runCatching { SenderBackendClient.sendTestNotice(target.receiverId, title, body, token) }.onSuccess { messageId ->
            statusChip.text = "Sent to FCM"; statusText.text = "The backend accepted the notice for ${target.label}."
            resultText.text = "Notice accepted for ${target.label}. FCM message ID: $messageId"; titleInput.setText(""); bodyInput.setText("")
        }.onFailure { error ->
            statusChip.text = "Send did not complete"
            statusText.text = error.message ?: "The backend could not send the notice."
            resultText.text = "No notice was sent."
        }.also {
            sendButton.isEnabled = true
            sendButton.text = "Send to selected receiver"
        }
    }

    private fun card() = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = dp(1).toFloat(); setCardBackgroundColor(Color.WHITE); strokeColor = Color.parseColor("#E0E7E4"); strokeWidth = dp(1) }
    private fun label(text: String, color: Int, size: Float, style: Int) = TextView(this).apply { this.text = text; setTextColor(color); textSize = size; typeface = Typeface.create("sans", style) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
