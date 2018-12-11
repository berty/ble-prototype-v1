// +build darwin
//
//  BertyManagerPeripheralDelegate.m
//  ble
//
//  Created by sacha on 28/11/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import "BertyPeripheralManagerDelegate.h"
#import "ble.h"

@implementation BertyPeripheralManagerDelegate

- (instancetype)initWithPeripheralDelegate:(BertyPeripheralDelegate *)delegate {
    self = [super init];

    self.peripheralDelegate = delegate;
    return self;
}

- (void)peripheralManagerDidUpdateState:(nonnull CBPeripheralManager *)peripheral {
    NSString *stateString = nil;
    switch(peripheral.state)
    {
        case CBManagerStateUnknown: {
            stateString = @"CBManagerStateUnknown";
            break;
        }
        case CBManagerStateResetting: {
            stateString = @"CBManagerStateResetting";
            break;
        }
        case CBManagerStateUnsupported: {
            stateString = @"CBManagerStateUnsupported";
            break;
        }
        case CBManagerStateUnauthorized: {
            stateString = @"CBManagerStateUnauthorized";
            break;
        }
        case CBManagerStatePoweredOff: {
            stateString = @"CBManagerStatePoweredOff";
            break;
        }
        case CBManagerStatePoweredOn: {
            stateString = @"CBManagerStatePoweredOn";
            [BertyUtils sharedUtils].PeripharalIsOn = true;
            break;
        }
        default: {
            stateString = @"State unknown, update imminent.";
            break;
        }
    }

    NSLog(@"peripheralManagerDidUpdateState: %@", stateString);
}

/*!
 *  @method peripheralManager:willRestoreState:
 *
 *  @param peripheral    The peripheral manager providing this information.
 *  @param dict            A dictionary containing information about <i>peripheral</i> that was preserved by the system at the time the app was terminated.
 *
 *  @discussion            For apps that opt-in to state preservation and restoration, this is the first method invoked when your app is relaunched into
 *                        the background to complete some Bluetooth-related task. Use this method to synchronize your app's state with the state of the
 *                        Bluetooth system.
 *
 *  @seealso            CBPeripheralManagerRestoredStateServicesKey;
 *  @seealso            CBPeripheralManagerRestoredStateAdvertisementDataKey;
 *
 */
//- (void)peripheralManager:(CBPeripheralManager *)peripheral willRestoreState:(NSDictionary<NSString *, id> *)dict {
//    NSLog(@"peripheralManager:peripheral willRestoreState:%@", dict);
//}

/*!
 *  @method peripheralManagerDidStartAdvertising:error:
 *
 *  @param peripheral   The peripheral manager providing this information.
 *  @param error        If an error occurred, the cause of the failure.
 *
 *  @discussion         This method returns the result of a @link startAdvertising: @/link call. If advertisement could
 *                      not be started, the cause will be detailed in the <i>error</i> parameter.
 *
 */
- (void)peripheralManagerDidStartAdvertising:(CBPeripheralManager *)peripheral error:(nullable NSError *)error {
    if (error) {
        NSLog(@"peripheralManagerDidStartAdvertising errorCode: %ld domain: %@ userInfo: %@", error.code, error.domain, error.userInfo);
        return;
    }
    NSLog(@"peripheralManagerDidStartAdvertising success");
}

/*!
 *  @method peripheralManager:didAddService:error:
 *
 *  @param peripheral   The peripheral manager providing this information.
 *  @param service      The service that was added to the local database.
 *  @param error        If an error occurred, the cause of the failure.
 *
 *  @discussion         This method returns the result of an @link addService: @/link call. If the service could
 *                      not be published to the local database, the cause will be detailed in the <i>error</i> parameter.
 *
 */
- (void)peripheralManager:(CBPeripheralManager *)peripheral didAddService:(CBService *)service error:(nullable NSError *)error {
    if (error) {
        NSLog(@"error: %@", [error localizedFailureReason]);
    }
    NSLog(@"peripheralManager: didAddService: %@", [service.UUID UUIDString]);
}

/*!
 *  @method peripheralManager:central:didSubscribeToCharacteristic:
 *
 *  @param peripheral       The peripheral manager providing this update.
 *  @param central          The central that issued the command.
 *  @param characteristic   The characteristic on which notifications or indications were enabled.
 *
 *  @discussion             This method is invoked when a central configures <i>characteristic</i> to notify or indicate.
 *                          It should be used as a cue to start sending updates as the characteristic value changes.
 *
 */
- (void)peripheralManager:(CBPeripheralManager *)peripheral central:(CBCentral *)central didSubscribeToCharacteristic:(CBCharacteristic *)characteristic {
    NSLog(@"peripheralManager:peripheral central: %@ didSubscribeToCharacteristic:%@", central.identifier, characteristic.UUID);
}

/*!
 *  @method peripheralManager:central:didUnsubscribeFromCharacteristic:
 *
 *  @param peripheral       The peripheral manager providing this update.
 *  @param central          The central that issued the command.
 *  @param characteristic   The characteristic on which notifications or indications were disabled.
 *
 *  @discussion             This method is invoked when a central removes notifications/indications from <i>characteristic</i>.
 *
 */
- (void)peripheralManager:(CBPeripheralManager *)peripheral central:(CBCentral *)central didUnsubscribeFromCharacteristic:(CBCharacteristic *)characteristic {
    NSLog(@"peripheralManager:peripheral central: %@ didUnsubscribeToCharacteristic:%@", central.identifier, characteristic.UUID);
}

- (void)sendReadResponse:(CBPeripheralManager *)peripheral request:(CBATTRequest *)request value:(NSData *)val {
    if (request.offset > val.length) {
        request.value = [NSData dataWithBytes:(unsigned char[]){0x00} length:1];
        [peripheral respondToRequest:request withResult:CBATTErrorRequestNotSupported];
    }

    NSUInteger size = val.length - request.offset;
    NSData  *resp = [NSData dataWithBytesNoCopy:(char *)[val bytes] + request.offset
                                         length:size
                                   freeWhenDone:NO];

    request.value = resp;
    [peripheral respondToRequest:request withResult:CBATTErrorSuccess];
}

/*!
 *  @method peripheralManager:didReceiveWriteRequests:
 *
 *  @param peripheral   The peripheral manager requesting this information.
 *  @param requests     A list of one or more <code>CBATTRequest</code> objects.
 *
 *  @discussion         This method is invoked when <i>peripheral</i> receives an ATT request or command for one or more characteristics with a dynamic value.
 *                      For every invocation of this method, @link respondToRequest:withResult: @/link should be called exactly once. If <i>requests</i> contains
 *                      multiple requests, they must be treated as an atomic unit. If the execution of one of the requests would cause a failure, the request
 *                      and error reason should be provided to <code>respondToRequest:withResult:</code> and none of the requests should be executed.
 *
 *  @see                CBATTRequest
 *
 */
- (void)peripheralManager:(CBPeripheralManager *)peripheral didReceiveWriteRequests:(NSArray<CBATTRequest *> *)requests {
    int i = 0;
    for (CBATTRequest *request in requests) {

        NSLog(@"peripheralManager:peripheral didReceiveWriteRequests: %@ %d", request.central.identifier, i);
        i += 1;
        BertyDevice *bDevice = [BertyUtils getDeviceFromRequest:request];
        BertyUtils *utils = [BertyUtils sharedUtils];
        if (bDevice == nil) {
            CBPeripheral *cliPeripheral = nil;
            NSArray<CBPeripheral *> *cliPeripherals = [centralManager retrieveConnectedPeripheralsWithServices:@[[BertyUtils sharedUtils].serviceUUID]];
            for (CBPeripheral *p in cliPeripherals) {
                if ([[p.identifier UUIDString] isEqualToString:[request.central.identifier UUIDString]]) {
                    cliPeripheral = p;
                    break;
                }
            }
            if (cliPeripheral == nil) {
                NSLog(@"error didReceiveWriteRequests");
                [peripheral respondToRequest:request withResult:CBATTErrorRequestNotSupported];
                return;
            }
            [cliPeripheral setDelegate:self.peripheralDelegate];
            
            bDevice = [[BertyDevice alloc] initWithPeripheral:cliPeripheral withCentralManager:getCentral()];
            [BertyUtils addDevice:bDevice];
            @try {
                [centralManager connectPeripheral:cliPeripheral options:nil];
            } @catch (NSException *exception) {
                NSLog(@"didReceiveWriteRequests connect failed");
            } @finally {
                NSLog(@"didReceiveWriteRequests connect called");
            }
        }
        if ([request.characteristic.UUID isEqual:utils.writerUUID]) {
//            sendBytesToConn([bDevice.ma UTF8String], [request.value bytes], (int)[request.value length]);
            [peripheral respondToRequest:request withResult:CBATTErrorSuccess];
        } else if ([request.characteristic.UUID isEqual:utils.maUUID]) {
            [peripheral respondToRequest:request withResult:CBATTErrorSuccess];
            NSString *ma = [[NSString alloc] initWithData:request.value encoding:NSUTF8StringEncoding];
            NSLog(@"val %@ %@ %lu %@ %@", ma, [request.characteristic.UUID UUIDString], [bDevice.peripheral maximumWriteValueLengthForType:CBCharacteristicWriteWithResponse], bDevice, bDevice.ma);
            if (bDevice.ma != nil) {
                bDevice.ma =  [NSString stringWithFormat:@"%@%@", bDevice.ma, ma];
            } else {
                bDevice.ma = ma;
            }

        } else if ([request.characteristic.UUID isEqual:utils.peerUUID]) {
            [peripheral respondToRequest:request withResult:CBATTErrorSuccess];
            NSString *peerID = [[NSString alloc] initWithData:request.value encoding:NSUTF8StringEncoding];
            NSLog(@"val %@ %@ %lu %@ %@", peerID, [request.characteristic.UUID UUIDString], [bDevice.peripheral maximumWriteValueLengthForType:CBCharacteristicWriteWithResponse], bDevice, bDevice.peerID);
            if (bDevice.peerID != nil) {
                bDevice.peerID =  [NSString stringWithFormat:@"%@%@", bDevice.peerID, peerID];
            } else {
                bDevice.peerID = peerID;
            }

            NSLog(@"%lu", bDevice.peerID.length);
            if (bDevice.peerID.length == 46) {
                NSLog(@"COUNTDOWN %@", bDevice);
                [bDevice.latchRdy countDown];
            }
        } else {
            [peripheral respondToRequest:request withResult:CBATTErrorInsufficientAuthorization];
        }
    }
}

/*!
 *  @method peripheralManagerIsReadyToUpdateSubscribers:
 *
 *  @param peripheral   The peripheral manager providing this update.
 *
 *  @discussion         This method is invoked after a failed call to @link updateValue:forCharacteristic:onSubscribedCentrals: @/link, when <i>peripheral</i> is again
 *                      ready to send characteristic value updates.
 *
 */
- (void)peripheralManagerIsReadyToUpdateSubscribers:(CBPeripheralManager *)peripheral {
    NSLog(@"peripheralManagerIsReadyToUpdateSubscribers");
}

/*!
 *  @method peripheralManager:didPublishL2CAPChannel:error:
 *
 *  @param peripheral   The peripheral manager requesting this information.
 *  @param PSM            The PSM of the channel that was published.
 *  @param error        If an error occurred, the cause of the failure.
 *
 *  @discussion         This method is the response to a  @link publishL2CAPChannel: @/link call.  The PSM will contain the PSM that was assigned for the published
 *                        channel
 *
 */
- (void)peripheralManager:(CBPeripheralManager *)peripheral didPublishL2CAPChannel:(CBL2CAPPSM)PSM error:(nullable NSError *)error {

}

/*!
 *  @method peripheralManager:didUnublishL2CAPChannel:error:
 *
 *  @param peripheral   The peripheral manager requesting this information.
 *  @param PSM            The PSM of the channel that was published.
 *  @param error        If an error occurred, the cause of the failure.
 *
 *  @discussion         This method is the response to a  @link unpublishL2CAPChannel: @/link call.
 *
 */
- (void)peripheralManager:(CBPeripheralManager *)peripheral didUnpublishL2CAPChannel:(CBL2CAPPSM)PSM error:(nullable NSError *)error {

}

/*!
 *  @method peripheralManager:didOpenL2CAPChannel:error:
 *
 *  @param peripheral   The peripheral manager requesting this information.
 *
 *  @discussion         This method is invoked when <i>peripheral</i> receives an ATT request or command for one or more characteristics with a dynamic value.
 *                      For every invocation of this method, @link respondToRequest:withResult: @/link should be called exactly once. If <i>requests</i> contains
 *                      multiple requests, they must be treated as an atomic unit. If the execution of one of the requests would cause a failure, the request
 *                      and error reason should be provided to <code>respondToRequest:withResult:</code> and none of the requests should be executed.
 *
 *  @see                CBATTRequest
 *
 */
- (void)peripheralManager:(CBPeripheralManager *)peripheral didOpenL2CAPChannel:(nullable CBL2CAPChannel *)channel error:(nullable NSError *)error {

}

@end
