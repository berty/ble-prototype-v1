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

-(void)viewWillAppear {
    [super viewWillAppear];
    [self.view.window setTitle:self.device.identifierString];
    
    NSLog(@"device %@", self.view.window);
    
    self.tableView.dataSource = self;
    self.tableView.delegate = self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do view setup here.
}

- (NSInteger)numberOfRowsInTableView:(NSTableView *)tableView {
    return [self.device.advData count];
}

- (nullable id)tableView:(NSTableView *)tableView objectValueForTableColumn:(NSTableColumn *)tableColumn row:(NSInteger)row {
    NSLog(@"TRYONG");
    if ([tableColumn.identifier isEqualToString:@"Key"]) {
        return self.device.advData.allKeys[row];
    } else if ([tableColumn.identifier isEqualToString:@"Value"]) {
        return self.device.advData.allValues[row];
    }
    return nil;
}

@end
