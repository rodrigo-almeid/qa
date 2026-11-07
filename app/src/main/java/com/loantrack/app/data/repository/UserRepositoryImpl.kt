package com.loantrack.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.loantrack.app.data.model.User
import com.loantrack.app.data.model.UserTier
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    private fun usersCollection() = firestore.collection("users")

    override suspend fun getCurrentUser(): User {
        val uid = auth.currentUser?.uid ?: return User()
        return try {
            val doc = usersCollection().document(uid).get().await()
            if (doc.exists()) {
                doc.toObject(User::class.java)?.copy(id = doc.id) ?: createDefaultUser(uid)
            } else {
                createDefaultUser(uid)
            }
        } catch (e: Exception) {
            User(id = uid)
        }
    }

    private suspend fun createDefaultUser(uid: String): User {
        val user = User(
            id = uid,
            email = auth.currentUser?.email ?: "",
            displayName = auth.currentUser?.displayName ?: "",
            tier = UserTier.BRONZE.name
        )
        return try {
            usersCollection().document(uid).set(user).await()
            user
        } catch (e: Exception) {
            user
        }
    }
}
