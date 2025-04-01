package me.fup.joy.proxygen.sample.ui.view.model

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.fup.joy.proxygen.sample.data.User
import me.fup.joy.proxygen.sample.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val loggedInUser = mutableStateOf<User?>(null)

    init {
        viewModelScope.launch {
            loggedInUser.value = userRepository.getLoggedInUser()
        }
    }

    fun login(successCallback: () -> Unit) {
        viewModelScope.launch {
            try {
                loggedInUser.value = userRepository.login(userName = "SuperUser", password = "password")
                successCallback()
            } catch (ignore: Exception) { }
        }
    }
}