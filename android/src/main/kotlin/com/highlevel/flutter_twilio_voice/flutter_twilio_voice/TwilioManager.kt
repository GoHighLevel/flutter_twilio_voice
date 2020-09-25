package com.highlevel.flutter_twilio_voice.flutter_twilio_voice

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class TwilioManager(context: Context, activity: Activity, powerManager: PowerManager, wakeLock: PowerManager.WakeLock, audioManager: AudioManager, notificationManager: NotificationManager) {

    private val TAG = "VoiceActivity"

    private val _wakeLock: PowerManager.WakeLock = wakeLock
    private val _powerManager: PowerManager = powerManager
    private val _audioManager: AudioManager = audioManager
    private val _notificationManager: NotificationManager = notificationManager
    private val _context: Context = context
    private val PERMISSION_REQUEST_CODE: Int = 1
    private val notificationId: Int = 123
    private val CHANNEL_ID: String = "$TAG/NOTIFICATION_CHANNEL_ID";
    private var isShowingNotification: Boolean = false
    private val _activity: Activity = activity
    lateinit var twilioAndroid: TwilioAndroid

    init {
        createNotificationChannel()
        registerPlatformChannels()
        checkAndRequestPermission()
        twilioAndroid = TwilioAndroid(_context, _wakeLock, _audioManager, createNotificationChannel = { createNotificationChannel()},cancelNotification = { cancelNotification()})

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "notification_channel";
            val importance: Int = NotificationManager.IMPORTANCE_DEFAULT
            val channel: NotificationChannel = NotificationChannel(CHANNEL_ID, name, importance)
            _notificationManager.createNotificationChannel(channel)
        }
    }

    fun cancelNotification() {
        if (!isShowingNotification) {
            val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(_context)
            notificationManager.cancel(notificationId)
            isShowingNotification = false
        }
    }

    private fun registerPlatformChannels() {

    }

    private fun checkAndRequestPermission() {
        val listPermissionNeeded: ArrayList<String> = ArrayList()
        for (perm in listPermissionNeeded) {
            if (ContextCompat.checkSelfPermission(_context, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(perm)
            }
        }

        if (!listPermissionNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(_activity, listPermissionNeeded.toTypedArray(), PERMISSION_REQUEST_CODE);
        }

    }

    fun checkPermissionForMicrophone(): Boolean {
        val resultMic: Int = ContextCompat.checkSelfPermission(_context, Manifest.permission.RECORD_AUDIO)
        return resultMic == PackageManager.PERMISSION_DENIED
    }

    fun buildCallNotification(name: String) {
        val intent: Intent = Intent(_context, _activity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        val pendingIntent: PendingIntent = PendingIntent.getActivity(_activity, 0, intent, 0)
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(
                _context, CHANNEL_ID
        )
//                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(name)
                .setContentText("Outbound call")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setUsesChronometer(true)
        val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(_context)
        notificationManager.notify(notificationId, builder.build())
        isShowingNotification = true
    }

}