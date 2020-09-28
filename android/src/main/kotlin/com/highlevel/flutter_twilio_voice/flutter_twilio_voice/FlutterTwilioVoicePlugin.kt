package com.highlevel.flutter_twilio_voice.flutter_twilio_voice

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Context.POWER_SERVICE
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
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
import java.util.*

/** FlutterTwilioVoicePlugin */
public class FlutterTwilioVoicePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private var _field = 0x00000020
    private lateinit var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    private lateinit var twilioManager: TwilioManager
    private val PERMISSION_REQUEST_CODE = 1

    var appPermission = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA)

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = flutterPluginBinding
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_twilio_voice")
        channel.setMethodCallHandler(this)
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            Toast.makeText(registrar.activeContext(), "Hello", Toast.LENGTH_LONG).show();
            val channel = MethodChannel(registrar.messenger(), "flutter_twilio_voice")
            channel.setMethodCallHandler(FlutterTwilioVoicePlugin())
            Log.e("FlutterTwili", "register with")
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
        Log.e("FlutterTwili", "method call ${call.arguments} ${call.method}")
        when (call.method) {
            "call" -> {
                Log.e("FlutterTwili", "call")
                twilioManager.startCall(call.argument<String>("name") as String,
                        call.argument<String>("accessToken") as String,
                        call.argument<String>("to") as String,
                        call.argument<String>("locationId") as String,
                        call.argument<String>("callerId") as String)

            }
            "hold" -> {
                Log.e("FlutterTwili", "hold")
                result.success(twilioManager.toggleHold())
            }
            "speaker" -> {
                Log.e("FlutterTwili", "speaker")
                result.success(twilioManager.toggleSpeaker(call.argument<Boolean>("speaker") as Boolean))
            }
            "mute" -> {
                Log.e("FlutterTwili", "mute")
                result.success(twilioManager.toggleMute())
            }
            "keyPress" -> {
                Log.e("FlutterTwili", "keyPress")
                twilioManager.keyPress(call.argument<String>("digit") as String)
            }
            "disconnect" -> {
                Log.e("FlutterTwili", "disconnect")
                twilioManager.disconnectCall()
            }
            else -> {
                Log.e("FlutterTwili", "something else ${call.method}")
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Log.e("FlutterTwili", "on detached")

        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.e("FlutterTwili", "onAttachedToActivity")
        checkAndRequestPermission(binding.activity)

        try {
            _field = PowerManager::class.java.javaClass.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null)
        } catch (e: Exception) {
        }

        twilioManager = TwilioManager(context = binding.activity,
                activity = binding.activity,
                audioManager = (binding.activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager?)!!,
                powerManager = (binding.activity.getSystemService(POWER_SERVICE) as PowerManager?)!!,
                wakeLock = (binding.activity.getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(_field, binding.activity.localClassName),
                notificationManager = (binding.activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager),
                channel = channel
        )
        binding.activity.volumeControlStream = AudioManager.STREAM_VOICE_CALL

    }

    override fun onDetachedFromActivityForConfigChanges() {
//        TODO("Not yet implemented")
        Log.e("FlutterTwili", "onDetachedFromActivityForConfigChanges")

    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
//        TODO("Not yet implemented")
        Log.e("FlutterTwili", "onReattachedToActivityForConfigChanges")
    }

    override fun onDetachedFromActivity() {
//        TODO("Not yet implemented")
        Log.e("FlutterTwili", "onDetachedFromActivity")
    }


}
