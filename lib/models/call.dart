import 'package:flutter/cupertino.dart';

class Call {
  String accessToken;
  String name;
  Map<String, String> dataToSend;

  Call({@required this.accessToken, @required this.name, this.dataToSend});
}
