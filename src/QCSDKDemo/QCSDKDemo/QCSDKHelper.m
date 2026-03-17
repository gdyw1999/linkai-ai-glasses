//
//  QCSDKHelper.m
//  QCBandSDK
//
//  Created by steve on 2021/8/2.
//

#import "QCSDKHelper.h"
#import <JLAudioUnitKit/JLAudioUnitKit.h>
#import <JLAudioUnitKit/JLOpusDecoder.h>

@interface QCSDKHelper() <JLOpusDecoderDelegate>

@property (nonatomic, strong) JLOpusDecoder *opusDecoder;
@property (nonatomic, strong) JLOpusFormat *format;
@property (nonatomic, strong) dispatch_queue_t serialQueue;
@property (nonatomic, assign) BOOL isProcessing;
@property (nonatomic, strong) NSMutableArray<void (^)(void)> *taskQueue;
@property (nonatomic, copy, nullable) NSString *currentOutputPath;
@end

@implementation QCSDKHelper

static QCSDKHelper *_instance = nil;

+ (instancetype)shareInstance {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _instance = [[QCSDKHelper alloc] init];
    });
    return _instance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _format = [JLOpusFormat defaultFormats];
        _serialQueue = dispatch_queue_create("com.sdk.opus.file.serialQueue", DISPATCH_QUEUE_SERIAL);
        _taskQueue = [NSMutableArray array];
        _isProcessing = NO;
    }
    return self;
}

#pragma mark - Public
- (void)convertOpusToPcm:(NSString *)inputPath
             outputPath:(NSString *)outputPath
               progress:(void (^)(float))progress
             completion:(void (^)(BOOL))completion {
    
    void (^task)(void) = ^{
        [self processFile:inputPath outputPath:outputPath progress:progress completion:completion];
    };
    
    dispatch_async(self.serialQueue, ^{
        [self.taskQueue addObject:[task copy]];
        [self processNextTaskIfNeeded];
    });
}

#pragma mark - Task Management
- (void)processNextTaskIfNeeded {
    if (!self.isProcessing && self.taskQueue.count > 0) {
        self.isProcessing = YES;
        void (^nextTask)(void) = [self.taskQueue firstObject];
        [self.taskQueue removeObjectAtIndex:0];
        nextTask();
    }
}

- (void)processFile:(NSString *)inputPath
         outputPath:(NSString *)outputPath
           progress:(void (^)(float))progress
         completion:(void (^)(BOOL))completion {
    
    dispatch_async(self.serialQueue, ^{
        @try {
            self.currentOutputPath = outputPath;
            
            // 删除旧文件
            if ([[NSFileManager defaultManager] fileExistsAtPath:outputPath]) {
                [[NSFileManager defaultManager] removeItemAtPath:outputPath error:nil];
            }
            
            self.format.hasDataHeader = NO;
            self.opusDecoder = [[JLOpusDecoder alloc] initDecoder:self.format delegate:self];
            
            NSData *opusData = [NSData dataWithContentsOfFile:inputPath];
            if (!opusData) {
                NSLog(@"read opus file fail");
                if (completion) completion(NO);
                return;
            }
            NSLog(@"opus date size: %lu bytes", (unsigned long)opusData.length);
            
            NSUInteger packetSize = 40 * 30;
            NSUInteger totalPackets = (opusData.length + packetSize - 1) / packetSize;
            
            for (NSUInteger i = 0; i < totalPackets; i++) {
                NSUInteger start = i * packetSize;
                NSUInteger length = MIN(packetSize, opusData.length - start);
                NSData *packet = [opusData subdataWithRange:NSMakeRange(start, length)];
                
                NSLog(@"=====> pocket: %lu, pocket size: %lu bytes", (unsigned long)i, (unsigned long)length);
                [self.opusDecoder opusDecoderInputData:packet];
                
                if (progress) {
                    progress((float)(i + 1) / (float)totalPackets);
                }
            }
            
            NSLog(@"convert finshed");
            usleep(1 * 1000000); // 2秒
            NSData *pcmData = [NSData dataWithContentsOfFile:outputPath];
            if (!pcmData) {
                NSLog(@"read PCM file fail");
                if (completion) completion(NO);
                return;
            }
            
            if (completion) completion(YES);
        }
        @catch (NSException *exception) {
            NSLog(@"file err: %@", exception);
            if (completion) completion(NO);
        }
        @finally {
            self.isProcessing = NO;
            [self processNextTaskIfNeeded];
        }
    });
}

#pragma mark - JLOpusDecoderDelegate
- (void)opusDecoder:(nonnull JLOpusDecoder *)decoder Data:(NSData * _Nullable)data error:(NSError * _Nullable)error {
    if (data && self.currentOutputPath) {
        [self appendDataChunk:data outputPath:self.currentOutputPath];
    }
}

#pragma mark - File Append
- (void)appendDataChunk:(NSData *)chunk outputPath:(NSString *)outputPath {
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSURL *outputURL = [NSURL fileURLWithPath:outputPath];
    NSString *dir = [outputURL URLByDeletingLastPathComponent].path;
    
    if (![fileManager fileExistsAtPath:dir]) {
        [fileManager createDirectoryAtPath:dir withIntermediateDirectories:YES attributes:nil error:nil];
    }
    
    if (![fileManager fileExistsAtPath:outputPath]) {
        [chunk writeToFile:outputPath atomically:YES];
        //NSLog(@"文件已创建并写入第一块数据: %@", outputPath);
    } else {
        NSFileHandle *fileHandle = [NSFileHandle fileHandleForWritingAtPath:outputPath];
        if (fileHandle) {
            [fileHandle seekToEndOfFile];
            [fileHandle writeData:chunk];
            [fileHandle closeFile];
            //NSLog(@"数据块已追加到文件: %@", outputPath);
        } else {
            //NSLog(@"无法打开文件: %@", outputPath);
        }
    }
}

@end


