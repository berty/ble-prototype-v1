//
//  DeviceController.m
//  macOS
//
//  Created by sacha on 07/12/2018.
//  Copyright © 2018 berty. All rights reserved.
//

#import "DeviceController.h"

#import <Foundation/NSKeyValueObserving.h>

@implementation DeviceController

- (instancetype)init {
    NSLog(@"INITED");
    return [super init];
}

- (NSArray *)arrangeObjects:(NSArray *)objects {
    if (searchString == nil || [searchString isEqualToString:@""]) {
        return [super arrangeObjects:objects];
    }
    
    NSMutableArray *filteredObjects = [NSMutableArray arrayWithCapacity:[objects count]];
    NSEnumerator *objectsEnumerator = [objects objectEnumerator];
    id item;
    
    while (item = [objectsEnumerator nextObject]) {
        if ([[item valueForKeyPath:@"identifierString"] rangeOfString:searchString options:NSAnchoredSearch].location != NSNotFound) {
            [filteredObjects addObject:item];
        }
    }
    return [super arrangeObjects:filteredObjects];
}

- (void)search:(id)sender {
    [self setSearchString:[sender stringValue]];
    [self rearrangeObjects];
}

- (void)setSearchString:(NSString *)aString
{
    searchString=aString;
}

@end
