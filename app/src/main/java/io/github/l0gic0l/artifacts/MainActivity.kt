package io.github.l0gic0l.artifacts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.l0gic0l.artifacts.ui.App
import io.github.l0gic0l.artifacts.ui.MainViewModel
import io.github.l0gic0l.artifacts.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val vm: MainViewModel = viewModel()
                App(vm)
            }
        }
    }
}
