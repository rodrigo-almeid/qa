package com.loantrack.app.util

import android.content.Context
import androidx.core.content.edit

object EmailConfig {
    private const val PREFS = "email_prefs"
    private const val KEY_PASSWORD = "smtp_password"
    private const val KEY_FROM = "smtp_from"
    private const val DEFAULT_FROM = "almeidainteligencia@gmail.com"
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = 587

    fun getSmtpFrom(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FROM, DEFAULT_FROM) ?: DEFAULT_FROM

    fun getSmtpHost() = SMTP_HOST
    fun getSmtpPort() = SMTP_PORT

    fun saveEmail(context: Context, email: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_FROM, email)
        }
    }

    fun savePassword(context: Context, password: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_PASSWORD, password)
        }
    }

    fun getPassword(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PASSWORD, "") ?: ""

    fun isConfigured(context: Context) = getPassword(context).isNotBlank()
}
