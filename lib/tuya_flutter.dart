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

  static Future<dynamic> queryHomeList() async {
    return await _channel.invokeMethod('queryHomeList');
  }

  static Future<dynamic> getHomeDetail({required int homeId}) async {
    return await _channel.invokeMethod('getHomeDetail', {'homeId': homeId});
  }

  static Future<dynamic> getHomeLocalCache({required int homeId}) async {
    return await _channel.invokeMethod('getHomeLocalCache', {'homeId': homeId});
  }

  static Future<dynamic> getDeviceList({required int homeId}) async {
    return await _channel.invokeMethod('getDeviceList', {'homeId': homeId});
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

  static Future<dynamic> sendDpCommand({
    required String devId,
    required String dpId,
    required int dpValue,
  }) async {
    final Map<String, dynamic> args = {
      'devId': devId,
      'dpId': dpId,
      'dpValue': dpValue,
    };
    return await _channel.invokeMethod('sendDpCommand', args);
  }
  
  static Future<dynamic> resetFactory({
    required String devId
  }) async {
    final Map<String, dynamic> args = {
      'devId': devId
    };
    return await _channel.invokeMethod('resetFactory', args);
  }


  // Mesh methods
  // static Future<dynamic> createMesh({
  //   required int homeId,
  //   required String meshName,
  // }) async {
  //   final Map<String, dynamic> args = {
  //     'homeId': homeId,
  //     'meshName': meshName,
  //   };
  //   return await _channel.invokeMethod('createMesh', args);
  // }

  // static Future<String?> removeMesh({required String meshId}) async {
  //   final Map<String, dynamic> args = {
  //     'meshId': meshId,
  //   };
  //   return await _channel.invokeMethod<String>('removeMesh', args);
  // }

  // static Future<dynamic> getMeshList({required int homeId}) async {
  //   final Map<String, dynamic> args = {
  //     'homeId': homeId,
  //   };
  //   return await _channel.invokeMethod('getMeshList', args);
  // }

  // static Future<String?> initMesh({required String meshId}) async {
  //   final Map<String, dynamic> args = {
  //     'meshId': meshId,
  //   };
  //   return await _channel.invokeMethod<String>('initMesh', args);
  // }

  // static Future<String?> destroyMesh() async {
  //   return await _channel.invokeMethod<String>('destroyMesh');
  // }

  // static Future<String?> meshScanDevices({
  //   String? meshName,
  //   String? meshId,
  //   int timeout = 100,
  // }) async {
  //   final Map<String, dynamic> args = {
  //     if (meshName != null) 'meshName': meshName,
  //     'timeout': timeout,
  //   };
  //   return await _channel.invokeMethod<String>('meshScanDevices', args);
  // }

  // static Future<String?> stopMeshScan() async {
  //   return await _channel.invokeMethod<String>('stopMeshScan');
  // }

  // static Future<dynamic> meshPairDevices({
  //   required String ssid,
  //   required String password,
  //   required String homeId, // as String
  //   required String version,
  //   required String meshId,
  //   required List<Map<String, dynamic>> foundDevices,
  // }) async {
  //   final Map<String, dynamic> args = {
  //     'ssid': ssid,
  //     'password': password,
  //     'homeId': homeId,
  //     'version': version,
  //     'foundDevices': foundDevices,
  //     'meshId': meshId,
  //   };
  //   return await _channel.invokeMethod('meshPairDevices', args);
  // }

  static Future<dynamic> getMeshSubDevList({required String meshId}) async {
    final Map<String, dynamic> args = {
      'meshId': meshId,
    };
    return await _channel.invokeMethod('getMeshSubDevList', args);
  }
}
