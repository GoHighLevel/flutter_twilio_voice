import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_twilio_voice/constants/method_channel_methods.dart';
import 'package:flutter_twilio_voice/exceptions/twilio_call_exceptions.dart';
import 'package:flutter_twilio_voice/models/call.dart';
import 'package:meta/meta.dart';

class   FlutterTwilioVoice {
  static const MethodChannel _channel =
      const MethodChannel('flutter_twilio_voice');
  bool isCalling = false,
      isRinging = false,
      isConnected = false,
      onHold = false,
      onSpeaker = true,
      onMute = false;
  
  Function onConnected;
  Function onDisconnected;
  Function onPermissionDenied;
  Function onConnectFailure;

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  FlutterTwilioVoice({
    @required this.onConnected,
    @required this.onPermissionDenied,
    @required this.onConnectFailure,
  }) {
    _listenToMethodCalls();
  }

  Future<void> call({@required Call call}) async {
    Completer<void> completer = Completer();

    _channel.invokeMethod(MethodChannelMethods.CALL, {
      "to": call.to,
      "accessToken": call.accessToken,
      "name": call.name,
      "locationId": call.locationId,
      "callerId": call.callerId,
    }).then((value) {
      completer.complete();
    }, onError: (PlatformException e) {
      throw TwilioCallException(
        error: e.message,
      );
    });
    return completer.future;
  }

  Future<bool> hold() async {
    Completer<bool> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.HOLD)
        .then((result) => completer.complete(result))
        .catchError((PlatformException e) {
      throw TwilioCallException(
        error: e.message,
      );
    });

    return completer.future;
  }

  Future<bool> speaker(bool speaker) async {
    Completer<bool> completer = Completer();
    _channel
        .invokeMethod(
            MethodChannelMethods.SPEAKER, {'speaker': speaker ?? false})
        .then((result) => completer.complete(result))
        .catchError((PlatformException e) {
          throw TwilioCallException(
            error: e.message,
          );
        });

    return completer.future;
  }

  Future<bool> mute() async {
    Completer<bool> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.MUTE)
        .then((result) => completer.complete(result))
        .catchError((PlatformException e) {});

    return completer.future;
  }

  Future<void> keyPress({@required String keyValue}) {
    Completer<void> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.KEY_PRESS, {'digit': keyValue})
        .then((result) => completer.complete())
        .catchError((PlatformException e) {
          throw TwilioCallException(
            error: e.message
          );
        });

    return completer.future;
  }

  Future<void> disconnect() {
    Completer<void> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.MUTE)
        .then((result) => completer.complete())
        .catchError((PlatformException e) {
      throw TwilioCallException(
        error: e.message
      );
    });

    return completer.future;
  }

  void _listenToMethodCalls() {
    _channel.setMethodCallHandler((MethodCall call) async {
      if (call.method == "call_listener") {
        String status = call.arguments['status'];
        print('status: $status');
        switch (status) {
          case "ringing":
            isCalling = isRinging = true;
            break;
          case "permission_denied":
            onPermissionDenied();
            break;
          case "disconnected":
            onDisconnected();
            break;
          case "connect_failure":
            isConnected = false;
            isCalling = isRinging = false;
            onConnectFailure();
            break;
          case "connected":
            isConnected = true;
            onConnected();
            break;
        }
      }
    });
  }
}
