import 'dart:ffi';

import 'package:flutter/services.dart';

class TuyaFlutter {
  static const MethodChannel _channel = MethodChannel('tuya_flutter');

  Future<String?> getPlatformVersion() async {
    return await _channel.invokeMethod<String>('getPlatformVersion');
  }

  static Future<String?> initTuya({String? appKey, String? appSecret}) async {
    final Map<String, dynamic> args = {
      if (appKey != null) 'appKey': appKey,
      if (appSecret != null) 'appSecret': appSecret,
    };
    return await _channel.invokeMethod<String>('initTuya', args);
  }

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
  static Future<dynamic> createHome({
    required String name,
    required double lon,
    required double lat,
    required String geoName,
    required List<String> rooms,
  }) async {
    final Map<String, dynamic> args = {
      'name': name,
      'lon': lon,
      'lat': lat,
      'geoName': geoName,
      'rooms': rooms,
    };
    return await _channel.invokeMethod('createHome', args);
  }
  static Future<String?> getActivatorToken({required int homeId}) async {
    final Map<String, int> args = {
      'homeId': homeId,
    };
    return await _channel.invokeMethod<String>('getActivatorToken', args);
  }

  static Future<String?> buildActivator({
    required String token,
    int timeout = 100,
    String? ssid,
    String? password,
    String? model,
  }) async {
    final Map<String, dynamic> args = {
      'token': token,
      'timeout': timeout,
      if (ssid != null) 'ssid': ssid,
      if (password != null) 'password': password,
      if (model != null) 'model': model,
    };
    return await _channel.invokeMethod<String>('buildActivator', args);
  }

  static Future<String?> startActivator() async {
    return await _channel.invokeMethod<String>('startActivator');
  }

  static Future<String?> stopActivator() async {
    return await _channel.invokeMethod<String>('stopActivator');
  }

}
