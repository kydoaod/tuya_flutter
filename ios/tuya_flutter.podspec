Pod::Spec.new do |s|
  s.name             = 'tuya_flutter'
  s.version          = '0.0.1'
  s.summary          = 'Flutter plugin for Tuya Smart SDK'
  s.description      = 'Flutter plugin wrapper for Tuya Smart Home SDK.'
  s.homepage         = 'http://www.alphaonedesign.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Alpha One Design' => 'info@alphaonedesign.com' }
  s.source           = { :path => '.' }
  s.source_files     = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  
  s.platform = :ios, '15.5'
  s.swift_version = '5.0'

  # Flutter dependency
  s.dependency 'Flutter'

  # Tuya SDK Dependencies (Hihiramin mula sa main app)
  s.dependency 'ThingSmartCryption'
  s.dependency 'ThingSmartHomeKit'
  s.dependency 'ThingSmartBusinessExtensionKit'

  # Ensure it can find the Tuya SDK in the consuming app
  s.xcconfig = {
    'FRAMEWORK_SEARCH_PATHS' => '$(inherited) "$(PODS_ROOT)/ThingSmartCryption/**" "$(PODS_ROOT)/ThingSmartHomeKit/**" "$(PODS_ROOT)/ThingSmartBusinessExtensionKit/**"',
    'IPHONEOS_DEPLOYMENT_TARGET' => '15.5'
  }

  # Ensure compatibility with iOS Simulators
  s.pod_target_xcconfig = { 
    'DEFINES_MODULE' => 'YES', 
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386 arm64' 
  }
end
