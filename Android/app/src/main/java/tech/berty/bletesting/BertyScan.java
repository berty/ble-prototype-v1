package tech.berty.bletesting;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.SparseArray;

import java.util.List;
import java.nio.charset.StandardCharsets;

import static android.bluetooth.BluetoothProfile.GATT;
import static android.bluetooth.BluetoothProfile.GATT_SERVER;
import static android.content.Context.BLUETOOTH_SERVICE;
import static tech.berty.bletesting.BertyUtils.SERVICE_UUID;

@SuppressLint("LongLogTag")
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BertyScan extends ScanCallback {
    private static final String TAG = "scan";

    public Context mContext;

    public BertyGatt mGattCallback;

    public static ScanSettings createScanSetting() {
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        return settings;
    }

    public static ScanFilter makeFilter() {
        ParcelUuid pUuid = new ParcelUuid(SERVICE_UUID);
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(pUuid)
                .build();

        return filter;
    }

    public BertyScan() {
        super();
        Thread.currentThread().setName("BertyScan");
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

            default: errorString = "UNKNOW FAIL";
                break;
        }
        BertyUtils.logger("error", TAG, "scanning failed: " + errorString);
        super.onScanFailed(errorCode);
    }

    public void parseResult(ScanResult result) {
        MainActivity.getInstance().addDeviceToList(result.getDevice().getAddress());

//        BluetoothDevice device = result.getDevice();
//        BertyDevice bDevice = BertyUtils.getDeviceFromAddr(device.getAddress());
//        BertyUtils.logger("debug", TAG, "parseResult() called " + bDevice);
//        if (bDevice == null) {
//            BluetoothManager mb = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
//            Log.e(TAG, "CONN STATE  " + mb.getConnectionState(device, GATT));
//            Log.e(TAG, "CONN STATE  " + mb.getConnectionState(device, GATT_SERVER));
//            BertyUtils.addDevice(device, mContext, mGattCallback);
//        }
    }
}