package com.example.unimarketguajira

import android.content.Context

data class User(
    val fullName: String,
    val email: String,
    val password: String
)

object UserManager {
    private const val PREFS_NAME = "UniMarketPrefs"
    private const val KEY_USERS = "users_list"

    fun registerUser(context: Context, user: User): Boolean {
        val users = getAllUsers(context).toMutableList()
        if (users.any { it.email == user.email }) return false
        
        users.add(user)
        saveUsers(context, users)
        return true
    }

    fun loginUser(context: Context, email: String, password: String): User? {
        return getAllUsers(context).find { it.email == email && it.password == password }
    }

    fun getAllUsers(context: Context): List<User> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val usersString = prefs.getString(KEY_USERS, "") ?: ""
        
        val users = mutableListOf<User>()
        // Siempre nos aseguramos de que el Admin exista si la lista está vacía
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