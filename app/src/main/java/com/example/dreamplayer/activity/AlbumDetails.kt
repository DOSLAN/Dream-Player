package com.example.dreamplayer.activity

import android.media.MediaMetadataRetriever
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dreamplayer.R
import com.example.dreamplayer.activity.MainActivity.Companion.musicFiles
import com.example.dreamplayer.adapter.AlbumDetailsAdapter
import com.example.dreamplayer.model.MusicFiles

class AlbumDetails : AppCompatActivity() {
    private val albumSongs = ArrayList<MusicFiles>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_details)
        val albumPhoto = findViewById<ImageView>(R.id.album_album_photo)
        val albumName = intent.getStringExtra("albumName")
        var j = 0
        for (i in 0..musicFiles.size){
            if (albumName.equals(musicFiles[i].album)){
                albumSongs.add(j, musicFiles[i])
                j++
            }
        }
        val image = getAlbumArt(albumSongs[0].path)
        if (image!=null){
            Glide.with(this).load(image).into(albumPhoto)
        }
        else{
            Glide.with(this).load(R.drawable.ic_launcher_background).into(albumPhoto)
        }
    }

    override fun onResume() {
        super.onResume()
        if (albumSongs.size >= 1){
            val albumDetailsAdapter = AlbumDetailsAdapter(this, albumSongs)
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        }
    }

    private fun getAlbumArt(uri: String): ByteArray? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(uri)
        val art = retriever.embeddedPicture
        retriever.release()
        return art
    }
}