package com.example.duetly.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.duetly.DbHelper
import com.example.duetly.R
import com.example.duetly.activities.encodeEmail
import com.example.duetly.adapters.PlaylistListAdapter
import com.example.duetly.adapters.UserMBoxAdapter
import com.example.duetly.adapters.UsersListAdapter
import com.example.duetly.models.Melodymate
import com.example.duetly.models.User
import com.example.duetly.models.UserMessage
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MBoxDialog
    (
    private val messagesList: MutableList<UserMessage>,
    private val user: User,
    private val firestore : FirebaseFirestore,
    private val firebaseDatabase : FirebaseDatabase

) : DialogFragment() {
    private var messageListenerRegistration: ListenerRegistration? = null
    private lateinit var rootView: View
    private lateinit var dbHelper: DbHelper
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.messages_box_layout, container, false)
        dbHelper = DbHelper(requireContext(), null)
        val messagesRec = rootView.findViewById<RecyclerView>(R.id.messagesList)

        setupMessagesAdapter(messagesRec, messagesList)

        return rootView
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(

            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(R.drawable.bg_transparent)

    }

    override fun onStop() {
        super.onStop()

        // Видаляємо всі слухачі для уникнення витоків пам'яті
        removeMessageListener()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false) // Disable outside touch to dismiss

        return dialog
    }

    companion object {
        const val TAG = "CodeInputDialogFragment"
    }

    private fun setupMessagesAdapter(messagesRec: RecyclerView, msList: List<UserMessage>) {

        val boxAdapter = UserMBoxAdapter(msList.toMutableList(),
            onAcceptListener = {
                it.first.deleteMessage(user.username, it.first.id,firestore)
                val newFriend = Melodymate(it.first.senderUsername!!, "friend")
                val encodeEmail = encodeEmail(user.email)
                newFriend.addMelodymateToDatabase(encodeEmail, newFriend, firebaseDatabase)
            },
            onRefuseListener = {

            }
        )
        messagesRec.layoutManager = LinearLayoutManager(requireContext())
        messagesRec.adapter = boxAdapter
        GlobalScope.launch(Dispatchers.IO) {
            setMessageListener(user.username) {
                boxAdapter.addMessage(it)
            }
        }
    }

    // Отримання всіх повідомлень


    private fun setMessageListener(receivedUser: String, onMessageReceived: (UserMessage) -> Unit) {
        messageListenerRegistration?.remove()
        messageListenerRegistration =
            firestore.collection("messages").document("$receivedUser-messages")
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
    private fun removeMessageListener() {
        messageListenerRegistration?.remove()
        messageListenerRegistration = null
    }
}
