//
//  ViewController.h
//  macOS
//
//  Created by sacha on 06/12/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import "ble.h"
#import "BertyCentralManagerDelegate.h"
#ifndef ViewController_h
#define ViewController_h
@interface ViewController : NSViewController <myDelegate>

@property (weak) IBOutlet NSTableView *tableView;
@property (strong) IBOutlet NSArrayController *DeviceAC;
- (IBAction)service:(id)sender;
- (IBAction)adv:(id)sender;
- (IBAction)scan:(id)sender;
- (IBAction)connect:(id)sender;

@end
    
#endif
