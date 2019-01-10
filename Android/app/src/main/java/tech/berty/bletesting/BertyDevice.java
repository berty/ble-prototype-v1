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
import java.util.Arrays;
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
    private static final int gattConnectAttemptTimeout = 420;
    private static final int gattConnectMaxAttempts = 10;
    private static final int gattConnectingAttemptTimeout = 240;
    private static final int gattConnectingMaxAttempts = 5;

    // Timeout and maximum attempts for service discovery and check
    private static final int servDiscoveryAttemptTimeout = 1000;
    private static final int servDiscoveryMaxAttempts = 20;
    private static final int servCheckTimeout = 30000;

    // Timeout for remote device response
    private static final int waitDeviceReadyTimeout = 110000;

    // Timeout for write operation
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
    private BluetoothGattCharacteristic writerCharacteristic;


    // Berty identification attributes
    private String dPeerID;
    private String dMultiAddr;
    private boolean identified;


    // Semaphores / latch / buffer used for async connection / handshake / data exchange
    private CountDownLatch handshakeDone = new CountDownLatch(4); // 2 for MA/PeerID received & 2 for MA/PeerID sent
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

    // TODO: REMOVE IF USELESS
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

    boolean handshakeDoneAwait(String caller, long timeout) throws InterruptedException {
        Logger.put("debug", TAG, "handshakeDoneAwait() called from " + caller + " with timeout: " + timeout + "for device: " + dDevice + " with current count: " + handshakeDone.getCount());

        boolean waited = handshakeDone.await(timeout, TimeUnit.MILLISECONDS);

        if (waited) {
            Logger.put("debug", TAG, "handshakeDoneAwait() succeeded from " + caller + " for device: " + dDevice);
        } else {
            Logger.put("warn", TAG, "handshakeDoneAwait() timeouted from " + caller + " for device: " + dDevice);
        }

        return waited;
    }

    void handshakeDoneCountDown(String caller) {
        Logger.put("debug", TAG, "handshakeDoneCountDown() called from " + caller + " for device: " + dDevice + " with current count: " + handshakeDone.getCount() + ", new count: " + (handshakeDone.getCount() - 1));

        handshakeDone.countDown();
    }

    void resetSemaAndLatch() {
        Logger.put("debug", TAG, "resetSemaAndLatch() called for device: " + dDevice);

        handshakeDone = new CountDownLatch(4);
        waitServiceCheck = new Semaphore(0);
        waitWriteDone = new Semaphore(1);
    }


    // GATT related
    void setGatt() {
        Logger.put("debug", TAG, "setGatt() called for device: " + dDevice);

        if (dGatt == null) {
            dGatt = dDevice.connectGatt(AppData.getCurrContext(), false, BleManager.getGattCallback());
        }
    }

    boolean connectGatt(int maxAttempts) {
        Logger.put("debug", TAG, "connectGatt() called for device: " + dDevice);

        try {
            setGatt();
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                Logger.put("debug", TAG, "connectGatt() attempt: " + (attempt + 1) + "/" + maxAttempts + ", device:" + dDevice + ", client state: " + Logger.connectionStateToString(getGattClientState()) + ", server state: "  + Logger.connectionStateToString(getGattServerState()));

                dGatt.connect();

                for (int gattConnectAttempt = 0; gattConnectAttempt < gattConnectMaxAttempts; gattConnectAttempt++) {
                    Logger.put("debug", TAG, "connectGatt() wait " + gattConnectAttemptTimeout + "ms (disconnected state) " + gattConnectAttempt + "/" + gattConnectMaxAttempts + " for device: " + dDevice);
                    Thread.sleep(gattConnectAttemptTimeout);

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
    boolean bertyHandshake() {
        Logger.put("info", TAG, "bertyHandshake() called for device: " + dDevice);

        try {
            if (sendInfosToRemoteDevice()) {
                identified = waitInfosFromRemoteDevice();
            }
            resetSemaAndLatch();

            if (identified) {
                Logger.put("info", TAG, "bertyHandshake() succeeded for device: " + dDevice);
            } else {
                Logger.put("error", TAG, "bertyHandshake() failed for device: " + dDevice);
            }

            return identified;
        } catch (Exception e) {
            Logger.put("error", TAG, "bertyHandshake() failed: " + e.getMessage());
            resetSemaAndLatch();
            return false;
        }
    }

    private boolean waitInfosFromRemoteDevice() {
        Logger.put("debug", TAG, "waitInfosFromRemoteDevice() called for device: " + dDevice);

        try {
            boolean done = handshakeDoneAwait("waitInfosFromRemoteDevice()", waitDeviceReadyTimeout);

            if (done) {
                Logger.put("debug", TAG, "waitInfosFromRemoteDevice() succeeded before timeout");
            } else {
                Logger.put("error", TAG, "waitInfosFromRemoteDevice() failed after timeout");
            }
            return done;
        } catch (Exception e) {
            Logger.put("error", TAG, "waitInfosFromRemoteDevice() failed: " + e.getMessage());
            return false;
        }
    }

    private boolean sendInfosToRemoteDevice() {
        Logger.put("debug", TAG, "sendInfosToRemoteDevice() called for device: " + dDevice);

        return (checkBertyDeviceCompliance() && writeMultiAddrAndPeerIDCharacteristics());
    }


    // Wait for discovery and check if service and characteristics are Berty device compliant
    private boolean checkBertyDeviceCompliance() {
        Logger.put("debug", TAG, "checkBertyDeviceCompliance() called for device: " + dDevice);

        return (checkBertyServiceCompliance() && checkBertyCharacteristicsCompliance());
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
//TODO: REMOVE WHEN DEBUG DONE
Logger.put("error", TAG, "dGatt.discoverServices(): " + dGatt.discoverServices());
            // Wait for services discovery completed and check that Berty service is found
            if (!waitServiceCheckTryAcquire("checkBertyServiceCompliance()", servCheckTimeout)) {
                Logger.put("error", TAG, "checkBertyServiceCompliance() failed: services discovery completion timeouted for device: " + dDevice);
//TODO: REMOVE WHEN DEBUG DONE
Logger.put("error", TAG, "dGatt.discoverServices(): " + dGatt.discoverServices());
                return false;
            }

//TODO: REMOVE WHEN DEBUG DONE
Logger.put("error", TAG, "dGatt.discoverServices(): " + dGatt.discoverServices());
            return (bertyService != null);
        } catch (Exception e) {
            Logger.put("error", TAG, "checkBertyServiceCompliance() failed: " + e.getMessage());
            return false;
        }

    }

    private boolean checkBertyCharacteristicsCompliance() {
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
                BluetoothGattCharacteristic c = future.get();

                if (c != null && c.getUuid().equals(BleManager.MA_UUID)) {
                    Logger.put("debug", TAG, "checkBertyCharacteristicsCompliance() MultiAddr characteristic retrieved: " + c);
                    maCharacteristic = c;
                } else if (c != null && c.getUuid().equals(BleManager.PEER_ID_UUID)) {
                    Logger.put("debug", TAG, "checkBertyCharacteristicsCompliance() PeerID characteristic retrieved: " + c);
                    peerIDCharacteristic = c;
                } else if (c != null && c.getUuid().equals(BleManager.WRITER_UUID)) {
                    Logger.put("debug", TAG, "checkBertyCharacteristicsCompliance() Writer characteristic retrieved: " + c);
                    writerCharacteristic = c;
                } else {
                    Logger.put("error", TAG, "checkBertyCharacteristicsCompliance() unknown characteristic retrieved: " + c);
                }
            }
            return (maCharacteristic != null && peerIDCharacteristic != null && writerCharacteristic != null);
        } catch (Exception e) {
            Logger.put("error", TAG, "populateCharacteristic() failed: " + e.getMessage());
            return false;
        }
    }


    // Write own MultiAddress and PeerID on remote device
    private boolean writeMultiAddrAndPeerIDCharacteristics() {
        Logger.put("debug", TAG, "waitCharacteristic() called for device: " + dDevice);

        return (writeOnCharacteristic(BleManager.getMultiAddr(), maCharacteristic) &&
                writeOnCharacteristic(BleManager.getPeerID(), peerIDCharacteristic));
    }

    // Write a string on remote device characteristic
    private boolean writeOnCharacteristic(final String value, final BluetoothGattCharacteristic characteristic) {
        Logger.put("debug", TAG, "waitWriteCharacteristic() called for device: " + dDevice);

        try {
            synchronized (toSend) {
                int length = value.length();
                int offset = 0;

                do {
                    // BLE protocol reserves 3 bytes out of MTU_SIZE for metadata
                    // https://www.oreilly.com/library/view/getting-started-with/9781491900550/ch04.html#gatt_writes
                    int chunkSize = (length - offset > dMtu - 3) ? dMtu - 3 : length - offset;
                    byte[] chunk = value.substring(offset, offset + chunkSize).getBytes(Charset.forName("UTF-8"));
                    offset += chunkSize;
                    toSend.add(chunk);
                } while (offset < length);

                while (!toSend.isEmpty()) {
                    characteristic.setValue(toSend.get(0));
                    while (!dGatt.writeCharacteristic(characteristic)) {
                        Logger.put("verbose", TAG, "waitWriteCharacteristic() waiting for value writing on characteristic");
                        Thread.sleep(10);
                    }
                    if (!waitWriteDoneTryAcquire("waitWriteCharacteristic()", writeDoneTimeout)) {
                        return false;
                    }
                    toSend.remove(0);
                }
                handshakeDoneCountDown("waitWriteCharacteristic()");
                return true;
            }
        } catch (Exception e) {
            Logger.put("error", TAG, "waitWriteCharacteristic() failed: " + e.getMessage());
            return false;
        }
    }


    // Write blob on remote device writerCharacteristic
    void write(byte[] blob) throws InterruptedException {
        Logger.put("debug", TAG, "write() called for device: " + dDevice);
        if (!handshakeDoneAwait("write()", writeDoneTimeout)) {
            throw new InterruptedException();
        }

        synchronized (toSend) {
            int length = blob.length;
            int offset = 0;

            do {
                // You always need to deduct 3bytes from the mtu
                int chunkSize = (length - offset > dMtu - 3) ? dMtu - 3 : length - offset;
                byte[] chunk = Arrays.copyOfRange(blob, offset, offset + chunkSize);
                offset += chunkSize;
                toSend.add(chunk);
            } while (offset < length);

            while (!toSend.isEmpty()) {
                writerCharacteristic.setValue(toSend.get(0));
                while (!dGatt.writeCharacteristic(writerCharacteristic)) {
                    Thread.sleep(1);
                }
                if (!waitWriteDoneTryAcquire("write()", writeDoneTimeout)) {
                    throw new InterruptedException();
                }
                toSend.remove(0);
            }
        }
    }
}