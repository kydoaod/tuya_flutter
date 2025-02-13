
import 'tuya_flutter_platform_interface.dart';
import 'package:flutter/services.dart';

class TuyaFlutter {
  static const MethodChannel _channel = MethodChannel('tuya_flutter');
  Future<String?> getPlatformVersion() {
    return TuyaFlutterPlatform.instance.getPlatformVersion();
  }
  static Future<String?> initTuya() async {
    return await _channel.invokeMethod<String>('initTuya');
  }
}
