#import "FlutterTwilioVoicePlugin.h"
#if __has_include(<flutter_twilio_voice/flutter_twilio_voice-Swift.h>)
#import <flutter_twilio_voice/flutter_twilio_voice-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_twilio_voice-Swift.h"
#endif

@implementation FlutterTwilioVoicePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterTwilioVoicePlugin registerWithRegistrar:registrar];
}
@end
