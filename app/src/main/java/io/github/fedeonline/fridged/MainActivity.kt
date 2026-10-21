package io.github.fedeonline.fridged

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.fedeonline.fridged.ui.FridgedRoot
import io.github.fedeonline.fridged.ui.theme.FridgedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always dark: light system-bar icons on the dark background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = (application as FridgedApplication).container
        setContent {
            FridgedTheme {
                FridgedRoot(container)
            }
        }
    }
}
