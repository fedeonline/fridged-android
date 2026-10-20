package io.github.fedeonline.fridged

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.fedeonline.fridged.ui.FridgedRoot
import io.github.fedeonline.fridged.ui.theme.FridgedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FridgedApplication).container
        setContent {
            FridgedTheme {
                FridgedRoot(container.authRepository)
            }
        }
    }
}
