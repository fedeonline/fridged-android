package io.github.fedeonline.fridged.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    scope: CoroutineScope,
) : AuthRepository {

    override val currentUser: StateFlow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(scope, SharingStarted.Eagerly, auth.currentUser?.toUser())

    override suspend fun signIn(email: String, password: String): SignInResult = try {
        val user = checkNotNull(auth.signInWithEmailAndPassword(email, password).await().user)
        // Security rules only let `owner` read anything: refuse other accounts up front.
        val claims = user.getIdToken(true).await().claims
        if (claims[OWNER_CLAIM] == true) {
            SignInResult.SUCCESS
        } else {
            auth.signOut()
            SignInResult.NOT_AUTHORIZED
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: FirebaseAuthInvalidCredentialsException) {
        SignInResult.INVALID_CREDENTIALS
    } catch (_: FirebaseAuthInvalidUserException) {
        SignInResult.INVALID_CREDENTIALS
    } catch (_: FirebaseTooManyRequestsException) {
        SignInResult.TOO_MANY_ATTEMPTS
    } catch (_: FirebaseNetworkException) {
        SignInResult.NETWORK_ERROR
    } catch (_: Exception) {
        SignInResult.UNKNOWN_ERROR
    }

    override fun signOut() = auth.signOut()

    private fun FirebaseUser.toUser() = User(email = email.orEmpty())

    private companion object {
        const val OWNER_CLAIM = "owner"
    }
}
