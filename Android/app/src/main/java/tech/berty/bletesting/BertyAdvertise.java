package tech.berty.bletesting;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Build;
import android.os.ParcelUuid;

import static tech.berty.bletesting.BertyUtils.SERVICE_UUID;

@SuppressLint("LongLogTag")
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BertyAdvertise extends AdvertiseCallback {
    private static final String TAG = "advertise";

    public static AdvertiseData makeAdvertiseData() {
        ParcelUuid pUuid = new ParcelUuid(SERVICE_UUID);

        AdvertiseData.Builder builder = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(pUuid);

        return builder.build();
    }

    public static AdvertiseSettings createAdvSettings(boolean connectible, int timeoutMillis) {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();

        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(connectible)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setTimeout(timeoutMillis);

        return builder.build();
    }

    public BertyAdvertise() {
        super();
        Thread.currentThread().setName("BertyAdvertise");
    }

    /**
     * Callback triggered in response to {@link BluetoothLeAdvertiser#startAdvertising} indicating
     * that the advertising has been started successfully.
     *
     * @param settingsInEffect The actual settings used for advertising, which may be different from
     *                         what has been requested.
     */
    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        BertyUtils.logger("debug", TAG, "onStartSuccess advertising: " + settingsInEffect);
        super.onStartSuccess(settingsInEffect);
    }

    /**
     * Callback when advertising could not be started.
     *
     * @param errorCode Error code (see ADVERTISE_FAILED_* constants) for advertising start
     *                  failures.
     */
    @Override
    public void onStartFailure(int errorCode) {

        String errorString;
        switch (errorCode) {
            case ADVERTISE_FAILED_DATA_TOO_LARGE: errorString = "ADVERTISE_FAILED_DATA_TOO_LARGE";
                break;

            case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS: errorString = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
                break;

            case ADVERTISE_FAILED_ALREADY_STARTED: errorString = "ADVERTISE_FAILED_ALREADY_STARTED";
                break;

            case ADVERTISE_FAILED_INTERNAL_ERROR: errorString = "ADVERTISE_FAILED_INTERNAL_ERROR";
                break;

            case ADVERTISE_FAILED_FEATURE_UNSUPPORTED: errorString = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
                break;

            default: errorString = "UNKNOWN ADVERTISE FAILURE";
                break;
        }
        BertyUtils.logger("error", TAG, "onStartFailure advertising: " + errorString);
        super.onStartFailure(errorCode);
    }
}