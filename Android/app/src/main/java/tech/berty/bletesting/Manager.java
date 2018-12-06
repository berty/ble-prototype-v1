package tech.berty.bletesting;

import android.app.Activity;
import android.app.ActivityManager;
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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.BLUETOOTH_SERVICE;
import static tech.berty.bletesting.BertyUtils.BLUETOOTH_ENABLE_REQUEST;

public class Manager {
    public static String TAG = "manager";
    private static Manager instance = null;
    public String ma;
    public String peerID;
    public boolean isAdvertising = false;
    public boolean isScanning = false;
    protected HashMap<String, BertyDevice> bertyDevices;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothGattServer mBluetoothGattServer;
    protected BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    protected BluetoothLeScanner mBluetoothLeScanner;
    protected BluetoothGattService mService;
    protected BertyGatt mGattCallback = new BertyGatt();
    private Context mContext;
    private BertyGattServer mGattServerCallback = new BertyGattServer();

    private BertyAdvertise mAdvertisingCallback = new BertyAdvertise();

    private BertyScan mScanCallback = new BertyScan();

    private Manager() {
        super();
        Thread.currentThread().setName("BleManager");
        BertyUtils.logger("debug", TAG, "initializing BLEManager");
        mScanCallback.mGattCallback = mGattCallback;
        mGattServerCallback.mGattCallback = mGattCallback;
    }

    public static String getMa() {
        return instance.ma;
    }

    public static String getPeerID() {
        return instance.peerID;
    }

    public static Manager getInstance() {
        if (instance == null) {
            synchronized (Manager.class) {
                if (instance == null) {
                    instance = new Manager();
                    instance.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    instance.bertyDevices = new HashMap<>();
                }
            }
        }

        return instance;
    }

    public void setmContext(Context ctx) {
        BertyUtils.logger("debug", TAG, "BLEManager context set");
        mContext = ctx;
        mScanCallback.mContext = ctx;
        mGattServerCallback.mContext = ctx;
    }

    public void setMa(String ma) {
        this.ma = ma;
        BertyUtils.maCharacteristic.setValue(ma);
        if (this.peerID != null && !this.peerID.equals("")) {
            AdvertiseSettings settings = BertyAdvertise.createAdvSettings(true, 0);
            AdvertiseData advData = BertyAdvertise.makeAdvertiseData();
            mBluetoothLeAdvertiser.startAdvertising(settings, advData, mAdvertisingCallback);
        }
        BertyUtils.logger("debug", TAG, "Ma setted: " + ma);
    }

    public void setPeerID(String peerID) {
        this.peerID = peerID;
        BertyUtils.peerIDCharacteristic.setValue(peerID);
        if (this.ma != null && !this.ma.equals("")) {
            AdvertiseSettings settings = BertyAdvertise.createAdvSettings(true, 0);
            AdvertiseData advData = BertyAdvertise.makeAdvertiseData();
            mBluetoothLeAdvertiser.startAdvertising(settings, advData, mAdvertisingCallback);
        }
        BertyUtils.logger("debug", TAG, "PeerID setted: " + peerID);
    }

    public void initScannerAndAdvertiser(Activity curActivity) {
//        BluetoothAdapter.getDefaultAdapter().disable();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            curActivity.startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_REQUEST);
            if (ContextCompat.checkSelfPermission(curActivity,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                /** intentionally empty */
            } else {
                ActivityCompat.requestPermissions(curActivity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        99);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                BluetoothManager mb = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mBluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
                    mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
                    BluetoothGattService svc = BertyUtils.createService();


                    mBluetoothGattServer = mb.openGattServer(mContext, mGattServerCallback);
                    mBluetoothGattServer.getServices();

                    mGattServerCallback.mBluetoothGattServer = mBluetoothGattServer;
                    BertyUtils.logger("debug", TAG, "test " + svc + " " + mBluetoothGattServer);

                    mBluetoothGattServer.addService(svc);
                    ScanSettings settings2 = BertyScan.createScanSetting();
                    ScanFilter filter = BertyScan.makeFilter();
                    BertyUtils.logger("debug", TAG, "starting scan");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mBluetoothLeScanner.startScan(Arrays.asList(filter), settings2, mScanCallback);
                    }
                }

            } catch (Exception e) {
                BertyUtils.logger("error", TAG, "error: " + e);
            }
        }
    }

    public void closeScannerAndAdvertiser() {
        stopScanning();
        stopAdvertising();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothGattServer.close();
        }
    }

    public void closeConnFromMa(String rMa) {
        BertyDevice bDevice = BertyUtils.getDeviceFromMa(rMa);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bDevice.gatt.disconnect();
            bDevice.gatt.close();
            bDevice.gatt = null;
            bDevice.device = null;
            Log.e(TAG, "CLOSE");
        }
        synchronized (bertyDevices) {
            bertyDevices.remove(bDevice.addr);
        }
    }

    public boolean isRunning(Context ctx) {
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (ctx.getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName()))
                return true;
        }

        return false;
    }

    public void startAdvertising() {
        AdvertiseSettings settings = BertyAdvertise.createAdvSettings(true, 0);
        AdvertiseData advData = BertyAdvertise.makeAdvertiseData();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BertyUtils.logger("debug", TAG, "startAdvertising settings: " + settings + " data: " + advData + " cb: " + mAdvertisingCallback);
//            mBluetoothLeAdvertiser.startAdvertising(settings, advData, mAdvertisingCallback);
        }
    }

    public void stopAdvertising() {
        isAdvertising = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BertyUtils.logger("debug", TAG, "stopAdvertising");
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertisingCallback);
        }
    }

    public void startScanning() {
        BertyUtils.logger("debug", TAG, "startScanning() called");
        ScanSettings settings = BertyScan.createScanSetting();
        ScanFilter filter = BertyScan.makeFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            mBluetoothLeScanner.startScan(Arrays.asList(filter), settings, mScanCallback);
        }
    }

    public void stopScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
    }

    public boolean dialPeer(String ma) {
        BertyDevice bDevice = BertyUtils.getDeviceFromMa(ma);
        if (bDevice != null) {
            return true;
        }
        return false;
    }

    public boolean write(byte[] p, String ma) {
        BertyDevice bDevice = BertyUtils.getDeviceFromMa(ma);

        if (bDevice == null) {
            BertyUtils.logger("error", TAG, "writing on unknow device failed");
            return false;
        }

        try {
            bDevice.write(p);
        } catch (Exception e) {
            BertyUtils.logger("error", TAG, "writing error " + e.getMessage());

            return false;
        }

        return true;
    }

    // @Override
    // protected void finalize() throws Throwable {
    //     try {
    //         mBluetoothGattServer.clearServices();
    //         for (BertyDevice value : BertyUtils.bertyDevices.values()) {
    //             if (value.gatt != null) {
    //                 value.gatt.disconnect();
    //                 value.gatt.close();
    //                 value.gatt = null;
    //                 value
    //                         .device = null;
    //                 BertyUtils.logger("debug", TAG, "CLOSE")
    //             }
    //         }
    //         stopAdvertising();
    //         stopScanning();
    //     } finally {
    //         super.finalize();
    //     }
    // }

}