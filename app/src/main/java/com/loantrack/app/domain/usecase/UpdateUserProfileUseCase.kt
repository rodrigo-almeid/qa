package com.loantrack.app.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(displayName: String): Result<Unit> = try {
        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        auth.currentUser!!.updateProfile(request).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
