package com.topchip.blegattlight;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SecondActivity extends Activity {
    private Switch mSwitch;
    private SeekBar mSeekBarLight;
    private SeekBar mSeekBarCTL;

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private UUID serviceUUID;
    private BluetoothGattCharacteristic characteristic2;                                            //The characteristic with all the values
    private boolean mConnected = false;

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final String SERVICE_UUID = "6e402001-b5a3-f393-e0a9-e50e24dcca9e";
    private final String CHAR2_UUID = "6e402002-b5a3-f393-e0a9-e50e24dcca9e";


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                checkGattServices(mBluetoothLeService.getSupportedGattServices());
            }
        }
    };


	@Override
    //Is run when the activity is started
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_second);

        mSwitch = (Switch) findViewById(R.id.switch2);
        mSeekBarLight = (SeekBar) findViewById(R.id.seekBarLight);
        mSeekBarCTL = (SeekBar) findViewById(R.id.seekBarCTL);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mSeekBarLight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;
            byte[] v = new byte[3];

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                v[0] = (byte)0xAA;
                v[1] = (byte)0x02;
                v[2] = (byte)progress;
                characteristic2.setValue(v);
                mBluetoothLeService.writeCharacteristic(characteristic2);
                //Toast.makeText(getApplicationContext(), "Changing seekbar's progress", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar"+progress, Toast.LENGTH_SHORT).show();
            }
        });

        mSeekBarCTL.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;
            byte[] v = new byte[3];

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                v[0] = (byte)0xAA;
                v[1] = (byte)0x03;
                v[2] = (byte)progress;
                characteristic2.setValue(v);
                mBluetoothLeService.writeCharacteristic(characteristic2);
                //Toast.makeText(getApplicationContext(), "Changing seekbar's progress", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar"+progress, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());                        //Connect the Broadcast Reciever
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }

        //mSwitch.setChecked(true);
       // mSeekBarLight.setProgress(100);
        //mSeekBarCTL.setProgress(100);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    //Is run when the scan button is hit
    public void openLight(View view) {
	    byte[] v = new byte[2];

        boolean on = ((Switch) view).isChecked();
        if(on) {
            v[0] = (byte)0xAA;
            v[1] = (byte)0x01;
            characteristic2.setValue(v);
            mBluetoothLeService.writeCharacteristic(characteristic2);

        }else{
            v[0] = (byte)0xAA;
            v[1] = (byte)0x00;
            characteristic2.setValue(v);
            mBluetoothLeService.writeCharacteristic(characteristic2);
        }
    }

    //Gets the correct service and sets it up
    private void checkGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            if(uuid.equals(SERVICE_UUID)) {
                serviceUUID = gattService.getUuid();                                                //Store the UUID information in the serviceUUID holder
                ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                        new ArrayList<HashMap<String, String>>();
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas =
                        new ArrayList<BluetoothGattCharacteristic>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();

                    if(uuid.equals(CHAR2_UUID)) {
                        characteristic2 = gattCharacteristic;                                       //Store characteristic2 data in the variable holder
                        //mBluetoothLeService.setCharacteristicNotification(characteristic2, true);   //Set up notifications of characteristic
                        Log.w(TAG, "characteristic2 found");
                    }
                }
            }
        }

    }
    //Used for the Broadcast Reciever, needed to connect to the sendBroadcast()
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}

