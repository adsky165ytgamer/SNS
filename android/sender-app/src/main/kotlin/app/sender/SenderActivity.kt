package app.sender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private enum class SenderTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home), HISTORY("Notices", Icons.Default.History),
    RECEIVERS("Receivers", Icons.Default.Devices), SETTINGS("Settings", Icons.Default.Settings)
}

private val SenderInk = Color(0xFFEDEDED)
private val SenderMuted = Color(0xFF9EA4AA)
private val SenderPanel = Color(0xFF151719)
private val SenderAccent = Color(0xFF8DE8C9)
private val SenderAccentDark = Color(0xFF173A31)

class SenderActivity : ComponentActivity() {
    private val authSession by lazy { GoogleAuthSession(this) }
    private val history by lazy { SenderHistory(applicationContext) }
    private var authIdentity by mutableStateOf<AuthenticatedIdentity?>(null)
    private val receivers = mutableStateListOf<LiveReceiver>()
    private var selectedReceiver by mutableStateOf<LiveReceiver?>(null)
    private var tab by mutableStateOf(SenderTab.HOME)
    private var composing by mutableStateOf(false)
    private var email by mutableStateOf("")
    private var password by mutableStateOf("")
    private var title by mutableStateOf("")
    private var body by mutableStateOf("")
    private var receiverQuery by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var loadingReceivers by mutableStateOf(false)
    private var noticeType by mutableStateOf("Information")
    private var statusTitle by mutableStateOf("Sender ready")
    private var statusDetail by mutableStateOf("Sign in, refresh live Receivers, then send a notice.")
    private var historyRevision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        setContent { NoticeFlowSenderTheme { SenderShell() } }
        lifecycleScope.launch { authIdentity = runCatching { authSession.current() }.getOrNull(); if (authIdentity != null) loadReceivers() }
    }

    @Composable private fun SenderShell() = Surface(Modifier.fillMaxSize(), color = Color(0xFF0E1011)) {
        Column(Modifier.fillMaxSize()) {
            TopBar()
            Box(Modifier.weight(1f)) {
                AnimatedContent(if (composing) null else tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "sender-content") { current ->
                    if (current == null) ComposerScreen() else when (current) {
                        SenderTab.HOME -> HomeScreen(); SenderTab.HISTORY -> HistoryScreen(); SenderTab.RECEIVERS -> ReceiversScreen(); SenderTab.SETTINGS -> SettingsScreen()
                    }
                }
            }
            if (!composing) NavigationBar(containerColor = Color(0xFF111315), tonalElevation = 0.dp) {
                SenderTab.entries.forEach { item -> NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label) }, colors = navColors()) }
            }
        }
    }

    @Composable private fun TopBar() = Surface(color = Color(0xFF111315)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("NOTICEFLOW", color = SenderAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp); Text(authIdentity?.displayName?.takeIf { it.isNotBlank() } ?: authIdentity?.email?.substringBefore('@') ?: "Sender", color = SenderInk, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            StatusPill()
        }
    }
    @Composable private fun StatusPill() { val ready = authIdentity != null; Surface(shape = RoundedCornerShape(50), color = if (ready) SenderAccentDark else Color(0xFF282C30)) { Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(7.dp).clip(CircleShape).background(if (ready) SenderAccent else Color(0xFFFFC86B))); Spacer(Modifier.width(7.dp)); Text(if (ready) "Ready" else "Sign in", color = if (ready) SenderAccent else SenderInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } } }

    @Composable private fun HomeScreen() = LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Heading("Good to see you.", "Create clear notices for your registered Receiver screens.") }
        item { Surface(shape = RoundedCornerShape(24.dp), color = SenderPanel) { Column(Modifier.padding(20.dp)) { Text("NEW SCHOOL NOTICE", color = SenderAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("Need to tell a class something?", color = SenderInk, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)); Text("Choose a live destination, write the notice, and send it through the authenticated delivery system.", color = SenderMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)); Spacer(Modifier.height(16.dp)); Button(onClick = { composing = true }, modifier = Modifier.fillMaxWidth(), colors = actionColors()) { Text("Create notice", fontWeight = FontWeight.Bold) } } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Stat("Receivers", receivers.size.toString(), Modifier.weight(1f)); Stat("Sent", history.items().size.toString(), Modifier.weight(1f)) } }
        item { SectionCard("Live delivery", statusTitle, Icons.Default.Cloud) { Text(statusDetail, color = SenderMuted, fontSize = 14.sp); Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = { loadReceivers() }, enabled = !loadingReceivers) { Text(if (loadingReceivers) "Refreshing…" else "Refresh live Receivers") } } }
    }

    @Composable private fun ComposerScreen() = LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ArrowBack, "Back", tint = SenderInk, modifier = Modifier.size(28.dp).clickable { composing = false }); Spacer(Modifier.width(12.dp)); Heading("Create notice", "Write a clear message for one real Receiver.") } }
        item { SectionCard("Destination", selectedReceiver?.label ?: "Choose a Receiver", Icons.Default.Devices) { Text(if (selectedReceiver == null) "Select an authenticated live destination before sending." else "This notice will be sent to the selected registered device.", color = SenderMuted, fontSize = 14.sp); Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = { tab = SenderTab.RECEIVERS; composing = false }) { Text(if (selectedReceiver == null) "Choose Receiver" else "Change Receiver") } } }
        item { SectionCard("Message", "Compose a school notice", Icons.Default.Edit) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Homework", "Important", "Information").forEach { type -> FilterChip(selected = noticeType == type, onClick = { noticeType = type }, label = { Text(type) }) } }
            Spacer(Modifier.height(12.dp)); OutlinedTextField(title, { title = it.take(140) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), colors = fields())
            Spacer(Modifier.height(10.dp)); OutlinedTextField(body, { body = it.take(4000) }, label = { Text("Description") }, minLines = 5, modifier = Modifier.fillMaxWidth(), colors = fields())
            Spacer(Modifier.height(14.dp)); Button(onClick = { sendNotice() }, enabled = selectedReceiver != null && !busy, modifier = Modifier.fillMaxWidth(), colors = actionColors()) { Text(if (busy) "Sending…" else "Send notice", fontWeight = FontWeight.Bold) }
        } }
    }

    @Composable private fun ReceiversScreen() = LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Heading("Receivers", "Live destinations from the authenticated NoticeFlow backend.") }
        item { OutlinedTextField(receiverQuery, { receiverQuery = it }, label = { Text("Search Receivers") }, modifier = Modifier.fillMaxWidth(), colors = fields()) }
        item { OutlinedButton(onClick = { loadReceivers() }, enabled = !loadingReceivers, modifier = Modifier.fillMaxWidth()) { Text(if (loadingReceivers) "Refreshing…" else "Refresh live Receivers") } }
        if (authIdentity == null) item { Empty("Sign in to load Receivers", "Your Sender account is required to access the live device list.") }
        else if (loadingReceivers) item { Empty("Loading live Receivers", "Contacting the authenticated NoticeFlow backend…") }
        else if (receivers.isEmpty()) item { Empty("No Receivers found", "Install, sign in, name, and connect a Receiver first.") }
        else items(receivers.filter { it.label.contains(receiverQuery, true) || it.receiverId.contains(receiverQuery, true) }, key = { it.receiverId }) { receiver -> ReceiverCard(receiver) }
    }
    @Composable private fun ReceiverCard(receiver: LiveReceiver) = Surface(shape = RoundedCornerShape(22.dp), color = if (selectedReceiver?.receiverId == receiver.receiverId) SenderAccentDark else SenderPanel, modifier = Modifier.fillMaxWidth().clickable { selectedReceiver = receiver; statusTitle = "Receiver selected"; statusDetail = "${receiver.label} is ready for your next notice." }) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(if (receiver.lastSeenAt != null) SenderAccent else SenderMuted)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(receiver.label, color = SenderInk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp); Text(if (receiver.lastSeenAt != null) "Recently seen" else "Registered device", color = SenderMuted, fontSize = 12.sp) }; if (selectedReceiver?.receiverId == receiver.receiverId) Icon(Icons.Default.Check, null, tint = SenderAccent) } }

    @Composable private fun HistoryScreen() { val records = remember(historyRevision) { history.items() }; LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Heading("Notice history", "Every accepted dispatch saved on this Sender.") }; if (records.isEmpty()) item { Empty("Nothing here yet", "Your successfully sent notices will appear here.") }; items(records, key = { it.messageId + it.sentAt }) { record -> Surface(shape = RoundedCornerShape(22.dp), color = SenderPanel) { Column(Modifier.padding(18.dp)) { Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.sentAt)), color = SenderAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(record.title, color = SenderInk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp)); Text(record.body, color = SenderMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 5.dp)); Text("To • ${record.receiverName}", color = SenderAccent, fontSize = 12.sp, modifier = Modifier.padding(top = 9.dp)) } } } } }

    @Composable private fun SettingsScreen() = LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Heading("Settings", "Account, delivery connection, and application details.") }; item { AccountCard() }; item { SectionCard("Backend", SenderBackendClient.endpointLabel(), Icons.Default.Cloud) { Text("Delivery uses authenticated HTTPS backend calls and Firebase Cloud Messaging.", color = SenderMuted, fontSize = 14.sp) } }; item { SectionCard("About NoticeFlow", "Sender v1.1.3 Beta", Icons.Default.Info) { Text("Material 3 v2 Sender interface with live Receiver controls.", color = SenderMuted, fontSize = 14.sp) } } }
    @Composable private fun AccountCard() = SectionCard("School account", authIdentity?.email ?: "Sign in to send live notices", Icons.Default.AccountCircle) { if (authIdentity == null) { OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), colors = fields()); Spacer(Modifier.height(10.dp)); OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = fields()); Spacer(Modifier.height(12.dp)); Button(onClick = { signIn() }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = actionColors()) { Text(if (busy) "Signing in…" else "Sign in", fontWeight = FontWeight.Bold) }; Row { TextButton(onClick = { createAccount() }, enabled = !busy) { Text("Create account", color = SenderAccent) }; TextButton(onClick = { resetPassword() }, enabled = !busy) { Text("Reset password", color = SenderAccent) } } } else { Text("Signed in as ${authIdentity?.email ?: "school account"}.", color = SenderInk, fontSize = 14.sp); Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = { signOut() }, enabled = !busy) { Text("Sign out") } } }

    private fun loadReceivers() = lifecycleScope.launch { val token = authIdentity?.idToken ?: return@launch; loadingReceivers = true; runCatching { SenderBackendClient.loadReceivers(token) }.onSuccess { receivers.clear(); receivers.addAll(it); statusTitle = "Live Receivers loaded"; statusDetail = "${it.size} available Receiver${if (it.size == 1) "" else "s"}." }.onFailure { statusTitle = "Could not load Receivers"; statusDetail = it.message ?: "The backend request failed." }; loadingReceivers = false }
    private fun sendNotice() = lifecycleScope.launch { val target = selectedReceiver ?: return@launch; if (title.isBlank() || body.isBlank()) { statusTitle = "Notice details required"; statusDetail = "Enter both a title and description."; return@launch }; val token = authIdentity?.idToken ?: run { tab = SenderTab.SETTINGS; composing = false; statusTitle = "Account required"; return@launch }; busy = true; runCatching { SenderBackendClient.sendTestNotice(target.receiverId, title.trim(), body.trim(), token) }.onSuccess { id -> history.record(target, title.trim(), body.trim(), id); historyRevision++; statusTitle = "Notice sent"; statusDetail = "Accepted for ${target.label}."; title = ""; body = ""; composing = false; tab = SenderTab.HISTORY }.onFailure { statusTitle = "Could not send notice"; statusDetail = it.message ?: "The backend request failed." }; busy = false }
    private fun signIn() = lifecycleScope.launch { busy = true; runCatching { authSession.signInWithEmail(email.trim(), password) }.onSuccess { authIdentity = it; password = ""; statusTitle = "Account connected"; statusDetail = "Refresh live Receivers to select a destination."; loadReceivers() }.onFailure { statusTitle = "Sign-in failed"; statusDetail = it.message ?: "Firebase authentication did not complete." }; busy = false }
    private fun createAccount() = lifecycleScope.launch { busy = true; runCatching { authSession.createEmailAccount(email.trim(), password) }.onSuccess { authIdentity = it; password = ""; statusTitle = "Account created"; statusDetail = "Refresh live Receivers to select a destination."; loadReceivers() }.onFailure { statusTitle = "Could not create account"; statusDetail = it.message ?: "Firebase authentication did not complete." }; busy = false }
    private fun resetPassword() = lifecycleScope.launch { busy = true; runCatching { authSession.sendPasswordReset(email.trim()) }.onSuccess { statusTitle = "Reset email sent"; statusDetail = "Check the inbox for $email." }.onFailure { statusTitle = "Reset failed"; statusDetail = it.message ?: "Could not send reset email." }; busy = false }
    private fun signOut() = lifecycleScope.launch { busy = true; authSession.signOut(); authIdentity = null; selectedReceiver = null; receivers.clear(); statusTitle = "Signed out"; statusDetail = "Sign in again before loading Receivers or sending notices."; busy = false }

    @Composable private fun Heading(title: String, subtitle: String) = Column { Text(title, color = SenderInk, fontSize = 29.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = SenderMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 5.dp)) }
    @Composable private fun Stat(label: String, value: String, modifier: Modifier) = Surface(shape = RoundedCornerShape(20.dp), color = SenderPanel, modifier = modifier) { Column(Modifier.padding(16.dp)) { Text(label, color = SenderMuted, fontSize = 12.sp); Text(value, color = SenderInk, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) } }
    @Composable private fun SectionCard(title: String, subtitle: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) = Surface(shape = RoundedCornerShape(24.dp), color = SenderPanel, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = SenderAccent); Spacer(Modifier.width(11.dp)); Column { Text(title, color = SenderInk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp); Text(subtitle, color = SenderMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } }; Spacer(Modifier.height(16.dp)); content() } }
    @Composable private fun Empty(title: String, detail: String) = Surface(shape = RoundedCornerShape(24.dp), color = SenderPanel) { Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Info, null, tint = SenderAccent); Text(title, color = SenderInk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.padding(top = 10.dp)); Text(detail, color = SenderMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp)) } }
    @Composable private fun fields() = OutlinedTextFieldDefaults.colors(focusedBorderColor = SenderAccent, unfocusedBorderColor = Color(0xFF3B4145), focusedLabelColor = SenderAccent, unfocusedLabelColor = SenderMuted, focusedTextColor = SenderInk, unfocusedTextColor = SenderInk, cursorColor = SenderAccent)
    @Composable private fun actionColors() = ButtonDefaults.buttonColors(containerColor = SenderAccent, contentColor = Color(0xFF10201B))
    @Composable private fun navColors() = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF10201B), indicatorColor = SenderAccent, selectedTextColor = SenderAccent, unselectedIconColor = SenderMuted, unselectedTextColor = SenderMuted)
}

@Composable private fun NoticeFlowSenderTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0E1011), surface = SenderPanel, primary = SenderAccent, onPrimary = Color(0xFF10201B), onBackground = SenderInk, onSurface = SenderInk, onSurfaceVariant = SenderMuted), content = content)
