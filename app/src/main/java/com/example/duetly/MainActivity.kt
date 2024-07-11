package com.example.duetly

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.duetly.Models.User
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface ReloadFragment {
    fun onFragment(fragment: Fragment)
}

class MainActivity : AppCompatActivity(),ReloadFragment {
    private var isAllowS = false
    private lateinit var musicListNavB: ImageButton
    private lateinit var mediaPlayer:MediaPlayer
    private lateinit var dbHelper:DbHelper
    private var music = MusicInfo()
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)
        dbHelper = DbHelper(this, null)
        MediaPlayerManager.initialize(this,"null")
        val fireDatabase = Firebase.database
        val user = dbHelper.getUser()
        val musActive = dbHelper.getPlayingMusicForId(1)
        music = musActive?.let { dbHelper.getMusic(it.idMusic) }?: MusicInfo()
        MediaPlayerManager.initialize(this,music.data)
        if (user.username == "32"){
            dbHelper.createUser(User("404","404","404"),this)
        }
        musicListNavB = findViewById(R.id.musicB)
        val bottomNavL = findViewById<LinearLayout>(R.id.bottomNav)
        val roomNavB = findViewById<ImageButton>(R.id.roomB)
        val friendsNavB = findViewById<ImageButton>(R.id.friendsB)
        val profileNavB = findViewById<ImageButton>(R.id.profileB)
        CoroutineScope(Dispatchers.Main).launch {
            startAppAnimation()
        }
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    // Додаткові дії, якщо дозвіл надано
                    val dbHelper = DbHelper(this, null)
                    val aMusic = dbHelper.getPlayingMusicForId(1)
                    if (aMusic == null) {
                        dbHelper.addPlayingMusic(-1, 0, 0, "NEXT")
                    }
                    isAllowS = true
                    setFragment(MainMusicList())
                    musicListNavB.isEnabled = false
                } else {
                    showToast(this, "Unable to upload files (no appropriate permission)")
                }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    showToast(this, "YES")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Поясніть користувачу, навіщо потрібен цей дозвіл
                }

                else -> {
                    // Запитайте дозвіл
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        profileNavB.setOnClickListener {
            if (user.email == "404"){
                setFragment(AuthFragment())
            }else {
                setFragment(ProfileUserFragment())
            }
            //changeColor(this,bottomNavL)
            musicListNavB.isEnabled = true
            profileNavB.isEnabled = false
        }
        checkPermissionAndExecuteFunction(this)

        val aMusic = dbHelper.getPlayingMusicForId(1)
        if (aMusic == null) {
            dbHelper.addPlayingMusic(-1, 0, 0, "NEXT")
        }
        musicListNavB.setOnClickListener {
            if (isAllowS) {
                setFragment(MainMusicList())
                musicListNavB.isEnabled = false
                profileNavB.isEnabled = true

            } else {
                showToast(this, "Unable to upload files (no appropriate permission)")
                checkPermissionAndExecuteFunction(this)
            }
        }

    }

    override fun onFragment(fragment: Fragment) {
        setFragment(fragment)
    }
    override fun onDestroy() {
        super.onDestroy()
        if (music.data != ""){
            mediaPlayer.stop()
            val currentPosition = mediaPlayer.currentPosition
            dbHelper.updatePM(music.id, 1, currentPosition)
        }
    }
    private fun setFragment(fragment: Fragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.frameLayoutContent, fragment)
        ft.commit()
    }

    private suspend fun startAppAnimation() {
        val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView =
            inflater.inflate(R.layout.start_app_anim_layout, null)
        val line = popupView.findViewById<LinearLayout>(R.id.line)
        setLineAnim(line)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
        delay(3000)
        popupWindow.dismiss()
    }
    private fun checkPermissionAndExecuteFunction(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("permissions", Context.MODE_PRIVATE)
        val permissionGranted = sharedPreferences.getBoolean("read_external_storage", false)

        if (permissionGranted) {
            isAllowS = true
            val dbHelper = DbHelper(context, null)
            val aMusic = dbHelper.getPlayingMusicForId(1)
            if (aMusic == null) {
                dbHelper.addPlayingMusic(-1, 0, 0,"NEXT")
            }
            return true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            return false
        }
    }
    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}

fun animateTextChange(newChar: Char,editText: EditText) {
    val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
    animator.setDuration(300)
    animator.addUpdateListener { animation ->
        val progress = animation.animatedValue as Float
        val text: String = editText.getText().toString()
        editText.setText(text.substring(0, text.length - 1) + newChar + progress)
    }
    animator.start()
}
fun getDrawableFromResourcePath(context: Context, resourcePath: String): Drawable? {
    // Отримуємо ідентифікатор ресурсу за його шляхом
    val resourceId =
        context.resources.getIdentifier(resourcePath, "drawable", context.packageName)

    // Перевіряємо, чи ідентифікатор був знайдений
    return if (resourceId != 0) {
        // Отримуємо Drawable з ресурсів
        context.resources.getDrawable(resourceId, context.theme)
    } else {
        // Якщо ресурс не знайдено, повертаємо null або виконуємо інші необхідні дії
        null
    }
}
fun savePermissionState(context: Context, granted: Boolean) {
    val sharedPreferences = context.getSharedPreferences("permissions", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putBoolean("read_external_storage", granted)
    editor.apply()
}
fun showToast(context: Context, message:String){
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
fun paintIcon(hexColor: String,context: Context,image:String):Drawable {
    val vectorDrawable = getDrawableFromResourcePath(context, image)
    val numericColor = Color.parseColor(hexColor)
    vectorDrawable!!.colorFilter =
        PorterDuffColorFilter(numericColor, PorterDuff.Mode.SRC_IN)
    return vectorDrawable
}
fun setLineAnim(i: LinearLayout,value: Float = 900f) {
    val anim = ObjectAnimator.ofFloat(i, "translationY", 0f, value)
    anim.duration = 1500
    anim.repeatCount = ObjectAnimator.INFINITE
    anim.interpolator = AccelerateDecelerateInterpolator() // Додає поступовість
    anim.start()
}
fun changeColor(context: Context,layout: LinearLayout){
    val colorFrom = ContextCompat.getColor(context, R.color.dark)
    val colorTo = ContextCompat.getColor(context, R.color.secColor)

    // Створюємо ValueAnimator
    val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
    colorAnimation.duration = 500
    colorAnimation.repeatCount = ObjectAnimator.INFINITE

    // Слухач змін значення анімації
    colorAnimation.addUpdateListener { animator ->
        layout.setBackgroundColor(animator.animatedValue as Int)
    }

    // Запускаємо анімацію
    colorAnimation.start()
}