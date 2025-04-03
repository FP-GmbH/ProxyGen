package me.fup.joy.proxygen.sample.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.fup.joy.proxygen.sample.UserRepositoryProxy
import me.fup.joy.proxygen.sample.data.User
import me.fup.joy.proxygen.sample.ui.LoginScreenActionsProxy
import me.fup.joy.proxygen.sample.ui.actions.LoginScreenActions
import me.fup.joy.proxygen.sample.ui.view.model.LoginViewModel

@Composable
fun LoginScreen(
    actions: LoginScreenActions,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val userName = viewModel.loggedInUser.value?.name ?: "not logged in"

    fun onLoginClicked() {
        viewModel.login {
            actions.onLoginCompleted()
        }
    }

    Scaffold {
        Column(
            modifier = modifier.padding(it)
        ) {
            Text(
                text = "Hello $userName",
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = ::onLoginClicked) {
                Text(
                    text = "Login",
                )
            }
        }
    }
}

@Composable
@Preview
fun LoginScreenPreview() {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val userRepository = UserRepositoryProxy(
        isLoggedInDelegate = false,
        getLoggedInUserDelegate = { null },
        loginDelegate = { userName, password -> User.createDummy(name = "loggedInUser") }
    )

    val loginScreenActions = LoginScreenActionsProxy(
        onLoginCompletedAction = { /* ignore */ },
        onLoginCompletedDelegate = { coroutineScope.launch { snackbarHostState.showSnackbar("onLoginCompleted") } }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        LoginScreen(
            actions = loginScreenActions,
            viewModel = LoginViewModel(
                userRepository = userRepository
            ),
            modifier = Modifier.padding(it),
        )
    }
}