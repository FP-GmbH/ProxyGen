package me.fup.joy.proxygen.sample.repository

import me.fup.joy.proxygen.sample.data.User
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor() : UserRepository {

    override suspend fun getLoggedInUser(): User? {
        // no logged in user
        return null
    }
}