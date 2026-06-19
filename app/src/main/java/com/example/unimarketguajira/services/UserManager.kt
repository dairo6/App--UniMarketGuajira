package com.example.unimarketguajira.services

import android.content.Context
import com.example.unimarketguajira.data.db.UniMarketDatabase
import com.example.unimarketguajira.data.entities.UserEntity
import com.example.unimarketguajira.models.User
import com.example.unimarketguajira.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserManager {
    private const val PREFS_NAME = "UniMarketPrefs"
    private const val KEY_LOGGED_USER = "logged_user_email"

    suspend fun registerUser(context: Context, user: User): Boolean {
        val userDao = UniMarketDatabase.getDatabase(context).userDao()

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(user.email).get().await()
                if (doc.exists()) {
                    return false
                }
                db.collection("users").document(user.email).set(user).await()
                // Sync to local database
                userDao.insertUser(UserEntity.fromModel(user))
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback local registration
        val existing = userDao.getUserByEmail(user.email)
        if (existing != null) return false

        userDao.insertUser(UserEntity.fromModel(user))
        return true
    }

    suspend fun loginUser(context: Context, email: String, password: String): User? {
        val userDao = UniMarketDatabase.getDatabase(context).userDao()

        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(email).get().await()
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    if (user != null && user.password == password) {
                        // Sync to local database
                        userDao.insertUser(UserEntity.fromModel(user))
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putString(KEY_LOGGED_USER, email).apply()
                        return user
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val userEntity = userDao.login(email, password)
        
        // Initialize admin if needed
        if (userEntity == null && email == "admin@unimarket.com" && password == "123456") {
            val adminUser = User("Admin", "admin@unimarket.com", "123456")
            userDao.insertUser(UserEntity.fromModel(adminUser))
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LOGGED_USER, adminUser.email).apply()
            return adminUser
        }

        if (userEntity != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LOGGED_USER, email).apply()
            return userEntity.toModel()
        }
        return null
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LOGGED_USER).apply()
    }

    fun getLoggedUserEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LOGGED_USER, null)
    }

    suspend fun getLoggedUser(context: Context): User? {
        val email = getLoggedUserEmail(context) ?: return null
        val userDao = UniMarketDatabase.getDatabase(context).userDao()
        return userDao.getUserByEmail(email)?.toModel()
    }

    suspend fun getUserByEmail(context: Context, email: String): User? {
        val userDao = UniMarketDatabase.getDatabase(context).userDao()
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(email).get().await()
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        userDao.insertUser(UserEntity.fromModel(user))
                        return user
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return userDao.getUserByEmail(email)?.toModel()
    }

    suspend fun updateUserProfile(context: Context, email: String, newName: String): Boolean {
        val userDao = UniMarketDatabase.getDatabase(context).userDao()
        val userEntity = userDao.getUserByEmail(email) ?: return false
        val updatedUser = userEntity.toModel().copy(fullName = newName)
        return updateUser(context, updatedUser)
    }

    suspend fun updateUser(context: Context, user: User): Boolean {
        val userDao = UniMarketDatabase.getDatabase(context).userDao()
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(user.email).set(user).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        userDao.insertUser(UserEntity.fromModel(user))
        return true
    }
}