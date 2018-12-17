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

    static final UUID SERVICE_UUID = UUID.fromString("A06C6AB8-886F-4D56-82FC-2CF8610D6664");
    static final UUID WRITER_UUID = UUID.fromString("000CBD77-8D30-4EFF-9ADD-AC5F10C2CC1C");
    static final UUID CLOSER_UUID = UUID.fromString("AD127A46-D065-4D72-B15A-EB2B3DA20561");
    static final UUID MA_UUID = UUID.fromString("9B827770-DC72-4C55-B8AE-0870C7AC15A8");
    static final UUID PEER_ID_UUID = UUID.fromString("0EF50D30-E208-4315-B323-D05E0A23E6B3");
    static final ParcelUuid P_SERVICE_UUID = new ParcelUuid(SERVICE_UUID);

    private static final BluetoothGattService mService = new BluetoothGattService(SERVICE_UUID, SERVICE_TYPE_PRIMARY);
    private static final BluetoothGattCharacteristic maCharacteristic = new BluetoothGattCharacteristic(MA_UUID, PROPERTY_WRITE, PERMISSION_WRITE);
    private static final BluetoothGattCharacteristic peerIDCharacteristic = new BluetoothGattCharacteristic(PEER_ID_UUID, PROPERTY_WRITE, PERMISSION_WRITE);
    private static final BluetoothGattCharacteristic writerCharacteristic = new BluetoothGattCharacteristic(WRITER_UUID, PROPERTY_WRITE, PERMISSION_WRITE);
    private static final BluetoothGattCharacteristic closerCharacteristic = new BluetoothGattCharacteristic(CLOSER_UUID, PROPERTY_WRITE, PERMISSION_WRITE);

    private BleManager() {
        Logger.put("debug", TAG, "BleManager constructor called");
        Thread.currentThread().setName("BleManager");
    }


    // Setters
    static void setMultiAddr(String multiAddr) {
        Logger.put("debug", TAG, "MultiAddr set: " + multiAddr);
        maCharacteristic.setValue(multiAddr);
    }
    static void setPeerID(String peerID) {
        Logger.put("debug", TAG, "PeerID set: " + peerID);
        peerIDCharacteristic.setValue(peerID);
    }


    // Getters
    static String getMultiAddr() { return maCharacteristic.getStringValue(0); }
    static String getPeerID() { return peerIDCharacteristic.getStringValue(0); }

    static GattClient getGattCallback() { return mGattCallback; }
    static BluetoothAdapter getAdapter() { return mBluetoothAdapter; }


    // State related
    static boolean isBluetoothNotReady() {
        if (!bluetoothReady) {
            Logger.put("debug", TAG, "Bluetooth Service not initialized yet");
        }
        return !bluetoothReady;
    }

    static boolean isNotAdvertising() {
        if (!advertising) {
            Logger.put("debug", TAG, "Not currently advertising");
        }
        return !advertising;
    }

    static boolean isNotScanning() {
        if (!scanning) {
            Logger.put("debug", TAG, "Not currently scanning");
        }
        return !scanning;
    }


    // Bluetooth service related
    static boolean initBluetoothService(Activity currentActivity) {
        Logger.put("debug", TAG, "initBluetoothService() called");
        Context context = AppData.getCurrContext();

        // Turn on Bluetooth adapter
        if (!mBluetoothAdapter.isEnabled()) {
            Logger.put("debug", TAG, "Bluetooth adapter is off: turning it on");

            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            currentActivity.startActivityForResult(enableBluetoothIntent, BLUETOOTH_ENABLE_REQUEST);
        }

        // Check Location permission (needed by BLE)
        if (ContextCompat.checkSelfPermission(currentActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Logger.put("debug", TAG, "Location permission isn't granted: requesting it");

            ActivityCompat.requestPermissions(currentActivity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    99);
        }

        try {
            BluetoothManager bleManager = (BluetoothManager)context.getSystemService(BLUETOOTH_SERVICE);
            if (bleManager == null) {
                Logger.put("error", TAG, "BLE Manager is null");
                return false;
            }
            Logger.put("debug", TAG, "BLE Manager: " + bleManager);

            BluetoothGattService bertyService = createService();
            Logger.put("debug", TAG, "Berty Service: " + bertyService);

            BluetoothGattServer gattServer = bleManager.openGattServer(context, mGattServerCallback);
            gattServer.addService(bertyService);
            mGattServerCallback.setBluetoothGattServer(gattServer);
            Logger.put("debug", TAG, "GATT Server: " + gattServer);

            bluetoothReady = true;

            return true;
        } catch (Exception e) {
            Logger.put("error", TAG, "Error: " + e);
            return false;
        }
    }

    static boolean closeBluetoothService() {
        Logger.put("debug", TAG, "closeBluetoothService() called");

        if (isBluetoothNotReady()) return false;

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
                !mService.addCharacteristic(writerCharacteristic) ||
                !mService.addCharacteristic(closerCharacteristic)) {
            Logger.put("error", TAG, "Characteristic adding failed");
        }

        return mService;
    }


    // Advertise related
    static boolean startAdvertising() {
        Logger.put("debug", TAG, "startAdvertising() called");

        if (isBluetoothNotReady()) return false;
        if (advertising) return true;

        AdvertiseSettings settings = Advertiser.buildAdvertiseSettings(true, AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY, AdvertiseSettings.ADVERTISE_TX_POWER_HIGH, 0);
        Logger.put("debug", TAG, "Advertise settings: " + settings);

        AdvertiseData data = Advertiser.buildAdvertiseData();
        Logger.put("debug", TAG, "Advertise data: " + data);

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertisingCallback);

        advertising = true;

        return true;
    }

    static boolean stopAdvertising() {
        Logger.put("debug", TAG, "stopAdvertising() called");

        if (isBluetoothNotReady() || isNotAdvertising()) return false;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertisingCallback);

        advertising = false;

        return true;
    }


    // Scan related
    static boolean startScanning() {
        Logger.put("debug", TAG, "startScanning() called");

        if (isBluetoothNotReady()) return false;
        if (scanning) return true;

        ScanSettings settings = Scanner.createScanSetting();
        Logger.put("debug", TAG, "Scan settings: " + settings);

        ScanFilter filter = Scanner.makeFilter();
        Logger.put("debug", TAG, "Scan filter: " + filter);

        mBluetoothLeScanner.startScan(Collections.singletonList(filter), settings, mScanCallback);

        scanning = true;

        return true;
    }

    static boolean stopScanning() {
        Logger.put("debug", TAG, "stopScanning() called");

        if (isBluetoothNotReady() || isNotScanning()) return false;

        mBluetoothLeScanner.stopScan(mScanCallback);

        scanning = false;

        return true;
    }
}