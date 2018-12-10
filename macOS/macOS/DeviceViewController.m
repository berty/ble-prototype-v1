//
//  DeviceViewController.m
//  macOS
//
//  Created by sacha on 07/12/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import "DeviceViewController.h"

@implementation DeviceViewController

@dynamic view;
@dynamic tableView;

-(void)viewWillAppear {
    [super viewWillAppear];
    [self.view.window setTitle:self.device.identifierString];
    
    NSLog(@"device %@", self.view.window);

    if (self.device.peripheral != nil) {
        self.bDevice = [BertyUtils getDevice:self.device.peripheral];
        [self.bDevice addObserver:self forKeyPath:@"ma" options:0 context:nil];
        [self.bDevice addObserver:self forKeyPath:@"peerID" options:0 context:nil];
        if (self.bDevice.ma != nil) {
            [self.maField setStringValue:self.bDevice.ma];
        }
        if (self.bDevice.peerID != nil) {
            [self.peerIDField setStringValue:self.bDevice.peerID];
        }
    }
    
    self.tableView.dataSource = self;
    self.tableView.delegate = self;
    self.serviceUUIDsView.dataSource = self;
    self.serviceUUIDsView.delegate = self;
    self.characteristicsView.dataSource = self;
    self.characteristicsView.delegate = self;
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context
{
    NSLog(@"LA MA %@", keyPath);
    if ([keyPath isEqualToString:@"ma"]) {
        NSLog(@"LA MA");
        dispatch_async(dispatch_get_main_queue(), ^{
            [self.maField setStringValue:self.bDevice.ma];
        });
    } else if ([keyPath isEqualToString:@"peerID"]) {
        NSLog(@"LA RPEER");
        dispatch_async(dispatch_get_main_queue(), ^{
            [self.peerIDField setStringValue:self.bDevice.peerID];
        });
    } else {
        [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
    }
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do view setup here.
}

- (NSInteger)numberOfRowsInTableView:(NSTableView *)tableView {
    if ([tableView.identifier isEqualToString:@"ServiceUUIDsView"]) {
        if (self.device.peripheral != nil) {
            return [self.device.peripheral.services count];
        }
        return 0;
    } else if ([tableView.identifier isEqualToString:@"CharacteristicsView"]) {
        if (self.device.peripheral != nil) {
            CBService *svc = [BertyUtils getServices:self.device.peripheral];
            return [svc.characteristics count];
        }
        return 0;
    }
    return [self.device.advData count];
}

- (nullable id)tableView:(NSTableView *)tableView objectValueForTableColumn:(NSTableColumn *)tableColumn row:(NSInteger)row {
    if ([tableColumn.identifier isEqualToString:@"Key"]) {
        return self.device.advData.allKeys[row];
    } else if ([tableColumn.identifier isEqualToString:@"Value"]) {
        return self.device.advData.allValues[row];
    } else if ([tableColumn.identifier isEqualToString:@"Service"]) {
        return [self.device.peripheral.services[row].UUID UUIDString];
    } else if ([tableColumn.identifier isEqualToString:@"Characteristics"]) {
        
        CBService *svc = [BertyUtils getServices:self.device.peripheral];
        return [svc.characteristics[row].UUID UUIDString];
    }
    return nil;
}

- (IBAction)addToBertyDevices:(id)sender {
    BertyDevice *device = [[BertyDevice alloc] initWithPeripheral:self.device.peripheral withCentralManager:getCentral()];
    [BertyUtils addDevice:device];
    self.bDevice = device;
    [self.bDevice addObserver:self forKeyPath:@"ma" options:0 context:nil];
    [self.bDevice addObserver:self forKeyPath:@"peerID" options:0 context:nil];
    NSLog(@"BertyDevices %@", [BertyUtils sharedUtils].bertyDevices);
}

- (IBAction)connect:(id)sender {
    [getCentral() connectPeripheral:self.device.peripheral options:nil];
}

- (IBAction)reloadService:(id)sender {
    [self.serviceUUIDsView reloadData];
}

- (IBAction)reloadCharacteristics:(id)sender {
    [self.characteristicsView reloadData];
}
- (IBAction)reloadString:(id)sender {
    if (self.device.peripheral != nil) {
        self.bDevice = [BertyUtils getDevice:self.device.peripheral];
        [self.bDevice addObserver:self forKeyPath:@"ma" options:0 context:nil];
        [self.bDevice addObserver:self forKeyPath:@"peerID" options:0 context:nil];
        if (self.bDevice.ma != nil) {
            [self.maField setStringValue:self.bDevice.ma];
        }
        if (self.bDevice.peerID != nil) {
            [self.peerIDField setStringValue:self.bDevice.peerID];
        }
    }
}
@end
