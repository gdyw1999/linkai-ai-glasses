//
//  QCSDKHelper.h
//  QCSDK
//
//  Created by steve on 2021/7/7.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface QCSDKHelper : NSObject

+ (instancetype)shareInstance;

#pragma mark - 其他
- (void)convertOpusToPcm:(NSString *)inputPath
              outputPath:(NSString *)outputPath
                progress:(void (^_Nullable)(float progress))progress
              completion:(void (^_Nullable)(BOOL success))completion;             
@end

NS_ASSUME_NONNULL_END
