package com.example.gameguesser.data

import java.security.MessageDigest

object PasswordUtils {

    fun hash(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
