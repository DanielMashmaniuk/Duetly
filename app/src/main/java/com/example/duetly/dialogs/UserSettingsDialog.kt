package com.example.duetly.dialogs

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import androidx.fragment.app.DialogFragment
import com.example.duetly.DbHelper
import com.example.duetly.R
import com.example.duetly.activities.encodeEmail
import com.example.duetly.activities.getUser
import com.example.duetly.models.UserSettings
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserSettingsDialog(firebaseDatabase: FirebaseDatabase,settings: UserSettings) : DialogFragment() {
    private lateinit var rootView: View
    var userSettings = settings
    val fireDatabase = firebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.user_settings_layout, container, false)

        val switchTrack = rootView.findViewById<LinearLayout>(R.id.switchTrack)
        val switchThumb = rootView.findViewById<LinearLayout>(R.id.switchThumb)
        val dbHelper = DbHelper(requireContext(),null)
        val user = dbHelper.getUser()
        if (userSettings.showEmail){
            animateTrueSwitch(requireContext(),switchTrack,switchThumb)
        }
        // Встановити обробник подій для зміни стану
        switchTrack.setOnClickListener {
            if (userSettings.showEmail){
                animateFalseSwitch(requireContext(),switchTrack,switchThumb)
                userSettings.showEmail = false
                GlobalScope.launch {
                    val enEmail = encodeEmail(user.email)
                    userSettings.updateShowEmail(enEmail, false)
                }
            }else{
                animateTrueSwitch(requireContext(),switchTrack,switchThumb)
                userSettings.showEmail = true
                GlobalScope.launch {
                    val enEmail = encodeEmail(user.email)
                    userSettings.updateShowEmail(enEmail, true)
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
    private fun animateTrueSwitch(context: Context, switchTrack: LinearLayout, switchThumb: LinearLayout) {
        // Конвертувати dp у px
        val dpToPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, context.resources.displayMetrics)

        // Застосувати перехідний drawable до switchTrack
        switchTrack.setBackgroundResource(R.drawable.track_checked_transition)
        val trackTransition = switchTrack.background as TransitionDrawable

        // Застосувати перехідний drawable до switchThumb
        switchThumb.setBackgroundResource(R.drawable.thumb_checked_transition)
        val thumbTransition = switchThumb.background as TransitionDrawable

        // Анімація для переміщення switchThumb вправо на 20 dp
        val moveAnimation = ValueAnimator.ofFloat(0f, dpToPx)
        moveAnimation.addUpdateListener { animator ->
            switchThumb.translationX = animator.animatedValue as Float
        }
        moveAnimation.duration = 300

        // Запуск анімацій
        trackTransition.startTransition(300)
        thumbTransition.startTransition(300)
        moveAnimation.start()
    }
    private fun animateFalseSwitch(context: Context, switchTrack: LinearLayout, switchThumb: LinearLayout) {
        // Конвертувати dp у px
        val dpToPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, context.resources.displayMetrics)
        val dpPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0f, context.resources.displayMetrics)

        // Застосувати перехідний drawable до switchTrack
        switchTrack.setBackgroundResource(R.drawable.track_unchecked_transition)
        val trackTransition = switchTrack.background as TransitionDrawable

        // Застосувати перехідний drawable до switchThumb
        switchThumb.setBackgroundResource(R.drawable.thumb_unchecked_transition)
        val thumbTransition = switchThumb.background as TransitionDrawable

        // Анімація для переміщення switchThumb вліво на 20 dp
        val moveAnimation = ValueAnimator.ofFloat(dpToPx, dpPx)
        moveAnimation.addUpdateListener { animator ->
            switchThumb.translationX = animator.animatedValue as Float
        }
        moveAnimation.duration = 300

        // Запуск анімацій
        trackTransition.startTransition(300)
        thumbTransition.startTransition(300)
        moveAnimation.start()
    }
}