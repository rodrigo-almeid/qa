package com.loantrack.app.util

import android.content.Context
import androidx.core.content.edit

object SecurityPrefs {
    private const val PREFS = "security_prefs"
    private const val KEY_TERMS_ACCEPTED = "terms_accepted"
    private const val KEY_TERMS_ACCEPTED_AT = "terms_accepted_at"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCK_UNTIL = "lock_until"

    const val MAX_ATTEMPTS = 5
    const val LOCK_DURATION_MS = 15 * 60 * 1000L // 15 minutos

    fun isTermsAccepted(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_TERMS_ACCEPTED, false)

    fun acceptTerms(ctx: Context) {
        prefs(ctx).edit {
            putBoolean(KEY_TERMS_ACCEPTED, true)
            putLong(KEY_TERMS_ACCEPTED_AT, System.currentTimeMillis())
        }
    }

    fun isBiometricEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit { putBoolean(KEY_BIOMETRIC_ENABLED, enabled) }
    }

    fun recordFailedAttempt(ctx: Context): Int {
        val current = getFailedAttempts(ctx) + 1
        prefs(ctx).edit {
            putInt(KEY_FAILED_ATTEMPTS, current)
            if (current >= MAX_ATTEMPTS) {
                putLong(KEY_LOCK_UNTIL, System.currentTimeMillis() + LOCK_DURATION_MS)
            }
        }
        return current
    }

    fun clearFailedAttempts(ctx: Context) {
        prefs(ctx).edit {
            putInt(KEY_FAILED_ATTEMPTS, 0)
            putLong(KEY_LOCK_UNTIL, 0L)
        }
    }

    fun isLocked(ctx: Context): Boolean {
        val lockUntil = prefs(ctx).getLong(KEY_LOCK_UNTIL, 0L)
        return lockUntil > System.currentTimeMillis()
    }

    fun lockRemainingSeconds(ctx: Context): Long {
        val lockUntil = prefs(ctx).getLong(KEY_LOCK_UNTIL, 0L)
        return maxOf(0L, (lockUntil - System.currentTimeMillis()) / 1000)
    }

    fun getFailedAttempts(ctx: Context): Int =
        prefs(ctx).getInt(KEY_FAILED_ATTEMPTS, 0)

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
