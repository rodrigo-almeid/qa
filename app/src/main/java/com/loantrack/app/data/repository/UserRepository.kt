package com.loantrack.app.data.repository

import com.loantrack.app.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getCurrentUser(): User
    fun getUserFlow(userId: String): Flow<User>
}
