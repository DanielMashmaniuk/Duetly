package com.example.duetly.activities

import android.Manifest
import android.util.Base64
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.duetly.fragments.AuthFragment
import com.example.duetly.DbHelper
import com.example.duetly.fragments.MainMusicList
import com.example.duetly.MediaPlayerManager
import com.example.duetly.NetworkChangeReceiver
import com.example.duetly.models.User
import com.example.duetly.fragments.MusicInfo
import com.example.duetly.fragments.ProfileUserFragment
import com.example.duetly.R
import com.example.duetly.dialogs.AlertDialog
import com.example.duetly.fragments.MelodyMatesFragment
import com.example.duetly.fragments.NotAvialiableInternetFragment
import com.example.duetly.fragments.waitAnimation
import com.example.duetly.models.UserSettings
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

interface ReloadFragment {
    fun onFragment(fragment: Fragment,isAuth: Boolean)
}

class MainActivity : AppCompatActivity(), ReloadFragment, AlertDialog.AlertDialogResult {
    private lateinit var mediaPlayer:MediaPlayer
    private lateinit var dbHelper: DbHelper
    private lateinit var fireDatabase: FirebaseDatabase
    private lateinit var networkChangeReceiver: NetworkChangeReceiver

    private var isAllowS = false
    private var isAutorised = false
    private var isUserProfile = false

    private lateinit var musicListNavB: ImageButton
    private lateinit var profileNavB: ImageButton
    private lateinit var friendsNavB: ImageButton

    private var music = MusicInfo()
    private var userProfile = User()
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)
        dbHelper = DbHelper(this, null)
        MediaPlayerManager.initialize(this, "null")
        fireDatabase = Firebase.database
        networkChangeReceiver = NetworkChangeReceiver()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                getUser(dbHelper) {
                    isUserProfile = (it?.username ?: User()) != "404"
                    userProfile = it?:User()
                }
            }
        }
        val user = dbHelper.getUser()
        if (user.username == "32"){
            dbHelper.createUser(
                User(
                    username = "404",
                    email = "404",
                    password = "404",
                ),
                this
            )
        }

        if (user.username != "404") {
            CoroutineScope(Dispatchers.Main).launch {
                checkExistUser(user) {
                    isAutorised = it
                    if (!isAutorised) {
                        dbHelper.updateUser(User())
                    }
                }
            }
        }else{
            isAutorised = false
        }

        val musActive = dbHelper.getPlayingMusicForId(1)
        music = musActive?.let { dbHelper.getMusic(it.idMusic) }?: MusicInfo()
        MediaPlayerManager.initialize(this, music.data)

        val bottomNavL = findViewById<LinearLayout>(R.id.bottomNav)
        musicListNavB = findViewById(R.id.musicB)
        val roomNavB = findViewById<ImageButton>(R.id.roomB)
        friendsNavB = findViewById<ImageButton>(R.id.friendsB)
        profileNavB = findViewById(R.id.profileB)
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
        checkPermissionAndExecuteFunction(this)

        val aMusic = dbHelper.getPlayingMusicForId(1)
        if (aMusic == null) {
            dbHelper.addPlayingMusic(-1, 0, 0, "NEXT")
        }
        profileNavB.setOnClickListener {
            if (isInternetAvailable(this)) {
                if (isAutorised) {
                    if (!isUserProfile) {
                        val waitView = waitAnimation(this)
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                getUser(dbHelper) {
                                    val bundle = Bundle().apply {
                                        putParcelable("user", it)
                                    }

                                    val fragment = ProfileUserFragment().apply {
                                        arguments = bundle
                                    }

                                    setFragment(fragment)
                                    waitView.dismiss()
                                }
                            }


                        }
                    }else{
                        val bundle = Bundle().apply {
                            putParcelable("user", userProfile)
                        }

                        val fragment = ProfileUserFragment().apply {
                            arguments = bundle
                        }

                        setFragment(fragment)
                    }
                } else {
                    setFragment(AuthFragment())
                }
            }else{
                setFragment(NotAvialiableInternetFragment())
            }
            musicListNavB.isEnabled = true
            profileNavB.isEnabled = false
            friendsNavB.isEnabled = true
        }
        friendsNavB.setOnClickListener{
            if (isInternetAvailable(this)){
                setFragment(MelodyMatesFragment())
            }else{
                setFragment(NotAvialiableInternetFragment())
            }
            musicListNavB.isEnabled = true
            profileNavB.isEnabled = true
            friendsNavB.isEnabled = false
        }
        musicListNavB.setOnClickListener {
            if (isAllowS) {
                setFragment(MainMusicList())
                musicListNavB.isEnabled = false
                profileNavB.isEnabled = true
                friendsNavB.isEnabled = true
            } else {
                showToast(this, "Unable to upload files (no appropriate permission)")
                checkPermissionAndExecuteFunction(this)
                musicListNavB.isEnabled = true
                profileNavB.isEnabled = true
                friendsNavB.isEnabled = true
            }
        }

    }

    override fun onFragment(fragment: Fragment,isAuth: Boolean) {
        isAutorised = isAuth
        if (isAutorised){
            val waitView = waitAnimation(this)
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    getUser(dbHelper) {
                        val bundle = Bundle().apply {
                            putParcelable("user", it)
                        }

                        val fragment = ProfileUserFragment().apply {
                            arguments = bundle
                        }

                        setFragment(fragment)
                    }
                }
                withContext(Dispatchers.Main) {
                    waitView.dismiss()
                }
            }
        }else {
            setFragment(AuthFragment())
        }
        //changeColor(this,bottomNavL)
        musicListNavB.isEnabled = true
        profileNavB.isEnabled = false
    }
    override fun onStart() {
        super.onStart()
        val isNet = isInternetAvailable(this)
        if (isNet) {
            setStatusOnline()
        }
        registerReceiver(networkChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

    }
    override fun onStop() {
        super.onStop()
        val isNet = isInternetAvailable(this)
        if (isNet) {
            setStatusOffline()
        }
        unregisterReceiver(networkChangeReceiver)

    }
    override fun onDestroy() {
        super.onDestroy()
        if (music.data != ""){
            mediaPlayer.stop()
            val currentPosition = mediaPlayer.currentPosition
            dbHelper.updatePM(music.id, 1, currentPosition)
        }
        val isNet = isInternetAvailable(this)
        if (isNet) {
            setStatusOffline()
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
    private fun checkExistUser(user: User,isAuth: (Boolean) -> Unit){
        val encEmail = encodeEmail(user.email.trim())
        val userRef = fireDatabase.getReference("users")
            .child(encEmail)
        userRef.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                isAuth(true)
            } else {
                isAuth(false)
            }
        }
    }
    fun setThemePreference(theme: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("theme", theme)
            apply()
        }
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    private fun setStatusOnline() {
        dbHelper = DbHelper(this,null)
        val user = dbHelper.getUser()
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val encodedEmail = encodeEmail(user.email)
        usersRef.child(encodedEmail).child("isOnline").setValue("true")
            .addOnSuccessListener {
                println("Статус онлайн встановлено успішно")
            }
            .addOnFailureListener { e ->
                println("Помилка встановлення статусу онлайн: ${e.message}")
            }
    }

    private fun setStatusOffline() {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val user = dbHelper.getUser()
        val encodedEmail = encodeEmail(user.email)
        usersRef.child(encodedEmail).child("isOnline").setValue("false")
            .addOnSuccessListener {
                println("Статус офлайн встановлено успішно")
            }
            .addOnFailureListener { e ->
                println("Помилка встановлення статусу офлайн: ${e.message}")
            }
    }

    override fun onDialogResult(result: Boolean) {
        if (result){
            isAutorised = false
            setFragment(AuthFragment())
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
fun fetchUserData(userLoc: User, callback: (User?) -> Unit) {
    val encEmail = encodeEmail( userLoc.email.trim())
    val userRef = Firebase.database.getReference("users").child(encEmail)
    userRef.get().addOnSuccessListener { dataSnapshot ->
        val userF = dataSnapshot.getValue<User>()?:User()
        callback(userF)
    }.addOnFailureListener {
        callback(null)
    }
}
suspend fun getUser(dbHelper: DbHelper,sendUser: (User?) -> Unit){
    val userL = dbHelper.getUser()
    val userF = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            fetchUserData(userL) { user ->
                continuation.resume(user, null)
            }
        }
    }
    sendUser(userF)
}

fun encodeEmail(email: String): String {
    return email.replace(".", ",")
        .replace("#", "%23")
        .replace("$", "%24")
        .replace("[", "%5B")
        .replace("]", "%5D")
}

fun decodeEmail(encodedEmail: String): String {
    return encodedEmail.replace(",", ".")
        .replace("%23", "#")
        .replace("%24", "$")
        .replace("%5B", "[")
        .replace("%5D", "]")
}
fun setSharedPrefences(sharedPreferences: SharedPreferences,isTrue:(Boolean)->Unit){
    when (sharedPreferences.getString("theme", "light")) {
        "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
    isTrue(true)
}
fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo ?: return false
        @Suppress("DEPRECATION")
        return networkInfo.isConnected
    }
}