package com.example.duetly.models

import android.os.Parcel
import android.os.Parcelable
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class User(
    var username: String = "404",
    var email: String = "404",
    private var password: String = "404",
    var settings: UserSettings = UserSettings(),
    val melodymates: MutableList<Melodymate> = mutableListOf()
) : Parcelable {
    private var hashedPassword: String = ""

    init {
        hashedPassword = hashPassword(password)
    }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "404",
        parcel.readString() ?: "404",
    )

    private fun hashPassword(password: String): String {
        val salt = ByteArray(16)
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

    // Новий метод для отримання хешованого пароля
    fun getHashedPassword(): String {
        return hashedPassword
    }

    // Новий метод для встановлення пароля
    fun setPassword(newPassword: String) {
        password = newPassword
        hashedPassword = hashPassword(newPassword)
    }

    private fun toHex(byteArray: ByteArray): String {
        return byteArray.joinToString("") { "%02x".format(it) }
    }

    private fun fromHex(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(username)
        parcel.writeString(email)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}