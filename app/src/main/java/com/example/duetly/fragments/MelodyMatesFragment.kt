package com.example.duetly.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.example.duetly.DbHelper
import com.example.duetly.R
import com.example.duetly.dialogs.InputCodeDialog
import com.example.duetly.dialogs.SearchNewMFDialog
import com.google.firebase.firestore.FirebaseFirestore

class MelodyMatesFragment : Fragment() {
        private lateinit var rootView: View
        private lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_melodymates_layout, container, false)
        val searchB = rootView.findViewById<LinearLayout>(R.id.addFriendButton)
        firestore = FirebaseFirestore.getInstance()
        val user = DbHelper(requireContext(),null).getUser()
        searchB.setOnClickListener{
            val codeInputDialog =
                SearchNewMFDialog(firestore,user)
            codeInputDialog.show(
                parentFragmentManager,
                SearchNewMFDialog.TAG
            )
        }
        return rootView

    }
    
}