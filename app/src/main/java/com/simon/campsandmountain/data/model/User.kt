package com.simon.campsandmountain.data.model

data class User(
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val role: String = "Guardaparque"
)
