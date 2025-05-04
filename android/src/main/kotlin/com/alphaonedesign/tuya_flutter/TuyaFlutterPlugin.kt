package com.alphaonedesign.tuya_flutter

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager

import com.thingclips.smart.android.blemesh.api.IThingBlueMeshActivatorListener
import com.thingclips.smart.android.blemesh.api.IThingBlueMeshSearch
import com.thingclips.smart.android.blemesh.api.IThingBlueMeshSearchListener
import com.thingclips.smart.android.blemesh.bean.SearchDeviceBean
import com.thingclips.smart.android.blemesh.builder.SearchBuilder
import com.thingclips.smart.android.blemesh.builder.ThingBlueMeshActivatorBuilder
import com.thingclips.smart.android.user.api.ILoginCallback
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.builder.ActivatorBuilder
import com.thingclips.smart.home.sdk.callback.IThingGetHomeListCallback
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingActivator
import com.thingclips.smart.sdk.api.IThingActivatorGetToken
import com.thingclips.smart.sdk.api.IThingSmartActivatorListener
import com.thingclips.smart.sdk.bean.BlueMeshBean
import com.thingclips.smart.sdk.bean.DeviceBean
import com.thingclips.smart.sdk.enums.ActivatorModelEnum

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.EventChannel

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
    private lateinit var meshEventChannel: EventChannel
    private var meshEventSink: EventChannel.EventSink? = null

    // Variables for activator flow (if needed)
    companion object {
        var activatorBuilder: ActivatorBuilder? = null
        var activator: IThingActivator? = null
        var mMeshSearch: IThingBlueMeshSearch? = null
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "tuya_flutter")
        channel.setMethodCallHandler(this)
        
        meshEventChannel = EventChannel(binding.binaryMessenger, "tuya_flutter/meshScanCallback")
        meshEventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                meshEventSink = events
            }
            override fun onCancel(arguments: Any?) {
                meshEventSink = null
            }
        })
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
                ThingHomeSdk.setDebugMode(false)
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
            "queryHomeList" -> {
                ThingHomeSdk.getHomeManagerInstance().queryHomeList(object : IThingGetHomeListCallback {
                    override fun onSuccess(homeList: MutableList<HomeBean>?) {
                        if (homeList.isNullOrEmpty()) {
                            result.success(emptyList<Map<String, Any>>())
                        } else {
                            // Map each HomeBean to a serializable Map
                            val homes = homeList.map { home ->
                                mapOf(
                                    "homeId" to home.homeId,
                                    "name" to home.name,
                                    "geoName" to home.geoName,
                                    "lon" to home.lon,
                                    "lat" to home.lat,
                                    "rooms" to home.rooms  // Assuming this is a List<String>
                                )
                            }
                            result.success(homes)
                        }
                    }
                    override fun onError(errorCode: String?, errorMsg: String?) {
                        result.error(errorCode ?: "QUERY_HOME_ERROR", errorMsg, null)
                    }
                    })
            }
            "getHomeDetail" -> {
                val homeIdInt = call.argument<Int>("homeId")
                if (homeIdInt == null) {
                    result.error("MISSING_ARGS", "Missing homeId", null)
                    return
                }
                val homeId = homeIdInt.toLong()
                ThingHomeSdk.newHomeInstance(homeId).getHomeDetail(object : IThingHomeResultCallback {
                    override fun onSuccess(bean: HomeBean) {
                        // Convert HomeBean to a map. Adjust fields as needed.
                        val homeMap = mapOf(
                            "homeId" to bean.homeId,
                            "name" to bean.name,
                            "geoName" to bean.geoName,
                            "lon" to bean.lon,
                            "lat" to bean.lat,
                            "rooms" to bean.rooms,
                            // Convert deviceList and groupList if available:
                            "deviceList" to bean.deviceList?.map { device ->
                                mapOf("devId" to device.devId, "name" to device.name)
                            },
                            "groupList" to bean.groupList?.map { group ->
                                mapOf("groupId" to group.id, "name" to group.name)
                            }
                        )
                        result.success(homeMap)
                    }
                    override fun onError(errorCode: String?, errorMsg: String?) {
                        result.error(errorCode ?: "HOME_DETAIL_ERROR", errorMsg, null)
                    }
                })
            }
            "getHomeLocalCache" -> {
                val homeIdInt = call.argument<Int>("homeId")
                if (homeIdInt == null) {
                    result.error("MISSING_ARGS", "Missing homeId", null)
                    return
                }
                val homeId = homeIdInt.toLong()
                ThingHomeSdk.newHomeInstance(homeId).getHomeLocalCache(object : IThingHomeResultCallback {
                    override fun onSuccess(bean: HomeBean) {
                        // Convert HomeBean to a map, similar to getHomeDetail
                        val homeMap = mapOf(
                            "homeId" to bean.homeId,
                            "name" to bean.name,
                            "geoName" to bean.geoName,
                            "lon" to bean.lon,
                            "lat" to bean.lat,
                            "rooms" to bean.rooms,
                            "deviceList" to bean.deviceList?.map { device ->
                                mapOf("devId" to device.devId, "name" to device.name)
                            },
                            "groupList" to bean.groupList?.map { group ->
                                mapOf("groupId" to group.id, "name" to group.name)
                            }
                        )
                        result.success(homeMap)
                    }
                    override fun onError(errorCode: String?, errorMsg: String?) {
                        result.error(errorCode ?: "HOME_LOCAL_CACHE_ERROR", errorMsg, null)
                    }
                })
            }
            "getDeviceList" -> {
                // This method queries home details and extracts the device list.
                val homeIdInt = call.argument<Int>("homeId")
                if (homeIdInt == null) {
                    result.error("MISSING_ARGS", "Missing homeId", null)
                    return
                }
                val homeId = homeIdInt.toLong()
                ThingHomeSdk.newHomeInstance(homeId).getHomeDetail(object : IThingHomeResultCallback {
                    override fun onSuccess(bean: HomeBean) {
                        val devices = bean.deviceList?.map { device ->
                            mapOf("devId" to device.devId, "name" to device.name)
                        }
                        result.success(devices)
                    }
                    override fun onError(errorCode: String?, errorMsg: String?) {
                        result.error(errorCode ?: "DEVICE_LIST_ERROR", errorMsg, null)
                    }
                })
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
                val model = call.argument<String>("model") ?: "THING_EZ"
                if (token == null) {
                    result.error("MISSING_ARGS", "Missing token for buildActivator", null)
                    return
                }
                val builder = ActivatorBuilder()
                    .setToken(token)
                    .setTimeOut(timeout.toLong())
                    .setContext(context as Application)
                if (!ssid.isNullOrEmpty() && !password.isNullOrEmpty()) {
                    builder.setSsid(ssid)
                    builder.setPassword(password)
                }
                when (model) {
                    "THING_EZ" -> builder.setActivatorModel(ActivatorModelEnum.THING_EZ)
                    "THING_AP" -> builder.setActivatorModel(ActivatorModelEnum.THING_AP)
                    else -> builder.setActivatorModel(ActivatorModelEnum.THING_EZ)
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
                        // Forward discovered device event.
                        channel.invokeMethod("activatorCallback", mapOf<String, Any?>(
                            "event" to "deviceFound",
                            "device" to devResp.getName(),
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
            "sendDpCommand" -> {
                val devId = call.argument<String>("devId")
                val dpId = call.argument<String>("dpId")
                val dpValue = call.argument<Int>("dpValue") ?: 0
                if (devId == null || dpId == null) {
                    result.error("MISSING_ARGS", "Missing devId or dpId for sendDpCommand", null)
                    return
                }
                val iThingDevice = ThingHomeSdk.newDeviceInstance(devId)
                // Gamitin ang publishDps para ipadala ang command gamit ang dpId na input
                iThingDevice.publishDps("{\"$dpId\": ${dpValue == 1}}", object : IResultCallback {
                    override fun onError(code: String?, error: String?) {
                        result.error(code ?: "SEND_DP_ERROR", error, null)
                    }
                    override fun onSuccess() {
                        result.success("Command sent successfully")
                    }
                })
            }
            "resetFactory" -> {
                val devId = call.argument<String>("devId")
                if (devId == null) {
                    result.error("MISSING_ARGS", "Missing devId for resetFactory", null)
                    return
                }
                val iThingDevice = ThingHomeSdk.newDeviceInstance(devId);
                iThingDevice.resetFactory(object: IResultCallback {
                    override fun onError(errorCode: String, errorMsg: String) {
                    }
                    override fun  onSuccess() {
                        result.success("Device factory reset successfully executed");
                    }
                });
            }

            /*"createMesh" -> {
                val homeIdInt = call.argument<Int>("homeId")
                if (homeIdInt == null) {
                    result.error("MISSING_ARGS", "Missing homeId for createMesh", null)
                    return
                }
                val homeId = homeIdInt.toLong()
                val meshName = call.argument<String>("meshName")
                if (meshName.isNullOrEmpty()) {
                    result.error("MISSING_ARGS", "Missing meshName for createMesh", null)
                    return
                }
                // Call the Mesh creation API.
                ThingHomeSdk.newHomeInstance(homeId).createBlueMesh(meshName, object: IThingResultCallback<BlueMeshBean> {
                    override fun onError(errorCode: String, errorMsg: String) {
                        result.error(errorCode, errorMsg, null)
                    }
                    override fun onSuccess(blueMeshBean: BlueMeshBean) {
                        // Return the meshId (or full mesh details) to Flutter.
                        // For example, if BlueMeshBean has a property meshId:
                        result.success(blueMeshBean.meshId)
                        // Alternatively, you can return a map with more details:
                        // result.success(mapOf("meshId" to blueMeshBean.meshId, "otherProp" to blueMeshBean.otherProp))
                    }
                })
            }
            "removeMesh" -> {
                // Get the meshId from the arguments (as an Int, then convert to Long)
                val meshId = call.argument<String>("meshId")
                if (meshId == null) {
                    result.error("MISSING_ARGS", "Missing meshId for removeMesh", null)
                    return
                }
                // Call the Tuya SDK removal API.
                ThingHomeSdk.newBlueMeshDeviceInstance(meshId).removeMesh(object : IResultCallback {
                    override fun onError(code: String?, error: String?) {
                        result.error(code ?: "REMOVE_MESH_ERROR", error, null)
                    }
                    override fun onSuccess() {
                        result.success("Mesh removed successfully")
                    }
                })
            }
            "getMeshList" -> {
                val homeIdInt = call.argument<Int>("homeId")
                if (homeIdInt == null) {
                    result.error("MISSING_ARGS", "Missing homeId for getMeshList", null)
                    return
                }
                val homeId = homeIdInt.toLong()
                ThingHomeSdk.newHomeInstance(homeId).getHomeDetail(object : IThingHomeResultCallback {
                    override fun onSuccess(bean: HomeBean) {
                        // Get the mesh list from the HomeBean
                        val meshList = bean.meshList
                        if (meshList.isNullOrEmpty()) {
                            result.success(emptyList<Map<String, Any>>())
                        } else {
                            val meshes = meshList.map { mesh ->
                                mapOf<String, Any>(
                                    "resptime" to mesh.resptime,
                                    "localKey" to mesh.localKey,
                                    "meshId" to mesh.meshId,
                                    "name" to mesh.name,
                                    "pv" to mesh.pv,
                                    "code" to mesh.code,
                                    "password" to mesh.password,
                                    "startTime" to mesh.startTime,
                                    "endTime" to mesh.endTime
                                )
                            }
                            result.success(meshes)
                        }
                    }
                    override fun onError(errorCode: String?, errorMsg: String?) {
                        result.error(errorCode ?: "HOME_DETAIL_ERROR", errorMsg, null)
                    }
                })
            }
            "initMesh" -> {
                // Get the meshId from the call arguments.
                val meshId = call.argument<String>("meshId")
                if (meshId.isNullOrEmpty()) {
                    result.error("MISSING_ARGS", "Missing meshId for initMesh", null)
                    return
                }
                // Call the API to initialize the mesh network.
                ThingHomeSdk.getThingBlueMeshClient().initMesh(meshId)
                result.success("Mesh initialized successfully")
            }
            "destroyMesh" -> {
                // Call the API to destroy the current mesh network.
                ThingHomeSdk.getThingBlueMeshClient().destroyMesh()
                result.success("Mesh destroyed successfully")
            }
            "meshScanDevices" -> {
                val timeout = call.argument<Int>("timeout") ?: 100
                val meshName = call.argument<String>("meshName") ?: "out_of_mesh"
                val meshId = call.argument<String>("meshId") ?: ""
                val searchBuilder = SearchBuilder()
                    .setMeshName(meshName)
                    .setTimeOut(timeout)
                    .setThingBlueMeshSearchListener(object: IThingBlueMeshSearchListener {
                        override fun onSearched(deviceBean: SearchDeviceBean) {
                            // Forward each discovered device to Flutter.
                            meshEventSink?.success(mapOf(
                                  "event" to "deviceFound",
                                  "meshAddress" to deviceBean.meshAddress,
                                  "name" to deviceBean.meshName
                            ))
                        }
                        override fun onSearchFinish() {
                            meshEventSink?.success(mapOf("event" to "finished"))
                        }
                    })
                    .build()
                mMeshSearch = ThingHomeSdk.getThingBlueMeshConfig().newThingBlueMeshSearch(searchBuilder)
                //ThingHomeSdk.getThingBlueMeshClient().startClient(mBlueMeshBean);
                mMeshSearch?.startSearch()
                result.success("Mesh scan initiated")
            }
            "stopMeshScan" -> {
                mMeshSearch?.stopSearch()
                result.success("Mesh scan stopped")
            }
            "meshPairDevices" -> {
                val ssid = call.argument<String>("ssid")
                val password = call.argument<String>("password")
                val homeIdInt = call.argument<Int>("homeId")
                val version = call.argument<String>("version") ?: "2.2"
                val meshId = call.argument<String>("meshId")  // Must be provided from Flutter.
                val foundDevices = call.argument<List<Map<String, Any>>>("foundDevices")
                
                if (ssid.isNullOrEmpty() || password.isNullOrEmpty() || homeIdInt == null || meshId.isNullOrEmpty() || foundDevices == null) {
                    result.error("MISSING_ARGS", "Missing required parameters for mesh pairing", null)
                    return
                }
                
                // Convert foundDevices (List<Map<String, Any>>) to List<SearchDeviceBean>.
                val searchDeviceBeans = foundDevices.mapNotNull { map ->
                    try {
                        // Create a new SearchDeviceBean instance using its default constructor.
                        val deviceBean = SearchDeviceBean()
                        // Set fields based on provided keys. Adjust defaults as needed.
                        deviceBean.meshName = map["meshName"] as? String ?: ""
                        deviceBean.meshAddress = (map["meshAddress"] as? Number)?.toInt() ?: 0
                        deviceBean.status = (map["status"] as? Number)?.toInt() ?: 0
                        deviceBean.macAdress = map["macAdress"] as? String ?: ""
                        deviceBean.rssi = (map["rssi"] as? Number)?.toInt() ?: 0
                        deviceBean.version = map["version"] as? String ?: ""
                        // Optionally set other fields if available:
                        // deviceBean.productId = (map["productId"] as? String)?.toByteArray()
                        // deviceBean.vendorId = (map["vendorId"] as? Number)?.toInt() ?: 0
                        // deviceBean.deviceRand = map["deviceRand"] as? String ?: ""
                        // deviceBean.auth = map["auth"] as? String ?: ""
                        // deviceBean.beaconMeshCategory = (map["beaconMeshCategory"] as? Number)?.toInt()?.toByte() ?: 0
                        deviceBean
                    } catch (e: Exception) {
                        null
                    }
                }
                
                // Build the mesh pairing activator using the provided meshId.
                val thingBlueMeshActivatorBuilder = ThingBlueMeshActivatorBuilder()
                    .setWifiSsid(ssid)
                    .setWifiPassword(password)
                    .setSearchDeviceBeans(searchDeviceBeans)
                    .setVersion(version)
                    .setBlueMeshBean(meshId)  // Use the meshId provided by the consuming app.
                    .setHomeId(homeIdInt.toLong())
                    .setThingBlueMeshActivatorListener(object : IThingBlueMeshActivatorListener {
                        override fun onSuccess(mac: String?, devBean: DeviceBean) {
                            channel.invokeMethod("meshPairCallback", mapOf(
                                "event" to "pairSuccess",
                                "devId" to devBean.devId,
                                "device" to devBean.name
                            ))
                        }
                        override fun onError(mac: String?, errorCode: String, errorMsg: String) {
                            channel.invokeMethod("meshPairCallback", mapOf(
                                "event" to "pairError",
                                "errorCode" to errorCode,
                                "errorMsg" to errorMsg
                            ))
                        }
                        override fun onFinish() {
                            channel.invokeMethod("meshPairCallback", mapOf("event" to "pairFinished"))
                        }
                    })
                
                val meshActivator = ThingHomeSdk.getThingBlueMeshConfig().newWifiActivator(thingBlueMeshActivatorBuilder)
                meshActivator.startActivator()
                result.success("Mesh pairing initiated")
            }*/
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
