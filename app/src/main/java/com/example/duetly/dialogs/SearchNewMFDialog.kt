package com.example.duetly.dialogs

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.duetly.DbHelper
import com.example.duetly.R
import com.example.duetly.activities.showToast
import com.example.duetly.adapters.UserMBoxAdapter
import com.example.duetly.adapters.UsersListAdapter
import com.example.duetly.fragments.waitAnimation
import com.example.duetly.models.User
import com.example.duetly.models.UserMessage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchNewMFDialog(private val firestore: FirebaseFirestore,private val user: User) : DialogFragment() {
    private lateinit var rootView: View
    private lateinit var usersListAdapter:UsersListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.search_new_mf_layout, container, false)
        val etNickname = rootView.findViewById<EditText>(R.id.etNickname)
        val searchB = rootView.findViewById<ImageButton>(R.id.searchB)
        val usersList = rootView.findViewById<RecyclerView>(R.id.usersList)
        val waitLayout = rootView.findViewById<ConstraintLayout>(R.id.EL)
        waitLayout.visibility = View.GONE
        searchB.setOnClickListener{
            searchB.animateSize()
            searchB.isEnabled = false
            if (etNickname.text.length > 3) {
                waitLayout.visibility = View.VISIBLE
                getUsersByNamePrefix(etNickname.text.toString().trim()) { userList ->
                    usersListAdapter = UsersListAdapter(requireContext(), userList) { userPair ->
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            val message = UserMessage(
                                senderUsername = user.username,
                                text = "You got a message to make friends with ${user.username}",
                                type = "Friend request",
                                isRead = false
                            )
                            message.sendMessage(userPair.first.username, message, firestore)
                        }
                    }
                    waitLayout.visibility = View.GONE
                    searchB.isEnabled = true
                    usersList.layoutManager = LinearLayoutManager(requireContext())
                    usersList.adapter = usersListAdapter
                }
            }

        }
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
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false) // Disable outside touch to dismiss

        return dialog
    }
    companion object {
        const val TAG = "CodeInputDialogFragment"
    }
    private fun getUsersByNamePrefix(prefix: String, onUsersList: (List<Pair<User, Boolean>>) -> Unit) {
        val database: DatabaseReference = FirebaseDatabase.getInstance().reference
        val endPrefix = prefix + "\uf8ff"

        val query = database.child("users")
            .orderByChild("username")
            .startAt(prefix)
            .endAt(endPrefix)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val users = mutableListOf<User>()
                val context = context ?: return // Переконайтеся, що контекст доступний
                val localUser = DbHelper(context, null).getUser()

                for (userSnapshot in dataSnapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        if (user.username != localUser.username) {
                            users.add(it)
                        }
                    }
                }

                getMessagesFromReceiver(localUser.username) { messages ->
                    val mutableListUsers = mutableListOf<Pair<User, Boolean>>()
                    val addedUsers = mutableSetOf<String>()

                    for (user in users) {
                        var found = false
                        for (ms in messages) {
                            if (ms.type == "Friend request" && ms.senderUsername == user.username) {
                                mutableListUsers.add(Pair(user, true))
                                found = true
                                break
                            }
                        }
                        if (!found && user.username !in addedUsers) {
                            mutableListUsers.add(Pair(user, false))
                            addedUsers.add(user.username)
                        }
                    }

                    val iterator = addedUsers.iterator()
                    while (iterator.hasNext()) {
                        val md = iterator.next()
                        for (fr in localUser.melodymates) { // Переконайтеся, що melodymates не null
                            if (md == fr.name) {
                                iterator.remove()
                                break
                            }
                        }
                    }

                    onUsersList(mutableListUsers)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Error: ${databaseError.message}")
            }
        })
    }




    private fun getMessagesFromReceiver(
        receivedName: String,
        onResult: (List<UserMessage>) -> Unit
    ) {
        // Отримуємо посилання на Firestore
        val db = FirebaseFirestore.getInstance()

        // Отримуємо всі повідомлення для конкретного користувача
        val userRef = db.collection("messages")
            .document("$receivedName-messages")
            .collection("userMessages")
        userRef.get()
            .addOnSuccessListener { querySnapshot ->
                val messages = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(UserMessage::class.java)
                }
                onResult(messages) // Повертаємо список повідомлень через колбек
            }
            .addOnFailureListener { exception ->
                println("Помилка отримання повідомлень: ${exception.message}")
                onResult(emptyList()) // Повертаємо порожній список у разі помилки
            }
    }
}
fun ImageView.animateSize(duration: Long = 300, scaleRatio: Float = 0.9f) {
    val scaleAnimation = ValueAnimator.ofFloat(1f, scaleRatio, 1f).apply {
        addUpdateListener { animator ->
            val scale = animator.animatedValue as Float
            scaleX = scale
            scaleY = scale
        }
    }

    val animatorSet = AnimatorSet().apply {
        playTogether(scaleAnimation)
        interpolator = AccelerateDecelerateInterpolator()
        this.duration = duration
    }

    animatorSet.start()
}