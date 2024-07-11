package com.example.duetly

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.duetly.Models.User
import com.google.firebase.auth.actionCodeSettings
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
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
    var CODE6 = -1
    lateinit var rootView: View
    lateinit var dbHelper: DbHelper
    lateinit var fireDatabase: FirebaseDatabase
    lateinit var verifyWindow: ConstraintLayout
    private lateinit var timerTextView: TextView
    private lateinit var countDownTimer: CountDownTimer
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
        verifyWindow = rootView.findViewById(R.id.verifyWindow)

        verifyWindow.visibility = View.GONE
        setupLoginView(rootView)

        val num1 = rootView.findViewById<LinearLayout>(R.id.num_1)
        val num2 = rootView.findViewById<LinearLayout>(R.id.num_2)
        val num3 = rootView.findViewById<LinearLayout>(R.id.num_3)
        val num4 = rootView.findViewById<LinearLayout>(R.id.num_4)
        val num5 = rootView.findViewById<LinearLayout>(R.id.num_5)
        val num6 = rootView.findViewById<LinearLayout>(R.id.num_6)

        val codeInp1 = rootView.findViewById<EditText>(R.id.code_inp_1)
        val codeInp2 = rootView.findViewById<EditText>(R.id.code_inp_2)
        val codeInp3 = rootView.findViewById<EditText>(R.id.code_inp_3)
        val codeInp4 = rootView.findViewById<EditText>(R.id.code_inp_4)
        val codeInp5 = rootView.findViewById<EditText>(R.id.code_inp_5)
        val codeInp6 = rootView.findViewById<EditText>(R.id.code_inp_6)

        val listNums = listOf<LinearLayout>(num1,num2,num3,num4,num5,num6)
        val listInputs = listOf<EditText>(codeInp1,codeInp2,codeInp3,codeInp4,codeInp5,codeInp6)
        updateBackground(listNums,listInputs)

        // Слухач зміни фокусу
        fun setFocusChangeListener(vararg editTexts: EditText) {
            editTexts.forEach { editText ->
                editText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        val idx = listInputs.indexOf(editText)
                        updateBackground(listNums, listInputs,idx)
                    }
                }
            }
        }

        fun setTextChangeListener(vararg editTexts: EditText) {
            editTexts.forEach { editText ->
                editText.addTextChangedListener(object : TextWatcher {
                    var lenght = 0
                    override fun beforeTextChanged(
                        charSequence: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        lenght = charSequence?.length?:0
                    }
                    override fun onTextChanged(
                        charSequence: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        var inpCodes = 0
                        for (i in listInputs){
                            if (i.text.isNotEmpty()){
                                inpCodes++
                            }
                        }
                        val idx = listInputs.indexOf(editText)
                        updateBackground(listNums,listInputs,idx)
                        if ((charSequence?.length ?: 0) > lenght){
                            if((charSequence?.length ?: 0) > 0 && idx < 5){
                                val nextEditText = listInputs[idx+1]
                                nextEditText.requestFocus()
                            }
                        }else{
                            if((charSequence?.length ?: 0) == 0 && idx > 0 && inpCodes>1){
                                val nextEditText = listInputs[idx-1]
                                nextEditText.requestFocus()
                            }else{
                                val nextEditText = listInputs[0]
                                nextEditText.requestFocus()
                            }
                        }
                        if (inpCodes == 6){
                            val newUser = User(
                                name,
                                email,
                                password
                            )
                            var reqCode = ""
                            for (i in listInputs){
                                reqCode += i.text.toString()
                            }
                            showToast(requireContext(),"${CODE6} - ${reqCode}")
                            if (reqCode == CODE6.toString()){
                                createUser(newUser)
                            }else{
                                for ((idxT, i) in listNums.withIndex()){
                                    animateIncorrectCode(i,R.drawable.bg_white_stroke,R.drawable.bg_incorrect_code,listInputs[idxT])
                                }

                            }
                        }
                    }

                    override fun afterTextChanged(editable: Editable?) {}
                })
            }
        }
        fun setKeyInputListener(vararg editTexts: EditText) {
            editTexts.forEach { editText ->
                editText.setOnKeyListener { v, keyCode, event ->
                    var check = 0
                    for (i in listInputs){
                        if (i.text.isNotEmpty()){
                            check++
                        }
                    }
                    if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL && editText.text.isEmpty()) {
                        var idx = listInputs.indexOf(editText)
                        var isC = true
                        while (isC) {
                            if (idx > 0) {
                                val nextEditText = listInputs[idx - 1]
                                if (nextEditText.text.length > 0) {
                                    nextEditText.requestFocus()
                                    updateBackground(listNums,listInputs,idx-1)
                                    isC = false
                                }else{
                                    idx--
                                }
                            }else{
                                isC = false
                            }
                        }
                    }else if(event.action == KeyEvent.ACTION_DOWN && editText.text.isNotEmpty() && check > 0){
                        var idx = listInputs.indexOf(editText)
                        var isC = true
                        if (idx < 5 && idx != 0) {
                            val nextEditText = listInputs[idx + 1]
                            if (nextEditText.text.isEmpty()) {
                                nextEditText.requestFocus()
                                updateBackground(listNums,listInputs,idx+1)
                                isC = false
                            }else{
                                idx++
                            }
                        }else{
                            isC = false
                        }
                    }
                    false
                }

            }
        }

        setFocusChangeListener(codeInp1, codeInp2, codeInp3, codeInp4, codeInp5, codeInp6)
        setTextChangeListener(codeInp1, codeInp2, codeInp3, codeInp4, codeInp5, codeInp6)
        setKeyInputListener(codeInp1, codeInp2, codeInp3, codeInp4, codeInp5, codeInp6)


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
                verifyWindow.visibility = View.VISIBLE
                timerTextView = rootView.findViewById(R.id.timer_text_view) // Ваш TextView для відображення таймера

                startTimer(5 * 60 * 1000)
                if (inputUserName.text.toString().isNotBlank()) {
                    if (inputEmail.text.toString().isNotBlank()) {
                        if (inputPassword.text.length >= 8) {
                            if (containsSpecialCharacters(inputPassword.text.toString())) {
                                if (isValidEmail(inputEmail.text.toString())) {
                                    if (inputPassword.text.toString() == inputCheckPassword.text.toString()) {
                                        name = inputUserName.text.toString()
                                        email = inputUserName.text.toString()
                                        password = inputPassword.text.toString()

                                        GlobalScope.launch {
                                            CODE6 = sendVerificationCode(inputEmail.text.toString())
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

    private fun sendVerificationCode(email: String): Int {
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
        return code.toInt()
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
    private fun updateBackground(listNums: List<LinearLayout>,listInputs: List<EditText>,selectedViewIdx: Int = 10){
        listInputs.forEach { editText ->
            val idx = listInputs.indexOf(editText)
            val num = listNums[idx]
            if (idx == selectedViewIdx){
                num.setBackgroundResource(R.drawable.bg_sec_stroke)
                editText.setTextColor(resources.getColor(R.color.secColor, null))
            }else if (editText.text.isNotEmpty()){
                num.setBackgroundResource(R.drawable.bg_active_code)
                editText.setTextColor(resources.getColor(R.color.dark, null))

            }else{
                num.setBackgroundResource(R.drawable.bg_white_stroke)
            }
        }
    }
    private fun createUser(newUser:User){
        val usersRef = fireDatabase.getReference("users")
        val userRef = usersRef.child(newUser.username)

        userRef.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                showToast(
                    requireContext(),
                    "A user with that name already exists"
                )
            } else {
                usersRef.child(newUser.username)
                    .setValue(newUser)
                    .addOnCompleteListener { userTask ->
                        if (userTask.isSuccessful) {
                            dbHelper.updateUser(newUser)
                            isLogUp = false
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
                                "ERROR_ADD_NEW_USER",
                                userTask.exception
                            )
                        }
                    }
            }
        }
    }
    private fun animateIncorrectCode(view: View, startDrawable: Int, endDrawable: Int,eText: EditText,duration: Long = 2000) {
        view.setBackgroundResource(endDrawable)
        eText.setTextColor(resources.getColor(R.color.dark))
        if (eText.isFocused){
            eText.clearFocus()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            eText.setText("")
            eText.clearFocus()
            view.setBackgroundResource(startDrawable)
        }, duration)
    }
    private fun startTimer(duration: Long) {
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timerTextView.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                timerTextView.text = "00:00"
                verifyWindow.visibility = View.GONE
                timerTextView.text = "05:00"
            }
        }
        countDownTimer.start()
    }
}

