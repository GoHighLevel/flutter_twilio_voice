package com.highlevel.flutter_twilio_voice.flutter_twilio_voice

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Context.POWER_SERVICE
import android.media.AudioManager
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

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
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = flutterPluginBinding
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_twilio_voice")
        channel.setMethodCallHandler(this);


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

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.e("FlutterTwili", "method call ${call.arguments} ${call.method}")
        when (call.method) {
            "call" -> {
                Log.e("FlutterTwili", "call")
                twilioManager.buildCallNotification(call.argument<String>("name") as String)
            }
            "hold" -> {
                Log.e("FlutterTwili", "hold")
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
//        TODO("Not yet implemented")
        Log.e("FlutterTwili", "onAttachedToActivity")
        try {
            _field = PowerManager::class.java.javaClass.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null)
        } catch (e: Exception) {
        }

        twilioManager = TwilioManager(context = binding.activity,
                activity = binding.activity,
                audioManager = (binding.activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager?)!!,
                powerManager = (binding.activity.getSystemService(POWER_SERVICE) as PowerManager?)!!,
                wakeLock = (binding.activity.getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(_field, binding.activity.localClassName),
                notificationManager = (binding.activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
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
