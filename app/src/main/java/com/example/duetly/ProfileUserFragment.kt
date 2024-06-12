package com.example.duetly

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.duetly.Models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.actionCodeSettings
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Random
import java.util.regex.Pattern

class ProfileUserFragment : Fragment() {
    private var fragmentReloadListener: ReloadFragment? = null
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dbHelper = DbHelper(requireContext(), null)
        fireDatabase = Firebase.database
        rootView =  inflater.inflate(R.layout.fragment_profile_user, container, false)
        CoroutineScope(Dispatchers.Main).launch {
            val userL = withContext(Dispatchers.IO) {
                dbHelper.getUser()
            }
            setupProfileView(rootView, userL)
        }
        return rootView
    }
    private fun setupProfileView(rootView: View, userL: User) {
        CoroutineScope(Dispatchers.Main).launch {
            val userF = withContext(Dispatchers.IO) {
                suspendCancellableCoroutine<User?> { continuation ->
                    fetchUserData(userL) { user ->
                        continuation.resume(user, null)
                    }
                }
            }
            val userNameT = rootView.findViewById<TextView>(R.id.textView4)
            val userEmailT = rootView.findViewById<TextView>(R.id.textView9)

            val logOutButton = rootView.findViewById<LinearLayout>(R.id.out)
            val favSongNameT = rootView.findViewById<TextView>(R.id.songName)
            val favSongAuthorT = rootView.findViewById<TextView>(R.id.songArtist)
            val favSongTimeT = rootView.findViewById<TextView>(R.id.songTime)
            userNameT.text = userF?.username ?: "Underfined"
            userEmailT.text = userF?.email ?: "null"
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
            logOutButton.setOnClickListener {
                dbHelper.updateUser(User("404", "404", "404"))
                fragmentReloadListener?.onFragment(AuthFragment())
            }
        }
    }
    private fun fetchUserData(userLoc: User, callback: (User?) -> Unit) {
        val userRef = Firebase.database.getReference("users").child(userLoc.username)
        userRef.get().addOnSuccessListener { dataSnapshot ->
            val userF = dataSnapshot.getValue<User>()
            callback(userF)
        }.addOnFailureListener {
            callback(null)
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
