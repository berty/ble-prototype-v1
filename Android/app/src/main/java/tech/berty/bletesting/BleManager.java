package tech.berty.bletesting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Build;
import android.annotation.TargetApi;

import android.bluetooth.BluetoothGattCharacteristic;
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

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;
import static android.content.Context.BLUETOOTH_SERVICE;

import java.util.Collections;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
final class BleManager {
    private static String TAG = "ble_manager";

    private static boolean bluetoothReady;
    private static boolean advertising;
    private static boolean scanning;

    private static GattServer mGattServerCallback = new GattServer();
    private static Advertiser mAdvertisingCallback = new Advertiser();
    private static GattClient mGattCallback = new GattClient();
    private static Scanner mScanCallback = new Scanner();

    private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothLeAdvertiser mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    private static BluetoothLeScanner mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

    private static final int BLUETOOTH_ENABLE_REQUEST = 1;

    static final UUID SERVICE_UUID = UUID.fromString("A06C6AB8-886F-4D56-82FC-2CF8610D6669");
    static final UUID WRITER_UUID = UUID.fromString("000CBD77-8D30-4EFF-9ADD-AC5F10C2CC1C");
    static final UUID MA_UUID = UUID.fromString("9B827770-DC72-4C55-B8AE-0870C7AC15A8");
    static final UUID PEER_ID_UUID = UUID.fromString("0EF50D30-E208-4315-B323-D05E0A23E6B3");
    static final ParcelUuid P_SERVICE_UUID = new ParcelUuid(SERVICE_UUID);

    private static final BluetoothGattService mService = new BluetoothGattService(SERVICE_UUID, SERVICE_TYPE_PRIMARY);
    private static final BluetoothGattCharacteristic maCharacteristic = new BluetoothGattCharacteristic(MA_UUID, PROPERTY_WRITE, PERMISSION_WRITE);
    private static final BluetoothGattCharacteristic peerIDCharacteristic = new BluetoothGattCharacteristic(PEER_ID_UUID, PROPERTY_WRITE, PERMISSION_WRITE);
    private static final BluetoothGattCharacteristic writerCharacteristic = new BluetoothGattCharacteristic(WRITER_UUID, PROPERTY_WRITE, PERMISSION_WRITE);

    private BleManager() {
        Logger.put("debug", TAG, "BleManager constructor called");
        Thread.currentThread().setName("BleManager");
    }

    // Compatibility checker
    static boolean isBleAdvAndScanCompatible() {
        if (mBluetoothAdapter == null) {
            Logger.put("error", TAG, "Device doesn't support Bluetooth");
        } else if (!mBluetoothAdapter.isEnabled()) {
            Logger.put("error", TAG, "Bluetooth is disabled");
        } else if (mBluetoothLeScanner == null) {
            Logger.put("error", TAG, "Device doesn't support BLE scanning");
        } else if (mBluetoothLeAdvertiser == null) {
            Logger.put("error", TAG, "Device doesn't support BLE advertising. MultipleAdvertisementSupported: " + mBluetoothAdapter.isMultipleAdvertisementSupported());
        } else {
            Logger.put("info", TAG, "Bluetooth adapter is turned on and BLE advertising / scanning are supported");
            return true;
        }

        return false;
    }

    // Setters
    static void setMultiAddr(String multiAddr) {
        Logger.put("info", TAG, "Own multiAddr set: " + multiAddr);

        maCharacteristic.setValue(multiAddr);
    }

    static void setPeerID(String peerID) {
        Logger.put("info", TAG, "Own peerID set: " + peerID);

        peerIDCharacteristic.setValue(peerID);
    }

    static void setAdvertisingState(boolean state) {
        Logger.put("debug", TAG, "setAdvertisingState() called with state: " + state);

        advertising = state;
    }

    static void setScanningState(boolean state) {
        Logger.put("debug", TAG, "setScanningState() called with state: " + state);

        scanning = state;
    }

    // Getters
    static String getMultiAddr() { return maCharacteristic.getStringValue(0); }

    static String getPeerID() { return peerIDCharacteristic.getStringValue(0); }

    static GattClient getGattCallback() { return mGattCallback; }

    static BluetoothAdapter getAdapter() { return mBluetoothAdapter; }


    // State related
    static boolean isBluetoothReady() {
        if (!bluetoothReady) {
            Logger.put("debug", TAG, "Bluetooth Service not initialized yet");
        }

        return bluetoothReady;
    }

    static boolean isAdvertising() {
        if (!advertising) {
            Logger.put("debug", TAG, "Not currently advertising");
        }

        return advertising;
    }

    static boolean isScanning() {
        if (!scanning) {
            Logger.put("debug", TAG, "Not currently scanning");
        }

        return scanning;
    }


    // Bluetooth service related
    static boolean initBluetoothService(Activity currentActivity) {
        Logger.put("debug", TAG, "initBluetoothService() called");
        Context context = AppData.getCurrContext();

        // Turn on Bluetooth adapter
        if (!mBluetoothAdapter.isEnabled()) {
            Logger.put("debug", TAG, "initBluetoothService() Bluetooth adapter is off: turning it on");

            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            currentActivity.startActivityForResult(enableBluetoothIntent, BLUETOOTH_ENABLE_REQUEST);
        }

        // Check Location permission (needed by BLE)
        if (ContextCompat.checkSelfPermission(currentActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Logger.put("debug", TAG, "initBluetoothService() location permission isn't granted: requesting it");

            ActivityCompat.requestPermissions(currentActivity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    99);
        }

        try {
            BluetoothManager bleManager = (BluetoothManager)context.getSystemService(BLUETOOTH_SERVICE);
            if (bleManager == null) {
                Logger.put("error", TAG, "initBluetoothService() failed: BLE Manager is null");
                return false;
            }

            BluetoothGattService bertyService = createService();
            Logger.put("debug", TAG, "initBluetoothService() service created: " + bertyService);

            BluetoothGattServer gattServer = bleManager.openGattServer(context, mGattServerCallback);
            gattServer.addService(bertyService);
            mGattServerCallback.setBluetoothGattServer(gattServer);

            bluetoothReady = true;
            Logger.put("info", TAG, "initBluetoothService() succeeded");

            return true;
        } catch (Exception e) {
            Logger.put("error", TAG, "initBluetoothService() failed: " + e.getMessage());
            return false;
        }
    }

    static boolean closeBluetoothService() {
        Logger.put("debug", TAG, "closeBluetoothService() called");

        if (!isBluetoothReady()) {
            Logger.put("warn", TAG, "closeBluetoothService() canceled");
            return false;
        }

        if (scanning) stopScanning();
        if (advertising) stopAdvertising();
        mGattServerCallback.closeGattServer();

        bluetoothReady = false;

        return true;
    }

    private static BluetoothGattService createService() {
        Logger.put("debug", TAG, "createService() called");

        if (!mService.addCharacteristic(maCharacteristic) ||
            !mService.addCharacteristic(peerIDCharacteristic) ||
            !mService.addCharacteristic(writerCharacteristic)) {
            Logger.put("error", TAG, "Characteristic adding failed");
        }

        return mService;
    }


    // Advertise related
    static void startAdvertising() {
        Logger.put("debug", TAG, "startAdvertising() called");

        if (!isBluetoothReady()) {
            Logger.put("warn", TAG, "startAdvertising() canceled");
            return;
        }

        AdvertiseSettings settings = Advertiser.buildAdvertiseSettings(true, AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY, AdvertiseSettings.ADVERTISE_TX_POWER_HIGH, 0);
        AdvertiseData data = Advertiser.buildAdvertiseData();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertisingCallback);
    }

    static void stopAdvertising() {
        Logger.put("debug", TAG, "stopAdvertising() called");

        if (!isBluetoothReady() || !isAdvertising()) {
            Logger.put("warn", TAG, "stopAdvertising() canceled");
            return;
        }

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertisingCallback);
        setAdvertisingState(false);
    }


    // Scan related
    static void startScanning() {
        Logger.put("debug", TAG, "startScanning() called");

        if (!isBluetoothReady()) {
            Logger.put("warn", TAG, "startScanning() canceled");
            return;
        }

        ScanSettings settings = Scanner.createScanSetting();
        Logger.put("debug", TAG, "Scan settings: " + settings);

        ScanFilter filter = Scanner.makeFilter();
        Logger.put("debug", TAG, "Scan filter: " + filter);

        mBluetoothLeScanner.startScan(Collections.singletonList(filter), settings, mScanCallback);
    }

    static void stopScanning() {
        Logger.put("debug", TAG, "stopScanning() called");

        if (!isBluetoothReady() || !isScanning()) {
            Logger.put("warn", TAG, "stopScanning() canceled");
            return;
        }

        mBluetoothLeScanner.stopScan(mScanCallback);
        setScanningState(false);
    }
}