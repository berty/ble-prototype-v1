package tech.berty.bletesting;

import android.os.Build;
import android.content.Context;
import android.annotation.TargetApi;

import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;

import static android.bluetooth.BluetoothProfile.GATT;
import static android.bluetooth.BluetoothProfile.GATT_SERVER;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class BertyDevice {
    private static final String TAG = "device";


    // Timeout and maximum attempts for GATT connection
    private static final int gattConnectMaxAttempts = 10;
    private static final int gattWaitConnectAttemptTimeout = 420;
    private static final int gattWaitConnectMaxAttempts = 10;
    private static final int gattConnectingAttemptTimeout = 240;
    private static final int gattConnectingMaxAttempts = 5;

    // Timeout and maximum attempts for service discovery and check
    private static final int deviceCheckTimeout = 30000;
    private static final int servDiscoveryAttemptTimeout = 1000;
    private static final int servDiscoveryMaxAttempts = 20;
    private static final int servCheckTimeout = 30000;

    // Timeout for remote device response
    private static final int waitDeviceReadyTimeout = 60000;

    // Timeout and maximum attempts for write operation
    private static final int initWriteAttemptTimeout = 40;
    private static final int initWriteMaxAttempts = 1000;
    private static final int writeDoneTimeout = 60000;

    // GATT connection attributes
    private BluetoothGatt dGatt;
    private BluetoothDevice dDevice;
    private String dAddr;
    private int dMtu;

    private static final int DEFAULT_MTU = 23;

    private BluetoothGattService bertyService;
    private BluetoothGattCharacteristic maCharacteristic;
    private BluetoothGattCharacteristic peerIDCharacteristic;
    BluetoothGattCharacteristic writerCharacteristic;


    // Berty identification attributes
    private String dPeerID;
    private String dMultiAddr;
    private boolean identified;

    // Semaphores / latch / buffer used for async connection / handshake / data exchange
    private CountDownLatch infosExchanged = new CountDownLatch(4); // 2 for MA/PeerID received & 2 for MA/PeerID sent
    private Semaphore handshakeDone = new Semaphore(0); // Lock until handshake succeeded or failed

    private Semaphore waitDeviceCheck = new Semaphore(0); // Wait for thread that check device compliance
    private Semaphore waitServiceCheck = new Semaphore(0); // Lock for callback that check discovered services
    private Semaphore waitWriteDone = new Semaphore(1); // Lock for waiting completion of write operation

    private Semaphore lockConnAttempt = new Semaphore(1); // Lock to prevent more than one GATT connection attempt at once
    private Semaphore lockHandshakeAttempt = new Semaphore(1); // Lock to prevent more than one handshake attempt at once

    private final List<byte[]> toSend = new ArrayList<>();


    BertyDevice(BluetoothDevice device) {
        dAddr = device.getAddress();
        dDevice = device;
        dMtu = DEFAULT_MTU;
    }


    // Berty identification related
    String getAddr() { return dAddr; }

    void setMultiAddr(String multiAddr) {
        Logger.put("info", TAG, "setMultiAddr() called for device: " + dDevice + " with current multiAddr: " + dMultiAddr + ", new multiAddr: " + multiAddr);

        dMultiAddr = multiAddr;

        if (dMultiAddr.length() > 36) {
            Logger.put("error", TAG, "setMultiAddr() error: MultiAddr can't be greater than 36 bytes, string will be truncated. Device: " + dDevice);
            dMultiAddr = dMultiAddr.substring(0, 35);
        }
    }

    String getMultiAddr() { return dMultiAddr; }

    void setPeerID(String peerID) {
        Logger.put("info", TAG, "setPeerID() called for device: " + dDevice + " with current peerID: " + dPeerID + ", new peerID: " + peerID);
        dPeerID = peerID;

        if (dPeerID.length() > 46) {
            Logger.put("error", TAG, "setPeerID() error: PeerID can't be greater than 46 bytes, string will be truncated. Device: " + dDevice);
            dPeerID = dPeerID.substring(0, 45);
        }
    }

    String getPeerID() { return dPeerID; }

    void setBertyService(BluetoothGattService service) {
        Logger.put("info", TAG, "setBertyService() called for device: " + dDevice + " with current service: " + bertyService + ", new service: " + service);

        bertyService = service;
    }

    BluetoothGattService getBertyService() { return bertyService; }

    boolean isIdentified() {
        Logger.put("verbose", TAG, "isIdentified() called for device: " + dDevice + ", state: " + (identified ? "identified" : "unidentified"));

        return identified;
    }


    // Semaphore and countdown related
    boolean lockConnAttemptTryAcquire(String caller, long timeout) {
        Logger.put("debug", TAG, "lockConnAttemptTryAcquire() called from " + caller + " with timeout: " + timeout + " for device: " + dDevice);

        try {
            boolean waited = lockConnAttempt.tryAcquire(timeout, TimeUnit.MILLISECONDS);

            if (waited) {
                Logger.put("debug", TAG, "lockConnAttemptTryAcquire() acquired from " + caller + " for device: " + dDevice);
            } else {
                Logger.put("warn", TAG, "lockConnAttemptTryAcquire() timeouted from " + caller + " for device: " + dDevice);
            }

            return waited;
        } catch (Exception e) {
            Logger.put("error", TAG, "lockConnAttemptTryAcquire() from " + caller + " for device: " + dDevice + " failed: " + e.getMessage());
            return false;
        }
    }

    void lockConnAttemptRelease(String caller) {
        Logger.put("debug", TAG, "lockConnAttemptRelease() called from " + caller + " for device: " + dDevice);

        lockConnAttempt.release();
    }

    boolean lockHandshakeAttemptTryAcquire(String caller, long timeout) {
        Logger.put("debug", TAG, "lockHandshakeAttemptTryAcquire() called from " + caller + " with timeout: " + timeout + " for device: " + dDevice);

        try {
            boolean waited = lockHandshakeAttempt.tryAcquire(timeout, TimeUnit.MILLISECONDS);

            if (waited) {
                Logger.put("debug", TAG, "lockHandshakeAttemptTryAcquire() acquired from " + caller + " for device: " + dDevice);
            } else {
                Logger.put("warn", TAG, "lockHandshakeAttemptTryAcquire() timeouted from " + caller + " for device: " + dDevice);
            }

            return waited;
        } catch (Exception e) {
            Logger.put("error", TAG, "lockHandshakeAttemptTryAcquire() from " + caller + " failed: " + e.getMessage());
            return false;
        }
    }

    void lockHandshakeAttemptRelease(String caller) {
        Logger.put("debug", TAG, "lockHandshakeAttemptRelease() called from " + caller + " for device: " + dDevice);

        lockHandshakeAttempt.release();
    }

    boolean handshakeDoneTryAcquire(String caller, long timeout) {
        Logger.put("debug", TAG, "handshakeDoneTryAcquire() called from " + caller + " with timeout: " + timeout + " for device: " + dDevice);

        try {
            boolean waited = handshakeDone.tryAcquire(timeout, TimeUnit.MILLISECONDS);

            if (waited) {
                Logger.put("debug", TAG, "handshakeDoneTryAcquire() acquired from " + caller + " for device: " + dDevice);
            } else {
                Logger.put("warn", TAG, "handshakeDoneTryAcquire() timeouted from " + caller + " for device: " + dDevice);
            }

            return waited;
        } catch (Exception e) {
            Logger.put("error", TAG, "handshakeDoneTryAcquire() from " + caller + " for device: " + dDevice + " failed: " + e.getMessage());
            return false;
        }
    }

    void handshakeDoneRelease(String caller) {
        Logger.put("debug", TAG, "handshakeDoneRelease() called from " + caller + " for device: " + dDevice);

        handshakeDone.release();
    }

    boolean waitServiceCheckTryAcquire(String caller, long timeout) throws InterruptedException {
        Logger.put("debug", TAG, "waitServiceCheckTryAcquire() called from " + caller + " with timeout: " + timeout + " for device: " + dDevice);

        boolean waited = waitServiceCheck.tryAcquire(timeout, TimeUnit.MILLISECONDS);

        if (waited) {
            Logger.put("debug", TAG, "waitServiceCheckTryAcquire() acquired from " + caller + " for device: " + dDevice);
        } else {
            Logger.put("warn", TAG, "waitServiceCheckTryAcquire() timeouted from " + caller + " for device: " + dDevice);
        }

        return waited;
    }

    void waitServiceCheckRelease(String caller) {
        Logger.put("debug", TAG, "waitServiceCheckRelease() called from " + caller + " for device: " + dDevice);

        waitServiceCheck.release();
    }

    boolean waitWriteDoneTryAcquire(String caller, long timeout) {
        Logger.put("debug", TAG, "waitWriteDoneAcquire() called from " + caller + " for device: " + dDevice);

        try {
            boolean waited = waitWriteDone.tryAcquire(timeout, TimeUnit.MILLISECONDS);

            if (waited) {
                Logger.put("debug", TAG, "waitWriteDoneTryAcquire() acquired from " + caller + " for device: " + dDevice);
            } else {
                Logger.put("warn", TAG, "waitWriteDoneTryAcquire() timeouted from " + caller + " for device: " + dDevice);
            }

            return waited;
        } catch (Exception e) {
            Logger.put("error", TAG, "waitWriteDoneTryAcquire() from " + caller + " for device: " + dDevice + " failed: " + e.getMessage());
            return false;
        }
    }

    void waitWriteDoneRelease(String caller) {
        Logger.put("debug", TAG, "waitWriteDoneRelease() called from " + caller + " for device: " + dDevice);

        waitWriteDone.release();
    }

    boolean infosExchangedAwait(String caller, long timeout) throws InterruptedException {
        Logger.put("debug", TAG, "infosExchangedAwait() called from " + caller + " with timeout: " + timeout + " for device: " + dDevice + " with current count: " + infosExchanged.getCount());

        boolean waited = infosExchanged.await(timeout, TimeUnit.MILLISECONDS);

        if (waited) {
            Logger.put("debug", TAG, "infosExchangedAwait() succeeded from " + caller + " for device: " + dDevice);
        } else {
            Logger.put("warn", TAG, "infosExchangedAwait() timeouted from " + caller + " for device: " + dDevice);
        }

        return waited;
    }

    void infosExchangedCountDown(String caller) {
        Logger.put("debug", TAG, "handshakeDoneCountDown() called from " + caller + " for device: " + dDevice + " with current count: " + infosExchanged.getCount() + ", new count: " + (infosExchanged.getCount() - 1));

        infosExchanged.countDown();
    }


    // GATT related
    void setGatt() {
        Logger.put("debug", TAG, "setGatt() called for device: " + dDevice);

        if (dGatt == null) {
            dGatt = dDevice.connectGatt(AppData.getCurrContext(), false, BleManager.getGattCallback());
        }
    }

    boolean connectGatt() {
        Logger.put("debug", TAG, "connectGatt() called for device: " + dDevice);

        try {
            setGatt();
            for (int attempt = 0; attempt < gattConnectMaxAttempts; attempt++) {
                Logger.put("debug", TAG, "connectGatt() attempt: " + (attempt + 1) + "/" + gattConnectMaxAttempts + ", device:" + dDevice + ", client state: " + Logger.connectionStateToString(getGattClientState()) + ", server state: "  + Logger.connectionStateToString(getGattServerState()));

                dGatt.connect();

                for (int gattWaitConnectAttempt = 0; gattWaitConnectAttempt < gattWaitConnectMaxAttempts; gattWaitConnectAttempt++) {
                    Logger.put("debug", TAG, "connectGatt() wait " + gattWaitConnectAttemptTimeout + "ms (disconnected state) " + gattWaitConnectAttempt + "/" + gattWaitConnectMaxAttempts + " for device: " + dDevice);
                    Thread.sleep(gattWaitConnectAttemptTimeout);

                    if (getGattClientState() == STATE_CONNECTING || getGattServerState() == STATE_CONNECTING) {
                        for (int gattConnectingAttempt = 0; gattConnectingAttempt < gattConnectingMaxAttempts; gattConnectingAttempt++) {
                            Logger.put("debug", TAG, "connectGatt() wait " + gattConnectingAttemptTimeout + "ms (connecting state) " + gattConnectingAttempt + "/" + gattConnectingMaxAttempts + " for device: " + dDevice);
                            Thread.sleep(gattConnectingAttemptTimeout);

                            if (isGattConnected()) {
                                break;
                            }
                        }
                    }

                    if (isGattConnected()) {
                        Logger.put("info", TAG, "connectGatt() connection succeeded for device: " + dDevice);
                        return true;
                    }
                }
                disconnectGatt();
                setGatt();
            }
        } catch (Exception e) {
            Logger.put("error", TAG, "connectGatt() failed: " + e.getMessage());
        }

        return false;
    }

    void disconnectGatt() {
        Logger.put("debug", TAG, "disconnectGatt() called for device: " + dDevice);

        if (dGatt != null) {
            dGatt.disconnect();
            dGatt.close();
            dGatt = null;
        }
    }

    void setMtu(int mtu) {
        Logger.put("info", TAG, "setMtu() called for device: " + dDevice + " with current mtu: " + dMtu + ", new mtu: " + mtu);

        dMtu = mtu;
    }

    int getGattClientState() {
        Logger.put("verbose", TAG, "getGattClientState() called for device: " + dDevice);

        final Context context = AppData.getCurrContext();
        final BluetoothManager manager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null) {
            Logger.put("error", TAG, "Can't get BLE Manager");
            return STATE_DISCONNECTED;
        }

        return manager.getConnectionState(dDevice, GATT);
    }

    int getGattServerState() {
        Logger.put("verbose", TAG, "getGattServerState() called for device: " + dDevice);

        final Context context = AppData.getCurrContext();
        final BluetoothManager manager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null) {
            Logger.put("error", TAG, "Can't get BLE Manager");
            return STATE_DISCONNECTED;
        }

        return manager.getConnectionState(dDevice, GATT_SERVER);
    }

    boolean isGattConnected() {
        Logger.put("verbose", TAG, "isGattConnected() called for device: " + dDevice);

        return (getGattServerState() == STATE_CONNECTED && getGattClientState() == STATE_CONNECTED);
    }


    // Check if remote device is Berty compliant and exchange libp2p infos
    // Wait (or timeout) until both devices are ready and infos are exchanged
    void bertyHandshake() {
        Logger.put("info", TAG, "bertyHandshake() called for device: " + dDevice);

        if (checkBertyDeviceCompliance()) {
            asyncInfosExchange();
        }

        // Auto remove device from index if asyncInfosExchange() failed
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handshakeDone.acquire();
                    if (identified) {
                        handshakeDone.release();
                    } else {
                        DeviceManager.removeDeviceFromIndex(DeviceManager.getDeviceFromAddr(dAddr));
                    }
                } catch (Exception e) {
                    Logger.put("error", TAG, "bertyHandshake() async exchange verification failed: " + e.getMessage());
                }
            }
        }).start();
    }


    // Wait for discovery and check if service and characteristics are Berty device compliant
    private boolean checkBertyDeviceCompliance() {
        Logger.put("debug", TAG, "checkBertyDeviceCompliance() called for device: " + dDevice);

        Thread asyncCheck = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Check if Berty service is found then check characteristics
                    if (checkBertyServiceCompliance()) {
                        checkBertyCharacteristicsCompliance();
                    }
                    waitDeviceCheck.release();
                } catch (Exception e) {
                    Logger.put("error", TAG, "checkBertyDeviceCompliance() failed: " + e.getMessage() + " for device: " + dDevice);
                }
            }
        });

        try {
            asyncCheck.start();
            if (waitDeviceCheck.tryAcquire(deviceCheckTimeout, TimeUnit.MILLISECONDS)) {
                if (bertyService != null && maCharacteristic != null && peerIDCharacteristic != null && writerCharacteristic != null) {
                    Logger.put("info", TAG, "checkBertyDeviceCompliance() succeeded for device: " + dDevice);
                    return true;
                } else {
                    Logger.put("info", TAG, "checkBertyDeviceCompliance() failed for device: " + dDevice);
                }
            } else {
                Logger.put("error", TAG, "checkBertyDeviceCompliance() timeouted for device: " + dDevice);
                asyncCheck.interrupt();
            }
        } catch (Exception e) {
            Logger.put("error", TAG, "checkBertyDeviceCompliance() failed: " + e.getMessage() + " for device: " + dDevice);
        }

        return false;
    }

    private boolean checkBertyServiceCompliance() {
        Logger.put("debug", TAG, "checkBertyServiceCompliance() called for device: " + dDevice);

        try {
            // Wait for services discovery started
            for (int servDiscoveryAttempt = 0; servDiscoveryAttempt < servDiscoveryMaxAttempts && !dGatt.discoverServices(); servDiscoveryAttempt++) {
                if (isGattConnected()) {
                    Logger.put("debug", TAG, "checkBertyServiceCompliance() device " + dDevice + " GATT is connected, waiting for service discovery: " + servDiscoveryAttempt + "/" + servDiscoveryMaxAttempts);
                    Thread.sleep(servDiscoveryAttemptTimeout);
                } else {
                    Logger.put("error", TAG, "checkBertyServiceCompliance() failed: device " + dDevice + " GATT is disconnected");
                    return false;
                }
            }
            // Wait for services discovery completed and check that Berty service is found
            if (!waitServiceCheckTryAcquire("checkBertyServiceCompliance()", servCheckTimeout)) {
                Logger.put("error", TAG, "checkBertyServiceCompliance() failed: services discovery completion timeouted for device: " + dDevice);
                return false;
            }

            return (bertyService != null);
        } catch (Exception e) {
            Logger.put("error", TAG, "checkBertyServiceCompliance() failed: " + e.getMessage());
            return false;
        }

    }

    private void checkBertyCharacteristicsCompliance() {
        Logger.put("debug", TAG, "checkBertyCharacteristicsCompliance() called for device: " + dDevice);

        class PopulateCharacteristic implements Callable<BluetoothGattCharacteristic> {
            private UUID uuid;

            private PopulateCharacteristic(UUID charaUUID) {
                Logger.put("debug", TAG, "PopulateCharacteristic with UUID: " + charaUUID);

                uuid = charaUUID;
            }

            public BluetoothGattCharacteristic call() {
                return getBertyService().getCharacteristic(uuid);
            }
        }

        ExecutorService es = Executors.newFixedThreadPool(3);
        List<PopulateCharacteristic> todo = new ArrayList<>(3);

        todo.add(new PopulateCharacteristic(BleManager.MA_UUID));
        todo.add(new PopulateCharacteristic(BleManager.PEER_ID_UUID));
        todo.add(new PopulateCharacteristic(BleManager.WRITER_UUID));

        try {
            List<Future<BluetoothGattCharacteristic>> answers = es.invokeAll(todo);
            for (Future<BluetoothGattCharacteristic> future : answers) {
                BluetoothGattCharacteristic characteristic = future.get();

                if (characteristic != null && characteristic.getUuid().equals(BleManager.MA_UUID)) {
                    Logger.put("debug", TAG, "checkBertyCharacteristicsCompliance() MultiAddr characteristic retrieved: " + characteristic + " for device: " + dDevice);
                    maCharacteristic = characteristic;
                } else if (characteristic != null && characteristic.getUuid().equals(BleManager.PEER_ID_UUID)) {
                    Logger.put("debug", TAG, "checkBertyCharacteristicsCompliance() PeerID characteristic retrieved: " + characteristic + " for device: " + dDevice);
                    peerIDCharacteristic = characteristic;
                } else if (characteristic != null && characteristic.getUuid().equals(BleManager.WRITER_UUID)) {
                    Logger.put("debug", TAG, "checkBertyCharacteristicsCompliance() Writer characteristic retrieved: " + characteristic + " for device: " + dDevice);
                    writerCharacteristic = characteristic;
                } else {
                    Logger.put("error", TAG, "checkBertyCharacteristicsCompliance() unknown characteristic retrieved: " + characteristic + " for device: " + dDevice);
                }
            }
        } catch (Exception e) {
            Logger.put("error", TAG, "checkBertyCharacteristicsCompliance() failed: " + e.getMessage() + " for device: " + dDevice);
        }
    }


     // Async two-way exchange of MultiAddr and PeerID
    private void asyncInfosExchange() {
        Logger.put("debug", TAG, "asyncInfosExchange() called for device: " + dDevice);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (sendInfosToRemoteDevice()) { // Send MultiAddr and PeerID to remote device
                    try {
                        identified = infosExchangedAwait("asyncInfosExchange()", waitDeviceReadyTimeout);

                        if (identified) {
                            Logger.put("debug", TAG, "asyncInfosExchange() succeeded before timeout");
                        } else {
                            Logger.put("error", TAG, "asyncInfosExchange() failed after timeout");
                        }
                    } catch (Exception e) {
                        Logger.put("error", TAG, "asyncInfosExchange() failed: " + e.getMessage());
                    }
                }

                handshakeDone.release();
            }
        }).start();
    }

    // Write own MultiAddress and PeerID on remote device
    private boolean sendInfosToRemoteDevice() {
        Logger.put("debug", TAG, "sendInfosToRemoteDevice() called for device: " + dDevice);

        if (writeOnCharacteristic(BleManager.getMultiAddr().getBytes(Charset.forName("UTF-8")), maCharacteristic)) {
            infosExchangedCountDown("sendInfosToRemoteDevice() MultiAddr");

            if (writeOnCharacteristic(BleManager.getPeerID().getBytes(Charset.forName("UTF-8")), peerIDCharacteristic)) {
                infosExchangedCountDown("writeMultiAddrAndPeerIDCharacteristics() PeerID");

                Logger.put("info", TAG, "sendInfosToRemoteDevice() succeeded for device: " + dDevice);

                return true;
            }
        }

        Logger.put("info", TAG, "sendInfosToRemoteDevice() failed for device: " + dDevice);

        return false;
    }

    // Write a blob on a specific remote device characteristic
    boolean writeOnCharacteristic(byte[] blob, BluetoothGattCharacteristic characteristic) {
        Logger.put("debug", TAG, "writeOnCharacteristic() called for device: " + dDevice);

        try {
            synchronized (toSend) {
                String strBlob = new String(blob, Charset.forName("UTF-8"));
                int length = strBlob.length();
                int offset = 0;

                do {
                    // BLE protocol reserves 3 bytes out of MTU_SIZE for metadata
                    // https://www.oreilly.com/library/view/getting-started-with/9781491900550/ch04.html#gatt_writes
                    int chunkSize = (length - offset > dMtu - 3) ? dMtu - 3 : length - offset;
                    byte[] chunk = strBlob.substring(offset, offset + chunkSize).getBytes(Charset.forName("UTF-8"));
                    offset += chunkSize;
                    toSend.add(chunk);
                } while (offset < length);

                while (!toSend.isEmpty()) {
                    characteristic.setValue(toSend.get(0));
                    for (int attempt = 0; !dGatt.writeCharacteristic(characteristic); attempt++) {
                        if (attempt == initWriteMaxAttempts) {
                            Logger.put("error", TAG, "writeOnCharacteristic() wait for write init timeouted for device:" + dDevice);
                            return false;
                        }

                        Logger.put("verbose", TAG, "writeOnCharacteristic() wait for write init: " + (attempt + 1) + "/" + initWriteMaxAttempts + ", device:" + dDevice);
                        Thread.sleep(initWriteAttemptTimeout);
                    }
                    if (!waitWriteDoneTryAcquire("writeOnCharacteristic()", writeDoneTimeout)) {
                        return false;
                    }
                    toSend.remove(0);
                }
                return true;
            }
        } catch (Exception e) {
            Logger.put("error", TAG, "writeOnCharacteristic() failed: " + e.getMessage());
            return false;
        }
    }
}