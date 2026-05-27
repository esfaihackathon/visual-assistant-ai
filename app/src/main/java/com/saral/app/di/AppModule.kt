package com.saral.app.di

import com.saral.app.data.mock.MockBankingRepository
import com.saral.app.data.repository.BankingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindBankingRepository(impl: MockBankingRepository): BankingRepository
}
