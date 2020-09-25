
import 'dart:async';

import 'package:flutter/services.dart';

class FlutterTwilioVoice {
  static const MethodChannel _channel =
      const MethodChannel('flutter_twilio_voice');

  static Future<String> get platformVersion async {
    _channel.invokeMethod('call');
    _channel.invokeMethod('hold');
    _channel.invokeMethod('ofndisgb');
   final String version = await _channel.invokeMethod('getPlatformVersion');
   return version;
  }
}
