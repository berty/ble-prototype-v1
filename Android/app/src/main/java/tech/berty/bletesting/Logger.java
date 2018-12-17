package tech.berty.bletesting;

import android.util.Log;

import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTING;

class Logger {

    private Logger() {}

    static void put(String level, String tag, String log) {
        switch (level) {
            case "debug":
                Log.d(tag, log);
                break;
            case "info":
                Log.i(tag, log);
                break;
            case "warn":
                Log.w(tag, log);
                break;
            case "error":
                Log.e(tag, log);
                break;
            default:
                Log.e(tag, log);
                break;
        }
    }

    static String connectionStateToString(int state) {
        switch (state) {
            case STATE_DISCONNECTED:
                return "disconnected";
            case STATE_CONNECTING:
                return "connecting";
            case STATE_CONNECTED:
                return "connected";
            case STATE_DISCONNECTING:
                return "disconnecting";
            default:
                return "unknown";
        }
    }
}