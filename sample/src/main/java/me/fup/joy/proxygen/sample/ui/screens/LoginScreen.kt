package me.fup.joy.proxygen.sample.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val userRepository = object : UserRepository {
        override suspend fun getLoggedInUser(): User? = User.createDummy(name = "somebody")
    }

    LoginScreen(
        viewModel = LoginViewModel(
            userRepository = userRepository
        )
    )
}