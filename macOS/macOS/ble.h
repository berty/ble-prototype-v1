//
//  ble.h
//  test
//
//  Created by sacha on 26/09/2018.
//  Copyright © 2018 sacha. All rights reserved.
//
#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import <Cocoa/Cocoa.h>
//#import "BertyUtils.h"
#import "BertyCentralManagerDelegate.h"
#import "BertyPeripheralManagerDelegate.h"

#import "BertyDevice.h"

#ifndef ble_h
#define ble_h


void init(char *ma, char *peerID, NSViewController *);
int startAdvertising(void);
int startDiscover(void);
int isDiscovering(void);
void connDevice(CBPeripheral *peripheral);
int isAdvertising(void);
int dialPeer(char *peerID);
char *readPeerID(char *peerID);
NSData *Bytes2NSData(void *bytes, int length);
void writeNSData(NSData *data, char *ma);
void closeConn(char *ma);
int isClosed(char *ma);
CBCentralManager *getCentral(void);
CBPeripheralManager *getPeripheral(void);

#endif /* ble_h */
