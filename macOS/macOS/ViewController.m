//
//  ViewController.m
//  macOS
//
//  Created by sacha on 06/12/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import "DeviceViewController.h"
#import "ViewController.h"
#import "Device.h"
#import "ble.h"
#import <CoreBluetooth/CoreBluetooth.h>

@implementation ViewController {
    Device *selected;
}

- (void)viewWillAppear {
    NSLog(@"CALLED 1");
    [super viewWillAppear];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    
    if (self.loaded == NO) {
        
    }
}

- (void)setDeviceAC:(DeviceController *)DeviceAC
{
    NSLog(@"De %@", DeviceAC);
//    if
    
    _DeviceAC = DeviceAC;
        NSLog(@"%hhd %hhd", _loaded, self.loaded);
    _loaded = YES;
    NSLog(@"%hhd", _loaded);
    Device *firs = [[Device alloc] init:@"first"];
    Device *firs2 = [[Device alloc] init:@"firs2t"];
    firs.connected = 3;
    firs2.connected = 3;
    [self.DeviceAC addObject:firs];
    [self.DeviceAC addObject:firs2];
    NSString *ma = @"d5406e06-ad83-4622-a7af-38dbe8c4fabe";
    NSString *peerID = @"QmNXdnmutcuwAG4BcvgrEwc3znArQc9JTq6ALFote1rBoU";
    
    init([ma UTF8String], [peerID UTF8String], self.DeviceAC);
    self.centralManager = getCentral();
    self.peripheralManager = getPeripheral();
    NSLog(@"self.De %@", _DeviceAC);
    [self.tableView reloadData];
    
    
    self.textview.delegate = self;
    ((BertyCentralManagerDelegate *)getCentral().delegate).deviceAC = self.DeviceAC;
    ((BertyCentralManagerDelegate *)getCentral().delegate).delegate = self;
    
    

}

- (void)addDevice:(Device *) dev {
    [self.DeviceAC addObject:dev];
}
-(void)removeDevice:(Device *)dev {
    [self.DeviceAC removeObject:dev];
}

- (void)prepareForSegue:(NSStoryboardSegue *)segue sender:(id)sender {
    NSLog(@" id : %@", segue.identifier);
    if ([segue.identifier isEqualToString:@"DeviceView"]) {
        NSLog(@" id2 : %@ , %lu", segue.identifier, self.tableView.selectedRow);
        DeviceViewController *view = (DeviceViewController *)segue.destinationController;
        view.device = self.DeviceAC.arrangedObjects[self.tableView.selectedRow];
        view.cbc = getCentral();
        view.cbm = getPeripheral();
    }
}

- (BOOL)shouldPerformSegueWithIdentifier:(NSStoryboardSegueIdentifier)identifier sender:(id)sender {
    NSLog(@"iden %@", identifier);
    if ([identifier isEqualToString:@"DeviceView"]) {
        if (self.tableView.selectedRow > [(NSArray*)[self.DeviceAC arrangedObjects] count]) {
            return NO;
        }
    }
    return YES;
}

- (void)callMeBaby {
    NSLog(@"Test");
}

- (IBAction)service:(id)sender {
    NSLog(@"Service Button clicked %@ %@ %hhd", getPeripheral(), getPeripheral().delegate, [BertyUtils sharedUtils].serviceAdded);
    if ([BertyUtils sharedUtils].serviceAdded == NO) {
        [BertyUtils sharedUtils].serviceAdded = YES;
        [getPeripheral() addService:[BertyUtils sharedUtils].bertyService];
    }
}

- (IBAction)adv:(id)sender {
    NSLog(@"adv Button clicked");
    
    [getPeripheral() startAdvertising:@{CBAdvertisementDataServiceUUIDsKey:@[[BertyUtils sharedUtils].serviceUUID]}];
    NSArray<CBPeripheral *> *peripherals = [centralManager retrieveConnectedPeripheralsWithServices:@[[BertyUtils sharedUtils].serviceUUID]];
    NSLog(@"%@", peripherals);
}

- (IBAction)scan:(id)sender {
    NSLog(@"SCAN %@, %@, %@", getCentral(), getCentral().delegate, self.DeviceAC);
    @try {
//        ((BertyCentralManagerDelegate *)getCentral().delegate).deviceAC = self.DeviceAC;
        [getCentral() stopScan];
        [getCentral() scanForPeripheralsWithServices:@[[BertyUtils sharedUtils].serviceUUID] options:@{CBCentralManagerScanOptionAllowDuplicatesKey:@YES}];
    } @catch (NSException *exception) {
        NSLog(@"%@", exception.reason);
    }
    @finally {
        NSLog(@"Finally condition");
    }

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

- (void)viewWillDisappear {
    NSLog(@"VIEW DISAPEARING");
}

- (void)viewDidDisappear {
    NSLog(@"VIEW DISAPEARIN123G");
//    [getPeripheral() removeService:[BertyUtils sharedUtils].bertyService];
//    for (Device *dev in self.DeviceAC.arrangedObjects) {
//        NSLog(@"dev %@", dev);
//        if (dev.peripheral != nil) {
//            [getCentral() cancelPeripheralConnection:dev.peripheral];
//        }
//    }
}

@end
