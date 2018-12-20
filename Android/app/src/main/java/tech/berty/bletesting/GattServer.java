package tech.berty.bletesting;

import java.nio.charset.Charset;
import java.util.UUID;
import android.os.Build;
import android.annotation.TargetApi;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import static android.bluetooth.BluetoothGatt.GATT_FAILURE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GattServer extends BluetoothGattServerCallback {
    private static final String TAG = "gatt_server";

    private BluetoothGattServer mBluetoothGattServer;

    GattServer() { super(); }

    void setBluetoothGattServer(BluetoothGattServer bluetoothGattServer) {
        Logger.put("debug", TAG, "Bluetooth GATT Server set: " + bluetoothGattServer);

        mBluetoothGattServer = bluetoothGattServer;
    }

    void closeGattServer() {
        Logger.put("debug", TAG, "closeGattServer() called");

        if (mBluetoothGattServer != null) {
            mBluetoothGattServer.close();
            mBluetoothGattServer = null;
        } else {
            Logger.put("error", TAG, "Bluetooth GATT Server not set");
        }
    }

    /**
     * Callback indicating when a remote device has been connected or disconnected.
     *
     * @param device   Remote device that has been connected or disconnected.
     * @param status   Status of the connect or disconnect operation.
     * @param newState Returns the new connection state. Can be one of
     *                 {@link BluetoothProfile#STATE_DISCONNECTED} or
     *                 {@link BluetoothProfile#STATE_CONNECTED}
     */
    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        Logger.put("debug", TAG, "onConnectionStateChange() called");
        Logger.put("debug", TAG, "With device: " + device + ", status: " + status + ", newState: " + Logger.connectionStateToString(newState));

        BertyDevice bertyDevice = DeviceManager.getDeviceFromAddr(device.getAddress());
        if (newState == STATE_DISCONNECTED) {
            if (bertyDevice != null) {
                DeviceManager.removeDeviceFromIndex(bertyDevice);
            }
        }
        super.onConnectionStateChange(device, status, newState);
    }

    /**
     * Indicates whether a local service has been added successfully.
     *
     * @param status  Returns {@link BluetoothGatt#GATT_SUCCESS} if the service
     *                was added successfully.
     * @param service The service that has been added
     */
    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
//        Logger.put("debug", TAG, "sendServiceAdded() called");
//        Logger.put("debug", TAG, "With status: " + status + ", service: " + service);

        super.onServiceAdded(status, service);
    }

    /**
     * A remote client has requested to read a local characteristic.
     *
     * <p>An application must call {@link BluetoothGattServer#sendResponse}
     * to complete the request.
     *
     * @param device         The remote device that has requested the read operation
     * @param requestId      The Id of the request
     * @param offset         Offset into the value of the characteristic
     * @param characteristic Characteristic to be read
     */
    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
//        Logger.put("debug", TAG, "onCharacteristicReadRequest() called");
//        Logger.put("debug", TAG, "With device: " + device + ", requestId: " + requestId + ", offset: " + offset + ", characteristic: " + characteristic);

        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
    }

    /**
     * A remote client has requested to write to a local characteristic.
     *
     * <p>An application must call {@link BluetoothGattServer#sendResponse}
     * to complete the request.
     *
     * @param device         The remote device that has requested the write operation
     * @param requestId      The Id of the request
     * @param characteristic Characteristic to be written to.
     * @param preparedWrite  true, if this write operation should be queued for
     *                       later execution.
     * @param responseNeeded true, if the remote device requires a response
     * @param offset         The offset given for the value
     * @param value          The value the client wants to assign to the characteristic
     */
    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        Logger.put("debug", TAG, "onCharacteristicWriteRequest() called");
        Logger.put("debug", TAG, "With device: " + device + ", requestId: " + requestId + ", characteristic: " + characteristic + ", preparedWrite: " + preparedWrite + ", responseNeeded: " + responseNeeded + ", offset: " + offset + ", value: " + new String(value, Charset.forName("UTF-8")) + ", len: " + value.length);

        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

        UUID charID = characteristic.getUuid();
        BertyDevice bertyDevice = DeviceManager.getDeviceFromAddr(device.getAddress());
        if (charID.equals(BleManager.WRITER_UUID)) {
            AppData.addMessageToList(device.getAddress(), "received: " + new String(value, Charset.forName("UTF-8")));
            if (responseNeeded) {
                mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, offset, value);
            }
//        } else if (charID.equals(BleManager.CLOSER_UUID)) {
//        // TODO
        } else if (charID.equals(BleManager.PEER_ID_UUID)) {
            if (bertyDevice != null && bertyDevice.getPeerID() != null) {
                bertyDevice.setPeerID(bertyDevice.getPeerID() + new String(value, Charset.forName("UTF-8")));
            } else if (bertyDevice != null) {
                bertyDevice.setPeerID(new String(value, Charset.forName("UTF-8")));
            }

            if (responseNeeded) {
                mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, offset, value);
            }
            if (bertyDevice != null && bertyDevice.getPeerID().length() == 46) {
                Logger.put("debug", TAG, "Countdown");
                bertyDevice.latchReadyCountDown();
            }
        } else if(charID.equals(BleManager.MA_UUID)) {
            if (bertyDevice != null && bertyDevice.getMultiAddr() != null) {
                bertyDevice.setMultiAddr(bertyDevice.getMultiAddr() + new String(value, Charset.forName("UTF-8")));
            } else if (bertyDevice != null) {
                bertyDevice.setMultiAddr(new String(value, Charset.forName("UTF-8")));
            }

            if (responseNeeded) {
                mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, offset, value);
            }
        }
        else {
            mBluetoothGattServer.sendResponse(device, requestId, GATT_FAILURE, offset, null);
        }
    }

    /**
     * A remote client has requested to read a local descriptor.
     *
     * <p>An application must call {@link BluetoothGattServer#sendResponse}
     * to complete the request.
     *
     * @param device     The remote device that has requested the read operation
     * @param requestId  The Id of the request
     * @param offset     Offset into the value of the characteristic
     * @param descriptor Descriptor to be read
     */
    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
//        Logger.put("debug", TAG, "onDescriptorReadRequest() called");
//        Logger.put("debug", TAG, "With device: " + device + ", requestId: " + requestId + ", offset: " + offset + ", descriptor: " + descriptor);

        super.onDescriptorReadRequest(device, requestId, offset, descriptor);
    }

    /**
     * A remote client has requested to write to a local descriptor.
     *
     * <p>An application must call {@link BluetoothGattServer#sendResponse}
     * to complete the request.
     *
     * @param device         The remote device that has requested the write operation
     * @param requestId      The Id of the request
     * @param descriptor     Descriptor to be written to.
     * @param preparedWrite  true, if this write operation should be queued for
     *                       later execution.
     * @param responseNeeded true, if the remote device requires a response
     * @param offset         The offset given for the value
     * @param value          The value the client wants to assign to the descriptor
     */
    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
//        Logger.put("debug", TAG, "onNotificationSent() called");
//        Logger.put("debug", TAG, "With device: " + device + ", requestId: " + requestId + ", descriptor: " + descriptor + ", preparedWrite: " + preparedWrite + ", responseNeeded: " + responseNeeded + ", offset: " + offset + ", len: " + value.length);

        super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
    }

    /**
     * Execute all pending write operations for this device.
     *
     * <p>An application must call {@link BluetoothGattServer#sendResponse}
     * to complete the request.
     *
     * @param device    The remote device that has requested the write operations
     * @param requestId The Id of the request
     * @param execute   Whether the pending writes should be executed (true) or
     */
    @Override
    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
//        Logger.put("debug", TAG, "onNotificationSent() called");
//        Logger.put("debug", TAG, "With device: " + device + ", requestId: " + requestId + ", execute: " + execute);

        super.onExecuteWrite(device, requestId, execute);
    }

    /**
     * Callback invoked when a notification or indication has been sent to
     * a remote device.
     *
     * <p>When multiple notifications are to be sent, an application must
     * wait for this callback to be received before sending additional
     * notifications.
     *
     * @param device The remote device the notification has been sent to
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the operation was successful
     */
    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
//        Logger.put("debug", TAG, "onNotificationSent() called");
//        Logger.put("debug", TAG, "With device: " + device + ", status: " + status);

        super.onNotificationSent(device, status);
    }

    /**
     * Callback indicating the MTU for a given device connection has changed.
     *
     * <p>This callback will be invoked if a remote client has requested to change
     * the MTU for a given connection.
     *
     * @param device The remote device that requested the MTU change
     * @param mtu    The new MTU size
     */
    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        Logger.put("debug", TAG, "onMtuChanged() called");
        Logger.put("debug", TAG, "With device: " + device + ", mtu: " + mtu);

        super.onMtuChanged(device, mtu);

        BertyDevice bertyDevice = DeviceManager.getDeviceFromAddr(device.getAddress());
        if (bertyDevice != null) {
            bertyDevice.setMtu(mtu);
        }
    }

    /**
     * Callback triggered as result of {@link BluetoothGattServer#setPreferredPhy}, or as a result
     * of remote device changing the PHY.
     *
     * @param device The remote device
     * @param txPhy  the transmitter PHY in use. One of {@link BluetoothDevice#PHY_LE_1M},
     *               {@link BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}
     * @param rxPhy  the receiver PHY in use. One of {@link BluetoothDevice#PHY_LE_1M},
     *               {@link BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}
     * @param status Status of the PHY update operation.
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    @Override
    public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
//        Logger.put("debug", TAG, "onPhyUpdate() called");
//        Logger.put("debug", TAG, "With device: " + device + ", txPhy: " + txPhy + ", rxPhy: " + rxPhy + ", status: " + status);

        super.onPhyUpdate(device, txPhy, rxPhy, status);
    }

    /**
     * Callback triggered as result of {@link BluetoothGattServer#readPhy}
     *
     * @param device The remote device that requested the PHY read
     * @param txPhy  the transmitter PHY in use. One of {@link BluetoothDevice#PHY_LE_1M},
     *               {@link BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}
     * @param rxPhy  the receiver PHY in use. One of {@link BluetoothDevice#PHY_LE_1M},
     *               {@link BluetoothDevice#PHY_LE_2M}, and {@link BluetoothDevice#PHY_LE_CODED}
     * @param status Status of the PHY read operation.
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    @Override
    public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
//        Logger.put("debug", TAG, "onPhyRead() called");
//        Logger.put("debug", TAG, "With device: " + device + ", txPhy: " + txPhy + ", rxPhy: " + rxPhy + ", status: " + status);

        super.onPhyRead(device, txPhy, rxPhy, status);
    }
}