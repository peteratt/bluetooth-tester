package com.example.peteratt.bluetoothtester;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages all Bluetooth connections.
 *
 * Created by peteratt on 4/11/17.
 */
final class ConnectionManager {

    private static final String LOG_TAG = ConnectionManager.class.getSimpleName();

    private static final String APP_UUID = "c0a1171b-3660-448e-a68e-a56ec4ef357f";
    private static final long DISCOVERY_CANCEL_TIMEOUT = TimeUnit.SECONDS.toMillis(20);

    private static final ConnectionManager instance = new ConnectionManager();

    static ConnectionManager getInstance() {
        return instance;
    }

    private final BluetoothAdapter bluetoothAdapter;
    private final List<OnScanDevices> callbacks;
    private final Handler handler;

    private ConnectThread currentConnectThread;

    private Runnable cancelDiscovery = new Runnable() {
        @Override
        public void run() {
            Log.v(LOG_TAG, "cancelDiscovery after 10 seconds");

            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            callbacks.clear();
        }
    };

    private ConnectionManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        callbacks = new ArrayList<>();
        handler = new Handler(Looper.getMainLooper());

        if (isBluetoothSupported()) {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

            BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                        for (OnScanDevices callback : callbacks) {
                            callback.onDeviceDiscovered(new BluetoothDeviceInfo(device, rssi));
                        }
                    }
                }
            };
            BluetoothTesterApplication.getAppContext().registerReceiver(discoveryReceiver, filter);
        }
    }

    boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    private boolean isBluetoothEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    void scanForDevices(OnScanDevices callback) {
        if (!isBluetoothSupported()) {
            callback.onError(Error.ERROR_BT_UNSUPPORTED);
            return;
        }

        if (!isBluetoothEnabled()) {
            callback.onError(Error.ERROR_BT_DISABLED);
            return;
        }
        callbacks.add(callback);

        if (!bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.startDiscovery();
        } else {
            handler.removeCallbacks(cancelDiscovery);
        }
        handler.postDelayed(cancelDiscovery, DISCOVERY_CANCEL_TIMEOUT);
    }

    void connectToDevice(BluetoothDevice bluetoothDevice, OnConnectToDeviceListener onConnectToDeviceListener) {
        if (bluetoothAdapter.isDiscovering()) {
            handler.removeCallbacks(cancelDiscovery);
            bluetoothAdapter.cancelDiscovery();
        }

        if (currentConnectThread != null && currentConnectThread.bluetoothSocket.isConnected()) {
            currentConnectThread.cancel();
        }

        currentConnectThread = new ConnectThread(bluetoothDevice, onConnectToDeviceListener);
        currentConnectThread.run();
    }

    public void disconnectFromCurrentDevice() {
        if (currentConnectThread != null) {
            currentConnectThread.cancel();
            currentConnectThread = null;
        }
    }

    enum Error {
        ERROR_NONE,
        ERROR_BT_UNSUPPORTED,
        ERROR_BT_DISABLED
    }

    interface OnScanDevices {
        void onDeviceDiscovered(BluetoothDeviceInfo deviceInfo);
        void onError(Error error);
    }

    interface OnConnectToDeviceListener {
        void onConnectionSuccessful();
        void onConnectionError();
        void onConnectionEnded();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final OnConnectToDeviceListener listener;

        ConnectThread(BluetoothDevice device, OnConnectToDeviceListener onConnectToDeviceListener) {
            this.listener = onConnectToDeviceListener;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(APP_UUID));
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket's create() method failed", e);
            }
            bluetoothSocket = tmp;
        }

        @Override
        public void run() {
            try {
                bluetoothSocket.connect();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onConnectionSuccessful();
                    }
                });
            } catch (IOException connectException) {
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    Log.e(LOG_TAG, "Could not close the client socket", closeException);
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onConnectionError();
                    }
                });
            }
        }

        void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Could not close the client socket", e);
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onConnectionEnded();
                }
            });
        }
    }
}
