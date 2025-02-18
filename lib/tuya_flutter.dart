import 'package:flutter/services.dart';

class TuyaFlutter {
  static const MethodChannel _channel = MethodChannel('tuya_flutter');

  /// Retrieves the platform version from native code.
  Future<String?> getPlatformVersion() async {
    return await _channel.invokeMethod<String>('getPlatformVersion');
  }

  /// Initializes the Tuya SDK.
  ///
  /// This method reads the Tuya App Key and Secret from the Flutter app’s
  /// AndroidManifest.xml (using the meta-data keys "TUYA_SMART_APPKEY"
  /// and "TUYA_SMART_SECRET") and initializes the SDK.
  static Future<String?> initTuya({String? appKey, String? appSecret}) async {
    final Map<String, dynamic> args = {
      if (appKey != null) 'appKey': appKey,
      if (appSecret != null) 'appSecret': appSecret,
    };
    return await _channel.invokeMethod<String>('initTuya', args);
  }

  /// Logs in using the Tuya SDK with email authentication.
  ///
  /// This method accepts the required parameters and performs the login.
  /// It returns a success message if the login is successful or an error message otherwise.
  static Future<String?> loginWithEmail({
    required String countryCode,
    required String email,
    required String passwd,
  }) async {
    final Map<String, dynamic> args = {
      'countryCode': countryCode,
      'email': email,
      'passwd': passwd,
    };
    return await _channel.invokeMethod<String>('loginWithEmail', args);
  }
}
