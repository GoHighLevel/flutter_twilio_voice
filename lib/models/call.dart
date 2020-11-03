import 'package:flutter/cupertino.dart';

class Call {
  String to;
  String accessToken;
  String name;
  String locationId;
  String callerId;
  String from;

  Call(
      {@required this.to,
      @required this.accessToken,
      @required this.name,
      @required this.locationId,
      @required this.callerId,
      this.from});
}
