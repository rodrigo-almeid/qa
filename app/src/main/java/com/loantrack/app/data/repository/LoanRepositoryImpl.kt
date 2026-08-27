package com.loantrack.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.loantrack.app.data.model.Loan
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LoanRepository {

    private fun loansCollection(userId: String) =
        firestore.collection("loans").document(userId).collection("loans")

    override fun getLoans(userId: String): Flow<List<Loan>> = callbackFlow {
        val listener = loansCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val loans = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Loan::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(loans)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getLoanById(userId: String, loanId: String): Loan? {
        return try {
            val doc = loansCollection(userId).document(loanId).get().await()
            doc.toObject(Loan::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveLoan(userId: String, loan: Loan): Result<Unit> {
        return try {
            val docRef = if (loan.id.isBlank()) {
                loansCollection(userId).document()
            } else {
                loansCollection(userId).document(loan.id)
            }
            val loanWithId = loan.copy(id = docRef.id, userId = userId)
            docRef.set(loanWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteLoan(userId: String, loanId: String): Result<Unit> {
        return try {
            loansCollection(userId).document(loanId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLoan(userId: String, loan: Loan): Result<Unit> {
        return try {
            loansCollection(userId).document(loan.id).set(loan).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllLoansOnce(userId: String): Result<List<Loan>> {
        return try {
            val snapshot = loansCollection(userId).get().await()
            val loans = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Loan::class.java)?.copy(id = doc.id)
            }
            Result.success(loans)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLoansOnce(userId: String): List<Loan> {
        return try {
            loansCollection(userId).get().await()
                .documents.mapNotNull { it.toObject(Loan::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
