package com.example.data.model

data class UserAccount(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isGuest: Boolean = false,
    val isAuthenticated: Boolean = false
) {
    companion object {
        val Guest = UserAccount(
            id = "guest_user",
            displayName = "Гостевой игрок",
            email = "guest@gamelingo.local",
            photoUrl = null,
            isGuest = true,
            isAuthenticated = true
        )
    }
}
