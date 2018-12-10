// +build darwin
//
//  main.m
//  kjh
//
//  Created by sacha on 26/09/2018.
//  Copyright © 2018 sacha. All rights reserved.
//

#import "ble.h"
#import <CoreBluetooth/CoreBluetooth.h>
#import <Foundation/Foundation.h>
#include <signal.h>


CBPeripheralManager *peripheralManager;
CBCentralManager *centralManager;

void handleSigInt(int sig) {
    exit(-1);
}

CBCentralManager *getCentral() {
    return centralManager;
}

CBPeripheralManager *getPeripheral() {
    NSLog(@"per %@", peripheralManager);
    return peripheralManager;
}

void initSignalHandling() {
    signal(SIGINT, handleSigInt);
}

void handleException(NSException* exception) {
    NSLog(@"Unhandled exception %@", exception);
}

BertyPeripheralManagerDelegate *BPMD;
BertyCentralManagerDelegate *BCMD;
void init(char *ma, char *peerID, NSViewController *vc) {
    if (centralManager == nil && peripheralManager == nil) {
        BertyPeripheralDelegate *peripheralDelegate = [[BertyPeripheralDelegate alloc] init];

//        dispatch_async(dispatch_get_main_queue(), ^{
//        dispactch_async(dispactch_get_main)
        BCMD = [[BertyCentralManagerDelegate alloc]initWithPeripheralDelegate:peripheralDelegate];
        
        BPMD = [[BertyPeripheralManagerDelegate alloc]initWithPeripheralDelegate:peripheralDelegate];
        
        peripheralManager = [[CBPeripheralManager alloc]
                             initWithDelegate:BPMD
                             queue:dispatch_queue_create("PeripheralManager", DISPATCH_QUEUE_SERIAL)
                             options:@{CBPeripheralManagerOptionShowPowerAlertKey:[NSNumber numberWithBool:YES]}];
         centralManager = [[CBCentralManager alloc]
                           initWithDelegate:BCMD
                           queue:dispatch_queue_create("CentralManager", DISPATCH_QUEUE_SERIAL)
                           options:@{CBCentralManagerOptionShowPowerAlertKey:[NSNumber numberWithBool:YES]}
                           ];
        
        [BCMD centralManagerDidUpdateState:centralManager];
        [BPMD peripheralManagerDidUpdateState:peripheralManager];
        [BertyUtils setMa:[NSString stringWithUTF8String:ma]];
        [BertyUtils setPeerID:[NSString stringWithUTF8String:peerID]];
        NSSetUncaughtExceptionHandler(handleException);
        initSignalHandling();
//        });
    }
}

void connDevice(CBPeripheral *peripheral) {
    [centralManager connectPeripheral:peripheral options:nil];
}

int startDiscover() {
    if (![centralManager isScanning]) {
//        NSDictionary *options = [NSDictionary dictionaryWithObjectsAndKeys:[NSNumber  numberWithBool:NO], CBCentralManagerScanOptionAllowDuplicatesKey, nil];
//        [centralManager scanForPeripheralsWithServices: @[[BertyUtils sharedUtils].serviceUUID] options:options];
        return 1;
    }
    return 0;
}

int isDiscovering() {
    return (int)[centralManager isScanning];
}

int isAdvertising() {
    return (int)[peripheralManager isAdvertising];
}

int startAdvertising() {
    NSLog(@"startAdvertising()");
    if (![peripheralManager isAdvertising]) {
//        [peripheralManager startAdvertising:@{CBAdvertisementDataServiceUUIDsKey:@[[BertyUtils sharedUtils].serviceUUID]}];
        return 1;
    }
    return 0;
}

NSData *Bytes2NSData(void *bytes, int length) { return [NSData dataWithBytes:bytes length:length]; }

void writeNSData(NSData *data, char *ma) {
    BertyDevice *bDevice = [BertyUtils getDeviceFromMa:[NSString stringWithUTF8String:ma]];
    [bDevice write:data];
}

int dialPeer(char *peerID) {
    if ([BertyUtils inDevicesWithMa:[NSString stringWithUTF8String:peerID]] == YES) {
        NSLog(@"TEST 1 %@", [NSString stringWithUTF8String:peerID]);
        return 1;
    }
    NSLog(@"TEST 0 %@", [NSString stringWithUTF8String:peerID]);
    return 0;
}

void closeConn(char *ma) {
//    [bcm close:[NSString stringWithUTF8String:ma]];
    // TODO
}

int isClosed(char *ma) {
    BertyDevice *bDevice = [BertyUtils getDeviceFromMa:[NSString stringWithUTF8String:ma]];
    if (bDevice.peripheral.state == CBPeripheralStateConnected) {
        return 0;
    }
    return 1;
}
