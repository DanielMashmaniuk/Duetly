package com.example.duetly.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.duetly.DbHelper
import com.example.duetly.fragments.MusicInfo
import com.example.duetly.fragments.Playlist
import com.example.duetly.R
import com.example.duetly.activities.encodeEmail
import com.example.duetly.models.User
import com.example.duetly.models.UserMessage
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import org.w3c.dom.Text

class UsersListAdapter(
    private val context: Context,
    private var usersList: List<Pair<User,Boolean>>,
    private val onSendListener:(Pair<User,Int>) -> Unit

) : RecyclerView.Adapter<UsersListAdapter.UsersListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_add_friend_layout, parent, false)

        return UsersListViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsersListViewHolder, position: Int) {
        val user = usersList[position]
        val send = holder.itemView.findViewById<ImageView>(R.id.add)
        val cancelB = holder.itemView.findViewById<ImageButton>(R.id.added)
        send.setOnClickListener{
            onSendListener(Pair(user.first,position))
            send.visibility = View.GONE
            cancelB.visibility = View.VISIBLE
        }
        holder.bind(user)
    }

    override fun getItemCount(): Int {
         return usersList.size
    }

    class UsersListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(user: Pair<User,Boolean>) {
            val nameT = itemView.findViewById<TextView>(R.id.name)
            val addB = itemView.findViewById<ImageButton>(R.id.add)
            val sended = itemView.findViewById<ImageButton>(R.id.added)
            val image = itemView.findViewById<LinearLayout>(R.id.linearLayout4)
            val genreT = itemView.findViewById<TextView>(R.id.genre)
            if (user.second){
                sended.visibility = View.VISIBLE
                addB.visibility = View.GONE
            }else{
                sended.visibility = View.GONE
                addB.visibility = View.VISIBLE
            }
            nameT.text = user.first.username

            if (user.first.musicGenre == "NONE") {
                genreT.visibility = View.GONE
                image.visibility = View.GONE
            } else {
                genreT.text = user.first.musicGenre
            }
        }
    }
}

