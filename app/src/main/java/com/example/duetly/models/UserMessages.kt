package com.example.duetly.models

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.UUID

data class UserMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderUsername: String? = null,  // Make nullable if needed
    val text: String? = null,             // Make nullable if needed
    val type: String? = null,             // Make nullable if needed
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {

    // Відправка повідомлення
    fun sendMessage(receivedUser: String, message: UserMessage, db: FirebaseFirestore) {
        db.collection("messages").document("$receivedUser-messages")
            .collection("userMessages")
            .add(message)
            .addOnSuccessListener {
                println("Повідомлення успішно надіслано")
            }
            .addOnFailureListener { e ->
                println("Помилка надсилання повідомлення: ${e.message}")
            }
    }


    // Прослуховування змін у реальному часі


    // Видалення повідомлення
    fun deleteMessage(receivedUser: String, messageId: String,db: FirebaseFirestore) {
        db.collection("messages").document("$receivedUser-messages")
            .collection("userMessages").document(messageId)
            .delete()
            .addOnSuccessListener {
                println("Повідомлення успішно видалено")
            }
            .addOnFailureListener { e ->
                println("Помилка видалення повідомлення: ${e.message}")
            }
    }
}
