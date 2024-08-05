package com.example.duetly.models

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.UUID

class UserMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderUsername: String,
    val text: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Відправка повідомлення
    fun sendMessage(receivedUser: String, message: UserMessage) {
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
    fun setMessageListener(receivedUser: String, onMessageReceived: (UserMessage) -> Unit) {
        db.collection("messages").document("$receivedUser-messages")
            .collection("userMessages")
            .orderBy("timestamp", Query.Direction.ASCENDING) // Сортування за часом
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Помилка прослуховування повідомлень: ${e.message}")
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            val message = dc.document.toObject(UserMessage::class.java)
                            onMessageReceived(message)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val message = dc.document.toObject(UserMessage::class.java)
                            println("Повідомлення змінено: ${message.text}, прочитано: ${message.isRead}")
                        }
                        DocumentChange.Type.REMOVED -> {
                            val message = dc.document.toObject(UserMessage::class.java)
                            println("Повідомлення видалено: ${message.text}")
                        }
                    }
                }
            }
    }

    // Видалення повідомлення
    fun deleteMessage(receivedUser: String, messageId: String) {
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

    // Отримання всіх повідомлень
    fun getAllMessages(receivedUser: String, onComplete: (List<UserMessage>) -> Unit) {
        db.collection("messages").document("$receivedUser-messages")
            .collection("userMessages")
            .orderBy("timestamp", Query.Direction.ASCENDING) // Сортування за часом
            .get()
            .addOnSuccessListener { result ->
                val messages = result.map { document ->
                    document.toObject(UserMessage::class.java)
                }
                onComplete(messages)
            }
            .addOnFailureListener { e ->
                println("Помилка отримання повідомлень: ${e.message}")
                onComplete(emptyList())
            }
    }
}
