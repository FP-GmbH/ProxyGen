package me.fup.joy.proxygen.sample.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import me.fup.joy.proxygen.sample.ui.actions.LoginScreenActions
import me.fup.joy.proxygen.sample.ui.screens.LoginScreen
import me.fup.joy.proxygen.sample.ui.theme.ProxyGenTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ProxyGenTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(
                        actions = LoginScreenActions(
                            onLoginCompletedAction = {
                                Toast.makeText(this, "Login completed", Toast.LENGTH_SHORT).show()
                            }
                        ),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}