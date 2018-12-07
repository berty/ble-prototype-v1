//
//  DeviceController.h
//  macOS
//
//  Created by sacha on 07/12/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import <Cocoa/Cocoa.h>

#ifndef DeviceController_h
#define DeviceController_h

@interface DeviceController : NSArrayController {
    NSString *searchString;
}

- (void)setSearchString:(NSString *)aString;
- (NSArray *)arrangeObjects:(NSArray *)objects;

@end

#endif
