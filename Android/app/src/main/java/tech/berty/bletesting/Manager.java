package tech.berty.bletesting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;

import java.util.Arrays;
import java.util.HashMap;

import static android.content.Context.BLUETOOTH_SERVICE;
import static tech.berty.bletesting.BertyUtils.BLUETOOTH_ENABLE_REQUEST;

public final class Manager {
    private static String TAG = "manager";

    private static String mMultiAddr;
    private static String mPeerID;
    private static Context mContext;

    private static boolean bluetoothReady;
    private static boolean advertising;
    private static boolean scanning;

    private static HashMap<String, BertyDevice> bertyDevices = new HashMap<>();

    private static BertyGattServer mGattServerCallback = new BertyGattServer();
    private static BertyAdvertise mAdvertisingCallback = new BertyAdvertise();
    private static BertyGatt mGattCallback = new BertyGatt();
    private static BertyScan mScanCallback = new BertyScan();

    private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothLeAdvertiser mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    private static BluetoothLeScanner mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

    private Manager() {
        BertyUtils.logger("debug", TAG, "Manager constructor called");

        Thread.currentThread().setName("BleManager");
    }

    // Setters
    public static void setMultiAddr(String multiAddr) {
        BertyUtils.logger("debug", TAG, "MultiAddr set: " + multiAddr);

        mMultiAddr = multiAddr;
        BertyUtils.maCharacteristic.setValue(multiAddr);
    }
    public static void setPeerID(String peerID) {
        BertyUtils.logger("debug", TAG, "PeerID set: " + peerID);

        mPeerID = peerID;
        BertyUtils.peerIDCharacteristic.setValue(mPeerID);
    }
    public static void setContext(Context context) {
        BertyUtils.logger("debug", TAG, "Context set: " + context);

        mContext = context;
    }

    // Getters
    public static String getMultiAddr() { return mMultiAddr; }
    public static String getPeerID() { return mPeerID; }
    public static Context getContext() { return mContext; }

    public static BertyGattServer getGattServerCallback() { return mGattServerCallback; }
    public static BertyAdvertise getAdvertisingCallback() { return mAdvertisingCallback; }
    public static BertyGatt getGattCallback() { return mGattCallback; }
    public static BertyScan getScanCallback() { return mScanCallback; }

    public static BluetoothAdapter getAdapter() { return mBluetoothAdapter; }
    public static BluetoothLeAdvertiser getBluetoothLeAdvertiser() { return mBluetoothLeAdvertiser; }
    public static BluetoothLeScanner getBluetoothLeScanner() { return mBluetoothLeScanner; }

    // State related
    private static boolean isManagerReady() {
        if (mMultiAddr == null) {
            BertyUtils.logger("error", TAG, "Manager isn't ready: mMultiAddr not set");
            return false;
        }
        if (mPeerID == null) {
            BertyUtils.logger("error", TAG, "Manager isn't ready: mPeerID not set");
            return false;
        }
        if (mContext == null) {
            BertyUtils.logger("error", TAG, "Manager isn't ready: mContext not set");
            return false;
        }
        return true;
    }

    private static boolean isBluetoothReady() {
        if (!bluetoothReady) {
            BertyUtils.logger("error", TAG, "Bluetooth Service not inited yet");
        }
        return bluetoothReady;
    }

    private static boolean isAdvertising() {
        if (!advertising) {
            BertyUtils.logger("error", TAG, "Not currently advertising");
        }
        return advertising;
    }

    private static boolean isScanning() {
        if (!scanning) {
            BertyUtils.logger("error", TAG, "Not currently scanning");
        }
        return scanning;
    }

    // Bluetooth service related
    public static boolean initBluetoothService(Activity currentActivity) {
        BertyUtils.logger("debug", TAG, "initBluetoothService() called");

        if (!isManagerReady()) { return false; }

        // Turn on Bluetooth adapter
        if (!mBluetoothAdapter.isEnabled()) {
            BertyUtils.logger("debug", TAG, "Bluetooth adapter is off: turning it on");

            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            currentActivity.startActivityForResult(enableBluetoothIntent, BLUETOOTH_ENABLE_REQUEST);
        }

        // Check Location permission (needed by BLE)
        if (ContextCompat.checkSelfPermission(currentActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            BertyUtils.logger("debug", TAG, "Location permission isn't granted: requesting it");

            ActivityCompat.requestPermissions(currentActivity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    99);
        }

        try {
            BluetoothManager bleManager = (BluetoothManager)mContext.getSystemService(BLUETOOTH_SERVICE);
            BertyUtils.logger("debug", TAG, "BLE Manager: " + bleManager);

            BluetoothGattService bertyService = BertyUtils.createService();
            BertyUtils.logger("debug", TAG, "Berty Service: " + bertyService);

            BluetoothGattServer gattServer = bleManager.openGattServer(mContext, mGattServerCallback);
            gattServer.addService(bertyService);
            mGattServerCallback.setBluetoothGattServer(gattServer);
            BertyUtils.logger("debug", TAG, "GATT Server: " + gattServer);

            bluetoothReady = true;

            return true;
        } catch (Exception e) {
            BertyUtils.logger("error", TAG, "Error: " + e);
            return false;
        }
    }

    public static boolean closeBluetoothService() {
        BertyUtils.logger("debug", TAG, "closeBluetoothService() called");

        if (!isBluetoothReady()) { return false; }

        stopScanning();
        stopAdvertising();
        mGattServerCallback.getBluetoothGattServer().close();

        bluetoothReady = false;

        return true;
    }

    // Advertise related
    public static boolean startAdvertising() {
        BertyUtils.logger("debug", TAG, "startAdvertising() called");

        if (!isBluetoothReady()) { return false; }
        if (advertising) { return true; }

        AdvertiseSettings settings = BertyAdvertise.buildAdvertiseSettings(true, AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY, AdvertiseSettings.ADVERTISE_TX_POWER_HIGH, 0);
        BertyUtils.logger("debug", TAG, "Advertise settings: " + settings);

        AdvertiseData data = BertyAdvertise.buildAdvertiseData();
        BertyUtils.logger("debug", TAG, "Advertise data: " + data);

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertisingCallback);

        advertising = true;

        return true;
    }

    public static boolean stopAdvertising() {
        BertyUtils.logger("debug", TAG, "stopAdvertising() called");

        if (!isBluetoothReady() || !isAdvertising()) { return false; }

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertisingCallback);

        advertising = false;

        return true;
    }

    // Scan related
    public static boolean startScanning() {
        BertyUtils.logger("debug", TAG, "startScanning() called");

        if (!isBluetoothReady()) { return false; }
        if (scanning) { return true; }

        ScanSettings settings = BertyScan.createScanSetting();
        BertyUtils.logger("debug", TAG, "Scan settings: " + settings);

        ScanFilter filter = BertyScan.makeFilter();
        BertyUtils.logger("debug", TAG, "Scan filter: " + filter);

        mBluetoothLeScanner.startScan(Arrays.asList(filter), settings, mScanCallback);

        scanning = true;

        return true;
    }

    public static boolean stopScanning() {
        BertyUtils.logger("debug", TAG, "stopScanning() called");

        if (!isBluetoothReady() || !isScanning()) { return false; }

        mBluetoothLeScanner.stopScan(mScanCallback);

        scanning = false;

        return true;
    }


    // Write related
    public static boolean write(byte[] blob, String multiAddr) {
        BertyDevice bertyDevice = BertyUtils.getDeviceFromMa(multiAddr);

        return write(blob, bertyDevice);
    }

    public static boolean write(byte[] blob, BertyDevice bertyDevice) {
        BertyUtils.logger("debug", TAG, "write() called");

        if (bertyDevice == null) {
            BertyUtils.logger("error", TAG, "Can't write: unknown device");
            return false;
        }

        try {
            bertyDevice.write(blob);
            return true;
        } catch (Exception e) {
            BertyUtils.logger("error", TAG, "Can't write: " + e);
            return false;
        }
    }
}