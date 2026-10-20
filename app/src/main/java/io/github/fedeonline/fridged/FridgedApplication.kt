package io.github.fedeonline.fridged

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import io.github.fedeonline.fridged.auth.AuthRepository
import io.github.fedeonline.fridged.auth.FirebaseAuthRepository
import io.github.fedeonline.fridged.model.Device
import kotlinx.coroutines.MainScope

class FridgedApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            authRepository = FirebaseAuthRepository(Firebase.auth, MainScope()),
            device = Device.fromId(BuildConfig.DEFAULT_DEVICE_ID),
        )
    }
}

/** Manual dependency container: the app is small enough not to need a DI framework. */
class AppContainer(
    val authRepository: AuthRepository,
    val device: Device,
)
