package com.ke.sentricall

import android.app.Activity
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
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
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

        @Volatile
        var isRecording: Boolean = false

        // Backwards compat alias if you used isRunning earlier
        val isRunning: Boolean
            get() = isRecording

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
            if (isRecording) {
                // already recording
                recordingCallback?.onRecordingStarted()
                return
            }

            val resultCode =
                intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            val resultData =
                intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

            if (resultData == null || resultCode != Activity.RESULT_OK) {
                recordingCallback?.onRecordingError("Missing permission data")
                stopSelf()
                return
            }

            // Android 14+: must be foreground BEFORE using MediaProjection
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)

            val mpm =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpm.getMediaProjection(resultCode, resultData)

            if (mediaProjection == null) {
                recordingCallback?.onRecordingError("MediaProjection is null")
                stopSelf()
                return
            }

            // 🔑 IMPORTANT: register callback BEFORE starting capture / creating VD
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.d("ScreenRecordingService", "MediaProjection onStop called")
                    stopRecordingInternal()
                }
            }, null)

            // Prepare output file
            val dir = File(
                getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "screen_records"
            )
            if (!dir.exists()) dir.mkdirs()

            val dateStr =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "sentricall_screen_$dateStr.mp4")
            outputFilePath = file.absolutePath

            // Get screen metrics from resources (service-safe)
            val metrics: DisplayMetrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val densityDpi = metrics.densityDpi

            // Setup MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(8_000_000)
                setVideoFrameRate(30)
                setVideoSize(width, height)
                setOutputFile(outputFilePath)
                prepare()
            }

            // Create virtual display AFTER callback registration
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "SentricallScreen",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()
            isRecording = true
            recordingCallback?.onRecordingStarted()

        } catch (e: SecurityException) {
            Log.e("ScreenRecordingService", "Security error starting screen record", e)
            recordingCallback?.onRecordingError(
                e.message ?: "MediaProjection security error"
            )
            stopSelf()
        } catch (e: IllegalStateException) {
            // This is where the "must register callback before starting capture" error used to come from
            Log.e("ScreenRecordingService", "IllegalState starting screen record", e)
            recordingCallback?.onRecordingError(
                e.message ?: "Illegal state starting screen recording"
            )
            stopSelf()
        } catch (e: Exception) {
            Log.e("ScreenRecordingService", "Error starting screen record", e)
            recordingCallback?.onRecordingError(
                e.message ?: "Error starting screen recording"
            )
            stopSelf()
        }
    }

    private fun stopRecordingInternal() {
        try {
            if (!isRecording) {
                stopSelf()
                return
            }

            isRecording = false

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
            recordingCallback?.onRecordingError(
                e.message ?: "Error stopping recording"
            )
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

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Sentricall is recording your screen")
            .setContentText("Tap to return to the app.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}