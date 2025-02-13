import 'package:flutter_test/flutter_test.dart';
import 'package:tuya_flutter/tuya_flutter.dart';
import 'package:tuya_flutter/tuya_flutter_platform_interface.dart';
import 'package:tuya_flutter/tuya_flutter_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockTuyaFlutterPlatform
    with MockPlatformInterfaceMixin
    implements TuyaFlutterPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final TuyaFlutterPlatform initialPlatform = TuyaFlutterPlatform.instance;

  test('$MethodChannelTuyaFlutter is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelTuyaFlutter>());
  });

  test('getPlatformVersion', () async {
    TuyaFlutter tuyaFlutterPlugin = TuyaFlutter();
    MockTuyaFlutterPlatform fakePlatform = MockTuyaFlutterPlatform();
    TuyaFlutterPlatform.instance = fakePlatform;

    expect(await tuyaFlutterPlugin.getPlatformVersion(), '42');
  });
}
