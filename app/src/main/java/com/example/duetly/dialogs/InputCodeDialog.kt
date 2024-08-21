package com.example.duetly.dialogs

import android.app.Dialog
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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.duetly.DbHelper
import com.example.duetly.fragments.ProfileUserFragment
import com.example.duetly.R
import com.example.duetly.activities.ReloadFragment
import com.example.duetly.activities.encodeEmail
import com.example.duetly.activities.showToast
import com.example.duetly.models.User
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job

class InputCodeDialog(name:String,email:String,password:String,code:String) : DialogFragment(){
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
    val CODE6 = code
    lateinit var rootView: View
    lateinit var dbHelper: DbHelper
    lateinit var fireDatabase: FirebaseDatabase
    private lateinit var timerTextView: TextView
    private lateinit var countDownTimer: CountDownTimer
    val nameUser = name
    val emailUser = email
    val passwordUser = password
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.code_input_layout, container, false)
        dbHelper = DbHelper(requireContext(), null)
        fireDatabase = Firebase.database
        timerTextView = rootView.findViewById(R.id.timer_text_view) // Ваш TextView для відображення таймера
        startTimer(5 * 60 * 1000)
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

        val listNums = listOf<LinearLayout>(num1, num2, num3, num4, num5, num6)
        val listInputs =
            listOf<EditText>(codeInp1, codeInp2, codeInp3, codeInp4, codeInp5, codeInp6)
        updateBackground(listNums, listInputs)

        // Слухач зміни фокусу
        fun setFocusChangeListener(vararg editTexts: EditText) {
            editTexts.forEach { editText ->
                editText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        val idx = listInputs.indexOf(editText)
                        updateBackground(listNums, listInputs, idx)
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
                        lenght = charSequence?.length ?: 0
                    }

                    override fun onTextChanged(
                        charSequence: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        var inpCodes = 0
                        for (i in listInputs) {
                            if (i.text.isNotEmpty()) {
                                inpCodes++
                            }
                        }
                        val idx = listInputs.indexOf(editText)
                        updateBackground(listNums, listInputs, idx)
                        if ((charSequence?.length ?: 0) > lenght) {
                            if ((charSequence?.length ?: 0) > 0 && idx < 5) {
                                val nextEditText = listInputs[idx + 1]
                                nextEditText.requestFocus()
                            }
                        } else {
                            if ((charSequence?.length ?: 0) == 0 && idx > 0 && inpCodes > 1) {
                                val nextEditText = listInputs[idx - 1]
                                nextEditText.requestFocus()
                            } else {
                                val nextEditText = listInputs[0]
                                nextEditText.requestFocus()
                            }
                        }
                        if (inpCodes == 6) {
                            val encEmail = encodeEmail(emailUser)
                            val newUser = User(
                                username = nameUser,
                                email = encEmail,
                                password = passwordUser
                            )
                            var reqCode = ""
                            for (i in listInputs) {
                                reqCode += i.text.toString()
                            }
                            showToast(requireContext(), "${CODE6} - ${reqCode}")
                            if (reqCode == CODE6) {
                                animateСorrectCode(listNums,listInputs,newUser)
                            } else {
                                for ((idxT, i) in listNums.withIndex()) {
                                    animateIncorrectCode(i, listInputs[idxT])
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
                    for (i in listInputs) {
                        if (i.text.isNotEmpty()) {
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
                                    updateBackground(listNums, listInputs, idx - 1)
                                    isC = false
                                } else {
                                    idx--
                                }
                            } else {
                                isC = false
                            }
                        }
                    } else if (event.action == KeyEvent.ACTION_DOWN && editText.text.isNotEmpty() && check > 0) {
                        var idx = listInputs.indexOf(editText)
                        var isC = true
                        if (idx < 5 && idx != 0) {
                            val nextEditText = listInputs[idx + 1]
                            if (nextEditText.text.isEmpty()) {
                                nextEditText.requestFocus()
                                updateBackground(listNums, listInputs, idx + 1)
                                isC = false
                            } else {
                                idx++
                            }
                        } else {
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

    private fun animateIncorrectCode(
        view: View,
        eText: EditText,
        duration: Long = 2000
    ) {
        view.setBackgroundResource(R.drawable.bg_incorrect_code)
        eText.setTextColor(resources.getColor(R.color.dark))
        if (eText.isFocused) {
            eText.clearFocus()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            eText.setText("")
            eText.clearFocus()
            view.setBackgroundResource(R.drawable.bg_white_stroke)
        }, duration)
    }
    private fun animateСorrectCode(
        listNums: List<LinearLayout>,
        listInputs: List<EditText>,
        user: User,
        duration: Long = 2000
    ) {
        listNums.forEach { eNum ->
            eNum.setBackgroundResource(R.drawable.bg_correct_code)
        }
        listInputs.forEach{eText->
            eText.setTextColor(resources.getColor(R.color.dark))
            if (eText.isFocused) {
                eText.clearFocus()
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            createUser(user)
        }, duration)
    }

    private fun updateBackground(
        listNums: List<LinearLayout>,
        listInputs: List<EditText>,
        selectedViewIdx: Int = 10
    ) {
        listInputs.forEach { editText ->
            val idx = listInputs.indexOf(editText)
            val num = listNums[idx]
            if (idx == selectedViewIdx) {
                num.setBackgroundResource(R.drawable.bg_sec_stroke)
                editText.setTextColor(resources.getColor(R.color.secColor, null))
            } else if (editText.text.isNotEmpty()) {
                num.setBackgroundResource(R.drawable.bg_active_code)
                editText.setTextColor(resources.getColor(R.color.dark, null))

            } else {
                num.setBackgroundResource(R.drawable.bg_white_stroke)
            }
        }
    }

    private fun createUser(newUser: User) {
        val usersRef = fireDatabase.getReference("users")
        val userRef = usersRef.child(newUser.email)

        userRef.get().addOnSuccessListener {
            usersRef.child(newUser.email)
                .setValue(newUser)
                .addOnCompleteListener { userTask ->
                    if (userTask.isSuccessful) {
                        dbHelper.updateUser(newUser)
                        dismiss()
                        fragmentReloadListener?.onFragment(
                            ProfileUserFragment(),
                            true
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
    private fun startTimer(duration: Long) {
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timerTextView.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                timerTextView.text = "00:00"
                dismiss()
                timerTextView.text = "05:00"
            }
        }
        countDownTimer.start()
    }
}
