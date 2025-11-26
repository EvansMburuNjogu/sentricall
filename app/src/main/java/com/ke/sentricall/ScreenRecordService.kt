package com.ke.sentricall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjection.Callback
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordingService : Service() {

    companion object {
        const val ACTION_START = "com.ke.sentricall.SCREEN_RECORD_START"
        const val ACTION_STOP = "com.ke.sentricall.SCREEN_RECORD_STOP"

        const val EXTRA_RESULT_CODE = "RESULT_CODE"
        const val EXTRA_RESULT_DATA = "RESULT_DATA"

        private const val NOTIFICATION_CHANNEL_ID = "screen_record_channel"
        private const val NOTIFICATION_ID = 9001

        var recordingCallback: RecordingCallback? = null
    }

    interface RecordingCallback {
        fun onRecordingStarted()
        fun onRecordingStopped(filePath: String?)
        fun onRecordingError(error: String)
    }

    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFilePath: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecordingInternal(intent)
            ACTION_STOP -> stopRecordingInternal()
        }
        return START_NOT_STICKY
    }

    private fun startRecordingInternal(intent: Intent) {
        try {
            if (mediaProjection != null) {
                // already running
                recordingCallback?.onRecordingStarted()
                return
            }

            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
            if (resultData == null || resultCode != android.app.Activity.RESULT_OK) {
                recordingCallback?.onRecordingError("Missing permission data")
                stopSelf()
                return
            }

            val mpm =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpm.getMediaProjection(resultCode, resultData)

            // Prepare file path
            val dir = File(
                getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "screen_records"
            )
            if (!dir.exists()) dir.mkdirs()

            val dateStr =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "sentricall_screen_$dateStr.mp4")
            outputFilePath = file.absolutePath

            // Setup recorder
            mediaRecorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(8_000_000)
                setVideoFrameRate(30)

                val metrics = DisplayMetrics()
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val display = display
                    display?.getRealMetrics(metrics)
                } else {
                    @Suppress("DEPRECATION")
                    wm.defaultDisplay.getRealMetrics(metrics)
                }
                val width = metrics.widthPixels
                val height = metrics.heightPixels

                setVideoSize(width, height)
                setOutputFile(outputFilePath)
                prepare()
            }

            // Foreground notification
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)

            // Virtual display
            val metrics = DisplayMetrics()
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = display
                display?.getRealMetrics(metrics)
            } else {
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealMetrics(metrics)
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "SentricallScreen",
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaProjection?.registerCallback(object : Callback() {
                override fun onStop() {
                    super.onStop()
                    stopRecordingInternal()
                }
            }, null)

            mediaRecorder?.start()
            recordingCallback?.onRecordingStarted()

        } catch (e: Exception) {
            Log.e("ScreenRecordingService", "Error starting screen record", e)
            recordingCallback?.onRecordingError(e.message ?: "Unknown error")
            stopSelf()
        }
    }

    private fun stopRecordingInternal() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w("ScreenRecordingService", "MediaRecorder stop error: $e")
                }
                reset()
                release()
            }
            mediaRecorder = null

            virtualDisplay?.release()
            virtualDisplay = null

            mediaProjection?.stop()
            mediaProjection = null

            stopForeground(true)
            recordingCallback?.onRecordingStopped(outputFilePath)
        } catch (e: Exception) {
            Log.e("ScreenRecordingService", "Error stopping screen record", e)
            recordingCallback?.onRecordingError(e.message ?: "Error stopping recording")
        } finally {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Screen recording",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // IMPORTANT: use an existing icon here.
        // If you have a better guard icon later, replace R.mipmap.ic_launcher.
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Sentricall is recording your screen")
            .setContentText("Tap to return to the app.")
            .setSmallIcon(R.mipmap.ic_launcher) // <- replace with your own drawable if you like
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        return builder.build()
    }
}