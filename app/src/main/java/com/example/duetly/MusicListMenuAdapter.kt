package com.example.duetly

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MusicListMenuAdapter(
    private val context: Context,
    private var musicList: List<MusicInfo>,
    private val onClickListener:(Pair<MusicInfo,Int>) -> Unit
) : RecyclerView.Adapter<MusicListMenuAdapter.MusicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.music_item_tiny, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {

        val music = musicList[position]
        holder.itemView.setOnClickListener {
            onClickListener.invoke(Pair(music, position))
        }
        holder.bind(music, context, position)
    }

    override fun getItemCount(): Int = musicList.size

    class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            music: MusicInfo,
            context: Context,
            position: Int,
        ) {
            val layItem = itemView.findViewById<ConstraintLayout>(R.id.list_item)
            val musicName: TextView = itemView.findViewById(R.id.songName)
            val musicTime = itemView.findViewById<TextView>(R.id.duration)
            val imageV = itemView.findViewById<ImageView>(R.id.imageView)
            val dbHelper = DbHelper(context, null)
            val aMusic = dbHelper.getPlayingMusicForId(1)

            val activeColorI = "#17022B"
            val activeColorT = ContextCompat.getColor(context, R.color.pryColor)
            val lightColor = ContextCompat.getColor(context, R.color.light)

            val inactiveColorI = "#7C0065"
            val inactiveColorT = ContextCompat.getColor(context, R.color.secColor)

            if (aMusic!!.idMusic == music.id) {
                layItem.setBackgroundResource(R.drawable.bg_sec_color)
                musicName.setTextColor(activeColorT)
                musicTime.setTextColor(activeColorT)
                val draw = paintIcon(activeColorI, context, "music_note_4_svgrepo_com")
                imageV.setImageDrawable(draw)
            } else {
                layItem.setBackgroundResource(R.drawable.background_music_list_item)
                musicName.setTextColor(lightColor)
                musicTime.setTextColor(lightColor)
                val draw = paintIcon(inactiveColorI, context, "music_note_4_svgrepo_com")
                imageV.setImageDrawable(draw)
            }


            musicName.text = music.displayName
            val musicMinutes = music.duration / 60000
            val musicSeconds = (music.duration - (musicMinutes * 60000)) / 1000
            if (musicSeconds < 10) {
                musicTime.text = "${musicMinutes}:0${musicSeconds}"
            } else {
                musicTime.text = "${musicMinutes}:${musicSeconds}"
            }
        }
    }
}
