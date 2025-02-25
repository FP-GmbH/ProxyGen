package me.fup.joy.proxygen.sample.repository

import me.fup.joy.proxygen.core.ProxyGen
import me.fup.joy.proxygen.sample.data.User

@ProxyGen
interface UserRepository {

    var isLoggedIn: Boolean

    suspend fun getLoggedInUser(): User?

    suspend fun login(userName: String, password: String): User?

    suspend fun logout() {}

    private fun onLoginCompleted() {
        // do something
    }
}