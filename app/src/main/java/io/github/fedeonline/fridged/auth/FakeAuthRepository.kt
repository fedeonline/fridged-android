package io.github.fedeonline.fridged.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [AuthRepository] for previews and tests. */
class FakeAuthRepository(
    initialUser: User? = null,
    var nextResult: SignInResult = SignInResult.SUCCESS,
) : AuthRepository {
    private val user = MutableStateFlow(initialUser)
    override val currentUser: StateFlow<User?> = user

    override suspend fun signIn(email: String, password: String): SignInResult {
        if (nextResult == SignInResult.SUCCESS) user.value = User(email)
        return nextResult
    }

    override fun signOut() {
        user.value = null
    }
}
