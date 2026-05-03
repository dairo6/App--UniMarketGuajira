package com.example.unimarketguajira.services

import android.content.Context
import com.example.unimarketguajira.models.User

object UserManager {
    private const val PREFS_NAME = "UniMarketPrefs"
    private const val KEY_USERS = "users_list"
    private const val KEY_LOGGED_USER = "logged_user_email"

    fun registerUser(context: Context, user: User): Boolean {
        val users = getAllUsers(context).toMutableList()
        if (users.any { it.email == user.email }) return false
        
        users.add(user)
        saveUsers(context, users)
        return true
    }

    fun loginUser(context: Context, email: String, password: String): User? {
        val user = getAllUsers(context).find { it.email == email && it.password == password }
        if (user != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LOGGED_USER, email).apply()
        }
        return user
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LOGGED_USER).apply()
    }

    fun getLoggedUser(context: Context): User? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val email = prefs.getString(KEY_LOGGED_USER, null) ?: return null
        return getAllUsers(context).find { it.email == email }
    }

    fun getAllUsers(context: Context): List<User> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val usersString = prefs.getString(KEY_USERS, "") ?: ""
        
        val users = mutableListOf<User>()
        if (usersString.isEmpty()) {
            val admin = User("Admin", "admin@unimarket.com", "123456")
            users.add(admin)
            saveUsers(context, users)
            return users
        }
        
        return usersString.split(";").filter { it.isNotEmpty() }.map {
            val parts = it.split("|")
            User(parts[0], parts[1], parts[2])
        }
    }

    private fun saveUsers(context: Context, users: List<User>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val usersString = users.joinToString(";") { "${it.fullName}|${it.email}|${it.password}" }
        prefs.edit().putString(KEY_USERS, usersString).apply()
    }
}