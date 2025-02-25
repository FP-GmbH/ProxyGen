package me.fup.joy.proxygen.sample.repository

import me.fup.joy.proxygen.sample.data.User
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor() : UserRepository {

    override var isLoggedIn = false

    override suspend fun getLoggedInUser(): User? {
        return if (isLoggedIn) User.createDummy() else null
    }

    override suspend fun login(userName: String, password: String): User? {
        isLoggedIn = true
        return User.createDummy()
    }

    override suspend fun logout() {
        isLoggedIn = false
    }
}