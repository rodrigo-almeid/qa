package com.loantrack.app.di

import com.loantrack.app.data.repository.LoanRepository
import com.loantrack.app.data.repository.LoanRepositoryImpl
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
    abstract fun bindLoanRepository(
        loanRepositoryImpl: LoanRepositoryImpl
    ): LoanRepository
}
