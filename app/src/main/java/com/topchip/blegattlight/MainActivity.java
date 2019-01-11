package com.topchip.blegattlight;


import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends Activity {
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private BluetoothDevice deviceScanFound;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;

    //private String BlueECG_ADD = "80:EA:CA:00:00:01";                                               //Address for BlueECG device
    private String BLEGattLight_NAME = "Node+";
    private String mDeviceAddress;

    @Override
    //What is done when the app is started
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, yay! Start the Bluetooth device scan.
                } else {
                    // Alert the user that this application requires the location permission to perform the scan.
                }
            }
        }
    }

    //Starts or stops a scan
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    //Is run when the scan button is hit
    public void launchScan(View view) {

        boolean on = ((Switch) view).isChecked();
        if(on) {
            scanLeDevice(true);
        }else{
            scanLeDevice(false);
        }
        Intent intent = new Intent(this, SecondActivity.class);
    }

    //Starts a connection
    private void launchConnect(BluetoothDevice device){

        final Intent intent = new Intent(this, SecondActivity.class);
        intent.putExtra(SecondActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(SecondActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            Switch switch1 = (Switch) findViewById(R.id.switch1);
            switch1.setChecked(false);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    //Handles the bluetooth callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceScanFound = device;
                            mDeviceAddress = deviceScanFound.getAddress();
                            //if(deviceScanFound.getAddress().equals(BlueECG_ADD)){
                             //   launchConnect(device);
                            //}
                            if(BLEGattLight_NAME.equals(deviceScanFound.getName())) {
                                launchConnect(device);
                            }
                        }
                    });
                }
            };


}
