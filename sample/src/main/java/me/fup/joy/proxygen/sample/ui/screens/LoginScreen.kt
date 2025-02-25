package me.fup.joy.proxygen.sample.ui.screens

import android.R.attr.name
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import me.fup.joy.proxygen.sample.UserRepositoryProxyGen
import me.fup.joy.proxygen.sample.data.User
import me.fup.joy.proxygen.sample.repository.UserRepository
import me.fup.joy.proxygen.sample.ui.view.model.LoginViewModel

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val userName = viewModel.loggedInUser.value?.name ?: "nobody"

    Text(
        text = "Hello ${userName}!",
        modifier = modifier
    )
}

@Composable
@Preview
fun LoginScreenPreview() {
    val userRepository = UserRepositoryProxyGen(isLoggedIn = false, getLoggedInUserDelegate = { User.createDummy(name = "Foo") })

    LoginScreen(
        viewModel = LoginViewModel(
            userRepository = userRepository
        )
    )
}