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
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static MainActivity instance;

    static String multiAddr = "d5406e06-ad83-4622-a7af-38dbe8c4fabe";
    static String peerID = "QmNXdnmutcuwAG4BcvgrEwc3znArQc9JTq6ALFote1rBoU";

    Button startButton;
    Button stopButton;
    Button advButton;
    Button scanButton;
    LinearLayout table;

    Boolean advOn = false;
    static String advPlay = "Advertising   \u25B6";
    static String advStop = "Advertising   \u25A0";

    Boolean scanOn = false;
    static String scanPlay = "Scanning   \u25B6";
    static String scanStop = "Scanning   \u25A0";

    ArrayList<String> scanned;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final Activity currentActivity = this;

        startButton = findViewById(R.id.button1);
        stopButton = findViewById(R.id.button2);
        advButton = findViewById(R.id.button3);
        scanButton = findViewById(R.id.button4);
        table = findViewById(R.id.linearLayout);

        Manager.setContext(this.getApplicationContext());
        Manager.setMultiAddr(multiAddr);
        Manager.setPeerID(peerID);

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Manager.initBluetoothService(currentActivity)) {
                    toggleButtons(true);
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Manager.closeBluetoothService()) {
                    advButton.setText(advPlay);
                    scanButton.setText(scanPlay);
                    clearDeviceList();
                    toggleButtons(false);
                }
            }
        });

        advButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!advOn) {
                    advButton.setText(advStop);
                    Manager.startAdvertising();
                } else {
                    advButton.setText(advPlay);
                    Manager.stopAdvertising();
                }
                advOn = !advOn;
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!scanOn) {
                    clearDeviceList();
                    scanButton.setText(scanStop);
                    Manager.startScanning();
                } else {
                    scanButton.setText(scanPlay);
                    Manager.stopScanning();
                }
                scanOn = !scanOn;
            }
        });

        toggleButtons(false);
        instance = this;
    }

    public static MainActivity getInstance() {
        return instance;
    }

    public void toggleButtons(Boolean toggle) {
        startButton.setEnabled(!toggle);
        stopButton.setEnabled(toggle);
        advButton.setEnabled(toggle);
        scanButton.setEnabled(toggle);
    }

    public void clearDeviceList() {
        table.removeAllViews();
        scanned = new ArrayList<String>();
    }

    public void addDeviceToList(final String address) {
        if (!scanned.contains(address)) {
            scanned.add(address);
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
}
