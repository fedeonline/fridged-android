package io.github.fedeonline.fridged.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fedeonline.fridged.auth.AuthRepository
import io.github.fedeonline.fridged.ui.home.HomeScreen
import io.github.fedeonline.fridged.ui.signin.SignInScreen
import io.github.fedeonline.fridged.ui.signin.SignInViewModel

/** Shows sign-in while signed out, the main sections otherwise. */
@Composable
fun FridgedRoot(authRepository: AuthRepository) {
    val user by authRepository.currentUser.collectAsStateWithLifecycle()
    val signedIn = user
    if (signedIn == null) {
        SignInScreen(viewModel { SignInViewModel(authRepository) })
    } else {
        HomeScreen(email = signedIn.email, onSignOut = authRepository::signOut)
    }
}
