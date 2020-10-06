import 'package:meta/meta.dart';

class TwilioCallException implements Exception {
  String error;

  TwilioCallException({@required this.error});

  @override
  String toString() {
    return error;
  }
}
