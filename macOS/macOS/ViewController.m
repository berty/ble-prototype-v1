//
//  ViewController.m
//  macOS
//
//  Created by sacha on 06/12/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import "ViewController.h"
#import "Device.h"
#import "ble.h"
#import <CoreBluetooth/CoreBluetooth.h>

@implementation ViewController 


- (void)viewDidLoad {
    [super viewDidLoad];
    
    Device *firs = [[Device alloc] init:@"first"];
    Device *firs2 = [[Device alloc] init:@"firs2t"];
    firs.connected = 3;
    firs2.connected = 3;
    [self.DeviceAC addObject:firs];
    [self.DeviceAC addObject:firs2];
    NSString *ma = @"d5406e06-ad83-4622-a7af-38dbe8c4fabe";
    NSString *peerID = @"QmNXdnmutcuwAG4BcvgrEwc3znArQc9JTq6ALFote1rBoU";
    
    init([ma UTF8String], [peerID UTF8String], self.DeviceAC);
    ((BertyCentralManagerDelegate *)getCentral().delegate).delegate = self;
    self.textview.delegate = self;
}

- (void)addDevice:(Device *) dev {
    [self.DeviceAC addObject:dev];
}
-(void)removeDevice:(Device *)dev {
    [self.DeviceAC removeObject:dev];
}


- (void)callMeBaby {
    NSLog(@"Test");
}

- (IBAction)service:(id)sender {
    NSLog(@"Service Button clicked %@", getPeripheral().delegate);
    [getPeripheral() addService:[BertyUtils sharedUtils].bertyService];
}

- (IBAction)adv:(id)sender {
    NSLog(@"adv Button clicked");
    
    [getPeripheral() startAdvertising:@{CBAdvertisementDataServiceUUIDsKey:@[[BertyUtils sharedUtils].serviceUUID]}];
    NSArray<CBPeripheral *> *peripherals = [centralManager retrieveConnectedPeripheralsWithServices:@[[BertyUtils sharedUtils].serviceUUID]];
    NSLog(@"%@", peripherals);
}

- (IBAction)scan:(id)sender {
    [getCentral() scanForPeripheralsWithServices:nil options:@{CBCentralManagerScanOptionAllowDuplicatesKey:@YES}];

//    [getCentral() scanForPeripheralsWithServices:@[[BertyUtils sharedUtils].serviceUUID] options:@{CBCentralManagerScanOptionAllowDuplicatesKey:@YES}];
    NSLog(@"scan Button clicked");
}

- (IBAction)connect:(id)sender {
    NSButton *button = (NSButton *)sender;
    NSTableCellView  *cell = (NSTableCellView *)button.superview;
    Device *device = (Device *)cell.objectValue;
    device.connected = 2;
    if (device.peripheral != nil) {
        [getCentral() connectPeripheral:device.peripheral options:nil];
    } else {
        NSLog(@"Error unknown peripheral");
    }
    
    NSLog(@"SENDER %ld %@ %@ %@", button.state, sender, cell, device.identifierString);
    return;
}

- (void)setRepresentedObject:(id)representedObject {
    [super setRepresentedObject:representedObject];

    // Update the view, if already loaded.
}

- (IBAction)search:(NSTextField *)sender {
    [self.DeviceAC setSearchString:sender.stringValue];
    [self.DeviceAC rearrangeObjects];
}

- (void) controlTextDidChange: (NSNotification *) aNotification {
    [self.DeviceAC setSearchString:self.textview.stringValue];
    [self.DeviceAC rearrangeObjects];
}


@end
