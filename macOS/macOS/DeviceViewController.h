//
//  DeviceViewController.h
//  macOS
//
//  Created by sacha on 07/12/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import "ViewController.h"
#import <Cocoa/Cocoa.h>

#import "Device.h"

#ifndef DeviceViewController_h
#define DeviceViewController_h

@interface DeviceViewController : ViewController <NSTableViewDataSource, NSTableViewDelegate>

@property (strong) Device *device;
@property (strong) IBOutlet NSView *view;
@property (weak) IBOutlet NSTableView *tableView;

@end

#endif
