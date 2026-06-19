package com.example.unimarketguajira.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.unimarketguajira.models.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val fullName: String,
    val password: String,
    val profilePhotoUrl: String = "",
    val phoneNumber: String = "",
    val university: String = "Universidad de La Guajira",
    val role: String = "Estudiante"
) {
    fun toModel() = User(fullName, email, password, profilePhotoUrl, phoneNumber, university, role)
    
    companion object {
        fun fromModel(user: User) = UserEntity(
            user.email,
            user.fullName,
            user.password,
            user.profilePhotoUrl,
            user.phoneNumber,
            user.university,
            user.role
        )
    }
}
