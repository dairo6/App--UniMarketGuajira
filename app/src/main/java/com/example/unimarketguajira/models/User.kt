package com.example.unimarketguajira.models

data class User(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val profilePhotoUrl: String = "",
    val phoneNumber: String = "",
    val university: String = "Universidad de La Guajira",
    val role: String = "Estudiante"
)