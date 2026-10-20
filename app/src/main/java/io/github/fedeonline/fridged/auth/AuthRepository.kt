package io.github.fedeonline.fridged.auth

import kotlinx.coroutines.flow.StateFlow

data class User(val email: String)

enum class SignInResult {
    SUCCESS,
    INVALID_CREDENTIALS,

    /** Credentials are valid but the account lacks the `owner` custom claim. */
    NOT_AUTHORIZED,
    TOO_MANY_ATTEMPTS,
    NETWORK_ERROR,
    UNKNOWN_ERROR,
}

interface AuthRepository {
    /** The signed-in user, or null when signed out. */
    val currentUser: StateFlow<User?>

    suspend fun signIn(email: String, password: String): SignInResult

    fun signOut()
}
