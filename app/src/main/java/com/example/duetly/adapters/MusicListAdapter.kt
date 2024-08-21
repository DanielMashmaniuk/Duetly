package com.example.duetly.adapters

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.duetly.DbHelper
import com.example.duetly.fragments.MusicInfo
import com.example.duetly.R
import com.example.duetly.activities.paintIcon

class MusicListAdapter(
    private val context: Context,
    private var musicList: List<MusicInfo>,
    private val isFavorite: Boolean = false,
    private val onClickListener:(Pair<MusicInfo,Int>) -> Unit
) : RecyclerView.Adapter<MusicListAdapter.MusicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_music_list_item, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val list = musicList.toMutableList()
        list.add(MusicInfo(-1, "HOLE", "HOLE", 404, "", ""))
        val music = list[position]
        holder.itemView.setOnClickListener {
            if (position != list.size - 1) {
                onClickListener.invoke(Pair(music, position))
            }
        }
        holder.bind(music, context, isFavorite, position, list.size)
    }

    override fun getItemCount(): Int = musicList.size + 1
    fun updateList(updatedMatchList: List<MusicInfo>) {
        musicList = updatedMatchList
        notifyDataSetChanged()
    }

    class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            music: MusicInfo,
            context: Context,
            isFavorite: Boolean,
            position: Int,
            listSize: Int
        ) {
            val layItem = itemView.findViewById<ConstraintLayout>(R.id.list_item)
            val musicName: TextView = itemView.findViewById(R.id.name)
            val musicArtist: TextView = itemView.findViewById(R.id.songArtist)
            val musicTime = itemView.findViewById<TextView>(R.id.songTime)
            val imageV = itemView.findViewById<ImageView>(R.id.imageView)
            val dbHelper = DbHelper(context, null)
            val aMusic = dbHelper.getPlayingMusicForId(1)

            val activeColorI = "#17022B"
            val activeColorT = ContextCompat.getColor(context, R.color.pryColor)
            val lightColor = ContextCompat.getColor(context, R.color.light)

            val inactiveColorI = "#7C0065"
            val inactiveColorT = ContextCompat.getColor(context, R.color.secColor)
            if (isFavorite) {
                imageV.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.favorites))
            }
            if (aMusic!!.idMusic == music.id) {
                layItem.setBackgroundResource(R.drawable.bg_sec_color)
                musicName.setTextColor(activeColorT)
                musicArtist.setTextColor(activeColorT)
                musicTime.setTextColor(activeColorT)
                val draw = if (isFavorite) {
                    paintIcon(activeColorI, context, "favorites")
                } else {
                    paintIcon(activeColorI, context, "music_note_4_svgrepo_com")
                }
                imageV.setImageDrawable(draw)
            } else {
                layItem.setBackgroundResource(R.drawable.background_music_list_item)
                musicName.setTextColor(lightColor)
                musicArtist.setTextColor(lightColor)
                musicTime.setTextColor(lightColor)
                val draw = if (isFavorite) {
                    paintIcon(inactiveColorI, context, "favorites")
                } else {
                    paintIcon(inactiveColorI, context, "music_note_4_svgrepo_com")
                }
                imageV.setImageDrawable(draw)
            }


            musicName.text = music.displayName
            musicArtist.text = music.artist
            val musicMinutes = music.duration / 60000
            val musicSeconds = (music.duration - (musicMinutes * 60000)) / 1000
            if (musicSeconds < 10) {
                musicTime.text = "${musicMinutes}:0${musicSeconds}"
            } else {
                musicTime.text = "${musicMinutes}:${musicSeconds}"
            }
            if (position >= listSize - 1) {
                layItem.setBackgroundResource(R.drawable.bg_transparent)
                imageV.visibility = View.GONE
                musicArtist.visibility = View.GONE
                musicName.visibility = View.GONE
                musicTime.visibility = View.GONE
            } else {
                setItemAnim(itemView,1L)
                imageV.visibility = View.VISIBLE
                musicArtist.visibility = View.VISIBLE
                musicName.visibility = View.VISIBLE
                musicTime.visibility = View.VISIBLE
            }
        }
    }
}
fun setItemAnim(i: View,delayMillis:Long, value: Float = 0.95f) {
    i.postDelayed({
        val anim = ObjectAnimator.ofFloat(i, "scaleX", 1f, value)
        anim.duration = 300
        anim.repeatCount = 1
        anim.repeatMode = ObjectAnimator.REVERSE // Змінити напрямок після завершення
        anim.start()
    }, delayMillis)
//    i.postDelayed({
//        val anim = ObjectAnimator.ofFloat(i, "scaleX", value, 1f)
//        anim.duration = 600
//        anim.repeatCount = ObjectAnimator.REVERSE // Безперервне повторення
//        anim.start()
//    }, delayMillis+600L)
}
