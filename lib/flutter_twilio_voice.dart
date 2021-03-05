import 'dart:async' show Completer;
import 'dart:collection' show HashMap;

import 'package:flutter/services.dart'
    show PlatformException, MethodChannel, MethodCall;
import 'package:flutter_twilio_voice/constants/method_channel_methods.dart';
import 'package:flutter_twilio_voice/exceptions/twilio_call_exceptions.dart';
import 'package:flutter_twilio_voice/models/call.dart';
import 'package:meta/meta.dart' show required;

class FlutterTwilioVoice {
  static const MethodChannel _channel =
      const MethodChannel('flutter_twilio_voice');
  bool isCalling = false,
      isRinging = false,
      isConnected = false,
      onHold = false,
      onSpeaker = true,
      onMute = false;

  final Function onConnected;
  final Function onDisconnected;
  final Function onPermissionDenied;
  final Function onConnectFailure;
  final Function onRinging;
  final String defaultIcon;

  FlutterTwilioVoice({
    @required this.defaultIcon,
    @required this.onConnected,
    @required this.onPermissionDenied,
    @required this.onConnectFailure,
    @required this.onDisconnected,
    @required this.onRinging,
  }) {
    _listenToMethodCalls();
  }

  Future<void> connectCall({@required Call call}) async {
    Completer<void> completer = Completer();
    print(call.dataToSend);
    _channel.invokeMethod(MethodChannelMethods.CALL, {
      'icon': defaultIcon,
      'name': call.name,
      'accessToken': call.accessToken,
      'data': call.dataToSend
    }).then((value) {
      completer.complete();
    }, onError: (Object e) {
      if (e is PlatformException)
        throw TwilioCallException(error: e.message);
      else
        throw e;
    });
    return completer.future;
  }

  Future<bool> hold() async {
    Completer<bool> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.HOLD)
        .then((result) => completer.complete(result))
        .catchError((Object e) {
      if (e is PlatformException)
        throw TwilioCallException(error: e.message);
      else
        throw e;
    });
    return completer.future;
  }

  Future<bool> speaker(bool speaker) async {
    Completer<bool> completer = Completer();
    _channel
        .invokeMethod(
            MethodChannelMethods.SPEAKER, {'speaker': speaker ?? false})
        .then((result) => completer.complete(result))
        .catchError((Object e) {
          if (e is PlatformException)
            throw TwilioCallException(error: e.message);
          else
            throw e;
        });

    return completer.future;
  }

  Future<bool> mute() async {
    Completer<bool> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.MUTE)
        .then((result) => completer.complete(result))
        .catchError((Object e) {
      if (e is PlatformException)
        throw TwilioCallException(error: e.message);
      else
        throw e;
    });

    return completer.future;
  }

  Future<void> pressKey({@required String keyValue}) {
    Completer<void> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.KEY_PRESS, {'digit': keyValue})
        .then((result) => completer.complete())
        .catchError((Object e) {
          if (e is PlatformException)
            throw TwilioCallException(error: e.message);
          else
            throw e;
        });

    return completer.future;
  }

  Future<void> disconnectCall() {
    Completer<void> completer = Completer();
    _channel
        .invokeMethod(MethodChannelMethods.DISCONNECT)
        .then((result) => completer.complete())
        .catchError((Object e) {
      if (e is PlatformException)
        throw TwilioCallException(error: e.message);
      else
        throw e;
    });

    return completer.future;
  }

  void _listenToMethodCalls() {
    _channel.setMethodCallHandler((MethodCall call) async {
      if (call.method == 'call_listener') {
        String status = call.arguments['status'];
        switch (status) {
          case 'ringing':
            isCalling = isRinging = true;
            onRinging();
            break;
          case 'permission_denied':
            onPermissionDenied();
            break;
          case 'disconnected':
            onDisconnected();
            break;
          case 'connect_failure':
            isConnected = false;
            isCalling = isRinging = false;
            onConnectFailure();
            break;
          case 'connected':
            isConnected = true;
            String sid, from, status;
            print('arguments');
            print(call.arguments);
            if (call.arguments != null) {
              sid = call.arguments['sid'];
              from = call.arguments['from'];
              status = call.arguments['status'];
            }

            onConnected(new HashMap()
              ..addAll({
                if (sid != null) 'sid': sid,
                if (from != null) 'from': from,
                if (status != null) 'status': status
              }));
            break;
        }
      }
    });
  }

  void getStatus() {
    _channel.invokeMethod('getStatus').then((result) {
      print(result);
    });
  }
}
