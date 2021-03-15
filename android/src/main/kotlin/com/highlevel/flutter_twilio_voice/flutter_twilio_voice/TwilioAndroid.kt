package com.highlevel.flutter_twilio_voice.flutter_twilio_voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.os.PowerManager
import com.twilio.voice.*
import io.flutter.plugin.common.MethodChannel

class TwilioAndroid(
    context: Context,
    wakeLock: PowerManager.WakeLock,
    audioManager: AudioManager,
    channel: MethodChannel,
    val cancelNotification: () -> Unit
) {
    private val _audioManager: AudioManager = audioManager
    private var savedAudioMode = AudioManager.MODE_INVALID
    private var params: HashMap<String, String> = HashMap<String, String>()
    private var activeCall: Call? = null
    private val callListener: Call.Listener = callListener()
    private val _wakeLock: PowerManager.WakeLock = wakeLock
    private val _context: Context = context
    val _channel: MethodChannel = channel
    private var isConnected: Boolean = false
    private var isRinging: Boolean = false
    private var isConnectionFailed: Boolean = false
    private var isDisConnected: Boolean = false
    private var isReconnecting: Boolean = false
    private var isReconnected: Boolean = false

    fun callListener(): Call.Listener {
        return object : Call.Listener {
            override fun onRinging(call: Call) {
                isRinging=true
                isConnected=false
                isConnectionFailed=false
                isDisConnected=false
                isReconnecting=false
                isReconnected=false
                val args = HashMap<String, String>()
                args.put("status", "ringing")
                _channel.invokeMethod("call_listener", args)
            }

            override fun onConnectFailure(call: Call, callException: CallException) {
                isRinging=false
                isConnected=false
                isConnectionFailed=true
                isDisConnected=false
                isReconnecting=false
                isReconnected=false
                setAudioFocus(false)
                stopWakeLock();
                val message: String = String.format(
                    "Call Error:%d, %s",
                    callException.errorCode,
                    callException.message
                )
                cancelNotification()
                val args = HashMap<String, String>()
                args.put("status", "connect_failure")
                args.put("message", message)
                _channel.invokeMethod("call_listener", args)
                _audioManager.mode = AudioManager.MODE_NORMAL
            }


            override fun onConnected(call: Call) {
                isRinging=false
                isConnected=true
                isConnectionFailed=false
                isDisConnected=false
                isReconnecting=false
                isReconnected=false
                setAudioFocus(true)
                activeCall = call
                val callSid: String? = call.sid
                val callFrom: String? = call.from
                val args: HashMap<String, String> = HashMap<String, String>()
                args.put("status", "connected")
                if (callSid != null)
                    args.put("sid", callSid)
                if (callFrom != null)
                    args.put("from", callFrom)
                _channel.invokeMethod("call_listener", args)
            }

            override fun onReconnecting(call: Call, callException: CallException) {
                isRinging=false
                isConnected=false
                isConnectionFailed=false
                isDisConnected=false
                isReconnecting=true
                isReconnected=false
            }

            override fun onReconnected(call: Call) {
                isRinging=false
                isConnected=false
                isConnectionFailed=false
                isDisConnected=false
                isReconnecting=false
                isReconnected=true
            }

            override fun onDisconnected(call: Call, callException: CallException?) {
                isRinging=false
                isConnected=false
                isConnectionFailed=false
                isDisConnected=true
                isReconnecting=false
                isReconnected=false
                setAudioFocus(false)
                stopWakeLock()
                val args: HashMap<String, String> = HashMap<String, String>()
                if (callException != null) {
                    val message: String = String.format(
                        "Call Error: %d %s",
                        callException.errorCode,
                        callException.message
                    )
                    args.put("message", message)
                }
                cancelNotification()
                args.put("status", "disconnected")
                _channel.invokeMethod("call_listener", args)
                _audioManager.mode = AudioManager.MODE_NORMAL
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

                val focusRequest: AudioFocusRequest =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(playbackAttributes)
                        .build()

                _audioManager.requestAudioFocus(focusRequest)
            } else {
                _audioManager.requestAudioFocus(
                    OnAudioFocusChangeListener { focusChange: Int -> },
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
            _audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        }
    }

    fun stopWakeLock() {
        if (_wakeLock.isHeld)
            _wakeLock.release()
    }

    fun invokeCall(accessToken: String, data: HashMap<String, String>) {
        data.entries.forEach { it ->
            params.put(it.key, it.value)
        }
        val codecList: ArrayList<AudioCodec> = ArrayList<AudioCodec>()
        codecList.add(OpusCodec())
        codecList.add(PcmuCodec())
        val connectOptions: ConnectOptions = ConnectOptions.Builder(accessToken)
            .params(params)
            .preferAudioCodecs(codecList)
            .build()
        activeCall = Voice.connect(_context, connectOptions, callListener)

    }

    fun disconnect():Boolean {
        if (activeCall != null) {
            activeCall!!.disconnect()
            activeCall = null
            return true
        }
        return false
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
            _audioManager.isSpeakerphoneOn = speaker
        } catch (e: Exception) {
        }
        return _audioManager.isSpeakerphoneOn
    }

    fun keyPress(digit: String) {
        if (activeCall != null) {
            activeCall!!.sendDigits(digit)
        }
    }

    fun getStatus(): MutableMap<String, Any> {

       return mutableMapOf<String, Any>(
            "speaker" to _audioManager.isSpeakerphoneOn,
            "mute" to activeCall!!.isMuted,
            "hold" to activeCall!!.isOnHold,
            "isRinging" to isRinging,
            "isConnected" to isConnected,
            "isConnectionFailed" to isConnectionFailed,
            "isDisconnected" to isDisConnected,
            "isReconnecting" to isReconnecting,
            "isReconnected" to isReconnected
        )
    }
}
