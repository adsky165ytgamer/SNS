package app.sender

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
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
import app.sender.auth.AuthenticatedIdentity
import app.sender.auth.GoogleAuthSession
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** NoticeFlow Sender v1.1.0 Alpha: calm targeting, composition, and delivery history. */
class SenderActivity : ComponentActivity() {
    private val authSession by lazy { GoogleAuthSession(this) }
    private val history by lazy { SenderHistory(applicationContext) }
    private var authIdentity: AuthenticatedIdentity? = null
    private var selectedReceiver: LiveReceiver? = null
    private val receivers = mutableListOf<LiveReceiver>()
    private var activeSection = Section.HOME
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private lateinit var statusChip: Chip
    private lateinit var statusText: TextView
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var titleInput: TextInputEditText
    private lateinit var bodyInput: TextInputEditText
    private var statusTitle = "Welcome to NoticeFlow"
    private var statusDetail = "Use the short introduction to learn the new sender flow."

    private enum class Section(val label: String) {
        HOME("Home"), SEND("Send"), HISTORY("History"), ABOUT("About")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.isNavigationBarContrastEnforced = false
        lifecycleScope.launch { authIdentity = runCatching { authSession.current() }.getOrNull() }
        if (history.hasCompletedOnboarding()) showApplication() else showOnboarding()
    }

    private fun showOnboarding() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1836"))
            setPadding(dp(24), dp(24), dp(24), dp(30))
        }
        page.addView(label("NOTICEFLOW  /  SENDER", Color.parseColor("#CFC7FF"), 12f, Typeface.BOLD))
        page.addView(label("Send with rhythm, not rush.", Color.WHITE, 31f, Typeface.BOLD).apply { setPadding(0, dp(18), 0, 0) })
        page.addView(label("NoticeFlow keeps the sending path deliberate: account, target, message, delivery record.", Color.parseColor("#E5E2FF"), 16f, Typeface.NORMAL).apply { setPadding(0, dp(12), 0, dp(20)) })
        page.addView(onboardingCard("01  Sign in with intent", "Your Email/Password account unlocks live receiver records. There is no shared sender identity."))
        page.addView(onboardingCard("02  Choose the exact screen", "Pick a real named Receiver returned by the backend before the composer opens."), margins(top = 12))
        page.addView(onboardingCard("03  Keep the story", "Every notice accepted by the backend is recorded in your private local delivery history."), margins(top = 12))
        page.addView(primaryButton("Enter Sender workspace") { history.completeOnboarding(); showApplication() }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)).apply { topMargin = dp(24) })
        page.addView(label("v1.1.0 Alpha  ·  Crafted by ad_vibe_dev", Color.parseColor("#CFC7FF"), 12f, Typeface.BOLD).apply { gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, dp(16), 0, 0) })
        applyInsets(page, page, null)
        setContentView(page)
        page.alpha = 0f
        page.animate().alpha(1f).setDuration(360L).start()
    }

    private fun showApplication() {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8F7FC")) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(24), dp(22), dp(18)); setBackgroundColor(Color.parseColor("#1A1836")) }
        header.addView(label("NOTICEFLOW  /  SENDER", Color.parseColor("#CFC7FF"), 11f, Typeface.BOLD))
        header.addView(label("The dispatch room", Color.WHITE, 25f, Typeface.BOLD).apply { setPadding(0, dp(7), 0, 0) })
        header.addView(label(authIdentity?.email ?: "Sign in to begin", Color.parseColor("#E5E2FF"), 14f, Typeface.NORMAL).apply { setPadding(0, dp(4), 0, 0) })
        page.addView(header)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(12), dp(8), dp(12), dp(8)); setBackgroundColor(Color.WHITE) }
        Section.entries.forEach { section ->
            nav.addView(MaterialButton(this).apply { text = section.label; isAllCaps = false; setOnClickListener { renderSection(section) } }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = if (section == Section.HOME) 0 else dp(5) })
        }
        page.addView(nav)
        applyInsets(page, header, nav)
        setContentView(page)
        renderSection(activeSection)
    }

    private fun renderSection(section: Section) {
        activeSection = section
        content.removeAllViews()
        navButtons(section)
        when (section) {
            Section.HOME -> renderHome()
            Section.SEND -> renderSend()
            Section.HISTORY -> renderHistory()
            Section.ABOUT -> renderAbout()
        }
        content.alpha = 0f
        content.translationY = dp(8).toFloat()
        content.animate().alpha(1f).translationY(0f).setDuration(220L).start()
    }

    private fun navButtons(selected: Section) {
        Section.entries.forEachIndexed { index, section ->
            val button = nav.getChildAt(index) as MaterialButton
            button.setTextColor(Color.parseColor(if (section == selected) "#FFFFFF" else "#433D78"))
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (section == selected) "#524B9D" else "#EEEFFF"))
        }
    }

    private fun renderHome() {
        val body = sectionBody()
        statusChip = Chip(this).apply { text = readinessLabel(); isClickable = false; chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#E6E2FF")); setTextColor(Color.parseColor("#433D78")) }
        body.addView(statusChip)
        statusText = label(statusDetail, Color.parseColor("#5C5A68"), 15f, Typeface.NORMAL).apply { setPadding(0, dp(10), 0, dp(16)) }
        body.addView(statusText)
        body.addView(featureCard("Your next move", nextMove(), "Move through the workspace in order: account, target, message, and history."))
        val recent = history.items().firstOrNull()
        body.addView(featureCard("Recent delivery", recent?.title ?: "Nothing dispatched yet", recent?.let { "Sent to ${it.receiverName.ifBlank { it.receiverId.take(8) }}" } ?: "Your first accepted notice will appear here."), margins(top = 14))
        body.addView(featureCard("Live capability", if (SenderBackendClient.isConfigured()) "Backend configured" else "Backend URL needed", "${if (authIdentity == null) "Account is not signed in." else "Account is active."} Receivers are only loaded from the live backend."), margins(top = 14))
        content.addView(body)
    }

    private fun renderSend() {
        val body = sectionBody()
        body.addView(sectionTitle("Send a notice", "The composer opens only after a real Receiver is chosen."))
        body.addView(accountCard(), margins(top = 14))
        body.addView(targetCard(), margins(top = 14))
        body.addView(composerCard(), margins(top = 14))
        content.addView(body)
    }

    private fun accountCard(): MaterialCardView = card().apply {
        addView(LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(label("1  ·  SENDER ACCOUNT", Color.parseColor("#524B9D"), 12f, Typeface.BOLD))
            addView(label(authIdentity?.let { "Signed in as ${it.email ?: "Sender account"}." } ?: "Use the Email/Password account enabled in school-notics.", Color.parseColor("#5C5A68"), 14f, Typeface.NORMAL).apply { setPadding(0, dp(7), 0, dp(12)) })
            if (authIdentity == null) {
                emailInput = TextInputEditText(this@SenderActivity).apply { setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
                passwordInput = TextInputEditText(this@SenderActivity).apply { setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
                addView(outlinedInput("Sender email", emailInput))
                addView(outlinedInput("Password", passwordInput), margins(top = 10))
                addView(primaryButton("Sign in securely") { signInWithEmail() }, margins(top = 14))
                val actions = LinearLayout(this@SenderActivity).apply { orientation = LinearLayout.HORIZONTAL }
                actions.addView(secondaryButton("Create account") { createAccount() }, LinearLayout.LayoutParams(0, dp(46), 1f))
                actions.addView(secondaryButton("Reset password") { resetPassword() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(8) })
                addView(actions, margins(top = 8))
            } else addView(secondaryButton("Sign out") { signOut() }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(46)))
        })
    }

    private fun targetCard(): MaterialCardView = card().apply {
        addView(LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(label("2  ·  DELIVERY TARGET", Color.parseColor("#524B9D"), 12f, Typeface.BOLD))
            addView(label("Only real enabled Receiver records are shown here. Select one exact device before writing.", Color.parseColor("#5C5A68"), 14f, Typeface.NORMAL).apply { setPadding(0, dp(7), 0, dp(12)) })
            addView(primaryButton(if (receivers.isEmpty()) "Load live Receivers" else "Refresh live Receivers") { loadReceivers() })
            if (selectedReceiver != null) addView(label("Selected  ·  ${selectedReceiver!!.label}", Color.parseColor("#524B9D"), 14f, Typeface.BOLD).apply { setPadding(0, dp(12), 0, 0) })
            if (receivers.isNotEmpty()) {
                receivers.forEach { receiver -> addView(receiverCard(receiver), margins(top = 10)) }
            } else addView(label("No devices loaded yet. Sign in, then load the real Receiver list.", Color.parseColor("#7A7785"), 13f, Typeface.NORMAL).apply { setPadding(0, dp(12), 0, 0) })
        })
    }

    private fun receiverCard(receiver: LiveReceiver): MaterialCardView = MaterialCardView(this).apply {
        val isSelected = receiver.receiverId == selectedReceiver?.receiverId
        radius = dp(16).toFloat(); cardElevation = 0f
        setCardBackgroundColor(Color.parseColor(if (isSelected) "#E7E4FF" else "#F9F8FF"))
        strokeColor = Color.parseColor(if (isSelected) "#524B9D" else "#DDD9F1")
        strokeWidth = dp(if (isSelected) 2 else 1)
        isClickable = true
        addView(LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(label(receiver.label, Color.parseColor("#24213F"), 16f, Typeface.BOLD))
            addView(label("${receiver.receiverId.take(10)}…  ·  ${receiver.lastSeenAt ?: "Registered device"}", Color.parseColor("#6C6878"), 12f, Typeface.NORMAL).apply { setPadding(0, dp(5), 0, 0) })
        })
        setOnClickListener { selectedReceiver = receiver; setStatus("Target selected", "${receiver.label} is selected. You can now write a notice."); renderSection(Section.SEND) }
    }

    private fun composerCard(): MaterialCardView = card().apply {
        val unlocked = selectedReceiver != null
        alpha = if (unlocked) 1f else .58f
        addView(LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(label("3  ·  WRITE AND DELIVER", Color.parseColor("#524B9D"), 12f, Typeface.BOLD))
            addView(label(if (unlocked) "Writing for ${selectedReceiver!!.label}. Review your notice before you send it." else "Choose a live Receiver above to unlock the composer.", Color.parseColor("#5C5A68"), 14f, Typeface.NORMAL).apply { setPadding(0, dp(7), 0, dp(12)) })
            titleInput = TextInputEditText(this@SenderActivity).apply { setSingleLine(); isEnabled = unlocked }
            bodyInput = TextInputEditText(this@SenderActivity).apply { minLines = 4; maxLines = 7; gravity = Gravity.TOP; isEnabled = unlocked }
            addView(outlinedInput("Notice title", titleInput))
            addView(outlinedInput("Notice message", bodyInput), margins(top = 10))
            addView(primaryButton("Send to selected Receiver") { sendNotice() }.apply { isEnabled = unlocked }, margins(top = 14))
        })
    }

    private fun renderHistory() {
        val body = sectionBody()
        body.addView(sectionTitle("Delivery history", "Only notices accepted by the backend are stored here on this Sender device."))
        val records = history.items()
        if (records.isEmpty()) body.addView(featureCard("No deliveries yet", "Your activity will appear here", "Send the first real notice from the Send section."), margins(top = 14))
        else {
            records.forEach { record -> body.addView(featureCard(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.sentAt)), record.title, "To ${record.receiverName.ifBlank { record.receiverId }}\n${record.body}"), margins(top = 10)) }
            body.addView(secondaryButton("Clear local history") { history.clear(); renderSection(Section.HISTORY) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(46)).apply { topMargin = dp(16) })
        }
        content.addView(body)
    }

    private fun renderAbout() {
        val body = sectionBody()
        body.addView(sectionTitle("NoticeFlow Sender", "A deliberate control room for school-wide communication."))
        body.addView(featureCard("v1.1.0 Alpha", "Separate spaces, clearer intent", "This Alpha release adds guided onboarding, multi-section navigation, and local delivery history."), margins(top = 14))
        body.addView(featureCard("Created by", "ad_vibe_dev", "NoticeFlow is designed as a proprietary school communication product."), margins(top = 14))
        body.addView(featureCard("License and access", "Proprietary — not open source", "No permission is granted to copy, redistribute, reverse engineer, or publish this application or its source without written authorization from the creator."), margins(top = 14))
        body.addView(secondaryButton("Replay introduction") { history.resetOnboarding(); showOnboarding() }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(46)).apply { topMargin = dp(16) })
        content.addView(body)
    }

    private fun signInWithEmail() = lifecycleScope.launch {
        runCatching { authSession.signInWithEmail(emailInput.text?.toString().orEmpty(), passwordInput.text?.toString().orEmpty()) }
            .onSuccess { authIdentity = it; setStatus("Account connected", "Your Sender account is ready. Load the live Receiver list next."); renderSection(Section.SEND) }
            .onFailure { setStatus("Sign-in needs attention", it.message ?: "Firebase authentication did not complete."); renderSection(Section.SEND) }
    }

    private fun createAccount() = lifecycleScope.launch {
        runCatching { authSession.createEmailAccount(emailInput.text?.toString().orEmpty(), passwordInput.text?.toString().orEmpty()) }
            .onSuccess { authIdentity = it; setStatus("Account created", "Your Sender account is ready. Load the live Receiver list next."); renderSection(Section.SEND) }
            .onFailure { setStatus("Could not create account", it.message ?: "Firebase authentication did not complete."); renderSection(Section.SEND) }
    }

    private fun resetPassword() = lifecycleScope.launch {
        val result = runCatching { authSession.sendPasswordReset(emailInput.text?.toString().orEmpty()) }
        setStatus(if (result.isSuccess) "Reset email sent" else "Reset needs attention", result.exceptionOrNull()?.message ?: "Check your inbox, set a new password, then return here.")
        renderSection(Section.SEND)
    }

    private fun signOut() = lifecycleScope.launch {
        authSession.signOut(); authIdentity = null; receivers.clear(); selectedReceiver = null
        setStatus("Signed out", "Sign in to load live Receivers and send notices.")
        renderSection(Section.SEND)
    }

    private fun loadReceivers() = lifecycleScope.launch {
        val identity = authIdentity
        if (identity == null) { setStatus("Account required", "Sign in before loading live Receivers."); renderSection(Section.SEND); return@launch }
        setStatus("Loading live Receivers", "Reading enabled Receiver records from the backend…")
        val result = runCatching { withContext(Dispatchers.IO) { SenderBackendClient.loadReceivers(identity.idToken) } }
        result.onSuccess { list ->
            receivers.clear(); receivers.addAll(list); selectedReceiver = null
            setStatus(if (list.isEmpty()) "No Receivers ready" else "${list.size} Receiver${if (list.size == 1) "" else "s"} ready", if (list.isEmpty()) "Connect a real Receiver app first." else "Choose one exact named screen.")
        }.onFailure { setStatus("Could not load devices", it.message ?: "Backend request did not complete.") }
        renderSection(Section.SEND)
    }

    private fun sendNotice() = lifecycleScope.launch {
        val receiver = selectedReceiver ?: return@launch
        val title = titleInput.text?.toString()?.trim().orEmpty()
        val body = bodyInput.text?.toString()?.trim().orEmpty()
        if (title.isBlank() || body.isBlank()) { setStatus("Write the notice first", "Both a title and message are required before dispatch."); renderSection(Section.SEND); return@launch }
        val token = authIdentity?.idToken ?: return@launch
        setStatus("Delivering notice", "Sending to ${receiver.label} through the live backend…")
        val result = runCatching { withContext(Dispatchers.IO) { SenderBackendClient.sendTestNotice(receiver.receiverId, title, body, token) } }
        result.onSuccess { messageId ->
            history.record(receiver, title, body, messageId)
            setStatus("Notice accepted", "${receiver.label} accepted the dispatch. It is now in your delivery history.")
            selectedReceiver = null
        }.onFailure { setStatus("Delivery needs attention", it.message ?: "The backend could not send the notice.") }
        renderSection(if (result.isSuccess) Section.HISTORY else Section.SEND)
    }

    private fun readinessLabel(): String = when {
        !SenderBackendClient.isConfigured() -> "Backend URL needed"
        authIdentity == null -> "Account required"
        else -> "Ready to send"
    }
    private fun nextMove(): String = when {
        authIdentity == null -> "Sign in to your Sender account"
        receivers.isEmpty() -> "Load live Receivers"
        selectedReceiver == null -> "Choose one delivery target"
        else -> "Write and send the notice"
    }
    private fun setStatus(title: String, detail: String) { statusTitle = title; statusDetail = detail; if (::statusChip.isInitialized) statusChip.text = title; if (::statusText.isInitialized) statusText.text = detail }

    private fun sectionBody() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(24)) }
    private fun sectionTitle(title: String, detail: String) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(label(title, Color.parseColor("#24213F"), 26f, Typeface.BOLD)); addView(label(detail, Color.parseColor("#5C5A68"), 15f, Typeface.NORMAL).apply { setPadding(0, dp(7), 0, 0) }) }
    private fun featureCard(eyebrow: String, title: String, detail: String): MaterialCardView = card().apply { addView(LinearLayout(this@SenderActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)); addView(label(eyebrow.uppercase(), Color.parseColor("#524B9D"), 11f, Typeface.BOLD)); addView(label(title, Color.parseColor("#24213F"), 18f, Typeface.BOLD).apply { setPadding(0, dp(6), 0, 0) }); addView(label(detail, Color.parseColor("#5C5A68"), 14f, Typeface.NORMAL).apply { setPadding(0, dp(7), 0, 0) }) }) }
    private fun onboardingCard(title: String, detail: String): MaterialCardView = MaterialCardView(this).apply { radius = dp(18).toFloat(); setCardBackgroundColor(Color.parseColor("#27244E")); strokeColor = Color.parseColor("#58528C"); strokeWidth = dp(1); addView(LinearLayout(this@SenderActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(16)); addView(label(title, Color.parseColor("#CFC7FF"), 13f, Typeface.BOLD)); addView(label(detail, Color.parseColor("#F0EFFF"), 14f, Typeface.NORMAL).apply { setPadding(0, dp(6), 0, 0) }) }) }
    private fun card() = MaterialCardView(this).apply { radius = dp(20).toFloat(); cardElevation = dp(1).toFloat(); setCardBackgroundColor(Color.WHITE); strokeColor = Color.parseColor("#E3E1ED"); strokeWidth = dp(1) }
    private fun primaryButton(text: String, action: () -> Unit) = MaterialButton(this).apply { this.text = text; setOnClickListener { action() } }
    private fun secondaryButton(text: String, action: () -> Unit) = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply { this.text = text; isAllCaps = false; setOnClickListener { action() } }
    private fun outlinedInput(hint: String, input: TextInputEditText) = TextInputLayout(this).apply { this.hint = hint; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE; if (hint == "Password") endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE; addView(input) }
    private fun margins(top: Int = 0) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }
    private fun label(text: String, color: Int, size: Float, style: Int) = TextView(this).apply { this.text = text; setTextColor(color); textSize = size; typeface = Typeface.create("sans", style) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun applyInsets(page: View, header: View, bottom: View?) { ViewCompat.setOnApplyWindowInsetsListener(page) { _, insets -> val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()); header.setPadding(header.paddingLeft + safe.left, header.paddingTop + safe.top, header.paddingRight + safe.right, header.paddingBottom); bottom?.setPadding(bottom.paddingLeft + safe.left, bottom.paddingTop, bottom.paddingRight + safe.right, bottom.paddingBottom + safe.bottom); insets }; ViewCompat.requestApplyInsets(page) }
}
