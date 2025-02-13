import Flutter
import UIKit

public class TuyaFlutterPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "tuya_flutter", binaryMessenger: registrar.messenger())
    let instance = TuyaFlutterPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "getPlatformVersion":
      result("iOS " + UIDevice.current.systemVersion)
    case "initTuya":
      ThingSmartSDK.sharedInstance().start(withAppKey: "YOUR_TUYA_APP_KEY", secretKey: "YOUR_TUYA_SECRET_KEY")
      result("Tuya SDK Initialized on iOS")
    default:
      result(FlutterMethodNotImplemented)
    }
  }
}
