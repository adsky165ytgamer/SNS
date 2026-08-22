package app.sender

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import app.sender.auth.GoogleAuthSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * NoticeFlow Sender — mobile-first UI replacement.
 *
 * UI-only redesign of the existing SenderActivity. The backend/authentication
 * contracts are intentionally preserved so this file can replace the current
 * activity without changing SenderBackendClient or the auth classes.
 *
 * Design goals:
 * - Modern mobile-first hierarchy
 * - Separate Home / Notices / Receivers / Settings sections
 * - Comfortable one-handed touch targets
 * - Responsive width handling for small and large phones
 * - Smooth, restrained motion
 * - No debug-looking walls of text
 */
class SenderActivity : ComponentActivity() {

    private val teal = Color.rgb(11, 104, 99)
    private val tealDark = Color.rgb(7, 76, 73)
    private val tealSoft = Color.rgb(224, 246, 242)
    private val pageBg = Color.rgb(247, 249, 248)
    private val surface = Color.WHITE
    private val ink = Color.rgb(22, 35, 39)
    private val muted = Color.rgb(91, 105, 108)
    private val border = Color.rgb(224, 231, 229)
    private val green = Color.rgb(35, 126, 82)
    private val amber = Color.rgb(151, 101, 18)
    private val red = Color.rgb(177, 57, 57)

    private lateinit var root: LinearLayout
    private lateinit var content: FrameLayout
    private lateinit var nav: LinearLayout
    private lateinit var statusChip: Chip
    private lateinit var receiverList: LinearLayout
    private lateinit var refreshButton: MaterialButton
    private lateinit var titleInput: TextInputEditText
    private lateinit var bodyInput: TextInputEditText
    private lateinit var sendButton: MaterialButton
    private lateinit var targetSummary: TextView
    private lateinit var authSummary: TextView
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var authButton: MaterialButton

    private val receiverCards = mutableMapOf<String, MaterialCardView>()
    private val history by lazy { SenderHistory(applicationContext) }
    private val sentHistory by lazy {
        history.items().map { DeliveryRecord(it.receiverId, it.receiverName, it.title, it.body, it.messageId, it.sentAt) }
            .map { SentNotice(it.title, it.body, it.receiverName, it.sentAt, it.messageId) }
            .toMutableList()
    }
    private var selectedReceiver: LiveReceiver? = null
    private val authSession by lazy { GoogleAuthSession(this) }
    private var authIdentity: AuthenticatedIdentity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.isNavigationBarContrastEnforced = false
        setContentView(buildApp())

        lifecycleScope.launch {
            runCatching { authSession.current() }.onSuccess {
                authIdentity = it
                updateAuthUi()
                renderHome()
            }
        }
    }

    // ---------------------------------------------------------------------
    // App shell
    // ---------------------------------------------------------------------

    private fun buildApp(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBg)
        }

        content = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, 0, 1f)
        }

        nav = buildBottomNav()
        root.addView(content)
        root.addView(nav, LinearLayout.LayoutParams(MATCH, dp(76)))

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            root.setPadding(0, 0, 0, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
        renderHome()
        return root
    }

    private fun buildBottomNav(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(surface)
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }

        val items = listOf(
            NavItem("Home", "⌂"),
            NavItem("Notices", "◷"),
            NavItem("Receivers", "▣"),
            NavItem("Settings", "⚙")
        )

        items.forEachIndexed { index, item ->
            val button = TextView(this).apply {
                text = "${item.icon}\n${item.label}"
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(if (index == 0) teal else muted)
                typeface = Typeface.create("sans", Typeface.BOLD)
                isClickable = true
                isFocusable = true
                setPadding(0, dp(5), 0, dp(4))
                setOnClickListener {
                    selectNav(index)
                    when (index) {
                        0 -> renderHome()
                        1 -> renderHistory()
                        2 -> renderReceivers()
                        3 -> renderSettings()
                    }
                }
                contentDescription = item.label
            }
            bar.addView(button, LinearLayout.LayoutParams(0, MATCH, 1f))
        }
        return bar
    }

    private fun selectNav(selected: Int) {
        for (i in 0 until nav.childCount) {
            (nav.getChildAt(i) as? TextView)?.setTextColor(if (i == selected) teal else muted)
        }
    }

    // ---------------------------------------------------------------------
    // Home
    // ---------------------------------------------------------------------

    private fun renderHome() {
        val page = scrollPage()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(28))
        }
        page.addView(column)

        val identityName = authIdentity?.displayName?.takeIf { it.isNotBlank() }
            ?: authIdentity?.email?.substringBefore('@')
            ?: "Teacher"

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val greeting = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        greeting.addView(text("GOOD ${timeOfDay().uppercase()}", teal, 11f, Typeface.BOLD))
        greeting.addView(text("Hello, ${identityName.replaceFirstChar { it.uppercase() }}", ink, 27f, Typeface.BOLD).apply {
            setPadding(0, dp(5), 0, 0)
        })
        greeting.addView(text("Send a clear notice to the right classroom.", muted, 14f, Typeface.NORMAL).apply {
            setPadding(0, dp(4), 0, 0)
        })
        top.addView(greeting, LinearLayout.LayoutParams(0, WRAP, 1f))
        top.addView(statusChipView())
        column.addView(top)

        val hero = card(radius = 24).apply {
            setCardBackgroundColor(teal)
            strokeWidth = 0
        }
        val heroInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        heroInner.addView(text("NEW SCHOOL NOTICE", Color.WHITE, 11f, Typeface.BOLD))
        heroInner.addView(text("Need to tell a class something?", Color.WHITE, 22f, Typeface.BOLD).apply {
            setPadding(0, dp(8), 0, dp(3))
        })
        heroInner.addView(text("Choose the destination, write the message, then send it through the live delivery system.", Color.rgb(226, 249, 245), 14f, Typeface.NORMAL))
        val create = primaryButton("Create notice") { openComposer() }
        heroInner.addView(create, LinearLayout.LayoutParams(MATCH, dp(52)).apply { topMargin = dp(18) })
        hero.addView(heroInner)
        column.addView(hero, marginBottom = 16)

        val section = sectionHeader("LIVE DELIVERY", "What is happening right now")
        column.addView(section)
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(statCard("Receivers", if (receiverCards.isEmpty()) "—" else receiverCards.size.toString(), "registered"), weight = 1f)
        stats.addView(statCard("Sent", sentHistory.size.toString(), "this session"), weight = 1f, left = 8)
        column.addView(stats, marginBottom = 18)

        column.addView(sectionHeader("RECENT", "Your latest notices"))
        if (sentHistory.isEmpty()) {
            column.addView(emptyCard("No notices yet", "Your sent notices will appear here after the first successful broadcast."))
        } else {
            sentHistory.take(3).forEach { column.addView(historyCard(it), marginBottom = 8) }
        }

        animatePage(column)
    }

    private fun statusChipView(): Chip {
        statusChip = Chip(this).apply {
            isClickable = false
            isCheckable = false
            text = if (authIdentity != null) "● READY" else "● SIGN IN"
            chipBackgroundColor = ColorStateList.valueOf(if (authIdentity != null) tealSoft else Color.rgb(255, 244, 220))
            setTextColor(if (authIdentity != null) teal else amber)
            textSize = 11f
            setPadding(dp(4), 0, dp(4), 0)
        }
        return statusChip
    }

    // ---------------------------------------------------------------------
    // Composer
    // ---------------------------------------------------------------------

    private fun openComposer() {
        renderComposer()
    }

    private fun renderComposer() {
        val page = scrollPage()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(32))
        }
        page.addView(column)

        column.addView(backRow("Create notice") { renderHome() })
        column.addView(text("Make it short, clear, and easy to understand.", muted, 14f, Typeface.NORMAL).apply {
            setPadding(0, dp(6), 0, dp(18))
        })

        val targetCard = card()
        val targetInner = verticalPadding()
        targetInner.addView(text("1  DESTINATION", teal, 11f, Typeface.BOLD))
        targetSummary = text(
            selectedReceiver?.label ?: "Choose a receiver",
            ink,
            18f,
            Typeface.BOLD
        ).apply { setPadding(0, dp(8), 0, dp(3)) }
        targetInner.addView(targetSummary)
        targetInner.addView(text(
            selectedReceiver?.let { "This notice will be sent to this registered device." } ?: "Select the exact classroom display before sending.",
            muted,
            13f,
            Typeface.NORMAL
        ))
        val choose = outlineButton(if (selectedReceiver == null) "Choose receiver" else "Change receiver") { renderReceivers(true) }
        targetInner.addView(choose, LinearLayout.LayoutParams(MATCH, dp(48)).apply { topMargin = dp(14) })
        targetCard.addView(targetInner)
        column.addView(targetCard, marginBottom = 12)

        val noticeCard = card()
        val noticeInner = verticalPadding()
        noticeInner.addView(text("2  MESSAGE", teal, 11f, Typeface.BOLD))

        val typeLabel = text("Notice type", muted, 13f, Typeface.BOLD).apply { setPadding(0, dp(14), 0, dp(7)) }
        noticeInner.addView(typeLabel)
        val types = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val typeChips = listOf("Homework", "Important", "Information")
        typeChips.forEachIndexed { i, labelText ->
            val chip = Chip(this).apply {
                text = labelText
                isCheckable = true
                isChecked = i == 2
                chipBackgroundColor = ColorStateList.valueOf(if (i == 2) tealSoft else Color.rgb(241, 244, 243))
                setTextColor(if (i == 2) teal else ink)
                setOnCheckedChangeListener { button, checked ->
                    if (checked) {
                        for (j in 0 until types.childCount) {
                            if (j != i) (types.getChildAt(j) as Chip).isChecked = false
                        }
                    }
                    (button as Chip).setChipBackgroundColor(ColorStateList.valueOf(if (checked) tealSoft else Color.rgb(241, 244, 243)))
                    button.setTextColor(if (checked) teal else ink)
                }
            }
            types.addView(chip, LinearLayout.LayoutParams(0, dp(42), 1f).apply { if (i > 0) leftMargin = dp(5) })
        }
        noticeInner.addView(types)

        val titleLayout = inputLayout("Title", "What do students need to know?", 140)
        titleInput = titleLayout.second
        noticeInner.addView(titleLayout.first, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(14) })

        val bodyLayout = inputLayout("Description", "Write the full notice here…", 4000, multiline = true)
        bodyInput = bodyLayout.second
        bodyInput.minLines = 5
        noticeInner.addView(bodyLayout.first, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })

        sendButton = primaryButton("Send notice") { sendNoticeFromUi() }
        sendButton.isEnabled = selectedReceiver != null
        noticeInner.addView(sendButton, LinearLayout.LayoutParams(MATCH, dp(54)).apply { topMargin = dp(18) })
        noticeCard.addView(noticeInner)
        column.addView(noticeCard)

        if (selectedReceiver == null) {
            column.addView(text("Choose a receiver first. The send button stays disabled until a real destination is selected.", amber, 12f, Typeface.BOLD).apply {
                setPadding(dp(5), dp(10), dp(5), 0)
            })
        }

        animatePage(column)
    }

    private fun sendNoticeFromUi() {
        val target = selectedReceiver ?: return
        val title = titleInput.text?.toString()?.trim().orEmpty()
        val body = bodyInput.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) { titleInput.error = "Enter a title"; return }
        if (body.isEmpty()) { bodyInput.error = "Enter a description"; return }

        sendButton.isEnabled = false
        sendButton.text = "Sending…"

        lifecycleScope.launch {
            val token = authIdentity?.idToken ?: run {
                showAuthDialog()
                sendButton.isEnabled = true
                sendButton.text = "Send notice"
                return@launch
            }

            runCatching {
                SenderBackendClient.sendTestNotice(target.receiverId, title, body, token)
            }.onSuccess { messageId ->
                history.record(target, title, body, messageId)
                sentHistory.add(0, SentNotice(title, body, target.label, System.currentTimeMillis(), messageId))
                Toast.makeText(this@SenderActivity, "Notice sent to ${target.label}", Toast.LENGTH_SHORT).show()
                renderSuccess(title, target.label, messageId)
            }.onFailure { error ->
                sendButton.isEnabled = true
                sendButton.text = "Send notice"
                Toast.makeText(this@SenderActivity, error.message ?: "Could not send notice", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderSuccess(title: String, receiver: String, messageId: String) {
        val page = scrollPage()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(40), dp(24), dp(30))
        }
        page.addView(column)

        val icon = TextView(this).apply {
            text = "✓"
            gravity = Gravity.CENTER
            textSize = 34f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setBackground(roundDrawable(green, 999f))
        }
        column.addView(icon, LinearLayout.LayoutParams(dp(78), dp(78)).apply { bottomMargin = dp(22) })
        column.addView(text("Notice sent", ink, 28f, Typeface.BOLD).apply { gravity = Gravity.CENTER })
        column.addView(text("Your notice was accepted by the delivery backend.", muted, 14f, Typeface.NORMAL).apply {
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(8), dp(20), dp(20))
        })

        val summary = card()
        val inner = verticalPadding()
        inner.addView(text(title, ink, 18f, Typeface.BOLD))
        inner.addView(text("To  •  $receiver", teal, 13f, Typeface.BOLD).apply { setPadding(0, dp(7), 0, 0) })
        inner.addView(text("FCM message accepted", green, 12f, Typeface.BOLD).apply { setPadding(0, dp(5), 0, 0) })
        summary.addView(inner)
        column.addView(summary, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(18) })

        column.addView(primaryButton("Send another") { openComposer() }, LinearLayout.LayoutParams(MATCH, dp(52)))
        column.addView(outlineButton("View notice history") { renderHistory() }, LinearLayout.LayoutParams(MATCH, dp(50)).apply { topMargin = dp(10) })
        animatePage(column)
    }

    // ---------------------------------------------------------------------
    // Receivers
    // ---------------------------------------------------------------------

    private fun renderReceivers(returnToComposer: Boolean = false) {
        val page = scrollPage()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(30))
        }
        page.addView(column)

        column.addView(backRow(if (returnToComposer) "Choose receiver" else "Receivers") {
            if (returnToComposer) renderComposer() else renderHome()
        })
        column.addView(text("Live destinations from your school backend.", muted, 14f, Typeface.NORMAL).apply {
            setPadding(0, dp(5), 0, dp(15))
        })

        val search = TextInputLayout(this).apply {
            hint = "Search receivers"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            startIconDrawable = null
        }
        val searchInput = TextInputEditText(this).apply {
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT
        }
        search.addView(searchInput)
        column.addView(search, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(10) })

        refreshButton = outlineButton("Refresh live receivers") { loadReceivers(returnToComposer) }
        column.addView(refreshButton, LinearLayout.LayoutParams(MATCH, dp(48)).apply { bottomMargin = dp(14) })

        receiverList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(receiverList)

        searchInput.addTextChangedListener(SimpleTextWatcher { query ->
            for (i in 0 until receiverList.childCount) {
                val view = receiverList.getChildAt(i)
                val receiver = view.tag as? LiveReceiver ?: continue
                view.visibility = if (receiver.label.contains(query, true) || receiver.receiverId.contains(query, true)) View.VISIBLE else View.GONE
            }
        })

        if (authIdentity == null) {
            column.addView(emptyCard("Sign in to load receivers", "Your account is required to access the live device list."))
        } else {
            loadReceivers(returnToComposer)
        }
        animatePage(column)
    }

    private fun loadReceivers(returnToComposer: Boolean = false) {
        lifecycleScope.launch {
            refreshButton.isEnabled = false
            refreshButton.text = "Refreshing…"
            if (authIdentity == null) {
                showAuthDialog()
                refreshButton.isEnabled = true
                refreshButton.text = "Refresh live receivers"
                return@launch
            }
            runCatching { SenderBackendClient.loadReceivers(authIdentity!!.idToken) }
                .onSuccess { receivers ->
                    receiverCards.clear()
                    receiverList.removeAllViews()
                    receivers.forEach { receiver ->
                        val card = receiverCard(receiver, returnToComposer)
                        card.tag = receiver
                        receiverList.addView(card, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(8) })
                    }
                    if (receivers.isEmpty()) receiverList.addView(emptyCard("No receivers found", "Install and register a Receiver device first."))
                }
                .onFailure { error ->
                    receiverList.removeAllViews()
                    receiverList.addView(emptyCard("Could not load receivers", error.message ?: "The backend request failed."))
                }
                .also {
                    refreshButton.isEnabled = true
                    refreshButton.text = "Refresh live receivers"
                }
        }
    }

    private fun receiverCard(receiver: LiveReceiver, returnToComposer: Boolean): MaterialCardView {
        val isSelected = selectedReceiver?.receiverId == receiver.receiverId
        return card(radius = 18).apply {
            setCardBackgroundColor(if (isSelected) tealSoft else surface)
            strokeColor = if (isSelected) teal else border
            strokeWidth = if (isSelected) dp(2) else dp(1)
            isClickable = true
            isFocusable = true
            val inner = LinearLayout(this@SenderActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(15), dp(14), dp(12), dp(14))
            }
            val dot = TextView(this@SenderActivity).apply {
                text = "●"
                textSize = 17f
                setTextColor(if (receiver.lastSeenAt != null) green else muted)
                gravity = Gravity.CENTER
            }
            inner.addView(dot, LinearLayout.LayoutParams(dp(34), dp(42)))
            val info = LinearLayout(this@SenderActivity).apply { orientation = LinearLayout.VERTICAL }
            info.addView(text(receiver.label, ink, 16f, Typeface.BOLD))
            info.addView(text(receiver.lastSeenAt?.let { "Recently seen" } ?: "Registered device", muted, 12f, Typeface.NORMAL).apply {
                setPadding(0, dp(3), 0, 0)
            })
            inner.addView(info, LinearLayout.LayoutParams(0, WRAP, 1f))
            inner.addView(text(if (isSelected) "✓" else "›", if (isSelected) teal else muted, 23f, Typeface.BOLD))
            addView(inner)
            setOnClickListener {
                selectReceiver(receiver)
                if (returnToComposer) renderComposer() else renderReceivers(false)
            }
            receiverCards[receiver.receiverId] = this
        }
    }

    private fun selectReceiver(receiver: LiveReceiver) {
        selectedReceiver = receiver
    }

    // ---------------------------------------------------------------------
    // History
    // ---------------------------------------------------------------------

    private fun renderHistory() {
        val page = scrollPage()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(30))
        }
        page.addView(column)
        column.addView(backRow("Notice history") { renderHome() })
        column.addView(text("Notices sent during this app session.", muted, 14f, Typeface.NORMAL).apply {
            setPadding(0, dp(5), 0, dp(18))
        })
        if (sentHistory.isEmpty()) {
            column.addView(emptyCard("Nothing here yet", "Send your first notice and it will appear in this list."))
        } else {
            sentHistory.forEach { column.addView(historyCard(it), marginBottom = 9) }
        }
        animatePage(column)
    }

    private fun historyCard(item: SentNotice): MaterialCardView = card(radius = 18).apply {
        val inner = verticalPadding()
        val row = LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(text(item.title, ink, 17f, Typeface.BOLD), LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(text("SENT", green, 10f, Typeface.BOLD))
        inner.addView(row)
        inner.addView(text(item.body, muted, 13f, Typeface.NORMAL).apply { setPadding(0, dp(6), 0, 0) })
        inner.addView(text("To  •  ${item.receiverLabel}", teal, 12f, Typeface.BOLD).apply { setPadding(0, dp(10), 0, 0) })
        addView(inner)
    }

    // ---------------------------------------------------------------------
    // Settings / auth
    // ---------------------------------------------------------------------

    private fun renderSettings() {
        val page = scrollPage()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(30))
        }
        page.addView(column)
        column.addView(text("Settings", ink, 28f, Typeface.BOLD))
        column.addView(text("Manage the Sender app and its connection.", muted, 14f, Typeface.NORMAL).apply {
            setPadding(0, dp(5), 0, dp(20))
        })

        val account = card()
        val inner = verticalPadding()
        inner.addView(text("ACCOUNT", teal, 11f, Typeface.BOLD))
        authSummary = text("Not signed in", ink, 17f, Typeface.BOLD).apply { setPadding(0, dp(8), 0, dp(3)) }
        inner.addView(authSummary)
        inner.addView(text("Firebase authentication used by the existing backend contract.", muted, 13f, Typeface.NORMAL))
        authButton = primaryButton(if (authIdentity == null) "Sign in" else "Sign out") { if (authIdentity == null) showAuthDialog() else signOut() }
        inner.addView(authButton, LinearLayout.LayoutParams(MATCH, dp(50)).apply { topMargin = dp(14) })
        account.addView(inner)
        column.addView(account, marginBottom = 12)

        column.addView(infoRow("Backend", SenderBackendClient.endpointLabel()))
        column.addView(infoRow("Delivery", "Firebase Cloud Messaging"))
        column.addView(infoRow("App", "NoticeFlow Sender V0.1"))

        animatePage(column)
    }

    private fun showAuthDialog() {
        val dialog = android.app.Dialog(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(18))
            setBackground(roundDrawable(surface, 24f))
        }
        box.addView(text("Sign in", ink, 23f, Typeface.BOLD))
        box.addView(text("Use the school-notics Sender account.", muted, 13f, Typeface.NORMAL).apply { setPadding(0, dp(5), 0, dp(16)) })

        val email = TextInputLayout(this).apply { hint = "Email"; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE }
        emailInput = TextInputEditText(this).apply { setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        email.addView(emailInput)
        box.addView(email)

        val password = TextInputLayout(this).apply { hint = "Password"; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE; endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE }
        passwordInput = TextInputEditText(this).apply { setSingleLine(); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        password.addView(passwordInput)
        box.addView(password, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        actions.addView(outlineButton("Cancel") { dialog.dismiss() }, LinearLayout.LayoutParams(0, dp(50), 1f))
        lateinit var login: MaterialButton
        login = primaryButton("Sign in") {
            lifecycleScope.launch {
                login.isEnabled = false
                login.text = "Signing in…"
                runCatching { authSession.signInWithEmail(emailInput.text?.toString().orEmpty(), passwordInput.text?.toString().orEmpty()) }
                    .onSuccess {
                        authIdentity = it
                        updateAuthUi()
                        dialog.dismiss()
                        renderHome()
                    }
                    .onFailure { email.error = it.message ?: "Sign in failed" }
                    .also { login.isEnabled = true; login.text = "Sign in" }
            }
        }
        actions.addView(login, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(8) })
        box.addView(actions, LinearLayout.LayoutParams(MATCH, dp(50)).apply { topMargin = dp(16) })

        val create = outlineButton("Create account") {
            lifecycleScope.launch {
                runCatching { authSession.createEmailAccount(emailInput.text?.toString().orEmpty(), passwordInput.text?.toString().orEmpty()) }
                    .onSuccess { authIdentity = it; updateAuthUi(); dialog.dismiss(); renderHome() }
                    .onFailure { email.error = it.message ?: "Could not create account" }
            }
        }
        box.addView(create, LinearLayout.LayoutParams(MATCH, dp(48)).apply { topMargin = dp(8) })

        dialog.setContentView(box)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * .92f).toInt(), WRAP)
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * .92f).toInt(), WRAP)
    }

    private fun updateAuthUi() {
        if (!::statusChip.isInitialized) return
        statusChip.text = if (authIdentity != null) "● READY" else "● SIGN IN"
        statusChip.chipBackgroundColor = ColorStateList.valueOf(if (authIdentity != null) tealSoft else Color.rgb(255, 244, 220))
        statusChip.setTextColor(if (authIdentity != null) teal else amber)
        if (::authSummary.isInitialized) authSummary.text = authIdentity?.let { "${it.authMethod}: ${it.email ?: it.displayName ?: "account"}" } ?: "Not signed in"
    }

    private fun signOut(): Job = lifecycleScope.launch {
        authSession.signOut()
        authIdentity = null
        selectedReceiver = null
        receiverCards.clear()
        updateAuthUi()
        renderHome()
        Toast.makeText(this@SenderActivity, "Signed out", Toast.LENGTH_SHORT).show()
    }

    // ---------------------------------------------------------------------
    // Reusable UI primitives
    // ---------------------------------------------------------------------

    private fun scrollPage(): ScrollView {
        content.removeAllViews()
        return ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            content.addView(this)
        }
    }

    private fun sectionHeader(kicker: String, title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(kicker, teal, 10f, Typeface.BOLD))
        addView(text(title, ink, 18f, Typeface.BOLD).apply { setPadding(0, dp(3), 0, dp(10)) })
    }

    private fun statCard(title: String, value: String, caption: String): MaterialCardView = card(radius = 18).apply {
        val inner = verticalPadding()
        inner.addView(text(title, muted, 12f, Typeface.BOLD))
        inner.addView(text(value, ink, 25f, Typeface.BOLD).apply { setPadding(0, dp(5), 0, 0) })
        inner.addView(text(caption, muted, 11f, Typeface.NORMAL).apply { setPadding(0, dp(2), 0, 0) })
        addView(inner)
    }

    private fun emptyCard(title: String, message: String): MaterialCardView = card(radius = 18).apply {
        val inner = verticalPadding()
        inner.addView(text(title, ink, 16f, Typeface.BOLD))
        inner.addView(text(message, muted, 13f, Typeface.NORMAL).apply { setPadding(0, dp(6), 0, 0) })
        addView(inner)
    }

    private fun infoRow(title: String, value: String): MaterialCardView = card(radius = 16).apply {
        val inner = LinearLayout(this@SenderActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        inner.addView(text(title, muted, 13f, Typeface.BOLD), LinearLayout.LayoutParams(0, WRAP, 1f))
        inner.addView(text(value, ink, 13f, Typeface.BOLD))
        addView(inner)
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(8) }
    }

    private fun backRow(title: String, onBack: () -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val back = TextView(this@SenderActivity).apply {
            text = "‹"
            textSize = 34f
            setTextColor(ink)
            gravity = Gravity.CENTER
            isClickable = true
            setOnClickListener { onBack() }
        }
        addView(back, LinearLayout.LayoutParams(dp(42), dp(50)))
        addView(text(title, ink, 25f, Typeface.BOLD), LinearLayout.LayoutParams(0, WRAP, 1f))
    }

    private fun inputLayout(hint: String, placeholder: String, max: Int, multiline: Boolean = false): Pair<TextInputLayout, TextInputEditText> {
        val layout = TextInputLayout(this).apply {
            this.hint = hint
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            counterMaxLength = max
            isCounterEnabled = true
            placeholderText = placeholder
        }
        val input = TextInputEditText(this).apply {
            if (multiline) {
                minLines = 4
                maxLines = 8
                gravity = Gravity.TOP
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            } else {
                setSingleLine()
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
        }
        layout.addView(input)
        return layout to input
    }

    private fun primaryButton(label: String, action: () -> Unit): MaterialButton = MaterialButton(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(Color.WHITE)
        backgroundTintList = ColorStateList.valueOf(teal)
        cornerRadius = dp(16)
        minHeight = dp(48)
        insetTop = 0
        insetBottom = 0
        setOnClickListener { action() }
    }

    private fun outlineButton(label: String, action: () -> Unit): MaterialButton = MaterialButton(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(teal)
        backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        strokeColor = ColorStateList.valueOf(Color.rgb(176, 207, 202))
        strokeWidth = dp(1)
        cornerRadius = dp(16)
        minHeight = dp(48)
        insetTop = 0
        insetBottom = 0
        setOnClickListener { action() }
    }

    private fun verticalPadding(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(17), dp(17), dp(17), dp(17))
    }

    private fun card(radius: Int = 20): MaterialCardView = MaterialCardView(this).apply {
        cardElevation = 0f
        setCardBackgroundColor(surface)
        strokeColor = border
        strokeWidth = dp(1)
        this.radius = dp(radius).toFloat()
    }

    private fun text(value: String, color: Int, size: Float, style: Int): TextView = TextView(this).apply {
        text = value
        setTextColor(color)
        textSize = size
        typeface = Typeface.create("sans", style)
        includeFontPadding = false
    }

    private fun animatePage(container: ViewGroup) {
        container.post {
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                child.alpha = 0f
                child.translationY = dp(10).toFloat()
                child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay((i.coerceAtMost(8) * 30L))
                    .setDuration(220L)
                    .start()
            }
        }
    }

    private fun roundDrawable(color: Int, radius: Float): android.graphics.drawable.GradientDrawable = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = if (radius >= 999f) 9999f else dp(radius.toInt()).toFloat()
    }

    private fun timeOfDay(): String = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        else -> "evening"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    private data class NavItem(val label: String, val icon: String)
    private data class SentNotice(val title: String, val body: String, val receiverLabel: String, val timestamp: Long, val messageId: String)

    private class SimpleTextWatcher(private val changed: (String) -> Unit) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = changed(s?.toString().orEmpty())
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }

    private fun ViewGroup.addView(view: View, marginBottom: Int) {
        addView(view, LinearLayout.LayoutParams(MATCH, WRAP).apply { this.bottomMargin = marginBottom })
    }

    private fun LinearLayout.addView(view: View, weight: Float, left: Int = 0) {
        addView(view, LinearLayout.LayoutParams(0, WRAP, weight).apply { leftMargin = dp(left) })
    }
}
