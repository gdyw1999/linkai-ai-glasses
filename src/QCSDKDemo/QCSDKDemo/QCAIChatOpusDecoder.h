//
//  QCAIChatOpusDecoder.h
//  QCSDK
//
//  Created by steve on 2025/8/16.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface QCAIChatOpusDecoder : NSObject

+ (instancetype)share;

- (void)start;
- (void)appendOpusData:(NSData*)data;
- (void)stop;
@end

NS_ASSUME_NONNULL_END
