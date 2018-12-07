//
//  BertyCentralManagerDelegate.h
//  ble
//
//  Created by sacha on 28/11/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#ifndef BertyCentralManagerDelegate_h
#define BertyCentralManagerDelegate_h

#import "BertyUtils.h"
#import "BertyPeripheralDelegate.h"
#import "BertyDevice.h"
#import "Device.h"
#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import <Cocoa/Cocoa.h>


extern void setConnClosed(char *);

@protocol myDelegate <NSObject>

- (void)addDevice:(Device *) dev;
- (void)removeDevice:(Device *) dev;

@end


@interface BertyCentralManagerDelegate : NSObject <CBCentralManagerDelegate>

@property (nonatomic, assign) dispatch_block_t adder;
@property (nonatomic, strong) BertyPeripheralDelegate *peripheralDelegate;
@property (nonatomic, assign) id<myDelegate> delegate;

- (instancetype)initWithPeripheralDelegate:(BertyPeripheralDelegate *)delegate ;

- (void)centralManagerDidUpdateState:(nonnull CBCentralManager *)central;

@end
 
#endif
