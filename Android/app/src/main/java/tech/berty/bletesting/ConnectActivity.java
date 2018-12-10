package tech.berty.bletesting;

import android.content.Context;
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

    public BertyGatt mGattCallback;
    public Context mContext;

    String address;
    BertyDevice bertydDevice;

    TextView addressTxt;
    Button connButton;
    Button sendButton;
    EditText dataInput;
    LinearLayout table;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        table = findViewById(R.id.linearLayout);

        toggleButtons(isConnected());

        connButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bertydDevice = BertyUtils.getDeviceFromAddr(address);
                if (bertydDevice == null) {
                    BluetoothDevice device = Manager.getAdapter().getRemoteDevice(address);
                    BluetoothManager manager = (BluetoothManager)instance.getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
                    BertyUtils.logger("debug", TAG, "CONN STATE  " + manager.getConnectionState(device, GATT));
                    BertyUtils.logger("debug", TAG, "CONN STATE  " + manager.getConnectionState(device, GATT_SERVER));
                    BertyUtils.addDevice(device, mContext, mGattCallback);
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String data = dataInput.getText().toString();
                Manager.getInstance().write(data.getBytes(), bertydDevice);
            }
        });

        instance = this;
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

    public void putLogs(String level, String tag, String log) {
        TextView text = new TextView(this);
        text.setText(tag + ": " + log);
        text.setTextSize(20);

        switch (level) {
            case "debug":  text.setTextColor(Color.DKGRAY);
                break;
            case "info":  text.setTextColor(Color.GREEN);
                break;
            case "warn":  text.setTextColor(Color.YELLOW);
                break;
            case "error":  text.setTextColor(Color.RED);
                break;
            default: text.setTextColor(Color.BLUE);
                break;
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0,0,0,10);
        text.setLayoutParams(params);

        table.addView(text);
    }
}
