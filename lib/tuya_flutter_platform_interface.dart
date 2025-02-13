import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'tuya_flutter_method_channel.dart';

abstract class TuyaFlutterPlatform extends PlatformInterface {
  /// Constructs a TuyaFlutterPlatform.
  TuyaFlutterPlatform() : super(token: _token);

  static final Object _token = Object();

  static TuyaFlutterPlatform _instance = MethodChannelTuyaFlutter();

  /// The default instance of [TuyaFlutterPlatform] to use.
  ///
  /// Defaults to [MethodChannelTuyaFlutter].
  static TuyaFlutterPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [TuyaFlutterPlatform] when
  /// they register themselves.
  static set instance(TuyaFlutterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
