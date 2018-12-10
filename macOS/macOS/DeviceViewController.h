//
//  DeviceViewController.h
//  macOS
//
//  Created by sacha on 07/12/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import "ViewController.h"
#import <Cocoa/Cocoa.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import "Device.h"

#ifndef DeviceViewController_h
#define DeviceViewController_h

@interface DeviceViewController : ViewController <NSTableViewDataSource, NSTableViewDelegate>

@property (weak) IBOutlet NSTextField *maField;
@property (weak) IBOutlet NSTextField *peerIDField;

@property (strong) BertyDevice *bDevice;
@property (strong) Device *device;
@property (strong) CBPeripheralManager *cbm;
@property (strong) CBCentralManager *cbc;
@property (strong) IBOutlet NSView *view;
@property (weak) IBOutlet NSTableView *characteristicsView;
@property (weak) IBOutlet NSTableView *tableView;
@property (weak) IBOutlet NSTableView *serviceUUIDsView;

- (IBAction)reloadService:(id)sender;
- (IBAction)reloadCharacteristics:(id)sender;
- (IBAction)addToBertyDevices:(id)sender;
- (IBAction)connect:(id)sender;


@end

#endif
