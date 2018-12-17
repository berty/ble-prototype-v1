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

public class MainActivity extends AppCompatActivity {
    private static String TAG = "main_activity";

    private static MainActivity instance;

    private Button startButton;
    private Button stopButton;
    private Button advButton;
    private Button scanButton;
    private LinearLayout table;

    private static String advPlay = "Advertising   \u25B6";
    private static String advStop = "Advertising   \u25A0";

    private static String scanPlay = "Scanning   \u25B6";
    private static String scanStop = "Scanning   \u25A0";

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
                if (BleManager.isNotAdvertising() && BleManager.startAdvertising()) {
                    advButton.setText(advStop);
                } else if (BleManager.stopAdvertising()) {
                    advButton.setText(advPlay);
                }
            }
        });

        // Scan button (toggle)
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BleManager.isNotScanning() && BleManager.startScanning()) {
                    clearDeviceList();
                    scanButton.setText(scanStop);
                } else if (BleManager.stopScanning()) {
                    scanButton.setText(scanPlay);
                }
            }
        });

        toggleButtons(!BleManager.isBluetoothNotReady());
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppData.setCurrContext(getApplicationContext());
        instance = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        instance = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
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

        advButton.setText(BleManager.isNotAdvertising() ? advPlay : advStop);
        scanButton.setText(BleManager.isNotScanning() ? scanPlay : scanStop);
    }

    // Method that disable clear device list
    private void clearDeviceList() {
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
}
