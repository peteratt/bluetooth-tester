package com.example.peteratt.bluetoothtester;

import android.bluetooth.BluetoothDevice;
import android.content.res.Resources;
import android.text.TextUtils;

/**
 * Holds information about a Bluetooth Device.
 *
 * Created by peteratt on 4/11/17.
 */
class BluetoothDeviceInfo {
    final BluetoothDevice device;
    private final int rssi;

    BluetoothDeviceInfo(BluetoothDevice device, int rssi) {
        this.device = device;
        this.rssi = rssi;
    }

    String getTitle() {
        String name = device.getName();
        String address = device.getAddress();

        if (!TextUtils.isEmpty(name)) {
            return name;
        } else if (!TextUtils.isEmpty(address)) {
            return address;
        } else {
            Resources resources = BluetoothTesterApplication.getAppContext().getResources();
            return resources.getString(R.string.unknown_device);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BluetoothDeviceInfo that = (BluetoothDeviceInfo) o;

        return rssi == that.rssi
                && (device != null ? device.equals(that.device) : that.device == null);

    }

    @Override
    public int hashCode() {
        int result = device != null ? device.hashCode() : 0;
        result = 31 * result + rssi;
        return result;
    }

    @Override
    public String toString() {
        Resources resources = BluetoothTesterApplication.getAppContext().getResources();

        StringBuilder builder = new StringBuilder();
        String address = device.getAddress();

        if (!TextUtils.isEmpty(address)) {
            builder.append(resources.getString(R.string.mac_address)).append(address).append("\n");
        }
        builder
                .append(resources.getString(R.string.bluetooth_class)).append("\n")
                .append(device.getBluetoothClass().toString()).append("\n");

        if (rssi > Short.MIN_VALUE) {
            builder.append(resources.getString(R.string.signal_strenth)).append(rssi).append(" dBm");
        }

        return builder.toString();
    }
}
