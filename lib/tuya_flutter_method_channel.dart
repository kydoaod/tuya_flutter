import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'tuya_flutter_platform_interface.dart';

/// An implementation of [TuyaFlutterPlatform] that uses method channels.
class MethodChannelTuyaFlutter extends TuyaFlutterPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('tuya_flutter');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
