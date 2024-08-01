package com.example.duetly.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.duetly.DbHelper
import com.example.duetly.fragments.Playlist
import com.example.duetly.R

class PlaylistListAdapter(
    private val context: Context,
    private var playlistList: List<Playlist>,
    private val onClickListener:(Pair<Playlist,Int>) -> Unit,
    private val onLongClickListener:(Pair<Playlist,Int>) -> Unit,
    private val onLastClickListener:(Pair<Playlist,Int>) -> Unit,



    ) : RecyclerView.Adapter<PlaylistListAdapter.MusicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.playlists_item, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val list = playlistList.toMutableList()
        list.add(Playlist(name = "HOLE"))
        val pl = list[position]
        val isLast = position == list.size-2
        val isEmty = position == list.size-1
        holder.itemView.setOnClickListener {
            if (!isLast && !isEmty) {
                onClickListener.invoke(Pair(pl, position))
            }else if(isLast){
                onLastClickListener.invoke(Pair(pl,position))
            }
        }
        holder.itemView.setOnLongClickListener {
            if (!isLast && !isEmty) {
                onLongClickListener.invoke(Pair(pl, position))
            }
            true
        }
        holder.bind(pl, context, isLast,isEmty)
    }

    override fun getItemCount(): Int = playlistList.size + 1

    class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(pl: Playlist, context: Context, isLast: Boolean, isEmty:Boolean) {
            val createBody = itemView.findViewById<ConstraintLayout>(R.id.createBBody)
            val plBody = itemView.findViewById<ConstraintLayout>(R.id.playlistBody)
            val name = itemView.findViewById<TextView>(R.id.plName)
            val sizeT = itemView.findViewById<TextView>(R.id.numberMusics)
            //val iconLast = itemView.findViewById<ImageView>(R.id.iconCreate)
            if (isLast) {
                plBody.visibility = View.GONE
            }else{
                createBody.visibility = View.GONE
            }
            if (isEmty){
                plBody.visibility = View.INVISIBLE
                createBody.visibility = View.GONE
            }
            name.text = pl.name
            val dbHelper = DbHelper(context,null)
            val sizeMusic = dbHelper.getAllMusicsByPlaylist(pl.id)
            sizeT.text = sizeMusic.size.toString()
        }
    }
}

