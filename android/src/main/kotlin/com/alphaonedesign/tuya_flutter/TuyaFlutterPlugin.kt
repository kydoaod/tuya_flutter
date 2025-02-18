package com.alphaonedesign.tuya_flutter

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import com.thingclips.smart.android.user.api.ILoginCallback
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/**
 * TuyaFlutterPlugin
 *
 * This plugin wraps native functions of the Tuya SDK.
 * It reads the configuration (App Key and Secret) from the Flutter app’s AndroidManifest.xml
 * using meta-data keys ("TUYA_SMART_APPKEY" and "TUYA_SMART_SECRET").
 *
 * Wrapped functions include:
 *  - getPlatformVersion
 *  - initTuya
 *  - loginWithEmail
 */
class TuyaFlutterPlugin : FlutterPlugin, MethodCallHandler {

    private lateinit var channel: MethodChannel
    private lateinit var context: Context

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "tuya_flutter")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "initTuya" -> {
                val appKeyArg = call.argument<String>("appKey")
                val appSecretArg = call.argument<String>("appSecret")
                // Kung walang ipinasang values, fallback sa meta-data
                val appKey = appKeyArg ?: getMetaData("TUYA_SMART_APPKEY")
                val appSecret = appSecretArg ?: getMetaData("TUYA_SMART_SECRET")
                if (appKey.isNullOrEmpty() || appSecret.isNullOrEmpty()) {
                    result.error("NULL_VALUES", "App Key or Secret not found.", null)
                    return
                }
                ThingHomeSdk.init(context as Application, appKey, appSecret)
                ThingHomeSdk.setDebugMode(true)
                ThingOptimusSdk.init(context as Application)
                result.success("Tuya SDK Initialized")
            }
            "loginWithEmail" -> {
                val countryCode = call.argument<String>("countryCode")
                val email = call.argument<String>("email")
                val passwd = call.argument<String>("passwd")
                if (countryCode == null || email == null || passwd == null) {
                    result.error("MISSING_ARGS", "Missing parameters for login", null)
                    return
                }
                ThingHomeSdk.getUserInstance().loginWithEmail(
                    countryCode,
                    email,
                    passwd,
                    object : ILoginCallback {
                        override fun onSuccess(user: User) {
                            result.success("Login successful")
                        }
                        override fun onError(errorCode: String, errorMsg: String) {
                            result.error(errorCode, errorMsg, null)
                        }
                    }
                )
            }
            else -> result.notImplemented()
        }
    }

    private fun getMetaData(key: String): String? {
        return try {
            val ai = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            ai.metaData?.getString(key)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
