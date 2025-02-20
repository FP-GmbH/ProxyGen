package me.fup.joy.proxygen.sample.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.fup.joy.proxygen.sample.repository.UserRepository
import me.fup.joy.proxygen.sample.repository.UserRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class MainModule {

    @Binds
    abstract fun bindUserRepository(userRepository: UserRepositoryImpl): UserRepository
}