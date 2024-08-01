package com.example.duetly.models

import com.google.firebase.database.FirebaseDatabase

class UserSettings(
    var showEmail:Boolean = true
) {
    fun updateShowEmail(userId: String, newShowEmail: Boolean) {
        val database = FirebaseDatabase.getInstance()
        val userSettingsRef = database.getReference("users").child(userId).child("settings")

        // Оновлення значення showEmail
        userSettingsRef.child("showEmail").setValue(newShowEmail)
            .addOnSuccessListener {
                // Успішне оновлення
                println("User showEmail updated successfully.")
            }
            .addOnFailureListener { e ->
                // Помилка при оновленні
                println("Error updating user showEmail: $e")
            }
    }
}