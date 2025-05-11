# AOD Tuya Flutter Library Integration Guide

This guide provides comprehensive instructions for integrating the AOD Tuya Flutter Library into your Flutter project, covering both Android and iOS platforms.

---

## 📱 Android Integration

### 1. Root-Level `build.gradle` Configuration

Ensure the following repositories are included in your root `build.gradle` file:

```groovy
allprojects {
    repositories {
        jcenter()
        maven { url 'https://maven-other.tuya.com/repository/maven-releases/' }
        maven { url "https://maven-other.tuya.com/repository/maven-commercial-releases/" }
        maven { url 'https://jitpack.io' }
        google()
        mavenCentral()
        maven { url 'https://maven.aliyun.com/repository/public' }
        maven { url 'https://central.maven.org/maven2/' }
        maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
        maven { url 'https://developer.huawei.com/repo/' }
    }
}
```



Set the build directories to maintain a clean project structure:

```groovy
rootProject.buildDir = "../build"
subprojects {
    project.buildDir = "${rootProject.buildDir}/${project.name}"
}
subprojects {
    project.evaluationDependsOn(":app")
}
```



Define a clean task to remove the build directory:

```groovy
tasks.register("clean", Delete) {
    delete rootProject.buildDir
}
```



### 2. App-Level `build.gradle` Configuration

In your app-level `build.gradle`, apply the necessary plugins:

```groovy
plugins {
    id "com.android.application"
    id "kotlin-android"
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id "dev.flutter.flutter-gradle-plugin"
}
```



Configure the Android settings:

```groovy
android {
    namespace = "com.alphaonedesign.frostblok"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8
    }

    defaultConfig {
        applicationId = "com.alphaonedesign.frostblok"
        minSdk = 23
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a"
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            shrinkResources false
            signingConfig = signingConfigs.debug
        }
    }

    packagingOptions {
        pickFirst 'lib/*/libc++_shared.so'
    }

    signingConfigs {
        debug {
            storeFile file("C:\\Users\\Kydo\\Documents\\Repositories\\Scratch\\Frostblok\\keystore.jks")
            storePassword "`1Qwertyuiop"
            keyAlias "key0"
            keyPassword "`1Qwertyuiop"
        }
    }
}
```



Exclude specific modules to prevent conflicts:

```groovy
configurations.all {
    exclude group: "com.thingclips.smart", module: "thingsmart-geofence-huawei"
    exclude group: "com.thingclips.smart", module: "thingplugin-annotation"
    exclude group: "com.thingclips.android.module", module: "thingmodule-annotation"
    exclude group: "com.thingclips.smart", module: "thingsmart-modularCampAnno"
}
```



Add the necessary dependencies:

```groovy
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.aar'])
    implementation 'com.alibaba:fastjson:1.1.67.android'
    implementation 'com.squareup.okhttp3:okhttp-urlconnection:3.14.9'
    implementation 'com.facebook.soloader:soloader:0.10.4+'
    implementation 'com.thingclips.smart:thingsmart:6.0.0'
}
```



Specify the Flutter source directory:

```groovy
flutter {
    source = "../.."
}
```



### 3. Adding the Security Algorithm AAR

Place the `swecurity-algorithm-1.0.0-beta.aar` file into the `android/app/libs` directory. This file can be downloaded from the Tuya Developer Platform:([Tuya Developer][1])

[Tuya Developer Platform - Feature Overview](https://developer.tuya.com/en/docs/app-development/featureoverview?id=Ka69nt97vtsfu)

---

## 🍎 iOS Integration

### 1. Podfile Configuration

Ensure your `ios/Podfile` includes the following configuration:

```ruby
platform :ios, '15.5.0'

ENV['COCOAPODS_DISABLE_STATS'] = 'true'

source 'https://github.com/CocoaPods/Specs.git'
source 'https://github.com/tuya/tuya-pod-specs.git'

def flutter_root
  generated_xcode_build_settings_path = File.expand_path(File.join('..', 'Flutter', 'Generated.xcconfig'), __FILE__)
  unless File.exist?(generated_xcode_build_settings_path)
    raise "#{generated_xcode_build_settings_path} must exist. If you're running pod install manually, make sure flutter pub get is executed first"
  end

  File.foreach(generated_xcode_build_settings_path) do |line|
    matches = line.match(/FLUTTER_ROOT\=(.*)/)
    return matches[1].strip if matches
  end
  raise "FLUTTER_ROOT not found in #{generated_xcode_build_settings_path}. Try deleting Generated.xcconfig, then run flutter pub get"
end

require File.expand_path(File.join('packages', 'flutter_tools', 'bin', 'podhelper'), flutter_root)

flutter_ios_podfile_setup

target 'Runner' do
  use_frameworks! :linkage => :static
  use_modular_headers!

  flutter_install_all_ios_pods File.dirname(File.realpath(__FILE__))

  pod "ThingSmartActivatorKit"
  pod "ThingSmartCryption", :path => './ios_core_sdk/ThingSmartCryption.podspec'
  pod "ThingSmartHomeKit"
  pod "ThingSmartBusinessExtensionKit"
end

post_install do |installer|
  installer.pods_project.targets.each do |target|
    flutter_additional_ios_build_settings(target)
    target.build_configurations.each do |config|
      config.build_settings['GCC_PREPROCESSOR_DEFINITIONS'] ||= [
        '$(inherited)',
        'PERMISSION_NOTIFICATIONS=1',
      ]
    end
  end

  installer.generated_projects.each do |project|
    project.targets.each do |target|
      target.build_configurations.each do |config|
        config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '15.5.0'
      end
    end
  end

  # Awesome Notifications pod modification
  awesome_pod_file = File.expand_path(File.join('plugins', 'awesome_notifications', 'ios', 'Scripts', 'AwesomePodFile'), '.symlinks')
  require awesome_pod_file
  update_awesome_pod_build_settings(installer)
end

# Awesome Notifications pod modification
awesome_pod_file = File.expand_path(File.join('plugins', 'awesome_notifications', 'ios', 'Scripts', 'AwesomePodFile'), '.symlinks')
require awesome_pod_file
update_awesome_main_target_settings('Runner', File.dirname(File.realpath(__FILE__)), flutter_root)
```



### 2. Adding the iOS Core SDK

Download and extract the `ios_core_sdk.tar.gz` file from the Tuya Developer Platform:([Dart packages][2])

[Tuya Developer Platform - Feature Overview](https://developer.tuya.com/en/docs/app-development/featureoverview?id=Ka69nt97vtsfu)

After extraction, place the following files into the `ios/ios_core_sdk` directory:

* `Build` directory (contains the security SDK exclusive to your app)
* `ThingSmartCryption.podspec` file([Dart packages][2])

Ensure these files are correctly referenced in your Podfile as shown above.

---

## 🧪 Testing the Integration

After completing the above configurations:

1. Run `flutter pub get` to fetch the dependencies.
2. For Android:

   * Run `flutter build apk` to build the Android application.
3. For iOS:

   * Navigate to the `ios` directory and run `pod install` to install the CocoaPods dependencies.
   * Run `flutter build ios` to build the iOS application.([Tuya Developer][1], [Tuya Developer][3])

Ensure that your development environment is properly set up for both Android and iOS platforms.

---

For more detailed information and updates, refer to the official Tuya Developer documentation:

* [Tuya Developer Platform - Feature Overview](https://developer.tuya.com/en/docs/app-development/featureoverview?id=Ka69nt97vtsfu)
