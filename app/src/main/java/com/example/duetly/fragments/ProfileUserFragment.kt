package com.example.duetly.fragments

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.duetly.activities.ReloadFragment
import com.example.duetly.DbHelper
import com.example.duetly.models.User
import com.example.duetly.R
import com.example.duetly.activities.decodeEmail
import com.example.duetly.activities.encodeEmail
import com.example.duetly.activities.showToast
import com.example.duetly.dialogs.AlertDialog
import com.example.duetly.dialogs.MBoxDialog
import com.example.duetly.dialogs.UserSettingsDialog
import com.example.duetly.models.UserMessage
import com.example.duetly.models.UserSettings
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ProfileUserFragment : Fragment(), AlertDialog.AlertDialogResult {
    private var fragmentReloadListener: ReloadFragment? = null
    private lateinit var firestore: FirebaseFirestore

    private var asyncJob: Job? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ReloadFragment) {
            fragmentReloadListener = context
        } else {
            throw ClassCastException("$context must implement OnDataPassListener")
        }
    }
    var isLogIn = false
    var isLogUp = false
    lateinit var rootView: View
    lateinit var dbHelper: DbHelper
    lateinit var fireDatabase: FirebaseDatabase
    var isRenaming = false
    var userName = ""
    var user = User()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let { args ->
            rootView =  inflater.inflate(R.layout.fragment_profile_user, container, false)
            dbHelper = DbHelper(requireContext(), null)
            fireDatabase = Firebase.database
            user = args.getParcelable<User>("user")!!
            userName = user.username
            setupProfileView(rootView, user)
            firestore = FirebaseFirestore.getInstance()

        }
        val userNicknameET = rootView.findViewById<EditText>(R.id.textView4)
        val logOutButton = rootView.findViewById<LinearLayout>(R.id.out)
        val settingsB = rootView.findViewById<LinearLayout>(R.id.settings)
        val safeNicknameB = rootView.findViewById<ImageButton>(R.id.safeNickname)
        val userMessagesB = rootView.findViewById<LinearLayout>(R.id.userMessages)

        val context = requireContext()
        safeNicknameB.visibility = View.GONE

        val renameB = rootView.findViewById<LinearLayout>(R.id.renameNickname)
        settingsB.setOnClickListener{
            val userSettingsDialog = UserSettingsDialog(user.settings)
            userSettingsDialog.show(parentFragmentManager,UserSettingsDialog.TAG)
            settingsB.animateSize()
        }
        renameB.setOnClickListener{
            if (!isRenaming) {
                isRenaming = true
                val inputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                safeNicknameB.visibility = View.VISIBLE
                userNicknameET.isFocusable = true
                userNicknameET.isFocusableInTouchMode = true
                userNicknameET.requestFocus()
                userNicknameET.setSelection(userNicknameET.text.length)
                inputMethodManager.showSoftInput(userNicknameET, InputMethodManager.SHOW_IMPLICIT)
                userNicknameET.requestFocus()
                renameB.animateSize()
            }else{
                safeNicknameB.visibility = View.GONE
                userNicknameET.isFocusable = false
                userNicknameET.isFocusableInTouchMode = false
                userNicknameET.setText(userName)
                isRenaming = false
            }
        }
        safeNicknameB.setOnClickListener{
            if (userNicknameET.text.toString() != userName) {
                val waitView = waitAnimation(requireContext())
                safeNicknameB.visibility = View.GONE
                userNicknameET.isFocusable = false
                userNicknameET.isFocusableInTouchMode = false
                isRenaming = false
                GlobalScope.launch(Dispatchers.IO) {
                    updateUserNickname(user.email, userName, userNicknameET.text.toString()) {
                        waitView.dismiss()
                        userName = userNicknameET.text.toString()
                    }
                }
            }else{
                safeNicknameB.visibility = View.GONE
                userNicknameET.isFocusable = false
                userNicknameET.isFocusableInTouchMode = false
                isRenaming = false
            }
        }
        userMessagesB.setOnClickListener{
            val waitView = waitAnimation(requireContext())

            GlobalScope.launch(Dispatchers.IO) {
                getAllMessages(user.username) {
                    val msList = it.toMutableList()
                    val mBoxDialog = MBoxDialog(msList,user,firestore,fireDatabase)
                    mBoxDialog.show(parentFragmentManager,MBoxDialog.TAG)
                    waitView.dismiss()
                }
            }

            userMessagesB.animateSize()
        }
        logOutButton.setOnClickListener {
            val alertDialog = AlertDialog("Log Out","Are you sure you want to log out of your account?")
            alertDialog.show(parentFragmentManager, AlertDialog.TAG)
            logOutButton.animateSize()
        }
        return rootView
    }
    private fun setupProfileView(rootView: View, user: User) {
        val userNameT = rootView.findViewById<EditText>(R.id.textView4)
        val userEmailT = rootView.findViewById<TextView>(R.id.textView9)

        val favSongNameT = rootView.findViewById<TextView>(R.id.name)
        val favSongAuthorT = rootView.findViewById<TextView>(R.id.songArtist)
        val favSongTimeT = rootView.findViewById<TextView>(R.id.songTime)
        userNameT.setText(user.username)
        val decEmail = decodeEmail(user.email)
        userEmailT.text = decEmail
        userNameT.isFocusable = false
        userNameT.isFocusableInTouchMode = false
        val favMusics = dbHelper.getAllMusicsFromH()
        var n = 0
        var favMusic = MusicInfo()
        for (i in favMusics) {
            if (i.number > n) {
                n = i.number
                favMusic = dbHelper.getMusic(i.idM)
            }
        }
        favSongNameT.text = favMusic.displayName
        favSongAuthorT.text = favMusic.artist
        val musicMinutes = favMusic.duration / 60000
        val musicSeconds = (favMusic.duration - (musicMinutes * 60000)) / 1000
        favSongTimeT.text = if (musicSeconds < 10) {
            "${musicMinutes}:0${musicSeconds}"
        } else {
            "${musicMinutes}:${musicSeconds}"
        }
    }

    override fun onDialogResult(result: Boolean) {
        if (result){
            dbHelper.updateUser(User(
                username = "404",
                email = "404",
                password = "404",
                settings = UserSettings()))
            fragmentReloadListener?.onFragment(AuthFragment(), false)
        }
    }
    private fun getAllMessages(receivedUser: String, onComplete: (List<UserMessage>) -> Unit) {
        firestore.collection("messages").document("$receivedUser-messages")
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
    private fun updateUserNickname(email: String, oldUsername: String, newUsername: String, onRenamed: (String) -> Unit) {
        if (oldUsername != newUsername) {
            val usersRef = fireDatabase.getReference("users")
            val encEmail = encodeEmail(email)

            usersRef.child(encEmail).get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if (user != null) {
                        user.username = newUsername

                        val removeOldUsername = hashMapOf<String, Any?>(
                            "usernames/$oldUsername" to null
                        )

                        val addNewUsername = hashMapOf<String, Any?>(
                            "users/$encEmail" to user,
                            "usernames/$newUsername" to encEmail
                        )

                        // Видаляємо старий нікнейм
                        fireDatabase.reference.updateChildren(removeOldUsername)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Додаємо новий нікнейм
                                    fireDatabase.reference.updateChildren(addNewUsername)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                dbHelper.updateUserUsername(newUsername)
                                                onRenamed(newUsername)
                                            } else {
                                                showToast(requireContext(), "Failed to update username")
                                                Log.e("FireDataBase", "ERROR_UPDATE_USERNAME", task.exception)
                                            }
                                        }
                                } else {
                                    showToast(requireContext(), "Failed to remove old username")
                                    Log.e("FireDataBase", "ERROR_REMOVE_OLD_USERNAME", task.exception)
                                }
                            }
                    } else {
                        showToast(requireContext(), "User data is null")
                    }
                } else {
                    showToast(requireContext(), "User not found")
                }
            }.addOnFailureListener { exception ->
                showToast(requireContext(), "Failed to fetch user data")
                Log.e("FireDataBase", "ERROR_FETCH_USER_DATA", exception)
            }
        }
    }
}
fun containsSpecialCharacters(input: String): Boolean {
    val pattern = Pattern.compile("[^a-zA-Z0-9]")
    val matcher = pattern.matcher(input)
    var isIncorrect = matcher.find()
    return if (isIncorrect){
        false
    }else{
        true
    }
}
fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
fun  waitAnimation(context: Context):PopupWindow{
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val popupView =
        inflater.inflate(R.layout.animate_wait_layout, null)
    val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
    return popupWindow
}
fun LinearLayout.animateSize(duration: Long = 300, scaleRatio: Float = 0.9f) {
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