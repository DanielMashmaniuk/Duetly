package com.example.duetly

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.duetly.activities.encodeEmail
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class NetworkChangeReceiver : BroadcastReceiver() {

    private val database = Firebase.database
    private val usersRef = database.getReference("users")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val isConnected = isNetworkAvailable(context)
            if (!isConnected) {
                // Якщо мережа недоступна, встановлюємо статус офлайн
                val user = DbHelper(context,null).getUser()
                val encodedEmail = encodeEmail(user.email)
                usersRef.child(encodedEmail).child("isOnline").setValue("false")
                    .addOnSuccessListener {
                        Log.d("NetworkChangeReceiver", "Статус офлайн встановлено успішно")
                    }
                    .addOnFailureListener { e ->
                        Log.e("NetworkChangeReceiver", "Помилка встановлення статусу офлайн: ${e.message}")
                    }
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
