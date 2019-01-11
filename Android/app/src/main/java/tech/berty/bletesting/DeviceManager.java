package tech.berty.bletesting;

import android.os.Build;
import android.annotation.TargetApi;
import java.nio.charset.Charset;

import java.util.HashMap;
import java.util.Map;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
final class DeviceManager {
    private static final String TAG = "device_manager";

    private static final int handshakeDoneTimeout = 120000;

    private static final HashMap<String, BertyDevice> bertyDevices = new HashMap<>();

    private DeviceManager() {}


    // Index managing
    static void addDeviceToIndex(BertyDevice bertyDevice) {
        Logger.put("debug", TAG, "addDeviceToIndex() called with device: " + bertyDevice + ", current index size: " + bertyDevices.size() + ", new index size: " + (bertyDevices.size() + 1));

        synchronized (bertyDevices) {
            if (!bertyDevices.containsKey(bertyDevice.getAddr())) {
                bertyDevices.put(bertyDevice.getAddr(), bertyDevice);
            } else {
                Logger.put("error", TAG, "Berty device already in index: " + bertyDevice.getAddr());
            }
        }
    }

    static void removeDeviceFromIndex(BertyDevice bertyDevice) {
        Logger.put("debug", TAG, "removeDeviceFromIndex() called with device: " + bertyDevice + ", current index size: " + bertyDevices.size() + ", new index size: " + (bertyDevices.size() - 1));

        bertyDevice.lockConnAttemptTryAcquire("removeDeviceFromIndex()", 180000);
        bertyDevice.disconnectGatt();
        synchronized (bertyDevices) {
            if (bertyDevices.containsKey(bertyDevice.getAddr())) {
                bertyDevices.remove(bertyDevice.getAddr());
            } else {
                Logger.put("error", TAG, "Berty device not found in index with address: " + bertyDevice.getAddr());
            }
        }
        bertyDevice.lockConnAttemptRelease("removeDeviceFromIndex()");
    }


    // Device getters
    static BertyDevice getDeviceFromAddr(String addr) {
        Logger.put("debug", TAG, "getDeviceFromAddr() called with address: " + addr);

        synchronized (bertyDevices) {
            if (bertyDevices.containsKey(addr)) {
                return bertyDevices.get(addr);
            }
        }

        Logger.put("warn", TAG, "Berty device not found with address: " + addr);

        return null;
    }

    private static BertyDevice getDeviceFromMultiAddr(String multiAddr) {
        Logger.put("debug", TAG, "getDeviceFromMultiAddr() called with MultiAddr: " + multiAddr);

        synchronized (bertyDevices) {
            BertyDevice bertyDevice;

            for (Map.Entry<String, BertyDevice> entry : bertyDevices.entrySet()) {
                bertyDevice = entry.getValue();
                if (bertyDevice.getMultiAddr().equals(multiAddr)) {
                    return bertyDevice;
                }
            }
        }

        Logger.put("error", TAG, "Berty device not found with MultiAddr: " + multiAddr);

        return null;
    }


    // Write related
    static boolean write(byte[] blob, String multiAddr) {
        return write(blob, getDeviceFromMultiAddr(multiAddr));
    }

    private static boolean write(byte[] blob, BertyDevice bertyDevice) {
        Logger.put("debug", TAG, "write() called with blob: " + new String(blob, Charset.forName("UTF-8")) + ", len: " + blob.length + ", device: " + bertyDevice);

        if (bertyDevice == null) {
            Logger.put("error", TAG, "write() failed: unknown device");
            return false;
        }

        try {
            if (bertyDevice.handshakeDoneTryAcquire("write() DeviceManager", handshakeDoneTimeout)) {
                bertyDevice.handshakeDoneRelease("write() DeviceManager");
                if (bertyDevice.isIdentified()) {
                    return bertyDevice.writeOnCharacteristic(blob, bertyDevice.writerCharacteristic);
                } else {
                    Logger.put("error", TAG, "write() failed: handshake failed");
                }
            } else {
                Logger.put("error", TAG, "write() timeouted: handshake still not done");
            }
        } catch (Exception e) {
            Logger.put("error", TAG, "write() failed: " + e.getMessage());
        }

        return false;
    }
}