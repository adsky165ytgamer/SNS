package app.sender.auth

import androidx.activity.ComponentActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import app.sender.BuildConfig
import kotlinx.coroutines.tasks.await

/**
 * Authenticated session for the Sender. Google Sign-In is preferred; if its
 * OAuth client is not configured or the provider cannot complete, Firebase
 * anonymous auth still gives the backend a verifiable ID token.
 */
class GoogleAuthSession(activity: ComponentActivity) {
    private val context = activity.applicationContext
    private val credentialManager = CredentialManager.create(activity)
    private val auth: FirebaseAuth

    init {
        FirebaseBootstrap.ensureInitialized(context)
        auth = FirebaseAuth.getInstance()
    }

    fun isGoogleConfigured(): Boolean = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank() &&
        !BuildConfig.GOOGLE_WEB_CLIENT_ID.startsWith("replace-")

    fun preferredButtonText(): String = "Sign in with email"

    fun isEmailPasswordConfigured(): Boolean = true

    suspend fun signInWithEmail(email: String, password: String): AuthenticatedIdentity {
        require(email.isNotBlank()) { "Enter the sender email address." }
        require(password.length >= 6) { "Password must contain at least 6 characters." }
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        return identity(auth.currentUser ?: error("Firebase did not return the signed-in account."), forceRefresh = true)
    }

    suspend fun createEmailAccount(email: String, password: String): AuthenticatedIdentity {
        require(email.isNotBlank()) { "Enter an email address for the Sender account." }
        require(password.length >= 6) { "Password must contain at least 6 characters." }
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
        return identity(auth.currentUser ?: error("Firebase did not return the new Sender account."), forceRefresh = true)
    }

    suspend fun sendPasswordReset(email: String) {
        require(email.isNotBlank()) { "Enter your sender email address first." }
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun current(): AuthenticatedIdentity? {
        val user = auth.currentUser ?: return null
        return identity(user, forceRefresh = false)
    }

    suspend fun signIn(): AuthenticatedIdentity {
        var googleFailure: Throwable? = null
        if (isGoogleConfigured()) {
            try {
                return googleSignIn()
            } catch (error: Throwable) {
                googleFailure = error
            }
        }
        val googlePart = googleFailure?.let { " Google: ${describe(it)}." } ?: ""
        throw IllegalStateException(
            "Google Sign-In is unavailable.$googlePart Use the Email/Password fields to sign in to the Sender.",
            googleFailure,
        )
    }

    private fun describe(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName.orEmpty()

    suspend fun signOut() {
        auth.signOut()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    private suspend fun googleSignIn(): AuthenticatedIdentity {
        auth.signOut()
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google returned an unsupported credential type."
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        auth.signInWithCredential(GoogleAuthProvider.getCredential(googleCredential.idToken, null)).await()
        return identity(auth.currentUser ?: error("Firebase did not return a signed-in Google user."), forceRefresh = true)
    }

    private suspend fun identity(user: FirebaseUser, forceRefresh: Boolean): AuthenticatedIdentity {
        val token = user.getIdToken(forceRefresh).await()?.token
            ?: error("Firebase did not return an ID token for the authenticated session.")
        return AuthenticatedIdentity(
            uid = user.uid,
            displayName = user.displayName,
            email = user.email,
            idToken = token,
            authMethod = if (user.isAnonymous) "Anonymous Auth" else if (user.providerData.any { it.providerId == "password" }) "Email/Password" else "Google Sign-In",
        )
    }
}

data class AuthenticatedIdentity(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val idToken: String,
    val authMethod: String,
)
