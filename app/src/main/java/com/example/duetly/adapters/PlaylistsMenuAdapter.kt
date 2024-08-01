package com.example.duetly.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.duetly.DbHelper
import com.example.duetly.fragments.MusicInfo
import com.example.duetly.fragments.Playlist
import com.example.duetly.R

class PlaylistsMenuAdapter(
    private val context: Context,
    private var playlistList: List<Playlist>,
    private val music: MusicInfo,
    private val onAddListener:(Pair<Playlist,Int>) -> Unit,
    private val onAddedListener:(Pair<Playlist,Int>) -> Unit

) : RecyclerView.Adapter<PlaylistsMenuAdapter.MusicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.playlists_menu_item, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val pl = playlistList[position]
        val add = holder.itemView.findViewById<ImageView>(R.id.add)
        val added = holder.itemView.findViewById<ImageView>(R.id.added)

        add.setOnClickListener{
            onAddListener.invoke(Pair(pl,position))
        }
        added.setOnClickListener{
            onAddedListener.invoke(Pair(pl,position))
        }
        holder.bind(pl,context,music)
    }

    override fun getItemCount(): Int = playlistList.size

    class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(pl: Playlist, context: Context, music: MusicInfo) {
            val dbHelper = DbHelper(context,null)
            val name = itemView.findViewById<TextView>(R.id.plName)
            val add = itemView.findViewById<ImageButton>(R.id.add)
            val added = itemView.findViewById<ImageButton>(R.id.added)
            name.text = pl.name
            val playlistsM = dbHelper.getAllMusicsByPlaylist(pl.id)
            val isInPl = playlistsM.contains(music.id)
            if (isInPl){
                add.visibility = View.GONE
                added.visibility = View.VISIBLE
            }else{
                add.visibility = View.VISIBLE
                added.visibility = View.GONE
            }
        }
    }
}

