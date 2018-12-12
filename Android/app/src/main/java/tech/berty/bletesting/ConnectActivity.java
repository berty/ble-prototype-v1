package tech.berty.bletesting;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;

import java.nio.charset.Charset;

import static android.bluetooth.BluetoothProfile.GATT;
import static android.bluetooth.BluetoothProfile.GATT_SERVER;

public class ConnectActivity extends AppCompatActivity {
    public static String TAG = "connect_activity";
    private static ConnectActivity instance;

    String address;
    BertyDevice bertydDevice;

    TextView addressTxt;
    Button connButton;
    Button sendButton;
    EditText dataInput;
    LinearLayout table;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.put("debug", TAG, "TEST 1");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Bundle bundle = getIntent().getExtras();
        address = bundle.getString("address");

        addressTxt = findViewById(R.id.textView1b);
        addressTxt.setText(address);

        connButton = findViewById(R.id.button1b);
        sendButton = findViewById(R.id.button2b);
        dataInput = findViewById(R.id.editText);
        table = findViewById(R.id.linearLayout1b);

        bertydDevice = DeviceManager.getDeviceFromAddr(address);
        toggleButtons(isConnected());

        connButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!isConnected()) {
                    BluetoothDevice device = BleManager.getAdapter().getRemoteDevice(address);
                    BluetoothManager manager = (BluetoothManager)instance.getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
                    BertyDevice bertyDevice = new BertyDevice(device);
                    Logger.put("debug", TAG, "Device " + device.toString());
                    Logger.put("debug", TAG, "CONN STATE  " + manager.getConnectionState(device, GATT));
                    Logger.put("debug", TAG, "CONN STATE  " + manager.getConnectionState(device, GATT_SERVER));
                    DeviceManager.addDeviceToIndex(bertyDevice);
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String data = dataInput.getText().toString();
                DeviceManager.write(data.getBytes(), bertydDevice);
            }
        });

        instance = this;
        Logger.put("debug", TAG, "TEST 2");

    }


    public static ConnectActivity getInstance() {
        return instance;
    }

    public Boolean isConnected() {
        return bertydDevice != null;
    }

    public void toggleButtons(Boolean connected) {
        connButton.setText(connected ? "Disconnect" : "Connect");
        sendButton.setEnabled(connected);
    }

    public void putMessage(byte[] message) {
        TextView text = new TextView(this);
        text.setText(new String(message, Charset.forName("UTF-8")));
        text.setTextSize(20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0,0,0,10);
        text.setLayoutParams(params);

        table.addView(text);
    }
}
