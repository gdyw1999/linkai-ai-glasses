//
//  QCFeatureParser.h
//  QCSDK
//
//  Created by steve on 2026/1/5.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface QCFeatureParser : NSObject

#pragma mark - Basic Device Info

/// Device type identifier returned by the glasses
/// Used to distinguish different hardware models
@property (nonatomic, assign) NSInteger deviceType;

/// Indicates whether real-time translation feature is supported
/// YES = supported, NO = not supported
@property (nonatomic, assign) BOOL translate;

/// Indicates whether wear detection is supported
/// YES = supports wear detection
/// NO  = wear detection not supported
@property (nonatomic, assign) BOOL deviceWear;

#pragma mark - Feature Flags (Byte 4)

/// Indicates whether volume control is supported
/// YES = volume control supported
@property (nonatomic, assign) BOOL volume;

/// Indicates whether the device is an earbuds-type device
/// YES = earbuds device
@property (nonatomic, assign) BOOL earBuds;

/// Indicates whether AI-related features are enabled
/// YES = AI features enabled
/// NO  = AI features disabled
@property (nonatomic, assign) BOOL aiEnable;

/// Indicates whether the device supports camera on/off switching
/// YES = camera switch supported
@property (nonatomic, assign) BOOL cameraSwitchSupported;

/// Indicates whether gyroscope-based video stabilization is supported
/// YES = gyroscope stabilization supported
@property (nonatomic, assign) BOOL gyroVideo;

/// Indicates whether rotating the camera triggers video recording
/// YES = rotate-to-record supported
@property (nonatomic, assign) BOOL cameraRotateRecording;

/// Indicates whether the device uses optical waveguide display technology
/// YES = optical waveguide device
@property (nonatomic, assign) BOOL opticalWaveguide;

/// Indicates whether vertical (portrait) video recording is supported
/// YES = vertical recording supported
@property (nonatomic, assign) BOOL verticalScreenRecording;

#pragma mark - Feature Flags (Byte 5)

/// Indicates whether offline voice command is supported
/// YES = offline voice command supported
@property (nonatomic, assign) BOOL offlineVoiceCmd;

/// Indicates whether video time watermark is supported
/// YES = video time watermark supported
@property (nonatomic, assign) BOOL timeWatermarkSupported;

/// Indicates whether the device is in aging / burn-in test mode
/// YES = aging mode enabled
@property (nonatomic, assign) BOOL agingMode;

/// Indicates whether the device supports reporting run mode
/// Includes both reading current run mode and主动上报 run mode
@property (nonatomic, assign) BOOL reportRunMode;

/// Indicates whether delayed run mode execution is supported
/// YES = delayed run mode supported
@property (nonatomic, assign) BOOL delayedRunMode;

/// Indicates whether video cropping is supported
/// YES = video cropping supported
@property (nonatomic, assign) BOOL videoCropping;

/// Indicates whether interpolation-based video stabilization is supported
/// YES = interpolation stabilization supported
@property (nonatomic, assign) BOOL interpolationStabilization;

/// Indicates whether image enhancement feature is supported
/// YES = image enhancement supported
@property (nonatomic, assign) BOOL imageEnhancement;

#pragma mark - Parser

/// Parse feature capability information from Bluetooth response data
/// @param retData Raw data returned by Bluetooth glasses
/// @return Parsed QGFeatureParser instance, or nil if data is invalid
+ (instancetype)parserWithData:(NSData *)retData;

@end

NS_ASSUME_NONNULL_END
