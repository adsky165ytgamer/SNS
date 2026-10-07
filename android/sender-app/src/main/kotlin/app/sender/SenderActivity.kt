package app.sender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import app.sender.auth.AuthenticatedIdentity
import app.sender.auth.GoogleAuthSession
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class SenderSection(val label: String, val description: String, val icon: ImageVector) {
    HOME("Overview", "Workspace", Icons.Default.Home),
    RECEIVERS("Receivers", "Live destinations", Icons.Default.Devices),
    HISTORY("Notice history", "Accepted dispatches", Icons.Default.History),
    SETTINGS("Settings", "Account and connection", Icons.Default.Settings),
}

private val SenderInk = Color(0xFFEAF0ED)
private val SenderMuted = Color(0xFF9BA8A3)
private val SenderCanvas = Color(0xFF0B1110)
private val SenderSurface = Color(0xFF131C1A)
private val SenderSurfaceRaised = Color(0xFF192521)
private val SenderAccent = Color(0xFF9FE6C5)
private val SenderAccentDeep = Color(0xFF244D3D)
private val SenderWarning = Color(0xFFFFC86B)

class SenderActivity : ComponentActivity() {
    private val authSession by lazy { GoogleAuthSession(this) }
    private val history by lazy { SenderHistory(applicationContext) }
    private var authIdentity by mutableStateOf<AuthenticatedIdentity?>(null)
    private val receivers = mutableStateListOf<LiveReceiver>()
    private var selectedReceiver by mutableStateOf<LiveReceiver?>(null)
    private var section by mutableStateOf(SenderSection.HOME)
    private var composing by mutableStateOf(false)
    private var workflowStep by mutableIntStateOf(1)
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private var title by mutableStateOf("")
    private var body by mutableStateOf("")
    private var receiverQuery by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var loadingReceivers by mutableStateOf(false)
    private var noticeType by mutableStateOf("Information")
    private var statusTitle by mutableStateOf("Ready to work")
    private var statusDetail by mutableStateOf("Sign in to load live Receiver destinations.")
    private var historyRevision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NoticeFlowSenderTheme { SenderShell() } }
        lifecycleScope.launch {
            authIdentity = runCatching { authSession.current() }.getOrNull()
            if (authIdentity != null) loadReceivers()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun SenderShell() {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = SenderSurface, windowInsets = WindowInsets.safeDrawing) {
                    DrawerHeader()
                    SenderSection.entries.forEach { item ->
                        NavigationDrawerItem(
                            label = { Column { Text(item.label, fontWeight = FontWeight.SemiBold); Text(item.description, color = SenderMuted, fontSize = 12.sp) } },
                            selected = section == item && !composing,
                            onClick = { section = item; composing = false; scope.launch { drawerState.close() } },
                            icon = { Icon(item.icon, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Direct Firebase workspace", color = SenderMuted, fontSize = 12.sp, modifier = Modifier.padding(24.dp))
                }
            },
        ) {
            Scaffold(
                containerColor = SenderCanvas,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = { SenderTopBar(onMenu = { scope.launch { drawerState.open() } }) },
                floatingActionButton = {
                    if (!composing && section == SenderSection.HOME) {
                        Button(onClick = { startNoticeFlow() }, colors = actionColors(), shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Create notice", fontWeight = FontWeight.Bold)
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    AnimatedContent(
                        targetState = if (composing) null else section,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "sender-page",
                    ) { current ->
                        if (current == null) ComposerScreen() else when (current) {
                            SenderSection.HOME -> HomeScreen()
                            SenderSection.RECEIVERS -> ReceiversScreen()
                            SenderSection.HISTORY -> HistoryScreen()
                            SenderSection.SETTINGS -> SettingsScreen()
                        }
                    }
                }
            }
        }
    }

    @Composable private fun DrawerHeader() = Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Text("NOTICEFLOW", color = SenderAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        Text("Sender", color = SenderInk, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(authIdentity?.email ?: "Not signed in", color = SenderMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(20.dp)); HorizontalDivider(color = Color(0xFF2B3833))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun SenderTopBar(onMenu: () -> Unit) = TopAppBar(
        title = { Column { Text(if (composing) "Create notice" else section.label, fontWeight = FontWeight.Bold); Text(if (composing) "A guided, review-first dispatch" else section.description, color = SenderMuted, fontSize = 12.sp) } },
        navigationIcon = { IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Open menu") } },
        actions = { StatusPill() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SenderCanvas, titleContentColor = SenderInk, navigationIconContentColor = SenderInk),
    )

    @Composable private fun StatusPill() {
        val ready = authIdentity != null
        Surface(shape = RoundedCornerShape(50), color = if (ready) SenderAccentDeep else Color(0xFF2A302E)) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (ready) SenderAccent else SenderWarning))
                Spacer(Modifier.width(7.dp)); Text(if (ready) "Connected" else "Sign in", color = SenderInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    @Composable private fun HomeScreen() = LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Intro("Good to see you.", "A focused workspace for sending clear notices to the right Receiver.") }
        item { OverviewHero() }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) { Metric("Live Receivers", receivers.size.toString(), Modifier.weight(1f)); Metric("Accepted notices", history.items().size.toString(), Modifier.weight(1f)) } }
        item { SectionCard("Connection", statusTitle, Icons.Default.Cloud) { Text(statusDetail, color = SenderMuted, fontSize = 14.sp, lineHeight = 21.sp); Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = { loadReceivers() }, enabled = !loadingReceivers) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text(if (loadingReceivers) "Refreshing…" else "Refresh destinations") } } }
        item { SectionCard("How delivery works", "Direct Firebase path", Icons.Default.Wifi) { Text("Choose a live Receiver, write the notice, review the exact target, and save it directly to that Receiver’s Firebase notice stream.", color = SenderMuted, fontSize = 14.sp, lineHeight = 21.sp) } }
    }

    @Composable private fun OverviewHero() = Surface(shape = RoundedCornerShape(24.dp), color = SenderSurfaceRaised, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Text("SEND WITH CONFIDENCE", color = SenderAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("One clear message.\nOne confirmed destination.", color = SenderInk, fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            Text("The review step protects against sending to the wrong screen.", color = SenderMuted, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 9.dp))
            Spacer(Modifier.height(18.dp)); Button(onClick = { startNoticeFlow() }, modifier = Modifier.fillMaxWidth(), colors = actionColors()) { Icon(Icons.Default.Send, null); Spacer(Modifier.width(8.dp)); Text("Start a notice", fontWeight = FontWeight.Bold) }
        }
    }

    @Composable private fun ComposerScreen() = LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { composing = false }) { Icon(Icons.Default.ArrowBack, "Back", tint = SenderInk) }; Spacer(Modifier.width(4.dp)); Intro("Create notice", "Complete each step before sending.") } }
        item { WorkflowProgress() }
        when (workflowStep) {
            1 -> { item { SectionCard("Choose a Receiver", "Only live destinations are shown", Icons.Default.Devices) { Text("Select the screen that should receive this notice.", color = SenderMuted, fontSize = 14.sp); Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = { loadReceivers() }, enabled = !loadingReceivers) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text(if (loadingReceivers) "Refreshing…" else "Refresh list") } } }; if (loadingReceivers) item { EmptyState("Loading destinations", "Checking the authenticated Firebase connection.", Icons.Default.Cloud) } else if (receivers.isEmpty()) item { EmptyState("No destinations yet", "Connect and name a Receiver before creating a live notice.", Icons.Default.Devices) } else items(receivers, key = { it.receiverId }) { ReceiverCard(it) } }
            2 -> item { SectionCard("Write the notice", selectedReceiver?.label ?: "Receiver required", Icons.Default.Edit) { Text("To: ${selectedReceiver?.label ?: "No Receiver selected"}", color = SenderAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Homework", "Important", "Information").forEach { type -> FilterChip(selected = noticeType == type, onClick = { noticeType = type }, label = { Text(type) }) } }; Spacer(Modifier.height(12.dp)); OutlinedTextField(title, { title = it.take(140) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), colors = fields()); Spacer(Modifier.height(10.dp)); OutlinedTextField(body, { body = it.take(4000) }, label = { Text("Message") }, minLines = 6, modifier = Modifier.fillMaxWidth(), colors = fields()); Spacer(Modifier.height(14.dp)); Button(onClick = { if (title.isBlank() || body.isBlank()) { statusTitle = "Message incomplete"; statusDetail = "Add a title and message before reviewing." } else workflowStep = 3 }, modifier = Modifier.fillMaxWidth(), colors = actionColors()) { Text("Review notice", fontWeight = FontWeight.Bold) } } }
            else -> item { SectionCard("Review and send", "Everything below will be written to Firebase", Icons.Default.CheckCircle) { ReviewRow("Receiver", selectedReceiver?.label ?: "Missing"); ReviewRow("Type", noticeType); ReviewRow("Title", title); Text(body, color = SenderMuted, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 12.dp)); Spacer(Modifier.height(18.dp)); OutlinedButton(onClick = { workflowStep = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Edit notice") }; Spacer(Modifier.height(8.dp)); Button(onClick = { sendNotice() }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = actionColors()) { Icon(Icons.Default.Send, null); Spacer(Modifier.width(8.dp)); Text(if (busy) "Sending…" else "Send notice", fontWeight = FontWeight.Bold) } } }
        }
    }

    @Composable private fun ReceiversScreen() = LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Intro("Receivers", "Live destinations available to this Sender account.") }
        item { OutlinedTextField(receiverQuery, { receiverQuery = it }, label = { Text("Search by name or ID") }, leadingIcon = { Icon(Icons.Default.Devices, null) }, modifier = Modifier.fillMaxWidth(), colors = fields(), singleLine = true) }
        item { OutlinedButton(onClick = { loadReceivers() }, enabled = !loadingReceivers, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text(if (loadingReceivers) "Refreshing…" else "Refresh live list") } }
        if (authIdentity == null) item { EmptyState("Sign in required", "Your account controls access to live Receiver records.", Icons.Default.AccountCircle) }
        else if (loadingReceivers) item { EmptyState("Loading live list", "Reading destinations from Firebase.", Icons.Default.Cloud) }
        else if (receivers.isEmpty()) item { EmptyState("No Receivers found", "Connect a named Receiver first, then refresh this list.", Icons.Default.Devices) }
        else items(receivers.filter { it.label.contains(receiverQuery, true) || it.receiverId.contains(receiverQuery, true) }, key = { it.receiverId }) { ReceiverCard(it) }
    }

    @Composable private fun ReceiverCard(receiver: LiveReceiver) = Surface(shape = RoundedCornerShape(18.dp), color = if (selectedReceiver?.receiverId == receiver.receiverId) SenderAccentDeep else SenderSurface, modifier = Modifier.fillMaxWidth().clickable { selectedReceiver = receiver; statusTitle = "Receiver selected"; statusDetail = "${receiver.label} is ready for your next notice."; if (composing && workflowStep == 1) workflowStep = 2 }) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(if (receiver.lastSeenAt != null) SenderAccent else SenderMuted)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(receiver.label, color = SenderInk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp); Text(if (receiver.lastSeenAt != null) "Recently seen" else "Registered device", color = SenderMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }; if (selectedReceiver?.receiverId == receiver.receiverId) Icon(Icons.Default.Check, null, tint = SenderAccent) }
    }

    @Composable private fun HistoryScreen() { val records = remember(historyRevision) { history.items() }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Intro("Notice history", "A local record of notices accepted by Firebase.") }; if (records.isEmpty()) item { EmptyState("Nothing sent yet", "Accepted notices will appear here with their destination and time.", Icons.Default.History) }; items(records, key = { it.messageId + it.sentAt }) { record -> Surface(shape = RoundedCornerShape(18.dp), color = SenderSurface, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(record.title, color = SenderInk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f)); Text("SENT", color = SenderAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold) }; Text("${record.receiverName} · ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.sentAt))}", color = SenderMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)); Text(record.body, color = SenderMuted, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)) } } } } }

    @Composable private fun SettingsScreen() = LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { Intro("Settings", "Account access and connection details in one place.") }; item { AccountCard() }; item { SectionCard("Firebase connection", "Direct Firestore workspace", Icons.Default.Cloud) { Text("Sender and Receiver use the same Firebase project directly. Authentication and access rules remain unchanged.", color = SenderMuted, fontSize = 14.sp, lineHeight = 21.sp) } }; item { SectionCard("About NoticeFlow", "Sender workspace", Icons.Default.Info) { Text("A focused notice composer built around a live destination list and a deliberate review step.", color = SenderMuted, fontSize = 14.sp, lineHeight = 21.sp) } } }

    @Composable private fun AccountCard() = SectionCard("School account", authIdentity?.email ?: "Sign in to send live notices", Icons.Default.AccountCircle) { if (authIdentity == null) { OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), colors = fields(), singleLine = true); Spacer(Modifier.height(10.dp)); OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = fields(), singleLine = true); Spacer(Modifier.height(12.dp)); Button(onClick = { signIn() }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = actionColors()) { Text(if (busy) "Signing in…" else "Sign in with email", fontWeight = FontWeight.Bold) }; Row { TextButton(onClick = { createAccount() }, enabled = !busy) { Text("Create account", color = SenderAccent) }; TextButton(onClick = { resetPassword() }, enabled = !busy) { Text("Reset password", color = SenderAccent) } }; Spacer(Modifier.height(12.dp)); HorizontalDivider(color = Color(0xFF2B3833)); Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = { signInWithGoogle() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AccountCircle, null); Spacer(Modifier.width(8.dp)); Text("Continue with Google") } } else { Text("Signed in as ${authIdentity?.email ?: "school account"}.", color = SenderInk, fontSize = 14.sp); Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = { signOut() }, enabled = !busy) { Text("Sign out") } } }

    private fun loadReceivers() = lifecycleScope.launch { if (authIdentity == null) return@launch; loadingReceivers = true; runCatching { DirectFirebaseStore.loadReceivers() }.onSuccess { receivers.clear(); receivers.addAll(it); statusTitle = "Live list updated"; statusDetail = "${it.size} available Receiver${if (it.size == 1) "" else "s"} from Firebase." }.onFailure { statusTitle = "Could not load Receivers"; statusDetail = it.message ?: "Firebase Firestore could not be read." }; loadingReceivers = false }
    private fun startNoticeFlow() { if (authIdentity == null) { section = SenderSection.SETTINGS; statusTitle = "Account required"; statusDetail = "Sign in before creating a live notice."; return }; workflowStep = 1; composing = true; if (receivers.isEmpty()) loadReceivers() }
    private fun sendNotice() = lifecycleScope.launch { val target = selectedReceiver ?: return@launch; if (workflowStep != 3 || title.isBlank() || body.isBlank()) { statusTitle = "Review required"; statusDetail = "Complete the Receiver, details, and review steps before sending."; return@launch }; val sender = authIdentity ?: run { section = SenderSection.SETTINGS; composing = false; statusTitle = "Account required"; return@launch }; busy = true; runCatching { DirectFirebaseStore.createNotice(sender.uid, target, title.trim(), body.trim(), noticeType) }.onSuccess { id -> history.record(target, title.trim(), body.trim(), id); historyRevision++; statusTitle = "Notice saved"; statusDetail = "Written directly to ${target.label}."; title = ""; body = ""; composing = false; workflowStep = 1; section = SenderSection.HISTORY }.onFailure { statusTitle = "Could not send notice"; statusDetail = it.message ?: "Firebase Firestore could not create the notice." }; busy = false }
    private fun signIn() = lifecycleScope.launch { busy = true; runCatching { authSession.signInWithEmail(email.trim(), password) }.onSuccess { authIdentity = it; password = ""; statusTitle = "Account connected"; statusDetail = "Refresh live Receivers to select a destination."; loadReceivers() }.onFailure { statusTitle = "Sign-in failed"; statusDetail = it.message ?: "Firebase authentication did not complete." }; busy = false }
    private fun signInWithGoogle() = lifecycleScope.launch { busy = true; runCatching { authSession.signIn() }.onSuccess { authIdentity = it; statusTitle = "Google account connected"; statusDetail = "Refresh live Receivers to select a destination."; loadReceivers() }.onFailure { statusTitle = "Google Sign-In failed"; statusDetail = it.message ?: "Google Sign-In did not complete." }; busy = false }
    private fun createAccount() = lifecycleScope.launch { busy = true; runCatching { authSession.createEmailAccount(email.trim(), password) }.onSuccess { authIdentity = it; password = ""; statusTitle = "Account created"; statusDetail = "Refresh live Receivers to select a destination."; loadReceivers() }.onFailure { statusTitle = "Could not create account"; statusDetail = it.message ?: "Firebase authentication did not complete." }; busy = false }
    private fun resetPassword() = lifecycleScope.launch { busy = true; runCatching { authSession.sendPasswordReset(email.trim()) }.onSuccess { statusTitle = "Reset email sent"; statusDetail = "Check the inbox for $email." }.onFailure { statusTitle = "Reset failed"; statusDetail = it.message ?: "Could not send reset email." }; busy = false }
    private fun signOut() = lifecycleScope.launch { busy = true; authSession.signOut(); authIdentity = null; selectedReceiver = null; receivers.clear(); statusTitle = "Signed out"; statusDetail = "Sign in again before loading Receivers or sending notices."; busy = false }

    @Composable private fun Intro(title: String, subtitle: String) = Column { Text(title, color = SenderInk, fontSize = 29.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = SenderMuted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp)) }
    @Composable private fun WorkflowProgress() = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Receiver", "Details", "Review").forEachIndexed { index, label -> Surface(shape = RoundedCornerShape(10.dp), color = if (index + 1 <= workflowStep) SenderAccentDeep else SenderSurface, modifier = Modifier.weight(1f)) { Text("${index + 1}  $label", color = if (index + 1 <= workflowStep) SenderAccent else SenderMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) } } }
    @Composable private fun Metric(label: String, value: String, modifier: Modifier) = Surface(shape = RoundedCornerShape(18.dp), color = SenderSurface, modifier = modifier) { Column(Modifier.padding(17.dp)) { Text(label, color = SenderMuted, fontSize = 12.sp); Text(value, color = SenderInk, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) } }
    @Composable private fun ReviewRow(label: String, value: String) { Column(Modifier.padding(top = 7.dp)) { Text(label.uppercase(), color = SenderMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(value, color = SenderInk, fontSize = 15.sp, modifier = Modifier.padding(top = 3.dp)) } }
    @Composable private fun SectionCard(title: String, subtitle: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) = Surface(shape = RoundedCornerShape(20.dp), color = SenderSurface, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = SenderAccent, modifier = Modifier.size(21.dp)); Spacer(Modifier.width(11.dp)); Column { Text(title, color = SenderInk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp); Text(subtitle, color = SenderMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } }; Spacer(Modifier.height(15.dp)); content() } }
    @Composable private fun EmptyState(title: String, detail: String, icon: ImageVector) = Surface(shape = RoundedCornerShape(20.dp), color = SenderSurface, modifier = Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = SenderAccent, modifier = Modifier.size(34.dp)); Text(title, color = SenderInk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.padding(top = 10.dp)); Text(detail, color = SenderMuted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 5.dp)) } }
    @Composable private fun fields() = OutlinedTextFieldDefaults.colors(focusedBorderColor = SenderAccent, unfocusedBorderColor = Color(0xFF3A4942), focusedLabelColor = SenderAccent, unfocusedLabelColor = SenderMuted, focusedTextColor = SenderInk, unfocusedTextColor = SenderInk, cursorColor = SenderAccent)
    @Composable private fun actionColors() = ButtonDefaults.buttonColors(containerColor = SenderAccent, contentColor = Color(0xFF10201B))
}

@Composable private fun NoticeFlowSenderTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = SenderCanvas, surface = SenderSurface, primary = SenderAccent, onPrimary = Color(0xFF10201B), onBackground = SenderInk, onSurface = SenderInk, onSurfaceVariant = SenderMuted), content = content)
