package com.example.dreamplayer.activity

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.example.dreamplayer.ApplicationClass.Companion.ACTION_PREVIOUS
import com.example.dreamplayer.ApplicationClass.Companion.ACTION_PLAY
import com.example.dreamplayer.ApplicationClass.Companion.ACTION_NEXT
import com.example.dreamplayer.ApplicationClass.Companion.CHANNEL_ID_2
import com.example.dreamplayer.MusicService
import com.example.dreamplayer.R
import com.example.dreamplayer.activity.MainActivity.Companion.musicFiles
import com.example.dreamplayer.activity.MainActivity.Companion.repeatBoolean
import com.example.dreamplayer.activity.MainActivity.Companion.shuffleBoolean
import com.example.dreamplayer.adapter.AlbumDetailsAdapter.Companion.albumFiles
import com.example.dreamplayer.adapter.MusicAdapter.Companion.mFiles
import com.example.dreamplayer.model.MusicFiles
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_player.*
import kotlin.random.Random

class PlayerActivity : AppCompatActivity() , ActionPlaying, ServiceConnection{
    private var position = -1
    private lateinit var songName : TextView
    private lateinit var artistName : TextView
    private lateinit var durationPlayed : TextView
    private lateinit var durationTotal : TextView
    private lateinit var coverArt : ImageView
    private lateinit var nextBtn : ImageView
    private lateinit var prevBtn : ImageView
    private lateinit var backBtn : ImageView
    private lateinit var shuffleBtn : ImageView
    private lateinit var repeatBtn : ImageView
    private lateinit var playPauseBtn : FloatingActionButton
    private lateinit var seekBar : SeekBar
    private lateinit var playThread : Thread
    private lateinit var prevThread : Thread
    private lateinit var nextThread: Thread
    private lateinit var musicService: MusicService
    companion object {
        var listSongs = ArrayList<MusicFiles>()
        lateinit var uri : Uri
        //lateinit var mediaPLayer: MediaPlayer
    }
    private lateinit var handler : Handler
    lateinit var mediaSessionCompat: MediaSessionCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        mediaSessionCompat = MediaSessionCompat(baseContext, "My Audio")
        initViews()
        getIntentMethod()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBarNotMy: SeekBar?, progress: Int, fromUser: Boolean) {
                if (::musicService.isInitialized && fromUser){
                    musicService.seekTo(progress * 1000)
                }
                val mCurrentPosition = musicService.currentPosition/1000
                seekBar.progress = mCurrentPosition
                durationPlayed.text = formattedTime(mCurrentPosition)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })

        handler = Handler(Looper.getMainLooper())
        this@PlayerActivity.runOnUiThread(runnable {
            if (::musicService.isInitialized){
                val mCurrentPosition = musicService.currentPosition/1000
                seekBar.progress = mCurrentPosition
                durationPlayed.text = formattedTime(mCurrentPosition)
            }
            handler.postDelayed(this, 1000)
        })
        shuffleBtn.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                if (shuffleBoolean){
                    shuffleBoolean = false
                    shuffleBtn.setImageResource(R.drawable.ic_baseline_shuffle_24)
                }
                else{
                    shuffleBoolean = true
                    shuffleBtn.setImageResource(R.drawable.ic_baseline_shuffle_off_24)
                }
            }
        })
        repeatBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (repeatBoolean){
                    repeatBoolean = false
                    repeatBtn.setImageResource(R.drawable.ic_baseline_repeat_off_24)
                }
                else{
                    repeatBoolean = true
                    repeatBtn.setImageResource(R.drawable.ic_baseline_repeat_24)
                }
            }

        })
    }

    override fun onResume() {
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, Context.BIND_AUTO_CREATE)
        playThreadBtn()
        prevThreadBtn()
        nextThreadBtn()
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        unbindService(this)
    }

    private fun nextThreadBtn() {
        nextThread = Thread(Runnable {
            nextBtn.setOnClickListener(View.OnClickListener {
                nextBtnClicked()
            })
        })
        nextThread.start()
    }

    override fun nextBtnClicked() {
        if (musicService.isPLaying()){
            musicService.stop()
            musicService.release()
            if (shuffleBoolean && !repeatBoolean){
                position = Random.nextInt(0,listSongs.size)
            }
            else if(!shuffleBoolean && !repeatBoolean) {
                position = ((position + 1) % listSongs.size)
            }
            uri = Uri.parse(listSongs[position].path)
            musicService.createMediaPlayer(position)
            metaData(uri)
            songName.text = listSongs[position].title
            artistName.text = listSongs[position].artist
            seekBar.max = musicService.duration / 1000
            this@PlayerActivity.runOnUiThread(runnable {
                if (::musicService.isInitialized){
                    val mCurrentPosition = musicService.currentPosition/1000
                    seekBar.progress = mCurrentPosition
                }
                handler.postDelayed(this, 1000)
            })
            musicService.onCompleted()
            showNotification(R.drawable.ic_baseline_pause_24)
            playPauseBtn.setBackgroundResource(R.drawable.ic_baseline_pause_24)
            musicService.start()
        }
        else{
            musicService.stop()
            musicService.release()
            position = ((position + 1) % listSongs.size)
            uri = Uri.parse(listSongs[position].path)
            musicService.createMediaPlayer(position)
            metaData(uri)
            songName.text = listSongs[position].title
            artistName.text = listSongs[position].artist
            seekBar.max = musicService.duration / 1000
            this@PlayerActivity.runOnUiThread(runnable {
                if (::musicService.isInitialized){
                    val mCurrentPosition = musicService.currentPosition/1000
                    seekBar.progress = mCurrentPosition
                }
                handler.postDelayed(this, 1000)
            })
            musicService.onCompleted()
            showNotification(R.drawable.ic_baseline_play_arrow_24)
            playPauseBtn.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)
        }
    }

    private fun prevThreadBtn() {
        prevThread = Thread(Runnable {
            prevBtn.setOnClickListener(View.OnClickListener {
                prevBtnClicked()
            })
        })
        prevThread.start()
    }

    override fun prevBtnClicked() {
        if (musicService.isPLaying()){
            musicService.stop()
            musicService.release()
            if (shuffleBoolean && !repeatBoolean){
                position = Random.nextInt(0,listSongs.size)
            }
            else if(!shuffleBoolean && !repeatBoolean) {
                if (position - 1 < 0) position = listSongs.size - 1
                else position--
            }
            uri = Uri.parse(listSongs[position].path)
            musicService.createMediaPlayer(position)
            metaData(uri)
            songName.text = listSongs[position].title
            artistName.text = listSongs[position].artist
            seekBar.max = musicService.duration / 1000
            this@PlayerActivity.runOnUiThread(runnable {
                if (::musicService.isInitialized){
                    val mCurrentPosition = musicService.currentPosition/1000
                    seekBar.progress = mCurrentPosition
                }
                handler.postDelayed(this, 1000)
            })
            musicService.onCompleted()
            showNotification(R.drawable.ic_baseline_pause_24)
            playPauseBtn.setBackgroundResource(R.drawable.ic_baseline_pause_24)
            musicService.start()
        }
        else{
            musicService.stop()
            musicService.release()
            if (shuffleBoolean && !repeatBoolean){
                position = Random.nextInt(0,listSongs.size)
            }
            else if(!shuffleBoolean && !repeatBoolean) {
                if (position - 1 < 0) position = listSongs.size - 1
                else position--
            }
            uri = Uri.parse(listSongs[position].path)
            musicService.createMediaPlayer(position)
            metaData(uri)
            songName.text = listSongs[position].title
            artistName.text = listSongs[position].artist
            seekBar.max = musicService.duration / 1000
            this@PlayerActivity.runOnUiThread(runnable {
                if (::musicService.isInitialized){
                    val mCurrentPosition = musicService.currentPosition/1000
                    seekBar.progress = mCurrentPosition
                }
                handler.postDelayed(this, 1000)
            })
            musicService.onCompleted()
            showNotification(R.drawable.ic_baseline_play_arrow_24)
            playPauseBtn.setBackgroundResource(R.drawable.ic_baseline_play_arrow_24)
        }
    }

    private fun playThreadBtn() {
        playThread = Thread(Runnable {
            playPauseBtn.setOnClickListener(View.OnClickListener {
                playPauseBtnClicked()
            })
        })
        playThread.start()
    }

    override fun playPauseBtnClicked() {
        if (musicService.isPlaying){
            showNotification(R.drawable.ic_baseline_play_arrow_24)
            playPauseBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            musicService.pause()
            seekBar.max = musicService.duration / 1000
            this@PlayerActivity.runOnUiThread(runnable {
                if (::musicService.isInitialized){
                    val mCurrentPosition = musicService.currentPosition/1000
                    seekBar.progress = mCurrentPosition
                }
                handler.postDelayed(this, 1000)
            })
        }
        else{
            playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_24)
            showNotification(R.drawable.ic_baseline_pause_24)
            musicService.start()
            seekBar.max = musicService.duration / 1000
            this@PlayerActivity.runOnUiThread(runnable {
                if (::musicService.isInitialized){
                    val mCurrentPosition = musicService.currentPosition/1000
                    seekBar.progress = mCurrentPosition
                }
                handler.postDelayed(this, 1000)
            })
        }
    }

    private fun formattedTime(mCurrentPosition: Int): String {
        val seconds : String = (mCurrentPosition % 60).toString()
        val minutes : String = (mCurrentPosition / 60).toString()
        val totalOut = "$minutes:$seconds"
        val totalNew = "$minutes:0$seconds"
        return if (seconds.length == 1){
            totalNew
        } else {
            totalOut
        }
    }

    private fun getIntentMethod() {
        position = intent.getIntExtra("position", -1)
        val sender = intent.getStringExtra("sender")
        listSongs = if (sender != null && sender == "albumDetails") albumFiles
                    else mFiles
        showNotification(R.drawable.ic_baseline_pause_24)
        playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_24)
        uri = Uri.parse(listSongs[position].path)
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra("servicePosition", position)
        startService(intent)
//        if (::musicService.isInitialized){
//            musicService.stop()
//            musicService.reset()
//            musicService.release()
//            musicService.createMediaPlayer(position)
//            musicService.start()
//        }
//        else{
//            musicService.createMediaPlayer(position)
//            musicService.start()
//            ::musicService.isInitialized = true
//        }
    }

    private fun initViews() {
        songName = findViewById<TextView>(R.id.song_name)
        artistName = findViewById<TextView>(R.id.song_artist)
        durationPlayed = findViewById<TextView>(R.id.durationPlayed)
        durationTotal = findViewById<TextView>(R.id.durationTotal)
        coverArt =findViewById<ImageView>(R.id.cover_art)
        nextBtn = findViewById<ImageView>(R.id.id_next)
        prevBtn = findViewById<ImageView>(R.id.id_prev)
        backBtn = findViewById<ImageView>(R.id.back_btn)
        shuffleBtn = findViewById<ImageView>(R.id.id_shuffle)
        repeatBtn = findViewById<ImageView>(R.id.id_repeat)
        playPauseBtn = findViewById<FloatingActionButton>(R.id.play_pause)
        seekBar = findViewById<SeekBar>(R.id.seekbar)
    }

    private fun metaData(uri: Uri){
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(uri.toString())
        val durationTotalInt = Integer.parseInt(listSongs[position].duration) / 1000
        durationTotal.text = formattedTime(durationTotalInt)
        val art : ByteArray? = retriever.embeddedPicture
        var bitmap : Bitmap?
        if (art != null){
            bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
            imageAnimation(this, coverArt, bitmap)
            Palette.from(bitmap).generate(Palette.PaletteAsyncListener {
                val swatch = it?.dominantSwatch
                if(swatch!=null){
                    val gradient = findViewById<ImageView>(R.id.imageViewGradient)
                    val mContainer = findViewById<RelativeLayout>(R.id.mContainer)
                    gradient.setBackgroundResource(R.drawable.gradient_bg)
                    mContainer.setBackgroundResource(R.drawable.main_bg)
                    val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                    intArrayOf(swatch.rgb, 0x00000000))
                    gradient.background = gradientDrawable
                    val gradientDrawableBg = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                    intArrayOf(swatch.rgb, swatch.rgb))
                    mContainer.background = gradientDrawableBg
                    songName.setTextColor(swatch.titleTextColor)
                    artistName.setTextColor(swatch.bodyTextColor)
                }
                else{
                    val gradient = findViewById<ImageView>(R.id.imageViewGradient)
                    val mContainer = findViewById<RelativeLayout>(R.id.mContainer)
                    gradient.setBackgroundResource(R.drawable.gradient_bg)
                    mContainer.setBackgroundResource(R.drawable.main_bg)
                    val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                        intArrayOf(0xff000000.toInt(), 0x00000000))
                    gradient.background = gradientDrawable
                    val gradientDrawableBg = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                        intArrayOf(0xff000000.toInt(), 0xff000000.toInt()))
                    mContainer.background = gradientDrawableBg
                    songName.setTextColor(Color.WHITE)
                    artistName.setTextColor(Color.DKGRAY)
                }
            })
        }
        else {
            Glide.with(this).asBitmap().load(R.drawable.ic_launcher_background).into(coverArt)
            val gradient = findViewById<ImageView>(R.id.imageViewGradient)
            val mContainer = findViewById<RelativeLayout>(R.id.mContainer)
            gradient.setBackgroundResource(R.drawable.gradient_bg)
            mContainer.setBackgroundResource(R.drawable.main_bg)
            songName.setTextColor(Color.WHITE)
            artistName.setTextColor(Color.DKGRAY)
        }
    }

    private fun imageAnimation(context: Context, imageView: ImageView, bitmap: Bitmap) {
        val animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
        val animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        animOut.setAnimationListener(object : Animation.AnimationListener{
            override fun onAnimationRepeat(p0: Animation?) {
            }

            override fun onAnimationEnd(p0: Animation?) {
                Glide.with(context).load(bitmap).into(imageView)
                animIn.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(p0: Animation?) {
                    }

                    override fun onAnimationEnd(p0: Animation?) {
                    }

                    override fun onAnimationStart(p0: Animation?) {
                    }

                })
                imageView.startAnimation(animIn)
            }

            override fun onAnimationStart(p0: Animation?) {
            }
        })
        imageView.startAnimation(animOut)
    }

    private fun runnable(body: Runnable.(Runnable)->Unit) = object : Runnable{
        override fun run() {
            this.body(this)
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val myBinder = service as MusicService.MyBinder
        musicService = myBinder.service
        Toast.makeText(this, "Connected$musicService", Toast.LENGTH_SHORT).show()
        seekBar.max = musicService.duration / 1000
        metaData(uri)
        songName.text = listSongs[position].title
        song_artist.text = listSongs[position].artist
        musicService.onCompleted()
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        //musicService = null
    }

    fun showNotification(playPauseBtn : Int){
        val intent = Intent(this, PlayerActivity::class.java)
        val contentIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
        val prevIntent = Intent(this, PlayerActivity::class.java)
        prevIntent.action = ACTION_PREVIOUS
        val prevPending = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val pauseIntent = Intent(this, PlayerActivity::class.java)
        prevIntent.action = ACTION_PLAY
        val pausePending = PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val nextIntent = Intent(this, PlayerActivity::class.java)
        prevIntent.action = ACTION_NEXT
        val nextPending = PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val picture = getAlbumArt(musicFiles[position].path)
        val thumb = if (picture!=null){
            BitmapFactory.decodeByteArray(picture, 0, picture.size) }
        else{
            BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_background)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_2).
                setSmallIcon(playPauseBtn).setLargeIcon(thumb).
                setContentTitle(musicFiles[position].title).setContentText(musicFiles[position].artist).
                addAction(R.drawable.ic_baseline_skip_previous_24, "Previous", prevPending).
                addAction(R.drawable.ic_baseline_pause_24, "Pause", pausePending).
                addAction(R.drawable.ic_baseline_skip_next_24, "next", nextPending).
                setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSessionCompat.sessionToken)).
                setPriority(NotificationCompat.PRIORITY_HIGH).
                setOnlyAlertOnce(true).
                build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notification)
    }

    private fun getAlbumArt(uri: String): ByteArray? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(uri)
        val art = retriever.embeddedPicture
        retriever.release()
        return art
    }
}