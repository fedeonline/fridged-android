package io.github.fedeonline.fridged.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fedeonline.fridged.AppContainer
import io.github.fedeonline.fridged.ui.band.BandViewModel
import io.github.fedeonline.fridged.ui.history.HistoryViewModel
import io.github.fedeonline.fridged.ui.home.HomeScreen
import io.github.fedeonline.fridged.ui.signin.SignInScreen
import io.github.fedeonline.fridged.ui.signin.SignInViewModel
import io.github.fedeonline.fridged.ui.status.StatusViewModel
import io.github.fedeonline.fridged.widget.WidgetData
import io.github.fedeonline.fridged.widget.WidgetRefreshWorker
import java.time.Instant
import kotlinx.coroutines.flow.catch

/** Shows sign-in while signed out, the main sections otherwise. */
@Composable
fun FridgedRoot(container: AppContainer) {
    val authRepository = container.authRepository
    val user by authRepository.currentUser.collectAsStateWithLifecycle()
    val signedIn = user
    if (signedIn == null) {
        SignInScreen(viewModel { SignInViewModel(authRepository) })
    } else {
        val device by container.deviceSelection.device.collectAsStateWithLifecycle()
        // The widget follows sign-in and the device choice; opening the app also refreshes it.
        val context = LocalContext.current
        LaunchedEffect(signedIn.email, device) { WidgetRefreshWorker.refreshNow(context) }
        // While the app is visible the widget mirrors its live status (same listener: no extra reads).
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(signedIn.email, device) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.fridgeRepository.status(device)
                    .catch { /* Access or network errors: the screens report them; the worker retries. */ }
                    .collect { WidgetRefreshWorker.show(context, WidgetData.of(it, Instant.now())) }
            }
        }
        val repository = container.fridgeRepository
        // Keyed by device: switching device gets fresh view models for the other fridge.
        HomeScreen(
            email = signedIn.email,
            onSignOut = {
                authRepository.signOut()
                WidgetRefreshWorker.refreshNow(context)
            },
            device = device,
            onSelectDevice = container.deviceSelection.select,
            statusViewModel = { viewModel(key = "status-${device.id}") { StatusViewModel(repository, device) } },
            historyViewModel = { viewModel(key = "history-${device.id}") { HistoryViewModel(repository, device) } },
            bandViewModel = { viewModel(key = "band-${device.id}") { BandViewModel(repository, device) } },
        )
    }
}
