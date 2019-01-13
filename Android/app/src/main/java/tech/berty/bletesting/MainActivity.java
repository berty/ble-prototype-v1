package tech.berty.bletesting;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.graphics.Color;
import android.view.View;
import android.content.pm.PackageManager;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "main_activity";

    private static MainActivity instance;

    static Button startButton;
    static Button stopButton;
    static Button advButton;
    static Button scanButton;
    private static LinearLayout table;

    static String advPlay = "Advertising   \u25B6";
    static String advStop = "Advertising   \u25A0";

    static String scanPlay = "Scanning   \u25B6";
    static String scanStop = "Scanning   \u25A0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String multiAddr = "d5406e06-ad83-4622-a7af-38dbe8c4fabe";
        String peerID = "QmNXdnXuTc0wAG4Bc4grEwc3znArQc9JTq6ALFo7e1rBoU";

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final Activity currentActivity = this;
        AppData.setCurrContext(getApplicationContext());
        instance = this;

        // Get reference / set text for interface elements
        startButton = findViewById(R.id.button1);
        startButton.setText("Start BLE");
        stopButton = findViewById(R.id.button2);
        stopButton.setText("Stop BLE");
        advButton = findViewById(R.id.button3);
        scanButton = findViewById(R.id.button4);
        table = findViewById(R.id.linearLayout);

        // Display all already scanned device
        for (String device: AppData.getDeviceList()) {
            addDeviceToList(device);
        }

        // Set own MultiAddr and PeerID in BleManager
        BleManager.setMultiAddr(multiAddr);
        BleManager.setPeerID(peerID);

        // Start BLE button
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (BleManager.initBluetoothService(currentActivity)) {
                    toggleButtons(true);
                }
            }
        });

        // Stop BLE button
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (BleManager.closeBluetoothService()) {
                    clearDeviceList();
                    toggleButtons(false);
                }
            }
        });

        // Advertise button (toggle)
        advButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BleManager.isAdvertising()) {
                    BleManager.stopAdvertising();
                } else {
                    BleManager.startAdvertising();
                }
            }
        });

        // Scan button (toggle)
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BleManager.isScanning()) {
                    BleManager.stopScanning();
                    clearDeviceList();
                } else {
                    BleManager.startScanning();
                }
            }
        });

        toggleButtons(BleManager.isBluetoothReady());
        BleManager.isBleAdvAndScanCompatible();
        startListRefresher();
    }

    @Override
    protected void onResume() {
        AppData.setCurrContext(getApplicationContext());
        instance = this;
        super.onResume();
    }

    @Override
    protected void onPause() {
        instance = null;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult() called");
        if (requestCode == BleManager.BLUETOOTH_ENABLE_REQUEST) {
           if (requestCode == RESULT_OK) {
               BleManager.bleTurnedOn = true;
           }
           BleManager.waitRequestResponse.release();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.e(TAG, "onRequestPermissionsResult() called");
        if (requestCode == BleManager.LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                BleManager.permGranted = true;
            }
            BleManager.waitRequestResponse.release();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    static MainActivity getInstance() {
        return instance;
    }

    // Method that disable / enable buttons
    private void toggleButtons(boolean bleReady) {
        startButton.setEnabled(!bleReady);
        stopButton.setEnabled(bleReady);
        advButton.setEnabled(bleReady);
        scanButton.setEnabled(bleReady);

        advButton.setText(BleManager.isAdvertising() ? advStop : advPlay);
        scanButton.setText(BleManager.isScanning() ? scanStop : scanPlay);
    }

    // Method that disable clear device list
    void clearDeviceList() {
        table.removeAllViews();
        AppData.clearDeviceList();
    }

    // Method that append new device in the list
    void addDeviceToList(final String address) {
        TextView text = new TextView(this);
        text.setText(address);
        text.setTextSize(25);
        text.setTextColor(Color.BLUE);

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        text.setLayoutParams(params);

        text.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent connIntent = new Intent(getApplicationContext(), ConnectActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("address", address);
                connIntent.putExtras(bundle);
                startActivityForResult(connIntent, 0);
            }
        });
        table.addView(text);
    }

    private void startListRefresher() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        AppData.waitUpdate.acquire();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                table.removeAllViews();
                                for(String device: AppData.getDeviceList()) {
                                    addDeviceToList(device);
                                }
                            }
                        });
                        Thread.sleep(840);
                        AppData.waitUpdate.release();
                        Thread.sleep(420);
                    } catch (Exception e) {
                    }
                }
            }
        }).start();
    }}
