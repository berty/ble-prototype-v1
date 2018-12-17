package tech.berty.bletesting;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.graphics.Color;

import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;

import static android.bluetooth.BluetoothProfile.GATT;
import static android.bluetooth.BluetoothProfile.GATT_SERVER;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

import java.util.ArrayList;

public class ConnectActivity extends AppCompatActivity {
    private static String TAG = "connect_activity";

    private static ConnectActivity instance;

    private BertyDevice bertyDevice;
    private String address;

    private TextView addressTxt;
    private TextView multiAddrTxt;
    private TextView peerIDTxt;

    private TextView gattClientTxt;
    private TextView gattServerTxt;
    private TextView gattClientState;
    private TextView gattServerState;
    private String connTxt = "\u2714";
    private String disconnTxt = "\u2718";
    private String connColor = "#009933";
    private String disconnColor = "#CC0000";

    private Button connButton;
    private Button sendButton;
    private EditText dataInput;
    private ScrollView scroll;
    private LinearLayout table;

    private Thread connectionWatcher;
    private boolean gattClientConnected;
    private boolean gattServerConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        AppData.setCurrContext(getApplicationContext());
        instance = this;

        // Setup top bar with back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get address sent from MainActivity through a bundle
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            address = bundle.getString("address");
        } else {
            Logger.put("error", TAG, "Can't get bundle from MainActivity");
        }

        // Get reference / set text for interface elements
        addressTxt = findViewById(R.id.textView1b);
        addressTxt.setText("Address: " + address);
        multiAddrTxt = findViewById(R.id.textView2b);
        peerIDTxt = findViewById(R.id.textView3b);
        gattClientTxt = findViewById(R.id.textView4b);
        gattClientTxt.setText("GATT Client:");
        gattClientState = findViewById(R.id.textView5b);
        gattServerTxt = findViewById(R.id.textView6b);
        gattServerTxt.setText("GATT Server:");
        gattServerState = findViewById(R.id.textView7b);
        connButton = findViewById(R.id.button1b);
        sendButton = findViewById(R.id.button2b);
        sendButton.setText("Send data");
        dataInput = findViewById(R.id.editText);
        dataInput.setHint("Bytes (less than or equal 20)");
        scroll = findViewById(R.id.scrollView1b);
        table = findViewById(R.id.linearLayout1b);

        // Display all already scanned device
        ArrayList<String> messageList = AppData.getMessageList(address);
        if (messageList != null) {
            for (String message: messageList) {
                putMessage(message);
            }
        }

        // Add device to DeviceManger index if not registered yet
        bertyDevice = DeviceManager.getDeviceFromAddr(address);

        if (bertyDevice == null) {
            BluetoothDevice device = BleManager.getAdapter().getRemoteDevice(address);
            bertyDevice = new BertyDevice(device);
            DeviceManager.addDeviceToIndex(bertyDevice);
        }

        // Set buttons in the right state depending if devices are connected or not
        toggleButtons();

        // Connect / disconnect on click on connButton (toggle)
        connButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (gattClientConnected || gattServerConnected) {
                    bertyDevice.disconnect();
                } else {
                    bertyDevice.connect();
                }
                toggleButtons();
            }
        });

        // Send data from dataInput on click on connButton
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String data = dataInput.getText().toString();

                if (DeviceManager.write(data.getBytes(), bertyDevice.getMultiAddr())) {
                    AppData.addMessageToList(address,"Sent: " + data);
                    dataInput.setText("");
                } else {
                    AppData.addMessageToList(address,"Not sent: " + data);
                }
            }
        });

        startConnectionWatcher();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppData.setCurrContext(getApplicationContext());
        if (connectionWatcher != null && !connectionWatcher.isAlive()) {
            connectionWatcher.start();
        } else {
            startConnectionWatcher();
        }
        instance = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (connectionWatcher != null && connectionWatcher.isAlive()) {
            connectionWatcher.stop();
        }
        instance = this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionWatcher != null && connectionWatcher.isAlive()) {
            connectionWatcher.stop();
        }
        instance = null;
    }

    static ConnectActivity getInstance() {
        return instance;
    }

    // Method that check if devices are connected
    private void startConnectionWatcher() {
        final BluetoothManager manager = (BluetoothManager)getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
        if (manager == null) {
            Logger.put("error", TAG, "Can't get BLE Manager");
            return;
        }

        final BluetoothDevice device = bertyDevice.getDevice();
        if (device == null) {
            Logger.put("error", TAG, "Can't get bluetooth device");
            return;
        }

        connectionWatcher = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int prevGattClientState = -1, prevGattServerState = -1;

                    while (!connectionWatcher.isInterrupted()) {
                        int gattClientState = manager.getConnectionState(device, GATT);
                        int gattServerState = manager.getConnectionState(device, GATT_SERVER);

                        if (prevGattClientState != gattClientState || prevGattServerState != gattServerState) {
                            Logger.put("debug", TAG, "Device " + device.toString());
                            Logger.put("debug", TAG, "GATT client connection state: " + Logger.connectionStateToString(gattClientState));
                            Logger.put("debug", TAG, "GATT server connection state: " + Logger.connectionStateToString(gattServerState));

                            prevGattClientState = gattClientState;
                            prevGattServerState = gattServerState;
                        }

                        gattClientConnected = gattClientState == STATE_CONNECTED;
                        gattServerConnected = gattServerState == STATE_CONNECTED;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                toggleButtons();
                            }
                        });

                        Thread.sleep(250);
                    }
                } catch (Exception e) {
                    Logger.put("error", TAG, "Failed in connectionWatcher: " + e.getMessage());
                }
            }
        });

        connectionWatcher.start();
    }

    // Method that disable / enable buttons and change text if connected
    private void toggleButtons() {
        multiAddrTxt.setText((bertyDevice.getMultiAddr() != null ? "MultiAddr: " + bertyDevice.getMultiAddr() : "MultiAddr: N/A"));
        peerIDTxt.setText((bertyDevice.getPeerID() != null ? "PeerID: " + bertyDevice.getPeerID() : "PeerID: N/A"));

        gattClientState.setText(gattClientConnected ? connTxt : disconnTxt);
        gattClientState.setTextColor(Color.parseColor( gattClientConnected ? connColor : disconnColor));
        gattServerState.setText(gattServerConnected ? connTxt : disconnTxt);
        gattServerState.setTextColor(Color.parseColor( gattServerConnected ? connColor : disconnColor));

        connButton.setText(gattClientConnected && gattServerConnected ? "Disconnect" : "Connect");
        sendButton.setEnabled(gattClientConnected && gattServerConnected);
    }

    // Methods that display sent / received messages
    void putMessage(String dest, String message) {
        if (dest.equals(address)) {
            putMessage(message);
        }
    }

    private void putMessage(String message) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0,0,0,10);
        text.setLayoutParams(params);

        table.addView(text);
        scroll.fullScroll(View.FOCUS_DOWN);
    }
}
