package com.example.duetly.models

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

data class Melodymate(
    val name: String,
    val status: String,
    val dateOfAcquaintance: Long = System.currentTimeMillis()
) {
    fun addMelodymateToDatabase(userId: String, newMelodymate: Melodymate,firebaseDatabase: FirebaseDatabase) {
        val userReference: DatabaseReference = firebaseDatabase.getReference("users").child(userId)

        // Додати новий melodymate до списку melodymates
        userReference.child("melodymates").push().setValue(newMelodymate)
            .addOnSuccessListener {
                println("Melodymate added successfully.")
            }
            .addOnFailureListener { exception ->
                println("Failed to add melodymate: ${exception.message}")
            }
    }
}