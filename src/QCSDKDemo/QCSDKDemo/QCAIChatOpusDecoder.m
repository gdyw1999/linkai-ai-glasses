//
//  QCAIChatOpusDecoder.m
//  QCSDK
//
//  Created by steve on 2025/8/16.
//

#import "QCAIChatOpusDecoder.h"
#import <JLAudioUnitKit/JLAudioUnitKit.h>
#import <JLAudioUnitKit/JLOpusDecoder.h>

#import <QCSDK/QCSDKManager.h>

@interface QCAIChatOpusDecoder() <JLOpusDecoderDelegate>

@property(nonatomic,strong) dispatch_queue_t serialQueue;
@property(nonatomic,strong)JLOpusFormat *format;
@property(nonatomic,strong)JLOpusDecoder *opusDecoder;
@end

@implementation QCAIChatOpusDecoder

+ (instancetype)share {
    
    static QCAIChatOpusDecoder* instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[QCAIChatOpusDecoder alloc] init];
    });
    return instance;
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        self.serialQueue = dispatch_queue_create("com.sdk.opus.realtime.serialQueue", DISPATCH_QUEUE_SERIAL);
    }
    return self;
}

- (void)start {
    
    dispatch_async(dispatch_get_main_queue(), ^{
        
        self.format = [JLOpusFormat defaultFormats];
        self.format.hasDataHeader = NO;
        self.opusDecoder = [[JLOpusDecoder alloc] initDecoder:self.format delegate:self];
    });
}

- (void)appendOpusData:(NSData*)data {
    dispatch_async(self.serialQueue, ^{
        [self.opusDecoder opusDecoderInputData:data];
    });
}

- (void)stop {
//    dispatch_async(dispatch_get_main_queue(), ^{
//        [self.opusDecoder opusOnRelease];
//    });
    
    //NSLog(@"----------------stopToOpusDecoder");
    //Step3.当不需要解码时，可以停⽌解码并释放资源
//    dispatch_async(dispatch_get_global_queue(0, 0), ^{
////        [OpusUnit opusDecoderStop];
//
//        [self.opusDecoder opusOnRelease];
//
//    });
}


#pragma mark - JLOpusDecoderDelegate
- (void)opusDecoder:(nonnull JLOpusDecoder *)decoder Data:(NSData * _Nullable)data error:(NSError * _Nullable)error {
    
    if (data) {
        if ([QCSDKManager shareInstance].delegate && [[QCSDKManager shareInstance].delegate respondsToSelector:@selector(didReceiveAIChatVoiceData:)]) {
            [[QCSDKManager shareInstance].delegate didReceiveAIChatVoiceData:data];
        }
    }
}

@end
