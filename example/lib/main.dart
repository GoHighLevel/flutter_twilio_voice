import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter_twilio_voice/flutter_twilio_voice.dart';
import 'package:flutter_twilio_voice/models/call.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  FlutterTwilioVoice twilioVoice;
  bool isCalling = false,
      isRinging = false,
      isConnected = false,
      onHold = false,
      onSpeaker = true,
      onMute = false;

  @override
  void initState() {
    super.initState();
    twilioVoice = FlutterTwilioVoice(
        defaultIcon: "periscope",
        onRinging: () {
          isCalling = isRinging = true;
          setState(() {});
        },
        onConnectFailure: () {
          print('faiure');
          closeScreen('Connection Failure');
        },
        onConnected: (data) {
          print('connected, Status: ${data['status']}');
          isConnected = true;

          setState(() {});
        },
        onPermissionDenied: () {
          print('permission denied');
          closeScreen('Permission Denied');
        },
        onDisconnected: () {
          print('call disconnected');
          closeScreen('Call Disconnected');
        });
    _fetchButtonStates();
    _makeCall();
  }
  void _keyPress(String keyValue) async {
    twilioVoice.pressKey(keyValue: keyValue);
  }

  void _makeCall() async {
    twilioVoice
        .connectCall(
            call: Call(
      accessToken: 'accessToken',
      callerId: 'callerId' ?? '',
      locationId: 'locationId',
      name: 'widget.contact.fullName',
      to: ' widget.contact.phone',
    ))
        .catchError((e) {
      print(e);
    });
  }

  @override
  Widget build(BuildContext context) {}

  void closeScreen(String message) {
    isConnected = false;
    isCalling = isRinging = false;
    // code to act on closing of call/error/permission denied
  }

  void _toggleHold() async {
    var result = await twilioVoice.hold();
    print("hold $result");
    setState(() {
      onHold = result;
    });
  }

  void _toggleMute() async {
    var result = await twilioVoice.mute();
    print("mute $result");
    setState(() {
      onMute = result;
    });
  }

  void _toggleSpeaker(bool speaker) async {
    var result = await twilioVoice.speaker(speaker);
    print("speaker $result");
    setState(() {
      onSpeaker = result;
    });
  }

  void _disconnect() {
    twilioVoice.disconnectCall();
  }

  @override
  void dispose() {
    _disconnect();
    super.dispose();
  }

  void _fetchButtonStates() {
    _toggleMute();
    _toggleHold();
    _toggleSpeaker(false);
  }
}
