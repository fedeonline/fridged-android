package io.github.fedeonline.fridged.ui.signin

import io.github.fedeonline.fridged.auth.FakeAuthRepository
import io.github.fedeonline.fridged.auth.SignInResult
import io.github.fedeonline.fridged.auth.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    private val auth = FakeAuthRepository()
    private lateinit var viewModel: SignInViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = SignInViewModel(auth)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `cannot submit with empty fields`() {
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.onEmailChange("me@example.com")
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `successful sign-in sets the session and clears the password`() {
        viewModel.onEmailChange(" me@example.com ")
        viewModel.onPasswordChange("secret")
        viewModel.signIn()

        assertEquals(User("me@example.com"), auth.currentUser.value)
        assertEquals("", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `failed sign-in shows the error until the user edits a field`() {
        auth.nextResult = SignInResult.NOT_AUTHORIZED
        viewModel.onEmailChange("me@example.com")
        viewModel.onPasswordChange("secret")
        viewModel.signIn()

        assertNull(auth.currentUser.value)
        assertEquals(SignInResult.NOT_AUTHORIZED, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.submitting)

        viewModel.onPasswordChange("other")
        assertNull(viewModel.uiState.value.error)
    }
}
