package com.highlevel.flutter_twilio_voice.flutter_twilio_voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.twilio.voice.*
import io.flutter.plugin.common.MethodChannel

class TwilioAndroid(context: Context,
                    wakeLock: PowerManager.WakeLock,
                    audioManager: AudioManager,
                    channel: MethodChannel,
                    val createNotificationChannel: () -> Unit,
                    val cancelNotification: () -> Unit) {
    private val TAG = "FlutterTwilio"

    private val PERMISSION_REQUEST_CODE = 1
    private val _audioManager: AudioManager = audioManager

    private var savedAudioMode = AudioManager.MODE_INVALID
    private var params: HashMap<String, String> = HashMap<String, String>()
    private var activeCall: Call? = null

    private val callListener: Call.Listener = callListener()
    private val _wakeLock: PowerManager.WakeLock = wakeLock
    private var isShowingNotification: Boolean = false;
    private val _context: Context = context
    var notificationId: Int = 123
    val _channel: MethodChannel = channel

    fun callListener(): Call.Listener {
        return object : Call.Listener {

            override fun onRinging(call: Call) {
                Log.e(TAG, "on Ringing")
                val args = HashMap<String, String>()
                args.put("status", "ringing")
                _channel.invokeMethod("call_listener", args)
            }

            override fun onConnectFailure(call: Call, callException: CallException) {
                Log.e(TAG, "on Connect Failure")
                setAudioFocus(false)
                stopWakeLock();
                val message: String = String.format("Call Error:%d, %s", callException.errorCode, callException.message)
                Log.e(TAG, message)
                cancelNotification()
                val args = HashMap<String, String>()
                args.put("status", "connect_failure")

                _channel.invokeMethod("call_listener", args)
            }


            override fun onConnected(call: Call) {
                Log.e(TAG, "on Connected")
                setAudioFocus(true)
                activeCall = call
                val callSid: String? = call.sid
                val callFrom: String? = call.from
                Log.e(TAG, "Connnected from: $callFrom")

                val args: HashMap<String, String> = HashMap<String, String>()
                args.put("status", "connected");
                args.put("sid", callSid!!)
                args.put("from", callFrom!!)
                _channel.invokeMethod("call_listener", args)
            }

            override fun onReconnecting(call: Call, callException: CallException) {
                Log.e(TAG, "on Reconnecting")
            }

            override fun onReconnected(call: Call) {
                Log.e(TAG, "on Reconnected")

            }

            override fun onDisconnected(call: Call, callException: CallException?) {
                Log.e(TAG, "on Disconnected")
                setAudioFocus(false)
                stopWakeLock()
                if (callException != null) {
                    val message: String = String.format("Call Error: %d %s", callException.errorCode, callException.message)
                }
                cancelNotification()
                val args: HashMap<String, String> = HashMap<String, String>()
                args.put("status", "disconnected")
                _channel.invokeMethod("call_listener", args)
            }

        }

    }

    fun setAudioFocus(setFocus: Boolean) {
        if (setFocus) {
            savedAudioMode = _audioManager.mode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes: AudioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()

                val focusRequest: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(playbackAttributes)
                        .build()

                _audioManager.requestAudioFocus(focusRequest)
            } else {
                val focusRequestResult: Int = _audioManager.requestAudioFocus(OnAudioFocusChangeListener { focusChange: Int -> }, AudioManager.STREAM_VOICE_CALL,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
            _audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        }
    }

    fun stopWakeLock() {
        if (_wakeLock.isHeld)
            _wakeLock.release()
    }

    fun startWakeLock() {
        if (!_wakeLock.isHeld) {
            _wakeLock.acquire(600000L) /* 10* 60* 1000L => 10 Minutes*/
        }
    }

    fun invokeCall(accessToken: String, to: String, locationId: String, callerId: String) {
        params.put("number", to)
        params.put("callerId", callerId)
        params.put("location", locationId)
        val codecList: ArrayList<AudioCodec> = ArrayList<AudioCodec>()
        codecList.add(OpusCodec())
        codecList.add(PcmuCodec())
        val connectOptions: ConnectOptions = ConnectOptions.Builder(accessToken)
                .params(params)
                .preferAudioCodecs(codecList)
                .build()
        activeCall = Voice.connect(_context, connectOptions, callListener)
    }

    fun disconnect() {
        if (activeCall != null) {
            activeCall!!.disconnect()
            activeCall = null
        }
    }

    fun hold(): Boolean {
        if (activeCall != null) {
            val hold: Boolean = !activeCall!!.isOnHold
            activeCall!!.hold(hold)

            return activeCall!!.isOnHold
        }
        return false
    }

    fun mute(): Boolean {
        if (activeCall != null) {
            val mute: Boolean = !activeCall!!.isMuted
            activeCall!!.mute(mute)
            return activeCall!!.isMuted
        }
        return false
    }

    fun speaker(speaker: Boolean): Boolean {
        try {
            _audioManager.setSpeakerphoneOn(speaker)
        } catch (e: Exception) {
            Log.e(TAG, "speaker: ", e)
        }
        return _audioManager.isSpeakerphoneOn
    }

    fun keyPress(digit: String) {
        if (activeCall != null) {
            activeCall!!.sendDigits(digit)
        }
    }
}
