package com.highlevel.flutter_twilio_voice.flutter_twilio_voice

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Context.POWER_SERVICE
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

public class FlutterTwilioVoicePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private var _field = 0x00000020
    private lateinit var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    private lateinit var twilioManager: TwilioManager
    private val PERMISSION_REQUEST_CODE = 1
    private lateinit var context: Context

    var appPermission = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA)

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = flutterPluginBinding
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_twilio_voice")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }


    companion object {
        private lateinit var instance: FlutterTwilioVoicePlugin

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            instance = FlutterTwilioVoicePlugin()
            instance.channel = MethodChannel(registrar.messenger(), "flutter_twilio_voice")
            instance.channel.setMethodCallHandler(instance)
            instance.initPlugin(registrar.activity())
            instance.context = registrar.activeContext()

        }
    }

    fun checkAndRequestPermission(activity: Activity) {
        val listPermissionNeeded: MutableList<String> = ArrayList()
        for (perm in appPermission) {
            if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(perm)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    listPermissionNeeded.toTypedArray(),
                    PERMISSION_REQUEST_CODE
            )
        }
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "call" -> {
                val isValid: Boolean = isValidDrawableResource(context, call.argument<String>("icon") as String)
                if (isValid)
                    twilioManager.defaultIcon = call.argument<String>("icon") as String
                twilioManager.startCall(call.argument<String>("name") as String,
                        call.argument<String>("accessToken") as String,
                        call.argument<HashMap<String, String>>("data") as HashMap<String, String>
                )
                result.success(true)

            }
            "hold" -> {
                result.success(twilioManager.toggleHold())
            }
            "speaker" -> {
                result.success(twilioManager.toggleSpeaker(call.argument<Boolean>("speaker") as Boolean))
            }
            "mute" -> {
                result.success(twilioManager.toggleMute())
            }
            "keyPress" -> {
                twilioManager.keyPress(call.argument<String>("digit") as String)
                result.success(true)
            }
            "disconnect" -> {
                twilioManager.disconnectCall()
                result.success(true)
            }
            else -> {
                result.success(false)
            }
        }
    }

    private fun isValidDrawableResource(context: Context, name: String): Boolean {
        val resourceId: Int = context.resources.getIdentifier(name, "drawable", context.packageName)
        return resourceId != 0
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    fun initPlugin(activity: Activity) {
        checkAndRequestPermission(activity)
        twilioManager = TwilioManager(context = activity,
                activity = activity,
                audioManager = (activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager?)!!,
                wakeLock = (activity.getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, activity.localClassName),
                notificationManager = (activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager),
                channel = channel
        )
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        checkAndRequestPermission(binding.activity)

        twilioManager = TwilioManager(context = binding.activity,
                activity = binding.activity,
                audioManager = (binding.activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager?)!!,
                wakeLock = (binding.activity.getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, binding.activity.localClassName),
                notificationManager = (binding.activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager),
                channel = channel
        )
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onDetachedFromActivity() {
    }


}
