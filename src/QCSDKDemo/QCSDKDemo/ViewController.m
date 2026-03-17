//
//  ViewController.m
//  QCSDKDemo
//
//  Created by steve on 2025/7/22.
//

#import "ViewController.h"
#import <QCSDK/QCVersionHelper.h>
#import <QCSDK/QCSDKManager.h>
#import <QCSDK/QCSDKCmdCreator.h>
#import "QCSDKHelper.h"
#import "QCAIChatOpusDecoder.h"
#import "QCScanViewController.h"
#import "QCCentralManager.h"

typedef NS_ENUM(NSInteger, QGDeviceActionType) {
    /// Get hardware version, firmware version, and WiFi firmware versions
    QGDeviceActionTypeGetVersion = 0,

    /// Set the current device time
    QGDeviceActionTypeSetTime,

    /// Get battery level and charging status
    QGDeviceActionTypeGetBattery,

    /// Get the number of photos, videos, and audio files on the device
    QGDeviceActionTypeGetMediaInfo,

    /// Trigger the device to take a photo
    QGDeviceActionTypeTakePhoto,

    /// Start or stop video recording
    QGDeviceActionTypeToggleVideoRecording,

    /// Start or stop audio recording
    QGDeviceActionTypeToggleAudioRecording,
    
    /// Take AI Image
    QGDeviceActionTypeToggleTakeAIImage,
    
    /// Download Media Source
    QGDeviceActionTypeDownloadMediaResources,
    
    /// Convert Opus to pcm
    QGDeviceActionTypeOpusToPcm,
    
    /// Get video recording settings (e.g., resolution, frame rate, encoding)
    QGDeviceActionTypeGetVideoSettings,
    
    /// Set video recording settings on the device
    QGDeviceActionTypeSetVideoSettings,
    
    /// Get audio recording settings (e.g., sample rate, bitrate, gain)
    QGDeviceActionTypeGetAudioSettings,
    
    /// Set audio recording settings on the device
    QGDeviceActionTypeSetAudioSettings,
    
    /// Get Voice Wakeup status on the device
    QGDeviceActionTypeGetVoiceWakeup,
    
    /// Set Voice Wakeup status on the device
    QGDeviceActionTypeSetVoiceWakeup,
    
    /// Ble OTA
    QGDeviceActionTypeBleOTA,
    
    /// WiFi OTA
    QGDeviceActionTypeWiFiOTA,
    
    /// restart
    QGDeviceActionTypeRestart,
    
    /// Factory Reset
    QGDeviceActionTypeFactoryReset,
    
    /// Reserved for future use
    QGDeviceActionTypeReserved,
    
    /// AI Voice Chat
    QGDeviceActionTypeAIVoiceChat,
};



@interface ViewController ()<UITableViewDelegate, UITableViewDataSource,QCCentralManagerDelegate,QCSDKManagerDelegate>

@property(nonatomic,strong)UIBarButtonItem *rightItem;
@property(nonatomic,strong)UITableView *tableView;

@property(nonatomic,copy)NSString *hardVersion;
@property(nonatomic,copy)NSString *firmVersion;
@property(nonatomic,copy)NSString *hardWiFiVersion;
@property(nonatomic,copy)NSString *firmWiFiVersion;

@property(nonatomic,copy)NSString *mac;

@property(nonatomic,assign)NSInteger battary;
@property(nonatomic,assign)BOOL charging;

@property(nonatomic,assign)NSInteger photoCount;
@property(nonatomic,assign)NSInteger videoCount;
@property(nonatomic,assign)NSInteger audioCount;

@property(nonatomic,assign)BOOL recordingVideo;
@property(nonatomic,assign)BOOL recordingAudio;

@property(nonatomic,strong)NSData *aiImageData;

@property(nonatomic,strong)NSString *downloadMediaResourcesInfo;

@property(nonatomic,assign)BOOL isAIChating;

@property(nonatomic,assign)NSInteger videoDuration;
@property(nonatomic,strong)NSArray <NSNumber*>*videoDurationSupports;

@property(nonatomic,assign)NSInteger audioDuration;

@property(nonatomic,assign)BOOL voiceWakeupStatus;

@property(nonatomic,assign)NSInteger bleOTAProgress;
@property(nonatomic,assign)NSInteger wifiOTAProgress;
@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    self.title = @"Feature(Tap to get data)";
    
    self.rightItem = [[UIBarButtonItem alloc] initWithTitle:@"Search"
                                                      style:(UIBarButtonItemStylePlain)
                                                     target:self
                                                     action:@selector(rightAction)];
    self.navigationItem.rightBarButtonItem = self.rightItem;
    
    self.tableView = [[UITableView alloc] initWithFrame:self.view.bounds style:(UITableViewStylePlain)];
    self.tableView.backgroundColor = [UIColor clearColor];
    self.tableView.delegate = self;
    self.tableView.dataSource = self;
    self.tableView.estimatedRowHeight = 60;
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    self.tableView.hidden = YES;
    [self.view addSubview:self.tableView];
    
    [QCSDKManager shareInstance].delegate = self;
    [QCSDKManager shareInstance].debug = true;
}

#pragma mark - Device Data Report
- (void)didUpdateBatteryLevel:(NSInteger)battery charging:(BOOL)charging {
    self.battary = battery;
    self.charging = charging;
    [self.tableView reloadData];
}

- (void)didUpdateMediaWithPhotoCount:(NSInteger)photo videoCount:(NSInteger)video audioCount:(NSInteger)audio type:(NSInteger)type {
    
    self.photoCount = photo;
    self.videoCount = video;
    self.audioCount = audio;
    self.downloadMediaResourcesInfo = @"";
    [self.tableView reloadData];
}

- (void)didReceiveAIChatImageData:(NSData *)imageData {
    NSLog(@"didReceiveAIChatImageData");
    self.aiImageData = imageData;
    [self.tableView reloadData];
}

- (void)willReceiveAIChatVoiceData {
    NSLog(@"willReceiveAIChatVoiceData");
    [[QCAIChatOpusDecoder share] start];
}

- (void)didReceiveAIChatVoiceOpusData:(NSData *)opusData {
    [[QCAIChatOpusDecoder share] appendOpusData:opusData];
}

- (void)didReceiveAIChatVoiceData:(NSData *)pcmData {
    NSLog(@"didReceiveAIChatVoiceData: data size:%zd",[pcmData length]);
    
    
    
    
    if (!self.isAIChating) {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(10 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            self.isAIChating = NO;
            NSLog(@"stop ai chat");
            [[QCSDKManager shareInstance] stopAIChat];
        });
    }
    self.isAIChating = YES;
}

- (void)didFinishReceivingAIChatVoiceData {
    NSLog(@"didFinishReceivingAIChatVoiceData");
    [[QCSDKManager shareInstance] stopAIChat];
}

#pragma mark - Feature Fuctions
- (void)getHardVersionAndFirmVersion {
    [QCSDKCmdCreator getDeviceVersionInfoSuccess:^(NSString * _Nonnull firmVersion, NSString * _Nonnull hdVersion, NSString * _Nonnull firmWifiVersion, NSString * _Nonnull hdWifiVersion) {
        
        self.hardVersion = hdVersion;
        self.firmVersion = firmVersion;
        self.hardWiFiVersion = hdWifiVersion;
        self.firmWiFiVersion = firmWifiVersion;
        [self.tableView reloadData];
        NSLog(@"hard Version:%@",hdVersion);
        NSLog(@"firm Version:%@",firmVersion);
        NSLog(@"hard Wifi Version:%@",hdWifiVersion);
        NSLog(@"firm Wifi Version:%@",firmWifiVersion);
    } fail:^{
        NSLog(@"get version fail");
    }];
}

- (void)getMacAddress {
    //[QCSDKCmdCreator get
    [QCSDKCmdCreator getDeviceMacAddressSuccess:^(NSString * _Nullable macAddress) {
        self.mac = macAddress;
        [self.tableView reloadData];
    } fail:^{
        NSLog(@"get mac address fail");
    }];
}

- (void)setTime {
    [QCSDKCmdCreator setupDeviceDateTime:^(BOOL isSuccess, NSError * _Nullable err) {
        if (err) {
            NSLog(@"get err fail");
        }
    }];
}

- (void)getBattary {
    [QCSDKCmdCreator getDeviceBattery:^(NSInteger battary, BOOL charging) {
        
        self.battary = battary;
        self.charging = charging;
        [self.tableView reloadData];
    } fail:^{
        
    }];
}

- (void)getMediaInfo {
    [QCSDKCmdCreator getDeviceMedia:^(NSInteger photo, NSInteger video, NSInteger audio, NSInteger type) {
        
        self.photoCount = photo;
        self.videoCount = video;
        self.audioCount = audio;
        [self.tableView reloadData];
    } fail:^{
        
    }];
}

- (void)takePhoto {
    //
    [QCSDKCmdCreator setDeviceMode:(QCOperatorDeviceModePhoto) success:^{
        
    } fail:^(NSInteger mode) {
        NSLog(@"set fail,current device model:%zd",mode);
    }];
}

- (void)recordVideo {
    
    if (self.recordingVideo) {
        
        [QCSDKCmdCreator setDeviceMode:(QCOperatorDeviceModeVideoStop) success:^{
            self.recordingVideo = NO;
            [self.tableView reloadData];
        } fail:^(NSInteger mode) {
            NSLog(@"set fail,current device model:%zd",mode);
        }];
    }
    else {
        [QCSDKCmdCreator setDeviceMode:(QCOperatorDeviceModeVideo) success:^{
            self.recordingVideo = YES;
            [self.tableView reloadData];
        } fail:^(NSInteger mode) {
            NSLog(@"set fail,current device model:%zd",mode);

        }];
    }
}

- (void)recordAudio {
    if (self.recordingAudio) {
        [QCSDKCmdCreator setDeviceMode:(QCOperatorDeviceModeAudioStop) success:^{
            self.recordingAudio = NO;
            [self.tableView reloadData];
        } fail:^(NSInteger mode) {
            NSLog(@"set fail,current device model:%zd",mode);
        }];
    } else {
        [QCSDKCmdCreator setDeviceMode:(QCOperatorDeviceModeAudio) success:^{
            self.recordingAudio = YES;
            [self.tableView reloadData];
        } fail:^(NSInteger mode) {
            NSLog(@"set fail,current device model:%zd",mode);
        }];
    }
}

- (void)takeAIImage {
    //- (void)didReceiveAIChatImageData:(NSData *)imageData
    [QCSDKCmdCreator setDeviceMode:(QCOperatorDeviceModeAIPhoto) success:^{
        
    } fail:^(NSInteger mode) {
        NSLog(@"set fail,current device model:%zd",mode);
    }];
}

- (void)downloadMediaResources {
    if (self.photoCount+self.videoCount+self.audioCount == 0) {
        self.downloadMediaResourcesInfo = @"No Meida Resources";
        [self.tableView reloadData];
        return;
    }
    
    self.downloadMediaResourcesInfo = @"connecting...";
    [self.tableView reloadData];
    
    [[QCSDKManager shareInstance] startToDownloadMediaResourceWithProgress:^(NSInteger receivedSize, NSInteger expectedSize, CGFloat progress) {
        self.downloadMediaResourcesInfo = [NSString stringWithFormat:@"download progress:%.0f%%",progress*100];
        NSLog(@"download progress:%.0f%%",progress*100);
        [self.tableView reloadData];
    } completion:^(NSString * _Nullable filePath, NSError * _Nullable error,NSInteger index,NSInteger count) {
        
        if (!error) {
            NSLog(@"download success(%zd) at :%@",index+1,filePath);
            if (index == count - 1) {
                NSLog(@"download finished");
                self.downloadMediaResourcesInfo = @"Download Finished";
                [self.tableView reloadData];
            }
        }
    }];
}

- (void)convertOpusToPcm {
    NSString *opusPath = [[NSBundle mainBundle] pathForResource:@"test" ofType:@"opus"];
    NSLog(@"opus file patch: %@", opusPath);
    
    NSString *docDir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];

    NSString *pcmPath = [docDir stringByAppendingPathComponent:@"test.pcm"];

    NSLog(@"pcm file patch: %@", pcmPath);

    [[QCSDKHelper shareInstance] convertOpusToPcm:opusPath outputPath:pcmPath progress:^(float progress) {
        
    } completion:^(BOOL success) {
        if (success) {
            NSLog(@"convert opus to pcm success");
        }
        else {
            NSLog(@"convert opus to pcm fail");
        }
    }];
}

- (void)getVideoInfo {
    [QCSDKCmdCreator getVideoInfoSuccess:^(NSInteger angle, NSInteger duration, NSArray<NSNumber *> * _Nonnull supports) {
        
        //angle:1-Landscape Video,2-Portrait
        //supports: unit-seconds
        self.videoDuration = duration;
        self.videoDurationSupports = supports;
        [self.tableView reloadData];
        
        
        NSLog(@"get video duration success:%zd",duration);
    } fail:^{
        NSLog(@"get video duration fail");
    }];
    
}

- (void)setVideoInfo {
    
    //set to 6*60 seconds
    [QCSDKCmdCreator setVideoInfo:1 duration:6*60 success:^{
        NSLog(@"set video duration success:%d",6*60);
        self.videoDuration = 6*60;
        [self.tableView reloadData];
    } fail:^{
        NSLog(@"set video duration fail");
    }];
}

- (void)getAudioInfo {
    
    [QCSDKCmdCreator getAudioInfoSuccess:^(NSInteger angle, NSInteger duration) {
        
        //angle: reserved parameters
        self.audioDuration = duration;
        [self.tableView reloadData];
        NSLog(@"get audio duration success:%zd",self.audioDuration);

    } fail:^{
        NSLog(@"get audio duration fail");
    }];
}

- (void)setWakeup {
    [QCSDKCmdCreator setVoiceWakeup:NO finished:^(BOOL suc, NSError * _Nullable err, id  _Nullable result) {
        if (suc) {
            NSLog(@"Close Voice Wakeup success");
        }
        else  {
            NSLog(@"Close Voice Wakeup fail");
        }
        [self.tableView reloadData];
    }];
}

- (void)getWakeup {
    
    [QCSDKCmdCreator getVoiceWakeupWithFinished:^(BOOL suc, NSError * _Nullable err, id _Nullable result) {
        if (suc) {
            self.voiceWakeupStatus = [result boolValue];
            if (self.voiceWakeupStatus) {
                NSLog(@"Voice Wakeup is On");
            }
            else {
                NSLog(@"Voice Wakeup is Off");
            }
            [self.tableView reloadData];
        }
    }];
}


- (void)setAudioInfo {
    
    //set to 6*60 seconds
    [QCSDKCmdCreator setAudioInfo:1 duration:60*60 success:^{
        NSLog(@"set audio duration success:%d",60*60);
        self.audioDuration = 60*60;
        [self.tableView reloadData];
    } fail:^{
        NSLog(@"set audio duration fail");
    }];
}

- (void)bleOTA {
    __block int p = -1;
    NSLog(@"🌟Please make sure the firmware file corresponds to the device, AM01C_2.20.02_251014 corresponds to AM01C_V2.2🌟");
    NSString *filePath = @"";
    if (filePath.length == 0) {
        [self showAlertWithTitle:@"Tips" message:@"Please configure the OTA file paths in the project." confirmText:nil cancel:NO confirm:^{
            
        }];
        return;
    }
    [[QCSDKManager shareInstance] startToBleOTAFirmwareUpdateWithFilePath:filePath start:^{
        NSLog(@"Start firmware upgrade");
    } progress:^(int percentage) {
        if (p != percentage) {
            p = percentage;
            self.bleOTAProgress = p;
            [self.tableView reloadData];
            NSLog(@"firmware progress:%ld",(long)percentage);
        }
    } success:^(int seconds) {
        NSLog(@"Firmware upgrade is successful, time:%lds",(long)seconds);
    } failed:^(NSError * _Nullable error) {
        NSLog(@"Firmware upgrade failed");
    }];
}

- (void)wifiOTA {
    __block int p = -1;
    NSLog(@"🌟Please make sure the firmware file corresponds to the device, 17_openwrt_v821_aiglass-ai corresponds to WIFIAM01C_V2.2🌟");
    NSString *filePath = @"";
    if (filePath.length == 0) {
        [self showAlertWithTitle:@"Tips" message:@"Please configure the OTA file paths in the project." confirmText:nil cancel:NO confirm:^{
            
        }];
        return;
    }
    [[QCSDKManager shareInstance] startToWiFiOTAFirmwareUpdateWithFilePath:filePath start:^{
        NSLog(@"Start firmware upgrade");
    } progress:^(int percentage) {
        if (p != percentage) {
            p = percentage;
            self.wifiOTAProgress = p;
            [self.tableView reloadData];
            NSLog(@"firmware progress:%ld",(long)percentage);
        }
    } success:^(int seconds) {
        NSLog(@"Firmware upgrade is successful, time:%lds",(long)seconds);
    } failed:^(NSError * _Nullable error) {
        NSLog(@"Firmware upgrade failed");
    }];
}

- (void)restart {
    [QCSDKCmdCreator setDeviceMode:(QCOperatorDeviceModeRestart) success:^{
        NSLog(@"restart success.");
    } fail:^(NSInteger err) {
        NSLog(@"restart fail.devic mode type:%zd",err);
    }];
}

- (void)factoryReset {
    [QCSDKCmdCreator setDeviceMode:(QCOperatorDeviceModeFactoryReset) success:^{
        NSLog(@"factory reset success.");
    } fail:^(NSInteger err) {
        NSLog(@"factory reset fail.devic mode type:%zd",err);
    }];
}

#pragma mark - Actions
- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    
    [QCCentralManager shared].delegate = self;
    [self didState:[QCCentralManager shared].deviceState];
}

- (void)rightAction {
    
    if([self.rightItem.title isEqualToString:@"Unbind"]) {
        [[QCCentralManager shared] remove];
    }
    else if ([self.rightItem.title isEqualToString:@"Search"])  {
        QCScanViewController *viewCtrl = [[QCScanViewController alloc] init];
        [self.navigationController pushViewController:viewCtrl animated:true];
    }
}

#pragma mark - QCCentralManagerDelegate
- (void)didState:(QCState)state {
    self.title = @"Feature";
    switch(state) {
        case QCStateUnbind:
            self.rightItem.title = @"Search";
            self.tableView.hidden = YES;
            break;
        case QCStateConnecting:
            self.title = [QCCentralManager shared].connectedPeripheral.name;
            self.rightItem.title = @"Connecting";
            self.rightItem.enabled = NO;
            self.tableView.hidden = YES;
            break;
        case QCStateConnected:
            self.title = [NSString stringWithFormat:@"%@(Tap to get data)",[QCCentralManager shared].connectedPeripheral.name];
            self.rightItem.title = @"Unbind";
            self.rightItem.enabled = YES;
            self.tableView.hidden = NO;
            break;
        case QCStateUnkown:
            break;
        case QCStateDisconnecting:
        case QCStateDisconnected:
            self.rightItem.title = @"Search";
            self.rightItem.enabled = YES;
            self.tableView.hidden = YES;
            break;
    }
}

- (void)didBluetoothState:(QCBluetoothState)state {
    
}

- (void)didConnected:(CBPeripheral *)peripheral     //用户可以返回设备类型
{
    NSLog(@"didConnected");
    self.rightItem.enabled = YES;
    self.title = peripheral.name;
}

- (void)didDisconnecte:(CBPeripheral *)peripheral {
    NSLog(@"didDisconnecte");
    self.title = @"Feature";
    
    self.rightItem.title = @"Search";
    self.rightItem.enabled = YES;
    self.tableView.hidden = YES;
}

- (void)didFailConnected:(CBPeripheral *)peripheral {
    
    NSLog(@"didFailConnected");
    self.rightItem.enabled = YES;
}


#pragma mark - UITableViewDataSource
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return QGDeviceActionTypeReserved;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {

    static NSString *cellIdentifier = @"Cell";

    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:cellIdentifier];

    if (!cell) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:cellIdentifier];
    }

    cell.detailTextLabel.numberOfLines = 0;
    cell.detailTextLabel.lineBreakMode = NSLineBreakByWordWrapping;
    cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
    cell.detailTextLabel.text = @"";
    cell.imageView.image = nil;
    
    switch ((QGDeviceActionType)indexPath.row) {
        case QGDeviceActionTypeGetVersion:
            cell.textLabel.text = @"Get hard Version & firm Version";
            cell.detailTextLabel.text = [NSString stringWithFormat:@"hardVersion:%@,\nfirmVersion:%@,\nhardWifiVersion:%@,\nfirmWifiVersion:%@", self.hardVersion, self.firmVersion, self.hardWiFiVersion, self.firmWiFiVersion];
            break;
        case QGDeviceActionTypeSetTime:
            cell.textLabel.text = @"Set Time";
            break;
        case QGDeviceActionTypeGetBattery:
            cell.textLabel.text = @"Get Battary";
            cell.detailTextLabel.text = [NSString stringWithFormat:@"battary:%zd,charing:%zd", self.battary, (NSInteger)self.charging];
            break;
        case QGDeviceActionTypeGetMediaInfo:
            cell.textLabel.text = @"Get media info";
            cell.detailTextLabel.text = [NSString stringWithFormat:@"photo:%zd,video:%zd,audio:%zd", self.photoCount, self.videoCount, self.audioCount];
            break;
        case QGDeviceActionTypeTakePhoto:
            cell.textLabel.text = @"Take Photo";
            break;
        case QGDeviceActionTypeToggleVideoRecording:
            cell.textLabel.text = self.recordingVideo ? @"Stop Recording Video" : @"Start Recording Video";
            break;
        case QGDeviceActionTypeToggleAudioRecording:
            cell.textLabel.text = self.recordingAudio ? @"Stop Record audio" : @"Start Record audio";
            break;
        case QGDeviceActionTypeToggleTakeAIImage:
            cell.textLabel.text = @"Take AI Image";
            if (self.aiImageData) {
                cell.imageView.image = [UIImage imageWithData:self.aiImageData];
            }
            break;
        case QGDeviceActionTypeDownloadMediaResources:
            cell.textLabel.text = @"Download media source";
            cell.detailTextLabel.text = self.downloadMediaResourcesInfo;
            break;
        case QGDeviceActionTypeOpusToPcm:
            cell.textLabel.text = @"convert opus to pcm";
            break;
        case QGDeviceActionTypeGetVideoSettings:
            cell.textLabel.text = @"get video info";
            cell.detailTextLabel.text = [NSString stringWithFormat:@"video duration:%zd",self.videoDuration];
            break;
        case QGDeviceActionTypeSetVideoSettings:
            cell.textLabel.text = @"set video info";
            cell.detailTextLabel.text = [NSString stringWithFormat:@"video duration:%zd",self.videoDuration];
            break;
        case QGDeviceActionTypeGetAudioSettings:
            cell.textLabel.text = @"get audio info";
            cell.detailTextLabel.text = [NSString stringWithFormat:@"audio duration:%zd",self.audioDuration];
            break;
        case QGDeviceActionTypeSetAudioSettings:
            cell.textLabel.text = @"set audio info";
            cell.detailTextLabel.text = [NSString stringWithFormat:@"audio duration:%zd",self.audioDuration];
            break;
        case QGDeviceActionTypeAIVoiceChat:
            cell.textLabel.text = @"AI Chat";
            break;
        case QGDeviceActionTypeGetVoiceWakeup:
            cell.textLabel.text = @"Get voice wakeup";
            cell.detailTextLabel.text = [NSString stringWithFormat:@"voice wakeup is %@",self.voiceWakeupStatus ? @"On":@"Off"];
            break;
        case QGDeviceActionTypeSetVoiceWakeup:
            cell.textLabel.text = @"Set voice wakeup";
            cell.detailTextLabel.text = [NSString stringWithFormat:@"set voice wakeup off"];
            break;
        case QGDeviceActionTypeBleOTA:
            cell.textLabel.text = @"Ble OTA";
            if (self.bleOTAProgress > 0) {
                cell.detailTextLabel.text = [NSString stringWithFormat:@"ble OTA Progress:%02zd%%",self.bleOTAProgress];
            }
            break;
        case QGDeviceActionTypeWiFiOTA:
            cell.textLabel.text = @"WiFi OTA";
            if (self.wifiOTAProgress > 0) {
                cell.detailTextLabel.text = [NSString stringWithFormat:@"wifi OTA Progress:%02zd%%",self.wifiOTAProgress];
            }
            break;
        case QGDeviceActionTypeRestart:
            cell.textLabel.text = @"Restart";
            break;
        case QGDeviceActionTypeFactoryReset:
            cell.textLabel.text = @"Factory Reset";
            break;
        case QGDeviceActionTypeReserved:
            break;
        default:
            break;
    }

    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:NO];
    QGDeviceActionType actionType = (QGDeviceActionType)indexPath.row;

    switch (actionType) {
        case QGDeviceActionTypeGetVersion:
            [self getHardVersionAndFirmVersion];
            break;
        case QGDeviceActionTypeSetTime:
            [self setTime];
            break;
        case QGDeviceActionTypeGetBattery:
            [self getBattary];
            break;
        case QGDeviceActionTypeGetMediaInfo:
            [self getMediaInfo];
            break;
        case QGDeviceActionTypeTakePhoto:
            [self takePhoto];
            break;
        case QGDeviceActionTypeToggleVideoRecording:
            [self recordVideo];
            break;
        case QGDeviceActionTypeToggleAudioRecording:
            [self recordAudio];
            break;
        case QGDeviceActionTypeToggleTakeAIImage:
            [self takeAIImage];
            break;
        case QGDeviceActionTypeDownloadMediaResources:
            [self downloadMediaResources];
            break;
        case QGDeviceActionTypeOpusToPcm:
            [self convertOpusToPcm];
            break;
        case QGDeviceActionTypeGetVideoSettings:
            [self getVideoInfo];
            break;
        case QGDeviceActionTypeSetVideoSettings:
            [self setVideoInfo];
            break;
        case QGDeviceActionTypeGetAudioSettings:
            [self getAudioInfo];
            break;
        case QGDeviceActionTypeSetAudioSettings:
            [self setAudioInfo];
            break;
        case QGDeviceActionTypeGetVoiceWakeup:
            [self getWakeup];
            break;
        case QGDeviceActionTypeSetVoiceWakeup:
            [self setWakeup];
            break;
        case QGDeviceActionTypeBleOTA:
            [self bleOTA];
            break;
        case QGDeviceActionTypeWiFiOTA:
            [self wifiOTA];
            break;
        case QGDeviceActionTypeRestart:
            [self restart];
            break;
        case QGDeviceActionTypeFactoryReset:
            [self factoryReset];
            break;
        case QGDeviceActionTypeAIVoiceChat:
            break;
        case QGDeviceActionTypeReserved:
        default:
            break;
    }

}

#pragma mark - Helper

- (void)showAlertWithTitle:(NSString *)title
                   message:(NSString *)message
               confirmText:(NSString *)confirmText
                    cancel:(BOOL)showCancel
                   confirm:(void (^)(void))confirmBlock {

    UIAlertController *alert = [UIAlertController alertControllerWithTitle:title
                                                                   message:message
                                                            preferredStyle:UIAlertControllerStyleAlert];

    if (showCancel) {
        UIAlertAction *cancel = [UIAlertAction actionWithTitle:@"Cancel"
                                                         style:UIAlertActionStyleCancel
                                                       handler:nil];
        [alert addAction:cancel];
    }

    UIAlertAction *confirm = [UIAlertAction actionWithTitle:confirmText ?: @"OK"
                                                      style:UIAlertActionStyleDefault
                                                    handler:^(UIAlertAction * _Nonnull action) {
        if (confirmBlock) {
            confirmBlock();
        }
    }];

    [alert addAction:confirm];

    [self presentViewController:alert animated:YES completion:nil];
}
@end
