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

class AuthFragment : Fragment() {
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
        rootView = inflater.inflate(R.layout.fragment_auth_layout, container, false)
        setupLoginView(rootView)

        return rootView
    }

    private fun setupLoginView(rootView: View) {
        val inputUserName = rootView.findViewById<EditText>(R.id.inputUserName)
        val inputEmail = rootView.findViewById<EditText>(R.id.inputEmail)
        val inputPassword = rootView.findViewById<EditText>(R.id.inputPassword)
        val inputCheckPassword = rootView.findViewById<EditText>(R.id.inputCheckPassword)

        val amountLName = rootView.findViewById<TextView>(R.id.numLettersName)
        val amountLPassword = rootView.findViewById<TextView>(R.id.numLettersPassword)
        val amountLCheckPassword = rootView.findViewById<TextView>(R.id.numLettersCP)


        val logUpB = rootView.findViewById<LinearLayout>(R.id.LogUp)
        val logInB = rootView.findViewById<LinearLayout>(R.id.LogIn)

        val passwordCont = rootView.findViewById<ConstraintLayout>(R.id.passwordL)
        val userNameCont = rootView.findViewById<ConstraintLayout>(R.id.usernameL)
        val userEmailCont = rootView.findViewById<ConstraintLayout>(R.id.emailL)
        val checkPasswordCont = rootView.findViewById<ConstraintLayout>(R.id.checkPasswordL)
        val actionsL = rootView.findViewById<LinearLayout>(R.id.actionsL)


        checkPasswordCont.visibility = View.GONE
        passwordCont.visibility = View.GONE
        userEmailCont.visibility = View.GONE
        userNameCont.visibility = View.GONE
        inputUserName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                amountLName.text = "${charSequence?.length}/30"
            }

            override fun afterTextChanged(editable: Editable?) {}
        })
        inputPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                amountLPassword.text = "${charSequence?.length}/35"
            }

            override fun afterTextChanged(editable: Editable?) {}
        })
        inputCheckPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                amountLCheckPassword.text = "${charSequence?.length}/35"
            }

            override fun afterTextChanged(editable: Editable?) {}
        })
        logUpB.setOnClickListener {
            if (isLogUp) {
                if (inputUserName.text.toString() != "") {
                    if (inputEmail.text.toString() != "") {
                        if (inputPassword.text.length >= 8) {
                            if (containsSpecialCharacters(inputPassword.text.toString())) {
                                if (isValidEmail(inputEmail.text.toString())) {
                                    val actionCodeSettings = actionCodeSettings {
                                        // URL you want to redirect back to. The domain (www.example.com) for this
                                        // URL must be whitelisted in the Firebase Console.
                                        url = "https://www.archi-app.website/finishSignUp"
                                        // This must be true
                                        handleCodeInApp = true
                                        setIOSBundleId("com.example.ios")
                                        setAndroidPackageName(
                                            "com.example.duetly",
                                            true, // installIfNotAvailable
                                            "1", // minimumVersion
                                        )
                                    }
                                    if (inputPassword.text.toString() == inputCheckPassword.text.toString()) {
                                        val newUser = User(
                                            inputUserName.text.toString(),
                                            inputEmail.text.toString(),
                                            inputPassword.text.toString()
                                        )
                                        Firebase.auth.sendSignInLinkToEmail(
                                            inputEmail.text.toString(),
                                            actionCodeSettings
                                        )
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    showToast(
                                                        requireContext(),
                                                        "Email sent."
                                                    )
                                                    val usersRef =
                                                        fireDatabase.getReference("users")
                                                    val userRef =
                                                        fireDatabase.getReference("users")
                                                            .child(
                                                                inputUserName.text.toString()
                                                                    .trim()
                                                            )
                                                    userRef.get()
                                                        .addOnSuccessListener { dataSnapshot ->
                                                            if (dataSnapshot.exists()) {
                                                                showToast(
                                                                    requireContext(),
                                                                    "A user with that name already exists"
                                                                )
                                                            } else {
                                                                usersRef.child(newUser.username)
                                                                    .setValue(newUser)
                                                                    .addOnCompleteListener { task ->
                                                                        if (task.isSuccessful) {
                                                                            dbHelper.updateUser(
                                                                                newUser
                                                                            )
                                                                            isLogUp =
                                                                                false
                                                                            fragmentReloadListener?.onFragment(
                                                                                ProfileUserFragment()
                                                                            )
                                                                        } else {
                                                                            showToast(
                                                                                requireContext(),
                                                                                "Something wrong"
                                                                            )
                                                                            Log.e(
                                                                                "FireDataBase",
                                                                                "ERROR_ADD_NEW_USER"
                                                                            )
                                                                        }
                                                                    }
                                                            }
                                                        }
                                                } else {
                                                    showToast(requireContext(), "SOMETHING WRONG")
                                                }
                                            }
                                    } else {
                                        showToast(requireContext(), "Passwords do not match")
                                    }
                                } else {
                                    showToast(requireContext(), "E-mail is incorrect")
                                }
                            } else {
                                showToast(
                                    requireContext(),
                                    "The password cannot contain special characters"
                                )
                            }
                        } else {
                            showToast(
                                requireContext(),
                                "The password must contain at least 8 characters"
                            )
                        }
                    } else {
                        showToast(requireContext(), "The E-mail field cannot be empty")
                    }
                } else {
                    showToast(requireContext(), "The username field cannot be empty")
                }

            } else {
                isLogUp = true
                isLogIn = false
                checkPasswordCont.visibility = View.VISIBLE
                passwordCont.visibility = View.VISIBLE
                userEmailCont.visibility = View.VISIBLE
                userNameCont.visibility = View.VISIBLE
            }
            actionsL.orientation = LinearLayout.HORIZONTAL
        }
        logInB.setOnClickListener {
            if (isLogIn) {
                if (inputUserName.text.toString() != "") {
                    if (inputEmail.text.toString() != "") {
                        if (inputPassword.text.length > 8) {
                            val userRef = fireDatabase.getReference("users")
                                .child(inputUserName.text.toString().trim())
                            userRef.get().addOnSuccessListener { dataSnapshot ->
                                if (dataSnapshot.exists()) {
                                    val userF = dataSnapshot.getValue<User>()
                                    if (userF != null) {
                                        if (userF.validatePassword(
                                                inputPassword.text.toString().trim()
                                            )
                                        ) {
                                            dbHelper.updateUser(
                                                userF
                                            )
                                            fragmentReloadListener?.onFragment(
                                                ProfileUserFragment()
                                            )
                                        } else {
                                            showToast(requireContext(), "Password is incorrect")
                                        }
                                    }
                                } else {
                                    showToast(
                                        requireContext(),
                                        "User with this login was not found, check"
                                    )
                                }
                            }
                        } else {
                            showToast(
                                requireContext(),
                                "The password must contain at least 8 characters"
                            )
                        }
                    } else {
                        showToast(requireContext(), "The E-mail field cannot be empty")
                    }
                } else {
                    showToast(requireContext(), "The username field cannot be empty")
                }
            } else {
                isLogUp = false
                isLogIn = true
                checkPasswordCont.visibility = View.GONE
                passwordCont.visibility = View.VISIBLE
                userEmailCont.visibility = View.GONE
                userNameCont.visibility = View.VISIBLE
            }
            actionsL.orientation = LinearLayout.HORIZONTAL
        }
    }
    private fun sendSignInLinkToEmail(email: String, username: String, password: String) {
        val actionCodeSettings = actionCodeSettings {
            url = "https://www.archi-app.website/finishSignUp?username=$username&password=$password"
            handleCodeInApp = true
            setIOSBundleId("com.example.ios")
            setAndroidPackageName(
                "com.example.duetly",
                true, /* installIfNotAvailable */
                "12" /* minimumVersion */
            )
        }

        Firebase.auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast(requireContext(), "Email sent.")
                } else {
                    showToast(requireContext(), "Error: ${task.exception?.message}")
                }
            }
    }

}
