import Flutter
import UIKit
import ThingSmartHomeKit
import ThingSmartBusinessExtensionKit
import ThingSmartCryption

public class TuyaFlutterPlugin: NSObject, FlutterPlugin {
    private var channel: FlutterMethodChannel
    private var context: UIApplication
    private var meshEventChannel: FlutterEventChannel
    private var meshEventSink: FlutterEventSink?
    private static var activator: ThingSmartActivator?
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "tuya_flutter", binaryMessenger: registrar.messenger())
        let instance = TuyaFlutterPlugin(channel: channel, context: UIApplication.shared, messenger: registrar.messenger())
        registrar.addMethodCallDelegate(instance, channel: channel)
        
        let meshEventChannel = FlutterEventChannel(name: "tuya_flutter/meshScanCallback", binaryMessenger: registrar.messenger())
        meshEventChannel.setStreamHandler(instance)
    }
    
    init(channel: FlutterMethodChannel, context: UIApplication, messenger: FlutterBinaryMessenger) {
        self.channel = channel
        self.context = context
        self.meshEventChannel = FlutterEventChannel(name: "tuya_flutter/meshScanCallback", binaryMessenger: messenger)
        super.init()
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "getPlatformVersion":
            result("iOS " + UIDevice.current.systemVersion)
        case "initTuya":
            guard let args = call.arguments as? [String: Any],
                  let appKey = args["appKey"] as? String,
                  let appSecret = args["appSecret"] as? String else {
                result(FlutterError(code: "NULL_VALUES", message: "App Key or Secret not found.", details: nil))
                return
            }
            ThingSmartSDK.sharedInstance().start(withAppKey: appKey, secretKey: appSecret)
            result("Tuya SDK Initialized")
        case "loginWithEmail":
            guard let args = call.arguments as? [String: Any],
                  let countryCode = args["countryCode"] as? String,
                  let email = args["email"] as? String,
                  let passwd = args["passwd"] as? String else {
                result(FlutterError(code: "MISSING_ARGS", message: "Missing parameters for login", details: nil))
                return
            }
            ThingSmartUser.sharedInstance().login(byEmail: countryCode, email: email, password: passwd, success: {
                self.channel.invokeMethod("loginCallback", arguments: ["status": "success", "message": "Login successful"])
            }, failure: { error in
                self.channel.invokeMethod("loginCallback", arguments: ["status": "error", "errorCode": error?.localizedDescription ?? "Unknown error"])
            })
            result("loginWithEmail initiated")
        case "createHome":
          result("Not implemented yet")
        case "queryHomeList":
          result("Not implemented yet")
        case "getHomeDetail":
          result("Not implemented yet")
        
        case "getDeviceList":
          guard let args = call.arguments as? [String: Any],
                let homeId = args["homeId"] as? Int64 else {
              result(FlutterError(code: "MISSING_ARGS", message: "Missing homeId", details: nil))
              return
          }
          if let home = ThingSmartHome(homeId: homeId) {
              if let deviceList = home.deviceList {
                  let devices = deviceList.map { device in
                      return [
                          "devId": device.devId,
                          "name": device.name ?? ""
                      ]
                  }
                  result(devices)
              } else {
                  result([])
              }
          } else {
              result(FlutterError(code: "NO_HOME", message: "Unable to initialize home", details: nil))
          }
        case "getActivatorToken":
            guard let args = call.arguments as? [String: Any], let homeId = args["homeId"] as? Int else {
                result(FlutterError(code: "MISSING_ARGS", message: "Missing homeId", details: nil))
                return
            }
            ThingSmartActivator().getTokenWithHomeId(Int64(homeId), success: { token in
                result(token)
            }, failure: { error in
                result(FlutterError(code: "TOKEN_ERROR", message: error?.localizedDescription ?? "Unknown error", details: nil))
            })
        case "buildActivator":
            guard let args = call.arguments as? [String: Any], let token = args["token"] as? String else {
                result(FlutterError(code: "MISSING_ARGS", message: "Missing token", details: nil))
                return
            }
            guard let ssid = args["ssid"] as? String,
                let password = args["password"] as? String else {
                result(FlutterError(code: "MISSING_ARGS", message: "Missing ssid or password", details: nil))
                return
            }
            let timeout = args["timeout"] as? Double ?? 100
            TuyaFlutterPlugin.activator = ThingSmartActivator.sharedInstance()
            TuyaFlutterPlugin.activator?.delegate = self
            TuyaFlutterPlugin.activator?.startConfigWiFi(.EZ, ssid: ssid, password: password, token: token, timeout: timeout)
            result("Activator built successfully")
        case "startActivator":
            // TuyaFlutterPlugin.activator?.start()
            result("Activator started")
        case "stopActivator":
            //TuyaFlutterPlugin.activator?.stop()
            result("Activator stopped")
        case "sendDpCommand":
            guard let args = call.arguments as? [String: Any], let devId = args["devId"] as? String, let dpId = args["dpId"] as? String, let dpValue = args["dpValue"] as? Int else {
                result(FlutterError(code: "MISSING_ARGS", message: "Missing devId or dpId", details: nil))
                return
            }
            let device = ThingSmartDevice(deviceId: devId)
            device?.publishDps([dpId: dpValue == 1], success: {
                result("Command sent successfully")
            }, failure: { error in
                result(FlutterError(code: "SEND_DP_ERROR", message: error?.localizedDescription ?? "Unknown error", details: nil))
            })
        case "resetFactory":
            guard let args = call.arguments as? [String: Any], let devId = args["devId"] as? String else {
                result(FlutterError(code: "MISSING_ARGS", message: "Missing devId", details: nil))
                return
            }
            let device = ThingSmartDevice(deviceId: devId)
            device?.resetFactory({
                print("reset success")
            }, failure: { (error) in
                if let e = error {
                    print("reset failure: \(e)")
                }
            })
        default:
            result(FlutterMethodNotImplemented)
        }
    }
}

extension TuyaFlutterPlugin: FlutterStreamHandler {
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        self.meshEventSink = events
        return nil
    }

    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        self.meshEventSink = nil
        return nil
    }
}

extension TuyaFlutterPlugin: ThingSmartActivatorDelegate {
    public func activator(_ activator: ThingSmartActivator, didReceiveDevice deviceModel: ThingSmartDeviceModel?, error: Error?) {
        if let error = error {
            self.channel.invokeMethod("activatorCallback", arguments: [
                "event": "error",
                "errorCode": error.localizedDescription,
                "errorMsg": error.localizedDescription
            ])
        } else if let deviceModel = deviceModel {
            self.channel.invokeMethod("activatorCallback", arguments: [
                "event": "deviceFound",
                "device": deviceModel.name ?? "Unknown Device",
                "devId": deviceModel.devId
            ])
        }
    }
}

