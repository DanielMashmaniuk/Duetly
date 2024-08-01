package com.example.duetly.models

import android.content.Context
import com.example.duetly.DbHelper

class LocalSettings(
    val musicSize:Int = 30000
) {
    private fun changeMusicSize(newValue:Int,context:Context){
        val dbHelper = DbHelper(context,null)
        dbHelper.updateLSMusicSize(newValue)
    }
}