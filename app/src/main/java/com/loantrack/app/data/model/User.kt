package com.loantrack.app.data.model

enum class UserTier(val maxActiveLoans: Int) {
    BRONZE(10),
    SILVER(50),
    GOLD(Int.MAX_VALUE)
}

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val tier: String = UserTier.BRONZE.name
) {
    val userTier: UserTier get() = try {
        UserTier.valueOf(tier)
    } catch (e: IllegalArgumentException) {
        UserTier.BRONZE
    }
}
