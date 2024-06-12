package com.example.duetly

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MenuMusicsAdapter(
    private val context: Context,
    private var musicList: List<MusicInfo>,
    private val playlist: Playlist,
    private val onAddListener:(Pair<MusicInfo,Int>) -> Unit,
    private val onAddedListener:(Pair<MusicInfo,Int>) -> Unit

) : RecyclerView.Adapter<MenuMusicsAdapter.MusicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.playlists_menu_item, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val m = musicList[position]
        val add = holder.itemView.findViewById<ImageView>(R.id.add)
        val added = holder.itemView.findViewById<ImageView>(R.id.added)

        add.setOnClickListener{
            onAddListener.invoke(Pair(m,position))
        }
        added.setOnClickListener{
            onAddedListener.invoke(Pair(m,position))
        }
        holder.bind(m,context,playlist)
    }

    override fun getItemCount(): Int = musicList.size

    class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(m: MusicInfo,context: Context,playlist: Playlist) {
            val dbHelper = DbHelper(context,null)
            val name = itemView.findViewById<TextView>(R.id.plName)
            val image = itemView.findViewById<ImageView>(R.id.imageView)
            val add = itemView.findViewById<ImageButton>(R.id.add)
            val added = itemView.findViewById<ImageButton>(R.id.added)
            name.text = m.displayName
            val playlistsM = dbHelper.getAllMusicsByPlaylist(playlist.id)
            val isInPl = playlistsM.contains(m.id)
            val drawable = ContextCompat.getDrawable(context,R.drawable.music_note_4_svgrepo_com)
            image.setImageDrawable(drawable)
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

