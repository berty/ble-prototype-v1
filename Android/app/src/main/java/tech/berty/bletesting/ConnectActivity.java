package tech.berty.bletesting;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;

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
    private Button connButton;
    private Button sendButton;
    private EditText dataInput;
    private ScrollView scroll;
    private LinearLayout table;

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
        toggleButtons(isConnected());

        // Connect / disconnect on click on connButton (toggle)
        connButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isConnected()) {
                    bertyDevice.disconnect();
                } else {
                    bertyDevice.connect();
                }
                toggleButtons(isConnected());
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
        instance = this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    static ConnectActivity getInstance() {
        return instance;
    }

    // Method that check if devices are connected
    private Boolean isConnected() {
        BluetoothManager manager = (BluetoothManager)getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
        if (manager == null) {
            Logger.put("error", TAG, "Can't get BLE Manager");
            return false;
        }

        BluetoothDevice device = bertyDevice.getDevice();
        if (device == null) {
            Logger.put("error", TAG, "Can't get bluetooth device");
            return false;
        }

        int gattClientState = manager.getConnectionState(device, GATT);
        int gattServerState = manager.getConnectionState(device, GATT_SERVER);

        Logger.put("debug", TAG, "Device " + device.toString());
        Logger.put("debug", TAG, "GATT client connection state: " + Logger.connectionStateToString(gattClientState));
        Logger.put("debug", TAG, "GATT server connection state: " + Logger.connectionStateToString(gattServerState));

        return (gattClientState == STATE_CONNECTED && gattServerState == STATE_CONNECTED);
    }

    // Method that disable / enable buttons and change text if connected
    private void toggleButtons(Boolean connected) {
        multiAddrTxt.setText((connected ? "MultiAddr: " + bertyDevice.getMultiAddr() : "MultiAddr: N/A"));
        peerIDTxt.setText((connected ? "PeerID: " + bertyDevice.getPeerID() : "PeerID: N/A"));
        connButton.setText(connected ? "Disconnect" : "Connect");
        sendButton.setEnabled(connected);
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
