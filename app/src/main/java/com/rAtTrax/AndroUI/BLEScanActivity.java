package com.rAtTrax.AndroUI;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Created by Mike on 4/19/15.
 * Taken from AOSP's sample Bluetooth scan activity.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEScanActivity extends ListActivity {

    //TODO: Need to alert user after two scan attempts to try classic bluetooth.

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 4000;
    private static final String TAG = "BLEScanActivity";


    /*Area to house BluetoothLeControl member vars */

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private int _attemptsCount;
    private Typeface roboto;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    //Handles the data being sent back from the BluetoothLeService class.
    private final Handler _bleHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] readBuf = (byte[]) msg.obj;
            // construct a string from the valid bytes in the buffer
            String readMessage;
            try {
                readMessage = new String(readBuf, 0, msg.arg1);
            } catch (NullPointerException e) {
                readMessage = "0";
            }
            //Redraw the needle to the correct value.
            Log.d("CHECKINGAGAIN", readMessage);
        }
    };

    /*End BluetoothLeControl Member vars */


    /*start scan methods */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        roboto = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");

        getActionBar().setTitle(R.string.title_devices);

        int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
        int subtitleId = getResources().getIdentifier("action_bar_subtitle", "id", "android");
        TextView actionBarTitle = (TextView) findViewById(titleId);
        TextView actionBarSubTitle = (TextView) findViewById(subtitleId);
        actionBarTitle.setTextColor(getResources().getColor(R.color.white));
        actionBarTitle.setTypeface(roboto);
        actionBarSubTitle.setTextColor(getResources().getColor(R.color.white));
        actionBarSubTitle.setTypeface(roboto);

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //Instantiate a new BLE service to pass around
        mBluetoothLeService = new BluetoothLeService(this, _bleHandler);

        //Start counting attempts to find a controller.
        _attemptsCount = 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);

        //from control
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null && mDeviceAddress != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress, this);
            Log.d(TAG, "Connect request result=" + result);
        }else{
            Log.d(TAG, "problem in onResume");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();

        //from control
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;

        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        setupControl(device.getName(), device.getAddress());
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                    _attemptsCount++;
                    Log.i(TAG, String.valueOf(_attemptsCount));
                    checkAttempsCount();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            _attemptsCount = 0;
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private void checkAttempsCount(){
        if(mLeDeviceListAdapter.getCount() < 1 && _attemptsCount > 1){

            //Show this dialog if no controllers found after two attempts.
            new AlertDialog.Builder(this)
                    .setTitle("Try Bluetooth Classic?")
                    .setMessage("Scanning using Bluetooth LE has not found any rAtTrax or mdv2 controllers. Try scanning using Bluetooth Classic if problem persists." +
                            "\n\nTurn Classic on in Settings -> General Settings")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing for now.
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

            //Set this back to zero
            _attemptsCount = 0;
        }
        if(_attemptsCount > 2)
            _attemptsCount = 0;
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = BLEScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
    /*end scan methods*/


    /* Start of Control */


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
                //updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
                connectService();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //tempViewHolder.deviceName.setText(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //Log.d("THISWORKS", intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    public void setupControl(String deviceName, String deviceAddress){
        mDeviceName = deviceName;
        mDeviceAddress = deviceAddress;

        connectDevice();
    }

    public void connectDevice(){
        Log.d(TAG, "Connecting device..");
        //Connect device, BroadcastReceiver call back calls connectService().
        if(mBluetoothLeService != null) {
            mBluetoothLeService.connect(mDeviceAddress, this);
        }else{
            Log.d(TAG, "mBluetoothLeService is null");
        }
    }

    public void connectService(){
        Log.d(TAG, "Connecting service..");
        List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();
        UUID tempUUID;

        //loop through the services, find the one we're looking for - for now assuming it contains the necessary characteristic...
        for(BluetoothGattService gattService : gattServices){
            tempUUID = gattService.getUuid();
            if(tempUUID.compareTo(mBluetoothLeService.UUID_rAtTrax)==0){
                //connect to characteristice, assuming this exists for now.
                connectCharacteristic(gattService.getCharacteristic(mBluetoothLeService.UUID_CHARACTERISTIC_rAtTrax));
            }
        }
    }

    public void connectCharacteristic(BluetoothGattCharacteristic characteristic){
        Log.d(TAG, "Connecting characteristic..");
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristic != null) {
                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            mBluetoothLeService.readCharacteristic(characteristic);
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = characteristic;
            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
            mBluetoothLeService.setHandler(_bleHandler);

            onBackPressed();
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void goHome(){
        if(mBluetoothLeService != null) {
            PassObject.setObject(mBluetoothLeService);
            PassObject.setType(2);
        }
        finish();
    }

    public void onBackPressed(){
        goHome();
        super.onBackPressed();
    }

}