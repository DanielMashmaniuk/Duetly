package com.example.duetly.Models

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class User(
    var username: String = "404",
    var email: String = "404",
    private var password: String = "404"
) {
    val hashedPassword: String

    init {
        hashedPassword = hashPassword(password)
    }

    private fun hashPassword(password: String): String {
        val salt = ByteArray(16) // Сіль для додаткової безпеки
        val random = SecureRandom()
        random.nextBytes(salt)

        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 64 * 8)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val hash = skf.generateSecret(spec).encoded

        return "${toHex(salt)}:${toHex(hash)}"
    }

    fun validatePassword(inputPassword: String): Boolean {
        val parts = hashedPassword.split(":")
        val salt = fromHex(parts[0])
        val hash = fromHex(parts[1])

        val spec = PBEKeySpec(inputPassword.toCharArray(), salt, 10000, hash.size * 8)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val inputHash = skf.generateSecret(spec).encoded

        return inputHash.contentEquals(hash)
    }

    private fun toHex(byteArray: ByteArray): String {
        val sb = StringBuilder(byteArray.size * 2)
        for (b in byteArray) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun fromHex(hex: String): ByteArray {
        val bytes = ByteArray(hex.length / 2)
        for (i in bytes.indices) {
            bytes[i] = hex.substring(2 * i, 2 * i + 2).toInt(16).toByte()
        }
        return bytes
    }
}
