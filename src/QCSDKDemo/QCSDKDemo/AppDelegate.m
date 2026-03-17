//
//  AppDelegate.m
//  QCSDKDemo
//
//  Created by steve on 2025/7/22.
//

#import "AppDelegate.h"
#import "ViewController.h"
#import <QCSDK/QCSDKManager.h>
@interface AppDelegate ()


@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // Override point for customization after application launch.
    
    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    self.window.backgroundColor = [UIColor whiteColor];
    UINavigationController *nav = [[UINavigationController alloc] initWithRootViewController:[ViewController new]];
    nav.navigationBarHidden = NO;
    
    self.window.rootViewController = nav;
    [self.window makeKeyAndVisible];
    
    return YES;
}

- (void)application:(UIApplication *)application handleEventsForBackgroundURLSession:(NSString *)identifier completionHandler:(void (^)(void))completionHandler {
    [[QCSDKManager shareInstance] restoreDownloadSessionWithIdentifier:identifier completionHandler:completionHandler];
}

@end
