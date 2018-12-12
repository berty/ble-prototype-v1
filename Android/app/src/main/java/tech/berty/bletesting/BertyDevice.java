package tech.berty.bletesting;

import android.os.Build;
import android.annotation.TargetApi;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.io.ByteArrayOutputStream;
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

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class BertyDevice {
    private static final String TAG = "device";
    private static final int DEFAULT_MTU = 23;

    private BluetoothGatt dGatt;
    private BluetoothDevice dDevice;
    private String dAddr;
    private String dPeerID;
    private String dMultiAddr;
    private int dMtu;

    private CountDownLatch latchConn = new CountDownLatch(1);
    private CountDownLatch latchReady = new CountDownLatch(2);
    private CountDownLatch latchChar = new CountDownLatch(4);

    private Semaphore serviceSema = new Semaphore(0);
    private Semaphore writeSema = new Semaphore(1);

    private final List<byte[]> toSend = new ArrayList<>();

    private BluetoothGattService dService;
    private BluetoothGattCharacteristic maCharacteristic;
    private BluetoothGattCharacteristic peerIDCharacteristic;
    private BluetoothGattCharacteristic writerCharacteristic;
//    private BluetoothGattCharacteristic closerCharacteristic;

    BertyDevice(BluetoothDevice device) {
        dAddr = device.getAddress();
        dGatt = device.connectGatt(MainActivity.getContext(), false, BleManager.getGattCallback(), BluetoothDevice.TRANSPORT_LE);
        dDevice = device;
        dMtu = DEFAULT_MTU;

        dGatt.connect();
        waitReady();
        waitConn();
    }

    void disconnect() {
        dGatt.disconnect();
        dGatt.close();
    }


    // Setters
    void setMutliAddr(String mutliAddr) {
        Logger.put("debug", TAG, "setMultiAddr() called for device: " + dDevice);
        Logger.put("debug", TAG, "With current multiAddr: " + dMultiAddr + ", new multiAddr: " + mutliAddr);
        dMultiAddr = mutliAddr;
    }

    void setPeerID(String peerID) {
        Logger.put("debug", TAG, "setPeerID() called for device: " + dDevice);
        Logger.put("debug", TAG, "With current peerID: " + dPeerID + ", new peerID: " + peerID);
        dPeerID = peerID;
    }

    void setMtu(int mtu) {
        Logger.put("debug", TAG, "setMtu() called for device: " + dDevice);
        Logger.put("debug", TAG, "With current mtu: " + dMtu + ", new mtu: " + mtu);
        dMtu = mtu;
    }

    void setService(BluetoothGattService service) {
        Logger.put("debug", TAG, "setService() called for device: " + dDevice);
        Logger.put("debug", TAG, "With current service: " + dService + ", new service: " + service);
        dService = service;
    }


    // Getters
    String getAddr() { return dAddr; }
    String getMultiAddr() { return dMultiAddr; }
    String getPeerID() { return dPeerID; }


    // Semaphore and countdown related
    void serviceSemaRelease() {
        Logger.put("debug", TAG, "serviceSemaRelease() called for device: " + dDevice);
        serviceSema.release();
    }

    void writeSemaRelease() {
        Logger.put("debug", TAG, "writeSemaRelease() called for device: " + dDevice);
        writeSema.release();
    }

    void latchConnCountDown() {
        Logger.put("debug", TAG, "latchConnCountDown() called for device: " + dDevice);
        Logger.put("debug", TAG, "With current count: " + latchConn.getCount() + ", new count: " + (latchConn.getCount() - 1));
        latchConn.countDown();
    }

    void latchReadyCountDown() {
        Logger.put("debug", TAG, "latchReadyCountDown() called for device: " + dDevice);
        Logger.put("debug", TAG, "With current count: " + latchReady.getCount() + ", new count: " + (latchReady.getCount() - 1));
        latchReady.countDown();
    }


    // Async handshake related
    private void waitReady() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.put("debug", TAG, "waitReady() called for device: " + dDevice);
                Thread.currentThread().setName("waitReady");
                try {
                    latchReady.await();
                } catch (Exception e) {
                    Logger.put("error", TAG, "Waiting/writing failed: " + e.getMessage());
                }
            }
        }).start();
    }

    private void waitConn() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.put("debug", TAG, "waitConn() called for device: " + dDevice);
                Thread.currentThread().setName("waitConn");
                try {
                    latchConn.await();
                    while (!dGatt.discoverServices()){
                        Logger.put("debug", TAG, "Waiting for service discovery");
                        Thread.sleep(1000);
                    }
                    waitService();
                } catch (Exception e) {
                    Logger.put("error", TAG, "Waiting/writing failed: " + e.getMessage());
                }

            }
        }).start();
    }

    private void waitService() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.put("debug", TAG, "waitService() called for device: " + dDevice);
                Thread.currentThread().setName("waitService");
                try {
                    serviceSema.acquire();
                    waitChar();
                    populateCharacteristic();
                } catch (Exception e) {
                    Logger.put("error", TAG, "Waiting/writing failed: " + e.getMessage());
                }

            }
        }).start();
    }

    private void waitChar() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.put("debug", TAG, "waitChar() called for device: " + dDevice);
                Thread.currentThread().setName("waitChar");
                try {
                    latchChar.await();
                    waitWriteMultiAddrThenPeerID();
                } catch (Exception e) {
                    Logger.put("error", TAG, "Waiting/writing failed: " + e.getMessage());
                }

            }
        }).start();
    }

    private void waitWriteMultiAddrThenPeerID() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.put("debug", TAG, "waitWriteMaThenPeerID() called for device: " + dDevice);
                Thread.currentThread().setName("waitWriteMaThenPeerID");
                try {
                    synchronized (toSend) {
                        dGatt.requestMtu(555);
                        byte[] value = BleManager.getMultiAddr().getBytes(Charset.forName("UTF-8"));
                        int length = value.length;
                        int offset = 0;

                        do {
                            // You always need to deduct 3 bytes from the mtu
                            int chunkSize = (length - offset > dMtu - 3) ? dMtu - 3 : length - offset;
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            output.write(Arrays.copyOfRange(value, offset, offset + chunkSize));
                            output.write(new byte[]{0});
                            byte[] chunk = output.toByteArray();
                            offset += chunkSize;
                            toSend.add(chunk);
                        } while (offset < length);

                        while (!toSend.isEmpty()) {
                            Logger.put("debug", TAG, "Wait multiAddr writing");
                            maCharacteristic.setValue(toSend.get(0));
                            while (!dGatt.writeCharacteristic(maCharacteristic)) {
                                Thread.sleep(1);
                            }
                            writeSema.acquire();
                            toSend.remove(0);
                        }

                        value = BleManager.getPeerID().getBytes(Charset.forName("UTF-8"));
                        length = value.length;
                        offset = 0;

                        do {
                            // You always need to deduct 3 bytes from the mtu
                            int chunkSize = (length - offset > dMtu - 3) ? dMtu - 3 : length - offset;
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            output.write(Arrays.copyOfRange(value, offset, offset + chunkSize));
                            output.write(new byte[]{0});
                            byte[] chunk = output.toByteArray();
                            offset += chunkSize;
                            toSend.add(chunk);
                        } while (offset < length);

                        while (!toSend.isEmpty()) {
                            Logger.put("debug", TAG, "Wait peerID writing");
                            peerIDCharacteristic.setValue(toSend.get(0));
                            while (!dGatt.writeCharacteristic(peerIDCharacteristic)) {
                                Thread.sleep(1);
                            }
                            writeSema.acquire();
                            toSend.remove(0);
                        }
                        Logger.put("debug", TAG, "Countdown 1");
                        latchReady.countDown();
                    }
                } catch (Exception e) {
                    Logger.put("error", TAG, "Waiting/writing failed: " + e.getMessage());
                }
            }
        }).start();
    }


    // Characteristic related
    private void populateCharacteristic() {
        Logger.put("debug", TAG, "populateCharacteristic() called for device: " + dDevice);

        ExecutorService es = Executors.newFixedThreadPool(4);
        List<PopulateCharacteristic> todo = new ArrayList<>(4);

        todo.add(new PopulateCharacteristic(BleManager.MA_UUID));
        todo.add(new PopulateCharacteristic(BleManager.PEER_ID_UUID));
        todo.add(new PopulateCharacteristic(BleManager.CLOSER_UUID));
        todo.add(new PopulateCharacteristic(BleManager.WRITER_UUID));

        try {
            List<Future<BluetoothGattCharacteristic>> answers = es.invokeAll(todo);
            for (Future<BluetoothGattCharacteristic> future : answers) {
                BluetoothGattCharacteristic c = future.get();

                if (c != null && c.getUuid().equals(BleManager.MA_UUID)) {
                    maCharacteristic = c;
                    latchChar.countDown();
                } else if (c != null && c.getUuid().equals(BleManager.PEER_ID_UUID)) {
                    peerIDCharacteristic = c;
                    latchChar.countDown();
                } else if (c != null && c.getUuid().equals(BleManager.CLOSER_UUID)) {
//                    closerCharacteristic = c;
                    latchChar.countDown();
                } else if (c != null && c.getUuid().equals(BleManager.WRITER_UUID)) {
                    writerCharacteristic = c;
                    latchChar.countDown();
                } else {
                    Logger.put("error", TAG, "Unknown characteristic retrieved: " + c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class PopulateCharacteristic implements Callable<BluetoothGattCharacteristic> {
        private UUID uuid;

        private PopulateCharacteristic(UUID charaUUID) {
            uuid = charaUUID;
        }

        public BluetoothGattCharacteristic call() {
            return dGatt.getService(BleManager.SERVICE_UUID).getCharacteristic(uuid);
        }
    }


    // Write related
    void write(byte[] blob) throws InterruptedException {
        Logger.put("debug", TAG, "write() called for device: " + dDevice);
        latchReady.await();

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
                writeSema.acquire();
                toSend.remove(0);
            }
        }
    }
}