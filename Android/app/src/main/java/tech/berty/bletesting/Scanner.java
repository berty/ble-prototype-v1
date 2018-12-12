package tech.berty.bletesting;

import android.os.Build;
import android.annotation.TargetApi;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import static android.bluetooth.BluetoothProfile.GATT;
import static android.bluetooth.BluetoothProfile.GATT_SERVER;
import static android.content.Context.BLUETOOTH_SERVICE;


import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Scanner extends ScanCallback {
    private static final String TAG = "scan";

    Scanner() { super(); }

    static ScanSettings createScanSetting() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
    }

    static ScanFilter makeFilter() {
        return new ScanFilter.Builder()
                .setServiceUuid(BleManager.P_SERVICE_UUID)
                .build();
    }


    /**
     * Callback when a BLE advertisement has been found.
     *
     * @param callbackType Determines how this callback was triggered. Could be one of
     *                     {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
     *                     {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
     *                     {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
     * @param result       A Bluetooth LE scan result.
     */
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        Logger.put("debug", TAG, "onScanResult() called");
        Logger.put("debug", TAG, "With callbackType: " + callbackType + ", result: " + result);

        parseResult(result);
        super.onScanResult(callbackType, result);
    }

    /**
     * Callback when batch results are delivered.
     *
     * @param results List of scan results that are previously scanned.
     */
    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        Logger.put("debug", TAG, "onBatchScanResult() called");
        Logger.put("debug", TAG, "With results: " + results);

        for (ScanResult result:results) {
            parseResult(result);
        }
        super.onBatchScanResults(results);
    }

    /**
     * Callback when scan could not be started.
     *
     * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
     */
    @Override
    public void onScanFailed(int errorCode) {
        String errorString;

        switch(errorCode) {
            case SCAN_FAILED_ALREADY_STARTED: errorString = "SCAN_FAILED_ALREADY_STARTED";
                break;

            case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED: errorString = "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";
                break;

            case SCAN_FAILED_INTERNAL_ERROR: errorString = "SCAN_FAILED_INTERNAL_ERROR";
                break;

            case SCAN_FAILED_FEATURE_UNSUPPORTED: errorString = "SCAN_FAILED_FEATURE_UNSUPPORTED";
                break;

            default: errorString = "UNKNOWN_FAILURE";
                break;
        }
        Logger.put("error", TAG, "Scan failed: " + errorString);
        super.onScanFailed(errorCode);
    }

    private static void parseResult(ScanResult result) {
        Logger.put("debug", TAG, "parseResult() called with device: " + result.getDevice());

        BluetoothDevice device = result.getDevice();
        BluetoothManager bleManager = (BluetoothManager)MainActivity.getContext().getSystemService(BLUETOOTH_SERVICE);

        if (bleManager != null) {
            Logger.put("debug", TAG, "Client connection state: " + bleManager.getConnectionState(device, GATT));
            Logger.put("debug", TAG, "Server connection state: " + bleManager.getConnectionState(device, GATT_SERVER));
        } else {
            Logger.put("error", TAG, "BLE Manager is null");
        }

        MainActivity.getInstance().addDeviceToList(device.getAddress());
//        BertyDevice bertyDevice = DeviceManager.getDeviceFromAddr(device.getAddress());
//
//        if (bertyDevice == null) {
//            DeviceManager.addDeviceToIndex(new BertyDevice(device));
//        }
    }
}