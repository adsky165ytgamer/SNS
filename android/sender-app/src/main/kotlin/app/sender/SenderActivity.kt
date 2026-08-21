package app.sender

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
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
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** Sender mobile-first workspace. TV/panel layouts are intentionally out of scope. */
class SenderActivity : ComponentActivity() {
    private val authSession by lazy { GoogleAuthSession(this) }
    private val history by lazy { SenderHistory(applicationContext) }
    private var authIdentity: AuthenticatedIdentity? = null
    private val receivers = mutableListOf<LiveReceiver>()
    private var selectedReceiver: LiveReceiver? = null
    private var activeSection = Section.HOME
    private var selectedType = "Information"
    private var selectedHistory: DeliveryRecord? = null
    private var receiverLoadMessage = "Tap load to read real Receiver records from the backend."
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var bottomNavigation: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var searchInput: TextInputEditText
    private lateinit var titleInput: TextInputEditText
    private lateinit var bodyInput: TextInputEditText

    private enum class Section(val label: String, val icon: String) {
        HOME("Home", "⌂"), NOTICES("Notices", "≡"), RECEIVERS("Receivers", "◎"), SETTINGS("Settings", "⚙")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.isNavigationBarContrastEnforced = false
        lifecycleScope.launch { authIdentity = runCatching { authSession.current() }.getOrNull(); if (::root.isInitialized) renderSection(activeSection) }
        buildShell()
    }

    private fun buildShell() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(CANVAS) }
        val appBar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(14)); setBackgroundColor(INK) }
        appBar.addView(label("NOTICEFLOW  /  SENDER", MINT, 11f, Typeface.BOLD))
        appBar.addView(label("Good to see you, sender.", Color.WHITE, 25f, Typeface.BOLD).apply { setPadding(0, dp(6), 0, 0) })
        appBar.addView(label("A mobile control room for messages that need to arrive.", Color.parseColor("#D9D7F2"), 13f, Typeface.NORMAL).apply { setPadding(0, dp(4), 0, 0) })
        root.addView(appBar)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(96)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val action = MaterialButton(this).apply {
            text = "+  Create Notice"
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(ACCENT)
            setOnClickListener { renderComposer() }
        }
        root.addView(action, LinearLayout.LayoutParams(-1, dp(52)).apply { leftMargin = dp(18); rightMargin = dp(18); bottomMargin = dp(8) })
        bottomNavigation = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(8), dp(7), dp(8), dp(8)); setBackgroundColor(Color.WHITE) }
        root.addView(bottomNavigation, LinearLayout.LayoutParams(-1, dp(72)))
        applyInsets(root, appBar, bottomNavigation)
        setContentView(root)
        renderSection(Section.HOME)
    }

    private fun renderSection(section: Section) {
        activeSection = section
        content.removeAllViews()
        bottomNavigation.removeAllViews()
        Section.entries.forEach { item ->
            val tab = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isClickable = true; setOnClickListener { renderSection(item) } }
            tab.addView(label(item.icon, if (item == section) ACCENT else MUTED, 22f, Typeface.BOLD).apply { gravity = Gravity.CENTER })
            tab.addView(label(item.label, if (item == section) ACCENT else MUTED, 11f, if (item == section) Typeface.BOLD else Typeface.NORMAL).apply { gravity = Gravity.CENTER; setPadding(0, dp(2), 0, 0) })
            bottomNavigation.addView(tab, LinearLayout.LayoutParams(0, -1, 1f))
        }
        when (section) {
            Section.HOME -> renderHome()
            Section.NOTICES -> renderNotices()
            Section.RECEIVERS -> renderReceivers()
            Section.SETTINGS -> renderSettings()
        }
        animateContent()
    }

    private fun renderHome() {
        content.addView(kicker("TODAY  /  SENDER DESK"))
        content.addView(label(if (authIdentity == null) "Sign in, then make the first move." else "Make the next notice count.", INK, 29f, Typeface.BOLD).apply { setPadding(0, dp(8), 0, dp(6)) })
        content.addView(label("Everything important is one tap away: create, choose, send, remember.", MUTED, 15f, Typeface.NORMAL).apply { setPadding(0, 0, 0, dp(18)) })
        content.addView(primaryCard("Create a notice", "Start with a clear message and a real audience.", "＋") { renderComposer() })
        content.addView(primaryButton("Load live Receivers") { renderSection(Section.RECEIVERS); loadReceivers() }, margins(10))
        content.addView(sectionLabel("AT A GLANCE"), margins(18))
        val records = history.items()
        val live = receivers.count()
        content.addView(statRow("Receivers", if (live == 0) "Not loaded" else "$live available", "Open Receivers to refresh the live list"))
        content.addView(statRow("Notices", records.size.toString(), "Saved on this Sender device"), margins(8))
        content.addView(statRow("Connection", if (SenderBackendClient.isConfigured()) "Backend ready" else "Needs URL", "Authenticated requests only"), margins(8))
        content.addView(sectionLabel("RECENT NOTICES"), margins(20))
        records.take(2).forEach { content.addView(historyCard(it), margins(8)) }
        if (records.isEmpty()) content.addView(emptyCard("Your notice history is waiting", "Accepted deliveries will appear here with their recipient and timestamp."), margins(8))
    }

    private fun renderNotices() {
        content.addView(kicker("NOTICES  /  HISTORY"))
        content.addView(label("Your notice trail", INK, 28f, Typeface.BOLD).apply { setPadding(0, dp(8), 0, dp(4)) })
        content.addView(label("Every card below is a local record of a dispatch accepted by the live backend.", MUTED, 15f, Typeface.NORMAL).apply { setPadding(0, 0, 0, dp(16)) })
        val records = history.items()
        if (records.isEmpty()) content.addView(emptyCard("No notices sent yet", "Create a notice, choose a live Receiver, and your delivery trail begins here."))
        else records.forEach { content.addView(historyCard(it), margins(10)) }
    }

    private fun renderReceivers() {
        content.addView(kicker("RECEIVERS  /  LIVE TARGETS"))
        content.addView(label("Choose a classroom", INK, 28f, Typeface.BOLD).apply { setPadding(0, dp(8), 0, dp(4)) })
        content.addView(label("Tap a card to select the exact device that should receive your notice.", MUTED, 15f, Typeface.NORMAL).apply { setPadding(0, 0, 0, dp(14)) })
        val search = TextInputEditText(this).apply { setSingleLine(); hint = "Search classrooms or device names"; inputType = InputType.TYPE_CLASS_TEXT }
        searchInput = search
        content.addView(outlinedInput("Search", search), margins(0))
        content.addView(primaryButton(if (receivers.isEmpty()) "Load live Receivers" else "Refresh live Receivers") { loadReceivers() }, margins(12))
        content.addView(settingsCard("Live list status", receiverLoadMessage, "Endpoint: ${SenderBackendClient.endpointLabel()}"), margins(12))
        if (receivers.isEmpty()) content.addView(emptyCard("No live targets loaded", "If the list remains empty, open Settings and sign in again before retrying."), margins(12))
        filteredReceivers().forEach { content.addView(receiverCard(it), margins(10)) }
    }

    private fun renderSettings() {
        content.addView(kicker("SETTINGS  /  PROFILE"))
        content.addView(label("Your sender identity", INK, 28f, Typeface.BOLD).apply { setPadding(0, dp(8), 0, dp(4)) })
        content.addView(label("Keep your account and connection details close, without crowding the sending flow.", MUTED, 15f, Typeface.NORMAL).apply { setPadding(0, 0, 0, dp(16)) })
        content.addView(accountCard())
        content.addView(settingsCard("Connection status", if (SenderBackendClient.isConfigured()) "Backend URL configured" else "Backend URL needs configuration", "Protected requests use Firebase ID tokens."), margins(10))
        content.addView(settingsCard("Notifications", "FCM delivery enabled", "Receiver devices handle notification presentation."), margins(10))
        content.addView(settingsCard("Diagnostics", "Open live connection checks", "Use the status and receiver refresh actions to troubleshoot."), margins(10))
        content.addView(settingsCard("About", "NoticeFlow v1.1.0 Alpha", "Created by ad_vibe_dev · Proprietary software · Not open source"), margins(10))
    }

    private fun renderComposer() {
        content.removeAllViews()
        bottomNavigation.removeAllViews()
        content.addView(kicker("CREATE NOTICE  /  STEP 1 OF 3"))
        content.addView(label("Make it clear.", INK, 29f, Typeface.BOLD).apply { setPadding(0, dp(8), 0, dp(4)) })
        content.addView(label("Choose a type, write the message, preview it, and send only after selecting a real target.", MUTED, 15f, Typeface.NORMAL).apply { setPadding(0, 0, 0, dp(16)) })
        content.addView(sectionLabel("NOTICE TYPE"))
        val chips = ChipGroup(this).apply { isSingleSelection = true; setPadding(0, dp(8), 0, dp(18)) }
        listOf("Homework", "Important", "Information").forEach { type -> chips.addView(Chip(this).apply { text = type; isCheckable = true; isChecked = type == selectedType; setOnCheckedChangeListener { _, checked -> if (checked) selectedType = type } }) }
        content.addView(chips)
        titleInput = TextInputEditText(this).apply { setSingleLine(); hint = "Example: English homework" }
        bodyInput = TextInputEditText(this).apply { minLines = 5; gravity = Gravity.TOP; hint = "Write the notice students should see" }
        content.addView(outlinedInput("Title", titleInput))
        content.addView(outlinedInput("Description", bodyInput), margins(12))
        content.addView(sectionLabel("RECIPIENT"), margins(18))
        content.addView(targetSummaryCard(), margins(8))
        content.addView(primaryButton("Choose recipients") { renderReceiversForComposer() }, margins(12))
        content.addView(secondaryButton("Preview notice") { showPreview() }, margins(8))
    }

    private fun renderReceiversForComposer() {
        renderReceivers()
        content.addView(primaryButton("Use selected recipient") { if (selectedReceiver != null) renderComposer() else setTransient("Select a Receiver card first") }, margins(14))
    }

    private fun showPreview() {
        val title = titleInput.text?.toString()?.trim().orEmpty()
        val body = bodyInput.text?.toString()?.trim().orEmpty()
        if (title.isBlank() || body.isBlank() || selectedReceiver == null) { setTransient("Choose a type, write a message, and select a recipient first"); return }
        content.removeAllViews()
        content.addView(kicker("CREATE NOTICE  /  PREVIEW"))
        content.addView(label("Ready to send?", INK, 29f, Typeface.BOLD).apply { setPadding(0, dp(8), 0, dp(14)) })
        content.addView(featureCard(selectedType, title, body))
        content.addView(label("To  ${selectedReceiver!!.label}", ACCENT, 16f, Typeface.BOLD).apply { setPadding(0, dp(16), 0, dp(8)) })
        content.addView(primaryButton("Send Notice") { sendNotice(title, body) }, margins(8))
        content.addView(secondaryButton("Edit notice") { renderComposer() }, margins(8))
    }

    private fun sendNotice(title: String, body: String) = lifecycleScope.launch {
        val receiver = selectedReceiver ?: return@launch
        val token = authIdentity?.idToken
        if (token == null) { setTransient("Sign in before sending a notice"); renderSection(Section.SETTINGS); return@launch }
        val result = runCatching { withContext(Dispatchers.IO) { SenderBackendClient.sendTestNotice(receiver.receiverId, title, body, token) } }
        result.onSuccess { messageId -> history.record(receiver, title, body, messageId); selectedReceiver = null; renderSection(Section.NOTICES) }
            .onFailure { setTransient(it.message ?: "Delivery needs attention") }
    }

    private fun loadReceivers() = lifecycleScope.launch {
        receiverLoadMessage = "Loading live Receiver records…"
        renderSection(Section.RECEIVERS)
        val current = authIdentity ?: runCatching { authSession.current() }.getOrNull().also { authIdentity = it }
        val token = current?.idToken
        if (token == null) {
            receiverLoadMessage = "Authentication is required before the protected Receiver list can load. Open Settings to sign in."
            setTransient("Sign in before loading live Receivers")
            renderSection(Section.RECEIVERS)
            return@launch
        }
        val result = runCatching { withContext(Dispatchers.IO) { SenderBackendClient.loadReceivers(token) } }
        result.onSuccess {
            receivers.clear(); receivers.addAll(it); selectedReceiver = null
            receiverLoadMessage = if (it.isEmpty()) "The backend responded successfully, but no enabled Receivers are registered." else "${it.size} enabled Receiver${if (it.size == 1) "" else "s"} loaded from the live backend."
            renderSection(Section.RECEIVERS)
        }.onFailure {
            receiverLoadMessage = it.message ?: "The live Receiver request failed."
            setTransient(receiverLoadMessage)
            renderSection(Section.RECEIVERS)
        }
    }

    private fun signIn() = lifecycleScope.launch {
        runCatching { authSession.signInWithEmail(emailInput.text?.toString().orEmpty(), passwordInput.text?.toString().orEmpty()) }
            .onSuccess { authIdentity = it; setTransient("Account connected"); renderSection(Section.HOME); loadReceivers() }
            .onFailure { setTransient(it.message ?: "Sign-in needs attention") }
    }

    private fun createAccount() = lifecycleScope.launch {
        runCatching { authSession.createEmailAccount(emailInput.text?.toString().orEmpty(), passwordInput.text?.toString().orEmpty()) }
            .onSuccess { authIdentity = it; setTransient("Account created"); renderSection(Section.HOME); loadReceivers() }
            .onFailure { setTransient(it.message ?: "Could not create account") }
    }

    private fun resetPassword() = lifecycleScope.launch {
        val result = runCatching { authSession.sendPasswordReset(emailInput.text?.toString().orEmpty()) }
        setTransient(if (result.isSuccess) "Reset email sent" else result.exceptionOrNull()?.message ?: "Reset needs attention")
    }

    private fun signOut() = lifecycleScope.launch { authSession.signOut(); authIdentity = null; receivers.clear(); selectedReceiver = null; setTransient("Signed out"); renderSection(Section.SETTINGS) }

    private fun accountCard(): MaterialCardView = card().apply {
        addView(LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(label("SENDER ACCOUNT", ACCENT, 11f, Typeface.BOLD))
            addView(label(authIdentity?.email ?: "Sign in with the Email/Password account enabled in school-notics.", MUTED, 14f, Typeface.NORMAL).apply { setPadding(0, dp(7), 0, dp(12)) })
            if (authIdentity == null) {
                emailInput = TextInputEditText(this@SenderActivity).apply { setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
                passwordInput = TextInputEditText(this@SenderActivity).apply { setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
                addView(outlinedInput("Email", emailInput))
                addView(outlinedInput("Password", passwordInput), margins(10))
                addView(primaryButton("Sign in securely") { signIn() }, margins(12))
                val row = LinearLayout(this@SenderActivity).apply { orientation = LinearLayout.HORIZONTAL }
                row.addView(secondaryButton("Create account") { createAccount() }, LinearLayout.LayoutParams(0, dp(46), 1f))
                row.addView(secondaryButton("Reset") { resetPassword() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(8) })
                addView(row, margins(8))
            } else addView(secondaryButton("Sign out") { signOut() }, LinearLayout.LayoutParams(-2, dp(46)))
        })
    }

    private fun receiverCard(receiver: LiveReceiver): MaterialCardView = card().apply {
        val selected = receiver.receiverId == selectedReceiver?.receiverId
        setCardBackgroundColor(Color.parseColor(if (selected) "#E5E1FF" else "#FFFFFF")); strokeColor = Color.parseColor(if (selected) "#6154C7" else "#E3E1ED"); strokeWidth = dp(if (selected) 2 else 1); isClickable = true
        addView(LinearLayout(this@SenderActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(15), dp(16), dp(15)); addView(label(receiver.label, INK, 17f, Typeface.BOLD)); addView(label("●  ${receiver.lastSeenAt ?: "Registered"} · ${receiver.receiverId.take(8)}…", Color.parseColor("#438A69"), 13f, Typeface.NORMAL).apply { setPadding(0, dp(6), 0, 0) }) })
        setOnClickListener { selectedReceiver = receiver; renderSection(Section.RECEIVERS) }
    }

    private fun filteredReceivers(): List<LiveReceiver> {
        if (!::searchInput.isInitialized) return receivers
        val query = searchInput.text?.toString()?.trim()?.lowercase().orEmpty()
        return receivers.filter { query.isBlank() || it.label.lowercase().contains(query) || it.receiverId.lowercase().contains(query) }
    }

    private fun targetSummaryCard(): MaterialCardView = if (selectedReceiver == null) emptyCard("No recipient selected", "Choose a classroom-style Receiver card before sending.") else featureCard("RECIPIENT", selectedReceiver!!.label, "This notice will be sent to the selected live device.")
    private fun historyCard(record: DeliveryRecord): MaterialCardView = featureCard(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(record.sentAt)), record.title, "${record.receiverName.ifBlank { record.receiverId }}  ·  Sent\n${record.body}").apply { setOnClickListener { selectedHistory = record; showHistoryDetail(record) } }
    private fun showHistoryDetail(record: DeliveryRecord) { content.removeAllViews(); bottomNavigation.removeAllViews(); content.addView(kicker("NOTICE DETAIL")); content.addView(label(record.title, INK, 29f, Typeface.BOLD).apply { setPadding(0, dp(8), 0, dp(8)) }); content.addView(featureCard("TYPE", "Notice", record.body)); content.addView(settingsCard("Recipients", record.receiverName.ifBlank { record.receiverId }, "Delivery accepted by the backend"), margins(10)); content.addView(settingsCard("Sent", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.sentAt)), "Message ID  ${record.messageId.take(12)}"), margins(10)); content.addView(secondaryButton("Back to notice history") { renderSection(Section.NOTICES) }, margins(16)) }
    private fun featureCard(eyebrow: String, title: String, detail: String): MaterialCardView = card().apply { addView(LinearLayout(this@SenderActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)); addView(label(eyebrow.uppercase(), ACCENT, 11f, Typeface.BOLD)); addView(label(title, INK, 18f, Typeface.BOLD).apply { setPadding(0, dp(6), 0, 0) }); addView(label(detail, MUTED, 14f, Typeface.NORMAL).apply { setPadding(0, dp(7), 0, 0) }) }) }
    private fun primaryCard(title: String, detail: String, glyph: String, action: () -> Unit): MaterialCardView = featureCard("QUICK ACTION", title, detail).apply { addView(primaryButton("$glyph  Open") { action() }) }
    private fun statRow(title: String, value: String, detail: String): MaterialCardView = featureCard(title, value, detail)
    private fun settingsCard(title: String, value: String, detail: String): MaterialCardView = featureCard(title, value, detail)
    private fun emptyCard(title: String, detail: String): MaterialCardView = featureCard("EMPTY STATE", title, detail)
    private fun card() = MaterialCardView(this).apply { radius = dp(20).toFloat(); cardElevation = dp(1).toFloat(); setCardBackgroundColor(Color.WHITE); strokeColor = Color.parseColor("#E3E1ED"); strokeWidth = dp(1) }
    private fun kicker(text: String) = label(text, ACCENT, 11f, Typeface.BOLD)
    private fun sectionLabel(text: String) = label(text, MUTED, 11f, Typeface.BOLD)
    private fun outlinedInput(hint: String, input: TextInputEditText) = TextInputLayout(this).apply { this.hint = hint; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE; if (hint == "Password") endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE; addView(input) }
    private fun primaryButton(text: String, action: () -> Unit) = MaterialButton(this).apply { this.text = text; isAllCaps = false; setOnClickListener { action() } }
    private fun secondaryButton(text: String, action: () -> Unit) = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply { this.text = text; isAllCaps = false; setOnClickListener { action() } }
    private fun label(text: String, color: Int, size: Float, style: Int) = TextView(this).apply { this.text = text; setTextColor(color); textSize = size; typeface = Typeface.create("sans", style) }
    private fun margins(top: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun animateContent() { content.alpha = 0f; content.translationY = dp(8).toFloat(); content.animate().alpha(1f).translationY(0f).setDuration(190L).start() }
    private fun setTransient(message: String) { if (::statusText.isInitialized) statusText.text = message; android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show() }
    private fun applyInsets(view: View, top: View, bottom: View) { ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets -> val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()); top.setPadding(top.paddingLeft + safe.left, top.paddingTop + safe.top, top.paddingRight + safe.right, top.paddingBottom); bottom.setPadding(bottom.paddingLeft + safe.left, bottom.paddingTop, bottom.paddingRight + safe.right, bottom.paddingBottom + safe.bottom); insets }; ViewCompat.requestApplyInsets(view) }

    companion object { private val CANVAS = Color.parseColor("#F8F7FC"); private val INK = Color.parseColor("#24213F"); private val MUTED = Color.parseColor("#6C6878"); private val ACCENT = Color.parseColor("#6154C7"); private val MINT = Color.parseColor("#A9F0DB") }
}
