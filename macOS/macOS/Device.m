//
//  Device.m
//  macOS
//
//  Created by sacha on 06/12/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import "Device.h"

@implementation Device

-(instancetype)init:(NSString*)identifierString {
    self = [super init];
    
    self.identifierString = identifierString;
    self.connected = 0;
    self.peripheral = nil;
    self.name = @"";
    self.advData = @{}; 
    
    return self;
}

- (void)setPeripheral:(CBPeripheral *)peripheral {
    dispatch_async(dispatch_get_main_queue(), ^{
    self->_peripheral = peripheral;
    self->_name = peripheral.name;
    });
}

- (id)copyWithZone:(NSZone *)zone
{
    id copy = [[[self class] alloc] init];

    return copy;
}

@end
