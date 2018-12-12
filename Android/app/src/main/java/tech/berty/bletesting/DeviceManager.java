package tech.berty.bletesting;

import android.os.Build;
import android.annotation.TargetApi;

import java.util.HashMap;
import java.util.Map;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
final class DeviceManager {
    private static final String TAG = "device_manager";

    private static final HashMap<String, BertyDevice> bertyDevices = new HashMap<>();

    private DeviceManager() {}


    // Index managing
    static void addDeviceToIndex(BertyDevice bertyDevice) {
        Logger.put("debug", TAG, "addDeviceToIndex() called");
        Logger.put("debug", TAG, "With device: " + bertyDevice + ", current index size: " + bertyDevices.size() + ", new index size: " + (bertyDevices.size() + 1));

        synchronized (bertyDevices) {
            bertyDevices.put(bertyDevice.getAddr(), bertyDevice);
        }
    }

    static void removeDeviceFromIndex(BertyDevice bertyDevice) {
        Logger.put("debug", TAG, "removeDeviceFromIndex() called");
        Logger.put("debug", TAG, "With device: " + bertyDevice + ", current index size: " + bertyDevices.size() + ", new index size: " + (bertyDevices.size() - 1));

        bertyDevice.disconnect();
        synchronized (bertyDevices) {
            bertyDevices.remove(bertyDevice.getAddr());
        }
    }


    // Device getters
    static BertyDevice getDeviceFromAddr(String addr) {
        Logger.put("debug", TAG, "getDeviceFromAddr() called with address: " + addr);

        synchronized (bertyDevices) {
            if (bertyDevices.containsKey(addr)) {
                return bertyDevices.get(addr);
            }
        }

        Logger.put("error", TAG, "Berty device not found with address: " + addr);

        return null;
    }

    static BertyDevice getDeviceFromMultiAddr(String multiAddr) {
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

    static boolean write(byte[] blob, BertyDevice bertyDevice) {
        Logger.put("debug", TAG, "write() called");
        Logger.put("debug", TAG, "With len: " + blob.length + ", device: " + bertyDevice);

        if (bertyDevice == null) {
            Logger.put("error", TAG, "Can't write: unknown device");
            return false;
        }

        try {
            bertyDevice.write(blob);
            return true;
        } catch (Exception e) {
            Logger.put("error", TAG, "Can't write: " + e);
            return false;
        }
    }
}