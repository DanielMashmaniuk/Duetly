package com.example.duetly.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.duetly.activities.ReloadFragment
import com.example.duetly.activities.showToast
import com.example.duetly.DbHelper
import com.example.duetly.dialogs.InputCodeDialog
import com.example.duetly.models.User
import com.example.duetly.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Properties
import java.util.Random
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

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
    var CODE6 = "-1"
    lateinit var rootView: View
    lateinit var dbHelper: DbHelper
    lateinit var fireDatabase: FirebaseDatabase
    private var codeInputDialog = InputCodeDialog("", "", "", "0")
    var name = ""
    var email = ""
    var password = "11111111"
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
                if (inputUserName.text.toString().isNotBlank()) {
                    if (inputEmail.text.toString().isNotBlank()) {
                        if (inputPassword.text.length >= 8) {
                            if (containsSpecialCharacters(inputPassword.text.toString())) {
                                if (isValidEmail(inputEmail.text.toString())) {
                                    if (inputPassword.text.toString() == inputCheckPassword.text.toString()) {
                                        name = inputUserName.text.toString().trim()
                                        email = inputEmail.text.toString().trim()
                                        password = inputPassword.text.toString().trim()
                                        GlobalScope.launch {
                                            checkAvailableLogin(name){
                                                if (it) {
                                                    checkAvailableEmail(email){
                                                        if (it) {
                                                            if (CODE6.toInt() < 22) {
                                                                GlobalScope.launch {
                                                                    sendVerificationCode(
                                                                        inputEmail.text.toString()
                                                                    ) {
                                                                        CODE6 = it
                                                                        codeInputDialog =
                                                                            InputCodeDialog(
                                                                                name,
                                                                                email,
                                                                                password,
                                                                                CODE6
                                                                            )
                                                                        codeInputDialog.show(
                                                                            parentFragmentManager,
                                                                            InputCodeDialog.TAG
                                                                        )
                                                                    }
                                                                }
                                                            } else {
                                                                codeInputDialog =
                                                                    InputCodeDialog(
                                                                        name,
                                                                        email,
                                                                        password,
                                                                        CODE6
                                                                    )
                                                                codeInputDialog.show(
                                                                    parentFragmentManager,
                                                                    InputCodeDialog.TAG
                                                                )
                                                            }
                                                        } else {
                                                            Handler(Looper.getMainLooper()).post {
                                                                showToast(
                                                                    requireContext(),
                                                                    "This email is registered for another user"
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Handler(Looper.getMainLooper()).post {
                                                        showToast(
                                                            requireContext(),
                                                            "This nickname is already in use, choose another one"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        showToast(requireContext(), "Passwords do not match")
                                    }
                                } else {
                                    showToast(requireContext(), "Invalid email format")
                                }
                            } else {
                                showToast(
                                    requireContext(),
                                    "Password cannot contain special characters"
                                )
                            }
                        } else {
                            showToast(
                                requireContext(),
                                "Password must be at least 8 characters long"
                            )
                        }
                    } else {
                        showToast(requireContext(), "Email field cannot be empty")
                    }
                } else {
                    showToast(requireContext(), "Username field cannot be empty")
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
                    if (inputPassword.text.length > 8) {
                        val userRef = fireDatabase.getReference("users")
                            .child(inputEmail.text.toString().trim())
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
                                            ProfileUserFragment(),
                                            true
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
                    showToast(requireContext(), "The username field cannot be empty")
                }
            } else {
                isLogUp = false
                isLogIn = true
                checkPasswordCont.visibility = View.GONE
                passwordCont.visibility = View.VISIBLE
                userEmailCont.visibility = View.VISIBLE
                userNameCont.visibility = View.GONE
            }
            actionsL.orientation = LinearLayout.HORIZONTAL
        }
    }

    private fun sendVerificationCode(email: String,sendCode:(String)->Unit) {
        val code = generateVerificationCode()

        val properties = Properties()
        properties["mail.smtp.host"] = "smtp.gmail.com"
        properties["mail.smtp.port"] = "587"
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"

        val session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(
                    "duetly088@gmail.com",
                    "b n i z i t b p f a r q h p c t"
                )
            }
        })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress("duetly088@gmail.com"))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
            message.subject = "Verification Code For Duet-ly"
            message.setText("Your verification code is: $code")

            Transport.send(message)
        } catch (e: MessagingException) {
            throw RuntimeException(e)
        }
        sendCode(code)
    }

    private fun generateVerificationCode(): String {
        val random = Random()
        val code = StringBuilder()
        for (i in 0 until 6) {
            val digit = random.nextInt(10)
            code.append(digit)
        }
        return code.toString()
    }

    private fun checkAvailableLogin(login: String, onCheckComplete: (Boolean) -> Unit) {
        val usersRef = fireDatabase.getReference("users").child(login)

        usersRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val dataSnapshot = task.result
                if (dataSnapshot.exists()) {
                    // Логін вже існує
                    onCheckComplete(false)
                } else {
                    // Логін не існує
                    onCheckComplete(true)
                }
            } else {
                // Обробка помилки при спробі доступу до бази даних
                Log.e("Firebase", "Error checking login availability", task.exception)
                onCheckComplete(false)
            }
        }
    }

    private fun checkAvailableEmail(email: String, onCheckComplete: (Boolean) -> Unit) {
        val usersRef = fireDatabase.getReference("users")

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    onCheckComplete(false)
                } else {
                    onCheckComplete(true)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Обробка помилок, якщо запит скасовано або виникла помилка бази даних
                Log.e(
                    "Firebase",
                    "Error checking email in login availability",
                    databaseError.toException()
                )
            }
        })
    }
}

