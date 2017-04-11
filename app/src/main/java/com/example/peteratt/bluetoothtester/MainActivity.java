package com.example.peteratt.bluetoothtester;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int APP_PERMISSIONS_REQUEST = 2;

    private final ConnectionManager connectionManager;
    private final DeviceListAdapter deviceListAdapter;

    @BindView(R.id.unsupported_message)
    TextView unsupportedMessage;

    @BindView(R.id.scan_button)
    Button scanButton;

    @BindView(R.id.device_list)
    RecyclerView deviceList;

    @OnClick(R.id.scan_button)
    void scan() {
        deviceListAdapter.clear();

        connectionManager.scanForDevices(new ConnectionManager.OnScanDevices() {
            @Override
            public void onDeviceDiscovered(BluetoothDeviceInfo deviceInfo) {
                Log.v(LOG_TAG, "new bluetooth deviceInfo discovered: " + deviceInfo.device.getName());
                deviceListAdapter.add(deviceInfo);
            }

            @Override
            public void onError(ConnectionManager.Error error) {
                switch (error) {
                    case ERROR_BT_UNSUPPORTED:
                        Log.w(LOG_TAG, "BT is unsupported, should not call scanForDevices");
                        break;
                    case ERROR_BT_DISABLED:
                        Log.i(LOG_TAG, "BT is disabled, opening dialog for enabling");
                        showEnableDialog();
                        break;
                    case ERROR_NONE:
                        break;
                }
            }
        });
    }

    public MainActivity() {
        connectionManager = ConnectionManager.getInstance();
        deviceListAdapter = new DeviceListAdapter();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (!connectionManager.isBluetoothSupported()) {
            scanButton.setVisibility(GONE);
            unsupportedMessage.setVisibility(VISIBLE);
        }

        deviceList.setLayoutManager(new LinearLayoutManager(this));
        deviceList.setAdapter(deviceListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            askForPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            scan();
        }
    }

    private void askForPermission(String permission) {
        String[] permissions = { permission };

        ActivityCompat.requestPermissions(
                this,
                permissions,
                APP_PERMISSIONS_REQUEST
        );
    }

    private void showEnableDialog() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private class DeviceListAdapter extends RecyclerView.Adapter {

        List<BluetoothDeviceInfo> devices = new ArrayList<>();

        void add(BluetoothDeviceInfo deviceInfo) {
            if (!devices.contains(deviceInfo)) {
                devices.add(deviceInfo);
                notifyItemInserted(devices.size() - 1);
            }
        }

        void clear() {
            devices.clear();
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ViewHolder) {
                ViewHolder deviceViewHolder = (ViewHolder) holder;
                BluetoothDeviceInfo device = devices.get(position);
                deviceViewHolder.setDeviceInfo(device);
            }
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private TextView deviceListItemTitle;
            private BluetoothDeviceInfo deviceInfo;

            ViewHolder(View itemView) {
                super(itemView);
                deviceListItemTitle = (TextView) itemView.findViewById(R.id.device_list_item_title);

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                Log.v(LOG_TAG, "selected deviceInfo");

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder
                        .setTitle(deviceInfo.getTitle())
                        .setMessage(deviceInfo.toString())
                        .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                connectToDevice(deviceInfo);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
            }

            void setDeviceInfo(BluetoothDeviceInfo deviceInfo) {
                this.deviceInfo = deviceInfo;
                deviceListItemTitle.setText(deviceInfo.getTitle());
            }
        }
    }

    private void connectToDevice(final BluetoothDeviceInfo deviceInfo) {
        Toast.makeText(
                MainActivity.this,
                getResources().getString(R.string.connecting_to, deviceInfo.getTitle()),
                Toast.LENGTH_LONG
        ).show();

        connectionManager.connectToDevice(deviceInfo.device, new ConnectionManager.OnConnectToDeviceListener() {
            @Override
            public void onConnectionSuccessful() {
                Toast.makeText(
                        MainActivity.this,
                        getResources().getString(R.string.successfully_connected_to, deviceInfo.getTitle()),
                        Toast.LENGTH_LONG
                ).show();
            }

            @Override
            public void onConnectionError() {
                Toast.makeText(
                        MainActivity.this,
                        getResources().getString(R.string.error_while_connecting_to, deviceInfo.getTitle()),
                        Toast.LENGTH_LONG
                ).show();
            }

            @Override
            public void onConnectionEnded() {
                Toast.makeText(
                        MainActivity.this,
                        getResources().getString(R.string.connection_finished_with, deviceInfo.getTitle()),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}
