package me.fup.joy.proxygen.sample.repository

import me.fup.joy.proxygen.sample.data.User

interface UserRepository {

    suspend fun getLoggedInUser(): User?
}