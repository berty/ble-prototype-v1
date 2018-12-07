//
//  Device.h
//  macOS
//
//  Created by sacha on 06/12/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>
#ifndef Device_h
#define Device_h

@interface Device : NSObject

@property (strong) NSString *identifierString;
@property (nonatomic, strong) CBPeripheral *peripheral;
@property (strong) NSString *name;
@property (nonatomic, assign) NSUInteger connected;
@property (strong) NSDictionary <NSString *, id>*advData;

-(instancetype)init:(NSString*)identifierString;

@end

#endif
