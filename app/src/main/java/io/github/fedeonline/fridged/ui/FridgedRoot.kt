package io.github.fedeonline.fridged.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fedeonline.fridged.AppContainer
import io.github.fedeonline.fridged.ui.home.HomeScreen
import io.github.fedeonline.fridged.ui.band.BandViewModel
import io.github.fedeonline.fridged.ui.history.HistoryViewModel
import io.github.fedeonline.fridged.ui.signin.SignInScreen
import io.github.fedeonline.fridged.ui.signin.SignInViewModel
import io.github.fedeonline.fridged.ui.status.StatusViewModel

/** Shows sign-in while signed out, the main sections otherwise. */
@Composable
fun FridgedRoot(container: AppContainer) {
    val authRepository = container.authRepository
    val user by authRepository.currentUser.collectAsStateWithLifecycle()
    val signedIn = user
    if (signedIn == null) {
        SignInScreen(viewModel { SignInViewModel(authRepository) })
    } else {
        HomeScreen(
            email = signedIn.email,
            onSignOut = authRepository::signOut,
            statusViewModel = { viewModel { StatusViewModel(container.fridgeRepository, container.device) } },
            historyViewModel = { viewModel { HistoryViewModel(container.fridgeRepository, container.device) } },
            bandViewModel = { viewModel { BandViewModel(container.fridgeRepository, container.device) } },
        )
    }
}
