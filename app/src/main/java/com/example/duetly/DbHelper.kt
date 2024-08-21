package com.example.duetly

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.duetly.fragments.MusicInfo
import com.example.duetly.fragments.Playlist
import com.example.duetly.models.LocalSettings
import com.example.duetly.models.User
import com.example.duetly.models.UserSettings
import java.util.UUID

class DbHelper(val context: Context, val factory: SQLiteDatabase.CursorFactory?):
    SQLiteOpenHelper(context, "duteBase", factory, 2) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_PLAYING_MUSIC_TABLE)
        db?.execSQL(CREATE_MUSICS_INFORMATION_TABLE)
        db?.execSQL(CREATE_FAVORITES_TABLE)
        db?.execSQL(CREATE_PLAYLISTS_TABLE)
        db?.execSQL(CREATE_PL_MUSICS_TABLE)
        db?.execSQL(CREATE_HISTORY_MUSICS_TABLE)
        db?.execSQL(CREATE_USER_PROFILE_TABLE)
        db?.execSQL(CREATE_LOCAL_SETTINGS_TABLE)

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db != null) {
            for (version in oldVersion until newVersion) {
                when (version) {
                    1 -> upgradeFromVersion1To2(db)
                }
            }
        }
    }
    private fun upgradeFromVersion1To2(db: SQLiteDatabase) {
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS local_settings (
            id INTEGER PRIMARY KEY,
            music_size INTEGER
        )
    """)
    }
    companion object {
        const val DATABASE_NAME = "duetly.db"
        const val DATABASE_VERSION = 2

        private const val CREATE_PLAYING_MUSIC_TABLE =
            "CREATE TABLE IF NOT EXISTS playing_music (id INTEGER PRIMARY KEY, idMusic INTEGER, isPlay INTEGER, time INTEGER, mode TEXT)"

        private const val CREATE_MUSICS_INFORMATION_TABLE =
            "CREATE TABLE IF NOT EXISTS musics (id LONG PRIMARY KEY, displayName TEXT, artist TEXT, duration LONG, imageUri TEXT, data TEXT)"

        private const val CREATE_FAVORITES_TABLE =
            "CREATE TABLE IF NOT EXISTS favorites (id LONG PRIMARY KEY)"

        private const val CREATE_PLAYLISTS_TABLE =
            "CREATE TABLE IF NOT EXISTS playlists (id TEXT PRIMARY KEY, name TEXT)"

        private const val CREATE_PL_MUSICS_TABLE =
            "CREATE TABLE IF NOT EXISTS pl_musics (id TEXT PRIMARY KEY, id_m LONG, id_pl TEXT)"
        private const val CREATE_HISTORY_MUSICS_TABLE =
            "CREATE TABLE IF NOT EXISTS history (id LONG PRIMARY KEY,id_m LONG ,number INTEGER)"
        private const val CREATE_USER_PROFILE_TABLE =
            "CREATE TABLE IF NOT EXISTS user (id TEXT PRIMARY KEY, username TEXT ,email TEXT ,password TEXT)"
        private const val CREATE_LOCAL_SETTINGS_TABLE = "CREATE TABLE IF NOT EXISTS local_settings (id INTEGER PRIMARY KEY,music_size INT)"
    }
        fun addPlayingMusic(id: Long, isPlay: Int, time: Int, mode: String) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("id", 1)
            put("idMusic", id)
            put("isPlay", isPlay)
            put("time", time)
            put("mode", mode)

        }
        db.insert("playing_music", null, contentValues)
        db.close()
    }

    fun getPlayingMusicForId(id: Long): PlayingMusic? {
        val db = this.readableDatabase

        val query = "SELECT * FROM playing_music WHERE id = ?"
        val cursor = db.rawQuery(query, arrayOf(id.toString()))
        var mus: PlayingMusic? = null
        cursor.use {
            if (it.moveToFirst()) {
                val idM = it.getInt(it.getColumnIndexOrThrow("id"))
                val active = it.getInt(it.getColumnIndexOrThrow("isPlay"))
                val idMusic = it.getLong(it.getColumnIndexOrThrow("idMusic"))
                val time = it.getInt(it.getColumnIndexOrThrow("time"))

                mus = PlayingMusic(
                    idM,
                    idMusic,
                    active,
                    time
                )
            }
        }
        return mus
    }

    fun getPlayingMode(id: Long): String {
        val db = this.readableDatabase

        val query = "SELECT * FROM playing_music WHERE id = ?"
        val cursor = db.rawQuery(query, arrayOf(id.toString()))
        var mode = ""
        cursor.use {
            if (it.moveToFirst()) {
                mode = it.getString(it.getColumnIndexOrThrow("mode"))

            }
        }
        return mode
    }

    fun updatePM(idM: Long, isPlay: Int, time: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("idMusic", idM)
            put("isPlay", isPlay)
            put("time", time)
        }
        val whereClause = "id = ?"
        val whereArgs = arrayOf("1")
        db.update("playing_music", values, whereClause, whereArgs)
        db.close()
    }

    fun changeMode(mode: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("mode", mode)
        }
        val whereClause = "id = ?"
        val whereArgs = arrayOf("1")
        db.update("playing_music", values, whereClause, whereArgs)
        db.close()
    }

    //--------------------------------MUSICS---------------------------------------------------
    fun addMusic(music: MusicInfo) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("id", music.id)
            put("displayName", music.displayName)
            put("artist", music.artist)
            put("duration", music.duration)
            put("imageUri", music.imageUri)
            put("data", music.data)
        }
        db?.insert("musics", null, values)
        db.close()
    }

    fun getAllMusics(): List<MusicInfo> {
        val musicList = mutableListOf<MusicInfo>()
        val selectQuery = "SELECT * FROM musics"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(it.getColumnIndexOrThrow("id"))
                    val displayName = it.getString(it.getColumnIndexOrThrow("displayName"))
                    val artist = it.getString(it.getColumnIndexOrThrow("artist"))
                    val duration = it.getLong(it.getColumnIndexOrThrow("duration"))
                    val imageUri = it.getString(it.getColumnIndexOrThrow("imageUri"))
                    val data = it.getString(it.getColumnIndexOrThrow("data"))

                    val cup = MusicInfo(id, displayName, artist, duration, imageUri, data)
                    musicList.add(cup)
                } while (it.moveToNext())
            }
        }

        cursor?.close()
        return musicList
    }
    fun getMusic(id: Long): MusicInfo {
        val db = this.readableDatabase

        val query = "SELECT * FROM musics WHERE id = ?"
        val cursor = db.rawQuery(query, arrayOf(id.toString()))
        var music = MusicInfo()
        cursor.use {
            if (it.moveToFirst()) {
                val displayName = it.getString(it.getColumnIndexOrThrow("displayName"))
                val artist = it.getString(it.getColumnIndexOrThrow("artist"))
                val duration = it.getLong(it.getColumnIndexOrThrow("duration"))
                val imageUri = it.getString(it.getColumnIndexOrThrow("imageUri"))
                val data = it.getString(it.getColumnIndexOrThrow("data"))
                music = MusicInfo(id,displayName,artist,duration,imageUri,data)
            }
        }
        return music
    }
    fun deleteMusic(id: Long) {
        val db = writableDatabase
        db.delete("musics", "id = ?", arrayOf(id.toString()))
        db.close()
    }

    //--------------------------------favorites-------------------------------------------------------
    fun addFavoriteMusic(id: Long) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("id", id)
        }
        db.insert("favorites", null, contentValues)
        db.close()
    }
    fun getAllFMusics(): List<Long> {
        val musicList = mutableListOf<Long>()
        val selectQuery = "SELECT * FROM favorites"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(it.getColumnIndexOrThrow("id"))
                    musicList.add(id)
                } while (it.moveToNext())
            }
        }

        cursor?.close()
        return musicList
    }
    fun getFM(id: Long): Long {
        val db = this.readableDatabase

        val query = "SELECT * FROM favorites WHERE id = ?"
        val cursor = db.rawQuery(query, arrayOf(id.toString()))
        var mode = 0L
        cursor.use {
            if (it.moveToFirst()) {
                mode = it.getLong(it.getColumnIndexOrThrow("id"))
            }
        }
        return mode
    }
    fun deleteFavoriteM(id: Long) {
        val db = writableDatabase
        db.delete("favorites", "id = ?", arrayOf(id.toString()))
        db.close()
    }
    //-----------------------------Playlists--------------------------------------------------
    fun addPlaylist(pl: Playlist) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("id", pl.id)
            put("name", pl.name)
        }
        db.insert("playlists", null, contentValues)
        db.close()
    }
    fun renamePl(id: String,name: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("name", name)
        }
        val whereClause = "id = ?"
        val whereArgs = arrayOf(id)
        db.update("playlists", values, whereClause, whereArgs)
        db.close()
    }
    fun getAllPlaylists(): List<Playlist> {
        val musicList = mutableListOf<Playlist>()
        val selectQuery = "SELECT * FROM playlists"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getString(it.getColumnIndexOrThrow("id"))
                    val name = it.getString(it.getColumnIndexOrThrow("name"))

                    val pl = Playlist(
                        id,
                        name
                    )
                    musicList.add(pl)
                } while (it.moveToNext())
            }
        }

        cursor?.close()
        return musicList
    }
//    fun getFM(id: Long): Long {
//        val db = this.readableDatabase
//
//        val query = "SELECT * FROM favorites WHERE id = ?"
//        val cursor = db.rawQuery(query, arrayOf(id.toString()))
//        var mode = 0L
//        cursor.use {
//            if (it.moveToFirst()) {
//                mode = it.getLong(it.getColumnIndexOrThrow("id"))
//            }
//        }
//        return mode
//    }
    fun deletePlaylist(id: String) {
        val db = writableDatabase
        db.delete("playlists", "id = ?", arrayOf(id.toString()))
        db.close()
    }
    //----------------------------------PL-MUSIC-----------------------------------------------
    fun addPlMusic(plM: PlMusic) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("id", plM.id)
            put("id_m", plM.idM)
            put("id_pl", plM.idPl)
        }
        db.insert("pl_musics", null, contentValues)
        db.close()
    }
    fun getAllMusicsByPlaylist(idPl:String): List<Long> {
        val musicList = mutableListOf<Long>()
        val selectQuery = "SELECT * FROM pl_musics WHERE id_pl = ?"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(idPl))

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(it.getColumnIndexOrThrow("id_m"))
                    musicList.add(id)
                } while (it.moveToNext())
            }
        }

        cursor?.close()
        return musicList
    }
    fun deleteMFromPl(idM: Long, idPl: String) {
        val db = writableDatabase
        db.delete("pl_musics", "id_m = ? AND id_pl = ?", arrayOf(idM.toString(), idPl))
        db.close()
    }
    //=========================history=====================================================
    fun addMusicToH(num:Long, music: MusicInfo, number: Int) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("id", num)
            put("id_m", music.id)
            put("number", number)
        }
        db.insert("history", null, contentValues)
        db.close()
    }
    fun getAllMusicsFromH(): List<HistoryMusic> {
        val musicList = mutableListOf<HistoryMusic>()
        val selectQuery = "SELECT * FROM history"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(it.getColumnIndexOrThrow("id"))
                    val idM = it.getLong(it.getColumnIndexOrThrow("id_m"))
                    val number = it.getInt(it.getColumnIndexOrThrow("number"))
                    musicList.add(HistoryMusic(id,idM,number))
                } while (it.moveToNext())
            }
        }

        cursor?.close()
        return musicList
    }
        fun getMFromH(id: Long): HistoryMusic {
        val db = this.readableDatabase

        val query = "SELECT * FROM history WHERE id_m = ?"
        val cursor = db.rawQuery(query, arrayOf(id.toString()))
        var hs = HistoryMusic()
        cursor.use {
            if (it.moveToFirst()) {
                val idM = it.getLong(it.getColumnIndexOrThrow("id_m"))
                val number = it.getInt(it.getColumnIndexOrThrow("number"))
                hs = HistoryMusic(id,idM,number)
            }
        }
        return hs
    }
    fun deleteMFromH(idM: Long) {
        val db = writableDatabase
        db.delete("history", "id_m = ?", arrayOf(idM.toString()))
        db.close()
    }
//-------------------------------------------User--------------------------------------------------

fun createUser(user: User,context: Context) {
    val db = this.writableDatabase
    val values = ContentValues().apply {
        put("id","1")
        put("username", user.username)
        put("email", user.email)
        put("password", user.getHashedPassword())
    }
    db?.insert("user", null, values)
    db.close()
}
    fun getUser(): User {
        val db = this.readableDatabase

        val query = "SELECT * FROM user WHERE id = ?"
        val cursor = db.rawQuery(query, arrayOf("1"))
        var user = User(
            username = "32",
            email = "404",
            password = "404")
        cursor.use {
            if (it.moveToFirst()) {
                val username = it.getString(it.getColumnIndexOrThrow("username"))
                val email = it.getString(it.getColumnIndexOrThrow("email"))
                val password = it.getString(it.getColumnIndexOrThrow("password"))
                user = User(
                    username =username,
                    email = email,
                    password = password)
            }
        }
        cursor.close()
        return user
    }
    fun updateUser(
        user: User,
    ) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("username", user.username)
            put("email", user.email)
            put("password", user.getHashedPassword())
        }
        val whereClause = "id = ?"
        val whereArgs = arrayOf("1")
        db.update("user", values, whereClause, whereArgs)
        db.close()
    }
    fun updateUserUsername(
        username:String,
    ) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("username", username)
        }
        val whereClause = "id = ?"
        val whereArgs = arrayOf("1")
        db.update("user", values, whereClause, whereArgs)
        db.close()
    }
    //-----------------------------------------------LS--------------------------------------------
    fun getLocalSettings(): LocalSettings {
        val db = this.readableDatabase

        val query = "SELECT * FROM user WHERE id = ?"
        val cursor = db.rawQuery(query, arrayOf("1"))
        var ls = LocalSettings(-1)
        cursor.use {
            if (it.moveToFirst()) {
                val musicSize = it.getInt(it.getColumnIndexOrThrow("music_size"))

                ls = LocalSettings(musicSize)
            }
        }
        cursor.close()
        if (ls.musicSize == -1){
            val values = ContentValues().apply {
                put("id",1)
                put("music_size", 30000)
            }
            db?.insert("local_settings", null, values)
            db.close()
            ls = LocalSettings()
        }
        return ls
    }
    fun updateLSMusicSize(
        newValue: Int,
    ) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("music_size", newValue)
        }
        val whereClause = "id = ?"
        val whereArgs = arrayOf("1")
        db.update("local_settings", values, whereClause, whereArgs)
        db.close()
    }
}
data class PlayingMusic(
    val id: Int,
    val idMusic:Long,
    val isPlay: Int,
    val time: Int
)
data class PlMusic(
    val id: String = UUID.randomUUID().toString(),
    val idM : Long,
    val idPl : String
)
data class HistoryMusic(
    val id:Long = -1,
    val idM:Long = -1,
    val number: Int = -1
)
