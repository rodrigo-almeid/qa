package com.loantrack.app.data.repository

import com.loantrack.app.data.model.User

interface UserRepository {
    suspend fun getCurrentUser(): User
}
