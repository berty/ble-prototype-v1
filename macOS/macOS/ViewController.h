//
//  ViewController.h
//  macOS
//
//  Created by sacha on 06/12/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import "ble.h"
#import "DeviceController.h"
#import "BertyCentralManagerDelegate.h"
#ifndef ViewController_h
#define ViewController_h
@interface ViewController : NSViewController <myDelegate, NSTextFieldDelegate, NSTableViewDelegate>


@property (strong) IBOutlet DeviceController *DeviceAC;
@property (weak) IBOutlet NSTextField *textview;
@property (strong) IBOutlet NSView *allView;

@property (weak) IBOutlet NSTableView *tableView;
- (IBAction)service:(id)sender;
- (IBAction)adv:(id)sender;
- (IBAction)scan:(id)sender;
- (IBAction)connect:(id)sender;
- (IBAction)search:(NSTextField *)sender;
- (void) controlTextDidChange: (NSNotification *) aNotification;

@end

#endif
