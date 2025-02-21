package com.alphaonedesign.tuya_flutter

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import com.thingclips.smart.android.user.api.ILoginCallback
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.builder.ActivatorBuilder
import com.thingclips.smart.home.sdk.callback.IThingGetHomeListCallback
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingActivator
import com.thingclips.smart.sdk.api.IThingActivatorGetToken
import com.thingclips.smart.sdk.api.IThingSmartActivatorListener
import com.thingclips.smart.sdk.bean.DeviceBean
import com.thingclips.smart.sdk.enums.ActivatorModelEnum

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

    // Variables for activator flow (if needed)
    companion object {
        var activatorBuilder: ActivatorBuilder? = null
        var activator: IThingActivator? = null
    }

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
                val appKey = appKeyArg ?: getMetaData("TUYA_SMART_APPKEY")
                val appSecret = appSecretArg ?: getMetaData("TUYA_SMART_SECRET")
                if (appKey.isNullOrEmpty() || appSecret.isNullOrEmpty()) {
                    result.error("NULL_VALUES", "App Key or Secret not found.", null)
                    return
                }
                ThingHomeSdk.init(context as Application, appKey, appSecret)
                ThingHomeSdk.setDebugMode(true)
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
                            // Forward success event to Flutter.
                            channel.invokeMethod("loginCallback", mapOf("status" to "success", "message" to "Login successful"))
                        }
                        override fun onError(errorCode: String, errorMsg: String) {
                            // Forward error event to Flutter.
                            channel.invokeMethod("loginCallback", mapOf("status" to "error", "errorCode" to errorCode, "errorMsg" to errorMsg))
                        }
                    }
                )
                result.success("loginWithEmail initiated")
            }
            "createHome" -> {
                val name = call.argument<String>("name")
                val lon = call.argument<Double>("lon")
                val lat = call.argument<Double>("lat")
                val geoName = call.argument<String>("geoName")
                val rooms = call.argument<List<String>>("rooms")
                if (name == null || lon == null || lat == null || geoName == null || rooms == null) {
                    result.error("MISSING_ARGS", "Missing parameters for createHome", null)
                    return
                }
                ThingHomeSdk.getHomeManagerInstance().createHome(
                    name, lon, lat, geoName, rooms,
                    object : IThingHomeResultCallback {
                        override fun onSuccess(bean: HomeBean) {
                            result.success(bean.homeId)
                        }
                        override fun onError(errorCode: String?, errorMsg: String?) {
                            result.error(errorCode ?: "CREATE_HOME_ERROR", errorMsg, null)
                        }
                    }
                )
            }
            "getActivatorToken" -> {
                val homeId = call.argument<Int>("homeId")
                if (homeId == null) {
                    result.error("MISSING_ARGS", "Missing homeId for getActivatorToken", null)
                    return
                }
                ThingHomeSdk.getActivatorInstance().getActivatorToken(
                    homeId.toLong(),
                    object : IThingActivatorGetToken {
                        override fun onSuccess(token: String) {
                            result.success(token) // Directly return token.
                        }
                        override fun onFailure(errorCode: String, errorMsg: String) {
                            result.error(errorCode, errorMsg, null)
                        }
                    }
                )
                // Do not call result.success() here.
            }
            "buildActivator" -> {
                val ssid = call.argument<String>("ssid")
                val password = call.argument<String>("password")
                val token = call.argument<String>("token")
                val timeout = call.argument<Int>("timeout") ?: 100
                // Use the provided model or default to "THING_EZ"
                val model = call.argument<String>("model") ?: "THING_EZ"
                if (token == null) {
                    result.error("MISSING_ARGS", "Missing token for buildActivator", null)
                    return
                }
                val builder = ActivatorBuilder()
                    .setToken(token)
                    .setTimeOut(timeout.toLong())
                    .setContext(context as Application)  // Ensure context is valid

                // Optionally set SSID and password if provided.
                if (!ssid.isNullOrEmpty() && !password.isNullOrEmpty()) {
                    builder.setSsid(ssid)
                    builder.setPassword(password)
                }
                
                // Always set the ActivatorModel – default to THING_EZ if model is missing or unrecognized.
                when (model) {
                    "THING_EZ" -> builder.setActivatorModel(ActivatorModelEnum.THING_EZ)
                    else -> builder.setActivatorModel(ActivatorModelEnum.THING_EZ) // Fallback default
                }
                
                builder.setListener(object : IThingSmartActivatorListener {
                    override fun onError(errorCode: String?, errorMsg: String?) {
                        channel.invokeMethod("activatorCallback", mapOf<String, Any?>(
                            "event" to "error",
                            "errorCode" to errorCode,
                            "errorMsg" to errorMsg
                        ))
                    }
                    override fun onActiveSuccess(devResp: DeviceBean) {
                        channel.invokeMethod("activatorCallback", mapOf<String, Any?>(
                            "event" to "deviceFound",
                            "device" to devResp.dpName,
                            "devId" to devResp.devId
                        ))
                    }
                    override fun onStep(step: String?, data: Any?) {
                        channel.invokeMethod("activatorCallback", mapOf<String, Any?>(
                            "event" to "progress",
                            "step" to step,
                            "data" to (data?.toString() ?: "")
                        ))
                    }
                })
                
                activatorBuilder = builder
                result.success("Activator built successfully")
            }

            "startActivator" -> {
                if (activatorBuilder == null) {
                    result.error("NO_BUILDER", "Activator builder not set", null)
                    return
                }
                activator = ThingHomeSdk.getActivatorInstance().newMultiActivator(activatorBuilder)
                activator?.start()
                result.success("Activator started")
            }
            "stopActivator" -> {
                activator?.stop()
                result.success("Activator stopped")
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
