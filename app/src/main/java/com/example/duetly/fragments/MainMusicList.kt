package com.example.duetly.fragments

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.duetly.activities.paintIcon
import com.example.duetly.activities.showToast
import com.example.duetly.adapters.MenuMusicsAdapter
import com.example.duetly.adapters.MusicListAdapter
import com.example.duetly.adapters.MusicListMenuAdapter
import com.example.duetly.adapters.PlaylistListAdapter
import com.example.duetly.adapters.PlaylistsMenuAdapter
import com.example.duetly.DbHelper
import com.example.duetly.MediaPlayerManager
import com.example.duetly.PlMusic
import com.example.duetly.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import com.example.duetly.dialogs.AlertDialog

data class MusicInfo(
    val id: Long = -1L,
    var displayName: String = "",
    val artist: String? = "",
    val duration: Long = -1L,
    val imageUri:String = "",
    val data: String = ""
)
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    var name:String = ""
)


class MainMusicList : Fragment(),AlertDialog.AlertDialogResult {
    private var startY = 0f
    private lateinit var editText: EditText
    private lateinit var searchMusic: ConstraintLayout
    private lateinit var createPlMenu: ConstraintLayout

    var modeGMusic = true
    var modeFavorites = false
    var modePlaylists = false
    var modeRecPl = false

    private var isActiveWindow: Boolean = false
    private var isPlListMus: Boolean = false
    private var isPlaying: Boolean = false
    lateinit var dbHelper: DbHelper
    lateinit var musicAdapter: MusicListAdapter
    lateinit var musicMenuAdapter: MusicListMenuAdapter
    lateinit var plMenuAdapter: PlaylistsMenuAdapter
    lateinit var menuMusicsAdapter: MenuMusicsAdapter
    lateinit var plAdapter: PlaylistListAdapter
    lateinit var musicRec: RecyclerView
    lateinit var musicWindow: ConstraintLayout
    lateinit var playlistInfo:  ConstraintLayout
    lateinit var songName: TextView
    lateinit var resumeImage: ImageButton
    lateinit var pauseImage: ImageButton
    lateinit var sizeT:TextView
    private var musicList = mutableListOf<MusicInfo>()
    private var temporaryMusicList = mutableListOf<MusicInfo>()
    private var currentMusicIndex = 0
    private var mode = ""
    private var isEditPl = false
    lateinit var mediaPlayer: MediaPlayer
    private val historyMusic = mutableListOf<MusicInfo>()
    private var historyIndex = 0
    val musicEmty: MusicInfo = MusicInfo()
    lateinit var music: MusicInfo
    lateinit var playlist: Playlist
    lateinit var rootView : View
    private var plId = "-1"
    private val READ_EXTERNAL_STORAGE_REQUEST = 1
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_main_music_list, container, false)
        editText = rootView.findViewById(R.id.inputNameMusic)
        searchMusic = rootView.findViewById(R.id.search_music)
        musicRec = rootView.findViewById(R.id.musicList)
        musicWindow = rootView.findViewById(R.id.musicWindow)
        playlistInfo = rootView.findViewById(R.id.playlistInfo)
        songName = rootView.findViewById(R.id.name)
        pauseImage = rootView.findViewById(R.id.pause)
        resumeImage = rootView.findViewById(R.id.resume)
        dbHelper = DbHelper(requireContext(), null)
        sizeT = rootView.findViewById(R.id.sizeMusics)
        createPlMenu = rootView.findViewById(R.id.playlistsMenu)
        createPlMenu.visibility = View.GONE
        val favoritesB = rootView.findViewById<LinearLayout>(R.id.favoritesListB)
        val playlistsB = rootView.findViewById<LinearLayout>(R.id.playlists)
        val recentB = rootView.findViewById<LinearLayout>(R.id.recentB)
        val favIcon = rootView.findViewById<ImageView>(R.id.favIcon)
        val plIcon = rootView.findViewById<ImageView>(R.id.plIcon)
        val recentIcon = rootView.findViewById<ImageView>(R.id.recIcon)
        val favText = rootView.findViewById<TextView>(R.id.favText)
        val plText  = rootView.findViewById<TextView>(R.id.plText)
        val recentText  = rootView.findViewById<TextView>(R.id.recText)
        music = musicEmty
        mode = dbHelper.getPlayingMode(1)
        musicList = mutableListOf()
        musicWindow.visibility = View.GONE
        playlistInfo.visibility = View.GONE
        CoroutineScope(Dispatchers.Main).launch {
            modeGMusic = true
            modeFavorites = false
            modePlaylists = false
            modeRecPl = false
            val getMusicList =
                withContext(Dispatchers.IO) { dbHelper.getAllMusics() }
            if (getMusicList.isNotEmpty()){
                musicList = getMusicList.toMutableList()
                setupMusicAdapter(musicList)
                temporaryMusicList = musicList

            }
            val aMusicInfo = withContext(Dispatchers.IO) {dbHelper.getPlayingMusicForId(1)}
            var isM = false
            for (i in musicList) {
                if (i.id == aMusicInfo!!.idMusic) {
                    isM = true
                    music = i
                }
            }
            if (aMusicInfo!!.idMusic != -1L && isM) {
                isActiveWindow = true
                setAnimMusicWindow(musicWindow)
                mediaPlayer = MediaPlayerManager.getMediaPlayer()
                songName.text = music.displayName
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying) {
                        resumeImage.visibility = View.GONE
                        pauseImage.visibility = View.VISIBLE
                    } else {
                        MediaPlayerManager.releaseMediaPlayer()
                        MediaPlayerManager.initialize(requireContext(), music.data)
                        resumeImage.visibility = View.VISIBLE
                        pauseImage.visibility = View.GONE
                    }
                }else{
                    MediaPlayerManager.releaseMediaPlayer()
                    MediaPlayerManager.initialize(requireContext(), music.data)
                    resumeImage.visibility = View.VISIBLE
                    pauseImage.visibility = View.GONE
                }
                mediaPlayer = MediaPlayerManager.getMediaPlayer()
                val index = musicList.indexOfFirst { it == music }
                currentMusicIndex = index
                mediaPlayer.setOnCompletionListener {
                    // Перевіряємо, чи є ще музика для відтворення
                    if (currentMusicIndex < musicList.size - 1) {
                        var nextMusic = musicEmty
                        nextMusic = when (mode) {
                            "NEXT" -> {
                                musicList[currentMusicIndex + 1]
                            }

                            "REPEAT" -> {
                                music
                            }

                            else -> {
                                val list = musicList
                                list.remove(music)
                                list.random()
                            }
                        }
                        playMusic(nextMusic, musicList)
                        dbHelper.updatePM(nextMusic.id, 1, 0)
                        songName.text = nextMusic.displayName
                        musicAdapter.notifyItemChanged(currentMusicIndex)
                        musicAdapter.notifyItemChanged(currentMusicIndex - 1)
                    } else {
                        val firstMusic = musicList[0]
                        playMusic(firstMusic, musicList)
                        dbHelper.updatePM(firstMusic.id, 1, 0)
                        songName.text = firstMusic.displayName

                    }
                }
            } else {
                isActiveWindow = false
                music = musicEmty
            }
            if (!isActiveWindow) {
                musicWindow.visibility = View.GONE
            } else {
                musicWindow.visibility = View.VISIBLE
            }
        }
            CoroutineScope(Dispatchers.Main).launch {
                    val musicsL = withContext(Dispatchers.IO) { dbHelper.getAllMusics() }
                    var popupWindow = PopupWindow()
                    if (musicsL.isEmpty()) {
                        delay(500)
                        popupWindow = showWaitAnim(requireContext())


                        val getMusicList =
                            withContext(Dispatchers.IO) { getMusicFiles(requireContext()) }

                        for (i in getMusicList) {
                            if (i.duration >= 30000) {
                                i.displayName = i.displayName.dropLast(4)
                                if (musicsL.contains(i)) {
                                } else {
                                    withContext(Dispatchers.IO) { dbHelper.addMusic(i) }
                                    musicList.add(i)
                                }
                            }
                        }
                        for (i in musicsL) {
                            if (getMusicList.contains(i)) {
                            } else {
                                withContext(Dispatchers.IO) { dbHelper.deleteMusic(i.id) }
                                if (musicList.contains(i)) {
                                    musicList.remove(i)
                                }
                            }
                        }

                        if (modeGMusic) {
                            temporaryMusicList = musicList
                            val list2 = mutableListOf<MusicInfo>()
                            for (i in getMusicList) {
                                if (i.duration >= 30000) {
                                    list2.add(i)
                                }
                            }
                            if (musicsL != list2) {
                                setupMusicAdapter(getMusicList)
                            }
                        }
                        if (popupWindow != null && popupWindow.isShowing) {
                            popupWindow.dismiss()
                        }
                    }

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        READ_EXTERNAL_STORAGE_REQUEST
                    )
                }

                editText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        charSequence: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        charSequence: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        historyMusic.clear()
                        val text = charSequence.toString().uppercase()
                        val setupList = mutableListOf<MusicInfo>()
                        for (m in temporaryMusicList) {
                            if (
                                m.displayName.uppercase().contains(text) ||
                                m.artist?.uppercase()?.contains(text) == true
                            ) {
                                setupList.add(m)
                            }
                        }
                        temporaryMusicList = setupList
                        setupMusicAdapter(temporaryMusicList)
                    }

                    override fun afterTextChanged(editable: Editable?) {}
                })

                resumeImage.setOnClickListener {
                    mediaPlayer.start()
                    resumeImage.visibility = View.GONE
                    pauseImage.visibility = View.VISIBLE
                    isPlaying = true

                }
                pauseImage.setOnClickListener {
                    mediaPlayer.pause()
                    pauseImage.visibility = View.GONE
                    resumeImage.visibility = View.VISIBLE
                    isPlaying = false
                }

                musicWindow.setOnClickListener {
                    showMusicFrame(requireContext())
                }
        }
        val activeColorI = "#17022B"
        val activeColorT = ContextCompat.getColor(requireContext(), R.color.pryColor)

        val inactiveColorI = "#7C0065"
        val inactiveColorT = ContextCompat.getColor(requireContext(), R.color.secColor)

        favoritesB.setOnClickListener{
            searchMusic.visibility = View.VISIBLE
            createPlMenu.visibility = View.GONE
            playlistInfo.visibility = View.GONE
            playlist = Playlist()
            if (!modeFavorites) {
                modeGMusic = false
                modeFavorites = true
                modePlaylists = false
                modeRecPl = false
                favoritesB.setBackgroundResource(R.drawable.bg_sec_color)
                playlistsB.setBackgroundResource(R.drawable.background_black_w_prycolor)
                recentB.setBackgroundResource(R.drawable.background_black_w_prycolor)

                val favI = paintIcon(activeColorI,requireContext(),"favorites")
                favIcon.setImageDrawable(favI)
                favText.setTextColor(activeColorT)

                val plI = paintIcon(inactiveColorI,requireContext(),"playlist_svgrepo_com")
                plIcon.setImageDrawable(plI)
                plText.setTextColor(inactiveColorT)

                val recentI = paintIcon(inactiveColorI,requireContext(),"recent_svgrepo_com")
                recentIcon.setImageDrawable(recentI)
                recentText.setTextColor(inactiveColorT)

                val favoritesMusics = dbHelper.getAllFMusics()
                val filtList = mutableListOf<MusicInfo>()
                for (m in musicList) {
                    for (f in favoritesMusics) {
                        if (m.id == f) {
                            filtList.add(m)
                        }
                    }
                }
                temporaryMusicList = filtList
                setupMusicAdapter(temporaryMusicList, true)
            }else{
                favoritesB.setBackgroundResource(R.drawable.background_black_w_prycolor)
                val favI = paintIcon(inactiveColorI,requireContext(),"favorites")
                favIcon.setImageDrawable(favI)
                favText.setTextColor(inactiveColorT)

                modeGMusic = true
                modeFavorites = false
                modePlaylists = false
                modeRecPl = false
                val allMusic = dbHelper.getAllMusics()
                musicList = allMusic.toMutableList()
                temporaryMusicList = musicList
                setupMusicAdapter(allMusic)
            }
        }
        playlistsB.setOnClickListener{
            playlist = Playlist()
            if (!modePlaylists) {
                searchMusic.visibility = View.GONE
                playlistInfo.visibility = View.GONE
                favoritesB.setBackgroundResource(R.drawable.background_black_w_prycolor)
                playlistsB.setBackgroundResource(R.drawable.bg_sec_color)
                recentB.setBackgroundResource(R.drawable.background_black_w_prycolor)

                val favI = paintIcon(inactiveColorI,requireContext(),"favorites")
                favIcon.setImageDrawable(favI)
                favText.setTextColor(inactiveColorT)

                val plI = paintIcon(activeColorI,requireContext(),"playlist_svgrepo_com")
                plIcon.setImageDrawable(plI)
                plText.setTextColor(activeColorT)

                val recentI = paintIcon(inactiveColorI,requireContext(),"recent_svgrepo_com")
                recentIcon.setImageDrawable(recentI)
                recentText.setTextColor(inactiveColorT)

                modeGMusic = false
                modeFavorites = false
                modePlaylists = true
                modeRecPl = false
                val pls = dbHelper.getAllPlaylists()
                setupPlaylistsAdapter(pls)
            }else{
                playlistInfo.visibility = View.GONE
                searchMusic.visibility = View.VISIBLE
                playlistsB.setBackgroundResource(R.drawable.background_black_w_prycolor)
                val plI = paintIcon(inactiveColorI,requireContext(),"playlist_svgrepo_com")
                plIcon.setImageDrawable(plI)
                plText.setTextColor(inactiveColorT)

                val allMusic = dbHelper.getAllMusics()
                musicList = allMusic.toMutableList()
                temporaryMusicList = musicList
                setupMusicAdapter(allMusic)
                modeGMusic = true
                modeFavorites = false
                modePlaylists = false
                modeRecPl = false
                createPlMenu.visibility = View.GONE
            }
        }
        recentB.setOnClickListener{
            playlist = Playlist()
            searchMusic.visibility = View.VISIBLE
            createPlMenu.visibility = View.GONE
            playlistInfo.visibility = View.GONE
            if (!modeRecPl) {
                modeGMusic = false
                modeFavorites = false
                modePlaylists = false
                modeRecPl = true
                favoritesB.setBackgroundResource(R.drawable.background_black_w_prycolor)
                playlistsB.setBackgroundResource(R.drawable.background_black_w_prycolor)
                recentB.setBackgroundResource(R.drawable.bg_sec_color)

                val favI = paintIcon(inactiveColorI,requireContext(),"favorites")
                favIcon.setImageDrawable(favI)
                favText.setTextColor(inactiveColorT)

                val plI = paintIcon(inactiveColorI,requireContext(),"playlist_svgrepo_com")
                plIcon.setImageDrawable(plI)
                plText.setTextColor(inactiveColorT)

                val recentI = paintIcon(activeColorI,requireContext(),"recent_svgrepo_com")
                recentIcon.setImageDrawable(recentI)
                recentText.setTextColor(activeColorT)

                val historyMusics = dbHelper.getAllMusicsFromH()
                val listT = historyMusics.sortedByDescending { it.id }
                for (y in listT){
                    Log.d("ID",y.toString())
                }
                val filtList = mutableListOf<MusicInfo>()
                for (f in listT) {
                    for (m in musicList) {
                        if (m.id == f.idM) {
                            filtList.add(m)
                        }
                    }
                }
                temporaryMusicList = filtList
                setupMusicAdapter(temporaryMusicList)
            }else{
                recentB.setBackgroundResource(R.drawable.background_black_w_prycolor)
                val recentI = paintIcon(inactiveColorI,requireContext(),"recent_svgrepo_com")
                recentIcon.setImageDrawable(recentI)
                recentText.setTextColor(inactiveColorT)

                modeGMusic = true
                modeFavorites = false
                modePlaylists = false
                modeRecPl = false
                val allMusic = dbHelper.getAllMusics()
                musicList = allMusic.toMutableList()
                temporaryMusicList = musicList
                setupMusicAdapter(allMusic)
            }
        }
        return rootView

    }

    override fun onDestroy() {
        super.onDestroy()
        modeGMusic = false
        modeFavorites = false
        modePlaylists = false
        modeRecPl = false
    }

    private fun showMusicFrame(context: Context): PopupWindow {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.music_info_frame, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        // Створити слухача жестів
        val MIN_FLING_VELOCITY = 500 // Налаштуйте це значення за бажанням

        val gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    // Зберегти початкову точку натискання
                    startY = e.y
                    return true
                }

                // Обробка скролу
                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (e1 != null && e2 != null) {
                        handleTouchEvent(popupView, e2, context, startY, popupWindow)
                    }
                    return super.onScroll(e1, e2, distanceX, distanceY)
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (velocityY > MIN_FLING_VELOCITY) { // Налаштуйте MIN_FLING_VELOCITY для бажаної чутливості
                        popupWindow.dismiss()
                        return true
                    }
                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            })
        popupView.setOnTouchListener { v, event ->
            gestureDetector.run { onTouchEvent(event) }
            // Виклик performClick для обробки кліків
            if (event.action == MotionEvent.ACTION_UP && !gestureDetector.onTouchEvent(event)) {
                v.performClick()
            }

            true
        }

        val songNameT = popupView.findViewById<TextView>(R.id.name)
        val artistT = popupView.findViewById<TextView>(R.id.songArtist)
        val currentTimeT = popupView.findViewById<TextView>(R.id.currentTime)
        val songTimeT = popupView.findViewById<TextView>(R.id.textView)
        val timeLine = popupView.findViewById<SeekBar>(R.id.currentTimeLine)
        val songImage = popupView.findViewById<ImageView>(R.id.musicImage)
        val optionsButton = popupView.findViewById<ImageView>(R.id.options)
        val favoriteFalse = popupView.findViewById<ImageButton>(R.id.favoriteFalse)
        val favoriteTrue = popupView.findViewById<ImageButton>(R.id.favoriteTrue)


        val pauseB = popupView.findViewById<ImageButton>(R.id.pause)
        val resumeB = popupView.findViewById<ImageButton>(R.id.start)
        val nextB = popupView.findViewById<ImageButton>(R.id.next)
        val previusB = popupView.findViewById<ImageButton>(R.id.previous)
        val musicListB = popupView.findViewById<ImageButton>(R.id.musicList)

        val repeatB = popupView.findViewById<ImageButton>(R.id.refresh)
        val repeatOneB = popupView.findViewById<ImageButton>(R.id.iteration)
        val mixB = popupView.findViewById<ImageButton>(R.id.shuffle)

        val currentPosition = mediaPlayer.currentPosition
        val isPlaying = mediaPlayer.isPlaying

        val musicMinutes = currentPosition / 60000
        val musicSeconds = (currentPosition - (musicMinutes * 60000)) / 1000
        if (musicSeconds < 10) {
            currentTimeT.text = "${musicMinutes}:0${musicSeconds}"
        } else {
            currentTimeT.text = "${musicMinutes}:${musicSeconds}"
        }
        setMusicInfo(songNameT, artistT, songTimeT)
        if (isPlaying) {
            resumeB.visibility = View.GONE
            pauseB.visibility = View.VISIBLE
        } else {
            resumeB.visibility = View.VISIBLE
            pauseB.visibility = View.GONE
        }
        checkFavorite(music,favoriteTrue,favoriteFalse)
        when (mode) {
            "NEXT" -> {
                repeatB.visibility = View.VISIBLE
                repeatOneB.visibility = View.GONE
                mixB.visibility = View.GONE
            }

            "REPEAT" -> {
                repeatB.visibility = View.GONE
                repeatOneB.visibility = View.VISIBLE
                mixB.visibility = View.GONE
            }

            else -> {
                repeatB.visibility = View.GONE
                repeatOneB.visibility = View.GONE
                mixB.visibility = View.VISIBLE
            }
        }
        musicListB.setOnClickListener{
            showBottomItemsList(context,3)
        }
        optionsButton.setOnClickListener{
            showPopupMenu(optionsButton,requireContext())
        }
        favoriteFalse.setOnClickListener{
            dbHelper.addFavoriteMusic(music.id)
            favoriteFalse.visibility = View.GONE
            favoriteTrue.visibility = View.VISIBLE
            setButtonAnimation(favoriteTrue)
        }
        favoriteTrue.setOnClickListener {
            dbHelper.deleteFavoriteM(music.id)
            favoriteFalse.visibility = View.VISIBLE
            favoriteTrue.visibility = View.GONE
            setButtonAnimation(favoriteFalse)
        }
        repeatB.setOnClickListener {
            dbHelper.changeMode("REPEAT")
            mode = "REPEAT"
            repeatB.visibility = View.GONE
            repeatOneB.visibility = View.VISIBLE
            mixB.visibility = View.GONE
            setButtonAnimation(repeatOneB)
        }
        repeatOneB.setOnClickListener {
            dbHelper.changeMode("MIX")
            mode = "MIX"
            repeatB.visibility = View.GONE
            repeatOneB.visibility = View.GONE
            mixB.visibility = View.VISIBLE
            setButtonAnimation(mixB)
        }
        mixB.setOnClickListener {
            dbHelper.changeMode("NEXT")
            mode = "NEXT"
            repeatB.visibility = View.VISIBLE
            repeatOneB.visibility = View.GONE
            mixB.visibility = View.GONE
            setButtonAnimation(repeatB)
        }
        pauseB.setOnClickListener {
            mediaPlayer.pause()
            resumeB.visibility = View.VISIBLE
            pauseB.visibility = View.GONE
            resumeImage.visibility = View.VISIBLE
            pauseImage.visibility = View.GONE
            setButtonAnimation(resumeB)
        }
        resumeB.setOnClickListener {
            mediaPlayer.start()
            resumeB.visibility = View.GONE
            pauseB.visibility = View.VISIBLE
            resumeImage.visibility = View.GONE
            pauseImage.visibility = View.VISIBLE
            setButtonAnimation(pauseB)

        }
        nextB.setOnClickListener {
            setButtonAnimation(nextB)
            timeLine.progress = 0
            currentTimeT.text = "0:00"
            var nextMusic = musicEmty
            nextMusic = when (mode) {
                "NEXT" -> {
                    if (temporaryMusicList.indexOf(music) < temporaryMusicList.size - 1) {
                        temporaryMusicList[temporaryMusicList.indexOf(music)+1]
                    } else {
                        temporaryMusicList[0]
                    }
                }

                "REPEAT" -> {
                    music
                }

                else -> {
                    val list = temporaryMusicList.toMutableList()
                    list.remove(music)
                    val mus = list.random()
                    mus
                }
            }
            playMusic(nextMusic, temporaryMusicList)
            checkFavorite(nextMusic,favoriteTrue,favoriteFalse)
            resumeB.visibility = View.GONE
            pauseB.visibility = View.VISIBLE
            resumeImage.visibility = View.GONE
            pauseImage.visibility = View.VISIBLE
            setMusicInfo(songNameT, artistT, songTimeT)

        }
        previusB.setOnClickListener {
            setButtonAnimation(previusB,)
            timeLine.progress = 0
            currentTimeT.text = "0:00"
            var nextMusic = musicEmty
            if (mode == "NEXT") {
                nextMusic = if (temporaryMusicList.indexOf(music) > 0) {
                    temporaryMusicList[temporaryMusicList.indexOf(music) - 1]
                } else {
                    temporaryMusicList[temporaryMusicList.size - 1]
                }
            } else if (mode == "REPEAT") {
                nextMusic = music
            } else {
                if (historyMusic.size > 1) {
                    if (historyIndex > 0) {
                        historyMusic.remove(historyMusic[historyMusic.size-1])
                        historyIndex = historyMusic.size - 1
                        nextMusic = historyMusic[historyIndex]
                    } else {
                        historyIndex = historyMusic.size - 1
                        val list = temporaryMusicList.toMutableList()
                        list.remove(music)
                        nextMusic = list.random()
                    }
                } else {
                    historyIndex = historyMusic.size - 1
                    val list = temporaryMusicList.toMutableList()
                    list.remove(music)
                    nextMusic = list.random()
                }
            }

            playMusic(nextMusic, temporaryMusicList)
            checkFavorite(nextMusic,favoriteTrue,favoriteFalse)
            resumeB.visibility = View.GONE
            pauseB.visibility = View.VISIBLE
            resumeImage.visibility = View.GONE
            pauseImage.visibility = View.VISIBLE
            setMusicInfo(songNameT, artistT, songTimeT)
        }
        mediaPlayer.setOnCompletionListener {
            // Перевіряємо, чи є ще музика для відтворення
            setMusicInfo(songNameT, artistT, songTimeT)

        }
        timeLine.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Не потрібно виконувати жодних дій тут, бо ми чекаємо, коли користувач відпустить повзунок
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mediaPlayer.pause()
                val time = (((seekBar?.progress ?: 0) * (music.duration)) / 1000).toInt()
                mediaPlayer.seekTo(time)
                if (pauseB.visibility == View.VISIBLE) {
                    mediaPlayer.start()
                }
            }
        })
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateSeekBar(timeLine, currentTimeT)
                handler.postDelayed(this, 1000) // Оновлюйте кожну секунду
            }
        }, 0)
        showMI(popupWindow)
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
        return popupWindow
    }
    private fun checkFavorite(mus: MusicInfo, like:ImageButton, unlike:ImageButton):Boolean{
        val favList = dbHelper.getAllFMusics()
        return if (favList.contains(mus.id)){
            like.visibility = View.VISIBLE
            unlike.visibility = View.GONE
            true
        }else{
            like.visibility = View.GONE
            unlike.visibility = View.VISIBLE
            false
        }
    }
    private fun updateSeekBar(timeLine: SeekBar, currentTimeT: TextView) {
        if (mediaPlayer != null || music != MusicInfo()) {
            timeLine.max = 1000
            val duration = mediaPlayer.duration
            val currentPosition = mediaPlayer.currentPosition
            val progress = (currentPosition.toDouble() / duration.toDouble() * 1000).toInt()
            val musicMinutes = currentPosition / 60000
            val musicSeconds = (currentPosition - (musicMinutes * 60000)) / 1000
            if (musicSeconds < 10) {
                currentTimeT.text = "${musicMinutes}:0${musicSeconds}"
            } else {
                currentTimeT.text = "${musicMinutes}:${musicSeconds}"
            }
            timeLine.progress = progress
        }
    }

    private fun setMusicInfo(nameT: TextView, artistT: TextView, timeT: TextView) {
        nameT.text = music.displayName
        artistT.text = music.artist
        val musMinutes = music.duration / 60000
        val musSeconds = (music.duration - (musMinutes * 60000)) / 1000
        if (musSeconds < 10) {
            timeT.text = "${musMinutes}:0${musSeconds}"
        } else {
            timeT.text = "${musMinutes}:${musSeconds}"
        }
    }

    private fun handleTouchEvent(
        view: View,
        event: MotionEvent,
        context: Context,
        startY: Float,
        popupWindow: PopupWindow
    ) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                view.clearAnimation()
            }

            MotionEvent.ACTION_UP -> {
                returnMI(view)
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaY = startY - event.y

                val currentY = view.y

                val parent = view.parent as ViewGroup
                val parentHeight = parent.height.toFloat()
                val absoluteY = currentY - deltaY
                view.y = when {
                    parentHeight - absoluteY > parentHeight -> 0f
                    else -> absoluteY
                }
                if (parentHeight - absoluteY <= parentHeight / 1.5) {
                    hideMI(popupWindow)
                } else {
                    returnMI(view)
                }
                true
            }
        }
    }
    private fun handleTouchEventForMenu(
        view: View,
        event: MotionEvent,
        startY: Float,
        popupWindow: PopupWindow
    ) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                view.clearAnimation()
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaY = startY - event.y

                val currentY = view.y

                val popupWindowH = popupWindow.height.toFloat()
                val absoluteY = currentY - deltaY
                if (popupWindowH - absoluteY < popupWindowH) {
                    view.y = absoluteY
                }
                if (popupWindowH - absoluteY <= popupWindowH / 2) {
                    hideMI(popupWindow)
                    isPlListMus = false
                } else {
                    returnMI(view)
                }
                true
            }
        }
    }
    private fun returnMI(view: View) {
        val parent = view.parent as ViewGroup
        val parentHeight = parent.height.toFloat()

        val currentY = view.y

        val animator = ObjectAnimator.ofFloat(view, "y", currentY, 0f)

        animator.duration = 500

        animator.start()
    }
    private fun setupPlaylistsAdapter(plList: List<Playlist>) {
        val list = plList.toMutableList()
        list.add(Playlist(name = "101"))
        plAdapter = PlaylistListAdapter(requireContext(), list,
            {
                plId = it.first.id
                playlist = it.first
                val plsMs = dbHelper.getAllMusicsByPlaylist(it.first.id)
                val list = mutableListOf<MusicInfo>()
                for (i in plsMs) {
                    for (m in musicList) {
                        if (i == m.id) {
                            list.add(m)
                        }
                    }
                }
                temporaryMusicList = list
                setupMusicAdapter(temporaryMusicList)
                modePlaylists = false
                playlistInfo.visibility = View.VISIBLE
                val namePl = rootView.findViewById<EditText>(R.id.plNameNav)
                val renamePl = rootView.findViewById<ImageButton>(R.id.renamePl)
                val addMusics = rootView.findViewById<ImageButton>(R.id.addMusics)
                val deletePl = rootView.findViewById<ImageButton>(R.id.delPlNav)
                namePl.setText(it.first.name)
                var popupWindow = PopupWindow()
                addMusics.setOnClickListener{
                    setButtonAnimation(addMusics,1.2f)
                    if (!isPlListMus) {
                        popupWindow = showBottomItemsList(requireContext(), 1, playlist)
                        isPlListMus = true
                    }else{
                        popupWindow.dismiss()
                        isPlListMus = false
                    }
                }
                val alertDialog = AlertDialog(
                    "Delete Playlist",
                    "Are you sure you want to delete this playlist?"
                )
                alertDialog.show(parentFragmentManager,AlertDialog.TAG)

                namePl.isFocusable = false
                namePl.isFocusableInTouchMode = false
                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                renamePl.setOnClickListener {
                    setButtonAnimation(renamePl,1.2f)
                    val drawable = if (isEditPl){
                        namePl.clearFocus()
                        namePl.isFocusable = false
                        namePl.isFocusableInTouchMode = false
                        if (inputMethodManager.isActive) {
                            inputMethodManager.hideSoftInputFromWindow(namePl.windowToken, 0)
                        }
                        isEditPl = false
                        dbHelper.renamePl(plId,namePl.text.toString())
                        ContextCompat.getDrawable(requireContext(), R.drawable.rename_svgrepo_com)
                    }else {
                        namePl.isFocusable = true
                        namePl.isFocusableInTouchMode = true
                        namePl.requestFocus()
                        namePl.setSelection(namePl.text.length)
                        inputMethodManager.showSoftInput(namePl, InputMethodManager.SHOW_IMPLICIT)
                        isEditPl = true
                        ContextCompat.getDrawable(requireContext(), R.drawable.check)
                    }
                    renamePl.setImageDrawable(drawable)


                }
                namePl.setOnEditorActionListener { v, actionId, event ->
                    if (
                        actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        ){
                        namePl.clearFocus()
                        namePl.isFocusable = false
                        namePl.isFocusableInTouchMode = false
                        inputMethodManager.hideSoftInputFromWindow(namePl.windowToken, 0)
                        isEditPl = false
                        dbHelper.renamePl(plId,namePl.text.toString())
                        renamePl.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                            R.drawable.rename_svgrepo_com
                        ))
                        true
                    } else {
                        false
                    }
                }
            },

            {

            },
            {
                if (createPlMenu.visibility == View.GONE) {
                    createPlMenu.setOnClickListener {
                        createPlMenu.visibility = View.GONE
                    }
                    createPlMenu.visibility = View.VISIBLE
                    val inputName = rootView.findViewById<EditText>(R.id.plNameInp)
                    val text = rootView.findViewById<TextView>(R.id.textView2)
                    val createB = rootView.findViewById<Button>(R.id.createPl)
                    inputName.text.clear()
                    text.text = "New Playlist"
                    createB.text = "Create"
                    createB.setOnClickListener {
                        if (inputName.text.toString() != "") {
                            val pl = Playlist(name = inputName.text.toString())
                            dbHelper.addPlaylist(pl)
                            val pls = dbHelper.getAllPlaylists()
                            setupPlaylistsAdapter(pls)
                            createPlMenu.visibility = View.GONE
                        } else {
                            showToast(requireContext(), "Name cannot be empty")
                        }
                    }
                } else {
                    createPlMenu.visibility = View.GONE
                }
            }
        )
        musicRec.layoutManager = LinearLayoutManager(requireContext())
        musicRec.adapter = plAdapter
    }
    private fun setupMusicAdapter(musicL: List<MusicInfo>, isFavorite: Boolean = false) {
        sizeT.text = musicL.size.toString()
        musicAdapter = MusicListAdapter(requireContext(), musicL,isFavorite) {
            dbHelper.updatePM(it.first.id, 1, 0)
            resumeImage.visibility = View.GONE
            pauseImage.visibility = View.VISIBLE
            if (!isActiveWindow) {
                setAnimMusicWindow(musicWindow)
                isActiveWindow = true
            }
            if (it.first == music){
                if (mediaPlayer.isPlaying){
                    mediaPlayer.pause()
                    resumeImage.visibility = View.VISIBLE
                    pauseImage.visibility = View.GONE
                    musicAdapter.notifyItemChanged(it.second)
                }else{
                    mediaPlayer.start()
                    resumeImage.visibility = View.GONE
                    pauseImage.visibility = View.VISIBLE
                    musicAdapter.notifyItemChanged(it.second)

                }
            }else {
                playMusic(it.first, musicL)
            }
        }
        musicRec.layoutManager = LinearLayoutManager(requireContext())
        musicRec.adapter = musicAdapter
    }

    private fun playMusic(mus: MusicInfo, musList: List<MusicInfo>) {
        val dbHelper = DbHelper(requireContext(), null)
        val index = musList.indexOfFirst { it == mus }
        if (mediaPlayer == null || music != MusicInfo() ) {

        }else{
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
        }
        val hsList = dbHelper.getAllMusicsFromH()
        val checkList = hsList.sortedBy { it.id }
        val hm = dbHelper.getMFromH(mus.id)
        if (hsList.isEmpty()){
            dbHelper.addMusicToH(0 ,mus,1)
        } else if (hm.idM == -1L){
            dbHelper.addMusicToH(checkList.last().id+1 ,mus,1)

        }else{
            dbHelper.deleteMFromH(mus.id)
            dbHelper.addMusicToH(checkList.last().id+1,mus,hm.number+1)

        }
        MediaPlayerManager.releaseMediaPlayer()
        MediaPlayerManager.initialize(requireContext(), mus.data)
        mediaPlayer = MediaPlayerManager.getMediaPlayer()
        mediaPlayer.start()
        isPlaying = true
        musicAdapter.notifyItemChanged(temporaryMusicList.indexOf(music))
        showToast(requireContext(),"${temporaryMusicList.indexOf(music)}")
        music = musList[index]
        songName.text = music.displayName
        dbHelper.updatePM(music.id, 1, 0)
        musicAdapter.notifyItemChanged(temporaryMusicList.indexOf(music))
        showToast(requireContext(),"${temporaryMusicList.indexOf(music)}")
        currentMusicIndex = index
        if (historyMusic.isNotEmpty()) {
            if (music != historyMusic[historyMusic.size-1]) {
                historyMusic.add(music)
            }
        }else {
            historyMusic.add(music)
        }
        // Встановлення прослуховувача завершення музики
        mediaPlayer.setOnCompletionListener {
            if (temporaryMusicList.size == 0) {
                var nextMusic = musicEmty
                nextMusic = when (mode) {
                    "NEXT" -> {
                        if(currentMusicIndex<temporaryMusicList.size-1){
                            temporaryMusicList[currentMusicIndex + 1]
                        }else{
                            temporaryMusicList[0]
                        }
                    }

                    "REPEAT" -> {
                        music
                    }

                    else -> {
                        val list = musicList
                        list.remove(music)
                        val m = list.random()
                        m
                    }
                }
                    playMusic(nextMusic, temporaryMusicList)
                    dbHelper.updatePM(nextMusic.id, 1, 0)
                    songName.text = nextMusic.displayName
            } else {
                MediaPlayerManager.releaseMediaPlayer()
                pauseImage.visibility = View.GONE
                resumeImage.visibility = View.VISIBLE
                setHideAnimMusicWindow(musicWindow)
                isActiveWindow = false
                dbHelper.updatePM(-1, 0, 0)
            }
        }
    }
    private fun showPopupMenu(view: ImageView,context: Context) {
        val popupMenu = PopupMenu(context, view)
        val addMusicToP = "Add music to playlist"

        popupMenu.menu.add(addMusicToP)

        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            handleMenuItemClick(menuItem,context)
        }

        popupMenu.show()
    }
    private fun handleMenuItemClick(menuItem: MenuItem,context: Context): Boolean {
        if(menuItem.itemId == 0){
            showBottomItemsList(context,0)
        }
        return true
    }

    private fun showBottomItemsList(context: Context,actionId:Int,playlist: Playlist = Playlist(name = "")): PopupWindow {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = if (actionId == 3) {
            inflater.inflate(R.layout.poput_mini_playlist, null)
        }else{
            inflater.inflate(R.layout.playlists_menu, null)
        }
        val popupWindow = PopupWindow(popupView, 1000, 1000)
        val recyclerView = popupView.findViewById<RecyclerView>(R.id.recyclerView)
        val animationStyle = R.style.PopupAnimation
        var startYMenu = 0F
        popupWindow.animationStyle = animationStyle
        val MIN_FLING_VELOCITY = 500

        val gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    // Зберегти початкову точку натискання
                    startYMenu = e.y
                    return true
                }

                // Обробка скролу
                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (e1 != null && e2 != null) {
                        handleTouchEventForMenu(popupView, e2, startYMenu, popupWindow)
                    }
                    return super.onScroll(e1, e2, distanceX, distanceY)
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (velocityY > MIN_FLING_VELOCITY) { // Налаштуйте MIN_FLING_VELOCITY для бажаної чутливості
                        popupWindow.dismiss()
                        isPlListMus = false
                        return true
                    }
                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            })
        popupView.setOnTouchListener { v, event ->
            gestureDetector.run { onTouchEvent(event) }
            // Виклик performClick для обробки кліків
            if (event.action == MotionEvent.ACTION_UP && !gestureDetector.onTouchEvent(event)) {
                v.performClick()
            }

            true
        }

        popupWindow.showAtLocation(popupView, Gravity.BOTTOM, 0, 0)
        if (actionId == 0) {
            val plList = dbHelper.getAllPlaylists()
            setupPlaylistsMenuAdapter(plList, recyclerView)
            val create = popupView.findViewById<Button>(R.id.createPl)
            create.setOnClickListener {
                val pl = Playlist(name = "New Playlist")
                dbHelper.addPlaylist(pl)
                val pls = dbHelper.getAllPlaylists()
                setupPlaylistsMenuAdapter(pls, recyclerView)
            }
        }else if (actionId == 1){
            setupMenuMusicsAdapter(musicList, recyclerView,playlist)
            val create = popupView.findViewById<Button>(R.id.createPl)
            create.visibility = View.INVISIBLE
        }else if (actionId == 3){
            setupMusicListMenuAdapter(temporaryMusicList,recyclerView)
            val title = popupView.findViewById<TextView>(R.id.textView2)
            title.text = if (modeRecPl) {
                "Recent Plays"
            }else if(modeFavorites){
                "Favorites"
            }else if(playlist.name != ""){
                playlist.name
            }else{
                "Songs"
            }
        }
        return popupWindow
    }
    private fun setupPlaylistsMenuAdapter(plList:List<Playlist>, recyclerView: RecyclerView){
        plMenuAdapter = PlaylistsMenuAdapter(requireContext(), plList,music,
            onAddListener = {
                val plMs = dbHelper.getAllMusicsByPlaylist(it.first.id)
                val isInPl = plMs.contains(music.id)
                if(!isInPl) {
                    val plM = PlMusic(idM = music.id, idPl = it.first.id)
                    dbHelper.addPlMusic(plM)

                    plMenuAdapter.notifyItemChanged(it.second)
                }
        },
            onAddedListener = {
                val plMs = dbHelper.getAllMusicsByPlaylist(it.first.id)
                val isInPl = plMs.contains(music.id)
                if (isInPl) {
                    dbHelper.deleteMFromPl(music.id, it.first.id)
                    plMenuAdapter.notifyItemChanged(it.second)
                }
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = plMenuAdapter
    }
    private fun setupMenuMusicsAdapter(musicL: List<MusicInfo>, recyclerView: RecyclerView, playlist: Playlist){
        menuMusicsAdapter = MenuMusicsAdapter(requireContext(), musicL,playlist,
            onAddListener = {
                val plMs = dbHelper.getAllMusicsByPlaylist(playlist.id)
                val isInPl = plMs.contains(it.first.id)
                if(!isInPl) {
                    val plM = PlMusic(idM = it.first.id, idPl = playlist.id)
                    dbHelper.addPlMusic(plM)
                    menuMusicsAdapter.notifyItemChanged(it.second)
                    temporaryMusicList.add(it.first)
                    musicAdapter.notifyItemInserted(temporaryMusicList.size-1)
                }
            },
            onAddedListener = {
                val plMs = dbHelper.getAllMusicsByPlaylist(playlist.id)
                val isInPl = plMs.contains(it.first.id)
                if (isInPl) {
                    dbHelper.deleteMFromPl(it.first.id, playlist.id)
                    menuMusicsAdapter.notifyItemChanged(it.second)
                    val idx = temporaryMusicList.indexOf(it.first)
                    temporaryMusicList.remove(it.first)
                    musicAdapter.notifyItemRemoved(idx)
                }
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = menuMusicsAdapter
    }
    private fun setupMusicListMenuAdapter(musicL: List<MusicInfo>, recyclerView: RecyclerView) {
        musicMenuAdapter= MusicListMenuAdapter(requireContext(), musicL) {
            dbHelper.updatePM(it.first.id, 1, 0)
            resumeImage.visibility = View.GONE
            pauseImage.visibility = View.VISIBLE
            if (!isActiveWindow) {
                setAnimMusicWindow(musicWindow)
                isActiveWindow = true
            }
            musicMenuAdapter.notifyItemChanged(musicL.indexOf(music))
            playMusic(it.first, musicL)
            musicMenuAdapter.notifyItemChanged(musicL.indexOf(music))

        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = musicAdapter
    }

    override fun onDialogResult(result: Boolean) {
        if (plId != "-1") {
            dbHelper.deletePlaylist(plId)
            val pls = dbHelper.getAllPlaylists()
            setupPlaylistsAdapter(pls)
            modePlaylists = true
        }
    }

}

fun getMusicFiles(context:Context): List<MusicInfo> {
    val musicList = mutableListOf<MusicInfo>()

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA
    )

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        null
    )

    cursor?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        var index = 0
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val displayName = cursor.getString(displayNameColumn)
            val artist = cursor.getString(artistColumn)
            val duration = cursor.getLong(durationColumn)
            val data = cursor.getString(dataColumn)
            val imageUri = getSongImageUri(context,id)
            musicList.add(MusicInfo(id, displayName, artist, duration,imageUri.toString(), data))
            index++
        }
    }

    return musicList
}

fun getSongImageUri(context: Context, songId: Long): Uri? {
    val projection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
    val selection = "${MediaStore.Audio.Media._ID} = ?"
    val selectionArgs = arrayOf(songId.toString())
    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )
    cursor?.use { cursor ->
        if (cursor.moveToFirst()) {
            val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
            return getAlbumArtUri(context, albumId)
        }
    }
    return null
}

fun getAlbumArtUri(context: Context, albumId: Long): Uri? {
    val projection = arrayOf(MediaStore.Audio.Albums.ALBUM_ART)
    val selection = "${MediaStore.Audio.Albums._ID} = ?"
    val selectionArgs = arrayOf(albumId.toString())
    val cursor = context.contentResolver.query(
        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )
    cursor?.use { cursor ->
        if (cursor.moveToFirst()) {
            val imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
            if (!imagePath.isNullOrEmpty()) {
                return Uri.parse(imagePath)
            }
        }
    }
    return null
}


fun setAnimMusicWindow(i: ConstraintLayout) {
    val translateAnimation = TranslateAnimation(
        Animation.ABSOLUTE, 0f,
        Animation.ABSOLUTE, 0f,
        Animation.ABSOLUTE, 120f,
        Animation.ABSOLUTE, 0f
    )
    translateAnimation.duration = 800
    translateAnimation.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationEnd(animation: Animation?) {
            i.visibility = View.VISIBLE
        }
        override fun onAnimationRepeat(animation: Animation?) {}
    })
    i.startAnimation(translateAnimation)
}
fun setHideAnimMusicWindow(i: ConstraintLayout) {
    val translateAnimation = TranslateAnimation(
        Animation.ABSOLUTE, 0f,
        Animation.ABSOLUTE, 0f,
        Animation.ABSOLUTE, 0f,
        Animation.ABSOLUTE, 120f
    )
    translateAnimation.duration = 800
    translateAnimation.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationEnd(animation: Animation?) {
            i.visibility = View.GONE
        }
        override fun onAnimationRepeat(animation: Animation?) {}
    })
    i.startAnimation(translateAnimation)
}
fun  showWaitAnim(context: Context): PopupWindow {
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val popupView  = inflater.inflate(R.layout.poput_wait, null)
    val popupWindow = PopupWindow(popupView, 1000, 1000)
    val progressBar = popupView.findViewById<ProgressBar>(R.id.progressBar)
    val color = ContextCompat.getColor(context, R.color.pryColor)
    progressBar.indeterminateDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)

    // Показати анімацію
    popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

    // Повернути PopupWindow для можливості закриття ззовні
    return popupWindow
}
fun hideMI(i: PopupWindow) {
    i.animationStyle = R.style.PopupAnimation
    i.dismiss()
}
fun showMI(i: PopupWindow) {
    i.animationStyle = R.style.PopupAnimation
    i.dismiss()
}
fun setButtonAnimation(i: ImageButton,value: Float = 1.1f) {
    val anim = ObjectAnimator.ofFloat(i, "scaleX", value, 1f)
    anim.duration = 300
    anim.repeatMode = ObjectAnimator.REVERSE

//    val anim2 = ObjectAnimator.ofFloat(i, "scaleY", 1.1f, 1f)
//    anim2.duration = 300
//    anim2.repeatMode = ObjectAnimator.REVERSE

    anim.start()
//    anim2.start()
}