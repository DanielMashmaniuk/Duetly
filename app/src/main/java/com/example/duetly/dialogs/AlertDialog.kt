package com.example.duetly.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.duetly.R

class AlertDialog(titleDialog:String,describeDialog:String) : DialogFragment() {
    private lateinit var rootView: View
    val title = titleDialog
    val describe = describeDialog
    interface OnAlertDialogResultListener {
        fun onDialogResult(result: Boolean)
    }

    private var listener: OnAlertDialogResultListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.alert_dialog_layout, container, false)
        val titleT = rootView.findViewById<TextView>(R.id.titleAlertD)
        val describeT = rootView.findViewById<TextView>(R.id.describeAlertD)
        val yesB = rootView.findViewById<LinearLayout>(R.id.yesAlertD)
        val noB = rootView.findViewById<LinearLayout>(R.id.noAlertD)

        titleT.text = title
        describeT.text = describe
        yesB.setOnClickListener {
            listener?.onDialogResult(true)
            dismiss()
        }
        noB.setOnClickListener {
            listener?.onDialogResult(false)
            dismiss()
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
}