package io.github.fedeonline.fridged.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fedeonline.fridged.auth.AuthRepository
import io.github.fedeonline.fridged.auth.SignInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    /** Outcome of the last failed attempt; null when there is nothing to show. */
    val error: SignInResult? = null,
) {
    val canSubmit: Boolean get() = !submitting && email.isNotBlank() && password.isNotEmpty()
}

class SignInViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val state = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = state.asStateFlow()

    fun onEmailChange(email: String) = state.update { it.copy(email = email, error = null) }

    fun onPasswordChange(password: String) = state.update { it.copy(password = password, error = null) }

    fun signIn() {
        val current = state.value
        if (!current.canSubmit) return
        state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(current.email.trim(), current.password)
            // On success the session flow swaps this screen out; nothing else to do.
            state.update {
                it.copy(
                    submitting = false,
                    password = if (result == SignInResult.SUCCESS) "" else it.password,
                    error = result.takeIf { r -> r != SignInResult.SUCCESS },
                )
            }
        }
    }
}
