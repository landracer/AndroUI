package com.rAtTrax.AndroUI;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.location.LocationManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Handler;
import android.os.Message;
import android.Manifest;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.rAtTrax.AndroUI.TinyDB;
import java.util.Timer;
import java.util.TimerTask;


public class PSensor extends Activity implements LocationListener {
    /**
     * Called when the activity is first created.
     */

    LocationManager locationManager;//initialize variable, type LocationManager which is a class that provides access to system location services
    // Constants
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Flags
    public static final boolean ENABLE_RPM = false;
    public static final boolean ENABLE_TRACE_BLUE_TOOTH = false;

    // Debugging
    private static final String TAG = "ProjectSensor";
    private static final boolean D = true;
    private boolean debug;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final int BLE_MESSAGE_STATE_CHANGE = 3;
    public static final int BLE_MESSAGE_DISCONNECTED = 0;
    public static final int BLE_MESSAGE_CONNECTING = 1;
    public static final int BLE_MESSAGE_CONNECTED = 2;

    //Used to show whats new dialog.
    private static final String PRIVATE_PREF = "myapp";
    private static final String VERSION_KEY = "version_number";

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    public static final int UPDATE_SENSOR_TMR = 100;
    public static Sensor_BinaryData sensorData;
    //Global Variables.
    TextView titleText, textView, textView2, textView3, textView4, textView5, textView6, textView7;//initialize my Views
    Typeface typeFaceBtn;
    Typeface typeFaceTitle;
    Button GPSbtn;
    Button btnConnect;
    Button btnSettings;
    Button btnWideband;
    Button btnBoost;
    Button btnOil;
    Button btnCustom;
    Button btnMulti1;
    Button btnMulti2;
    Button btnRPM;
    Button btnSpeed;
    Button btnVolts;
    int counts, radius, maxspeed;
    double lat1, lon1, lat2, lon2, alt1, alt2, time1, distance, speed;//variables to save the coordinates, time, distance and speed
    long start, finish, time;//variables that will help me count time in milliseconds
    boolean above;

    //Bluetooth LE
    private Boolean isBLE;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeService _bluetoothLEService = null;
    public BluetoothSerialService mSerialService = null;
    int intReadMsgPrevious = 0;
    //TinyDB ttb = new TinyDB(this);
    public void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);



        //Set the screen to the main.xml layout.
        setContentView(R.layout.psensor_layout);

        //Show the whats new dialog if this is the first time run
        showWhatsNew();

        //Get the instances of the layout objects.
       // textView = findViewById(R.id.textView);
      //  textView2 = findViewById(R.id.textView2);
       // textView3 = findViewById(R.id.textView3);
        //textView4 = findViewById(R.id.textView4);
       // textView5 = findViewById(R.id.textView5);
      //  textView4.setVisibility(View.INVISIBLE);
      //  textView5.setVisibility(View.INVISIBLE);
        textView3  = findViewById(R.id.textView3);
        titleText   = (TextView) findViewById(R.id.title_text);
        btnConnect  = (Button) findViewById(R.id.connectBtn);
        btnSettings = (Button) findViewById(R.id.settingsBtn);
        GPSbtn      = (Button) findViewById(R.id.GPSbutton);
        btnWideband = (Button) findViewById(R.id.widebandBtn);
        btnBoost    = (Button) findViewById(R.id.boostBtn);
        btnOil      = (Button) findViewById(R.id.oilBtn);
        btnCustom   = (Button) findViewById(R.id.customBtn);
        btnMulti1   = (Button) findViewById(R.id.multiBtn1);
        btnMulti2   = (Button) findViewById(R.id.multiBtn2);
        btnRPM      = (Button) findViewById(R.id.rpmBtn);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        distance =0;
        start = 0;
        counts = 0;
        above = false;

        if (!ENABLE_RPM) {
            btnRPM.setVisibility(View.GONE);
        }
        //btnSpeed = (Button) findViewById(R.id.speedBtn);
        //btnVolts = (Button) findViewById(R.id.voltBtn);

        try {
            typeFaceBtn = Typeface.createFromAsset(getAssets(), "fonts/CaviarDreams_Bold.ttf");
            typeFaceTitle = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Initialize debug
        debug = false;

        //Set the font of the title text
        GPSbtn.setTypeface(typeFaceTitle);
        textView3.setTypeface(typeFaceBtn);
        titleText.setTypeface(typeFaceBtn);
        btnConnect.setTypeface(typeFaceBtn);
        btnSettings.setTypeface(typeFaceBtn);
        btnWideband.setTypeface(typeFaceBtn);
        btnBoost.setTypeface(typeFaceBtn);
        btnOil.setTypeface(typeFaceBtn);
        btnCustom.setTypeface(typeFaceBtn);
        btnMulti1.setTypeface(typeFaceBtn);
        btnMulti2.setTypeface(typeFaceBtn);
        btnRPM.setTypeface(typeFaceBtn);
//        btnSpeed.setTypeface(typeFaceBtn);
//        btnVolts.setTypeface(typeFaceBtn);

        sensorData = new Sensor_BinaryData();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)//if the permission for location access is given
            GPSbtn.setText("Start");
        else//if not
            GPSbtn.setText("Activation");

        //Bluetooth LE check
        isBLE = false;
//        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//            isBLE = false;
//        } else { //Bluetooth is supprted, check if it's turned on in settings.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);


        isBLE = !sp.getBoolean("isBluetoothClassic", false);
//        }


        //Check if there is a BluetoothSerialService object being passed back. If true then don't run through initial setup.
        Object obj = PassObject.getObject();

        //Assign it to global mSerialService variable in this activity.
        if (!debug) {
            if (!isBLE) {
                mSerialService = (BluetoothSerialService) obj;

                if (mSerialService != null) {
                    //Update the BluetoothSerialService instance's handler to this activities.
                    mSerialService.setHandler(mHandler);
                    //Update the connection status on the dashboard.
                    if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
                        btnConnect.setText("Connected! \n Tap to Disconnect");
                    } else {
                        btnConnect.setText("Connect");
                    }
                } else {
                    //Looks like an initial launch - Call the method that sets up bluetooth on the device.
                    btnConnect.setText("Connect");
                    if (!debug) {
                        setupBT();
                    }
                }
            } else {

                //Bluetooth LE branch for oncreate
                _bluetoothLEService = (BluetoothLeService) obj;

                if (_bluetoothLEService != null) {
                    _bluetoothLEService.setHandler(_BLEHandler);
                    if (_bluetoothLEService.getRSSI()) { //OLD: getBLEConnectionState() == BluetoothLeService.STATE_CONNECTED
                        btnConnect.setText("Connected! \n Tap to Disconnect");
                    } else {
                        btnConnect.setText("Connect");
                    }
                } else { //_bluetoothLeService is null
                    btnConnect.setText("Connect");
                }
            }
        }



        GetSensorTimerTask tmrGetSensor = new GetSensorTimerTask(this);
        Timer t = new Timer();
        t.scheduleAtFixedRate(tmrGetSensor, 0, UPDATE_SENSOR_TMR);
    }
    @Override//GPS Location Listener settings
    public void onLocationChanged(Location location) {//when the location has changed
        if(counts==0) {//If the counter is 0 which means it is the first time that the location has changed
            start = System.nanoTime();//a timer starts
            lat1 = location.getLatitude();//the coordinates are saved
            lon1 = location.getLongitude();
            distance = 0;//I initialize the distance
            //alt1 = location.getAltitude();//this would also save the altitude
            counts +=1;
        }
        else if (counts == 1){//if the location has changed 5 times (I choose 5 times nad not 0 in order for the calculations to be less)
            finish = System.nanoTime();//I finish the timer
            lat2 = location.getLatitude();//the coordinates are saved
            lon2 = location.getLongitude();
            //alt2 = location.getAltitude();
            time = finish - start;//I save the time in nanoseconds
            time1 = (double)time / 1_000_000_000.0;//I convert time in seconds
            distance = distance + measureDistance(lat1,lat2,lon1,lon2);//I calculate the distance between two points through my custom method measureDistance
            speed = distance / time1; //calculate the speed in meters/second
            speed = speed * 2.23694; //convert speed from m/s to mph change to 3.6 for km/h
            //speed = (int) speed; //i get rid of the decimal numbers
            textView3.setText(String.format("%.2f", speed));//the speed is shown

            start = 0; //restart time and counter
            finish = 0;
            counts=0;
          TinyDB tinyDB = new TinyDB(getApplicationContext());
            tinyDB.putDouble("GPSspeed", speed);
            System.out.println(String.format(("speed:%.2f"), speed));//debug -NO CODES LIKE THIS ARE NEEDED FOR RUNTIME. More follow...
           // checkSpeed(lat2,lon2,speed);//method that checks if current speed is above the limit
            //checkPOIs(lat2,lon2);//method that checks if we are within radius of a poi
        }
        else{
            lat2 = location.getLatitude();
            lon2 = location.getLongitude();
            distance = distance + measureDistance(lat1,lat2,lon1,lon2);//i calculate the distance travelled
            lat1 = lat2;
            lon1 = lon2;
            counts +=1;
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void start(View view) {//on start button click
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && GPSbtn.getText() == "Start") {//if the permission for location access is given
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);//we request location updates
            GPSbtn.setText("Exit");
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && GPSbtn.getText() == "Exit") {//if we want to exit the app
            System.exit(0);
        } else if (GPSbtn.getText() == "Activation") {//if we haven't given permission yet
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 6);//we request the permission
            GPSbtn.setText("Start");
        }
    }


    public double measureDistance(Double lat1, Double lat2, Double lon1, Double lon2) {//custom method that calculates distance between two points
        //formula found on the web
        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        return distance;
    }


    public void onClickActivity(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.connectBtn:
                if (!isBLE) {
                    connectDevice(); //classic
                } else {
                    connectBLE(); //BLE
                }
                break;
            case R.id.settingsBtn:
                passBluetooth();
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                break;
            case R.id.widebandBtn:
                passBluetooth();
                startActivity(new Intent(getApplicationContext(), WidebandActivity.class));
                break;
            case R.id.customBtn:
                passBluetooth();
                startActivity(new Intent(getApplicationContext(), TemperatureActivity.class));
                break;
            case R.id.boostBtn:
                passBluetooth();
                startActivity(new Intent(getApplicationContext(), BoostActivity.class));
                break;
            case R.id.rpmBtn:
                passBluetooth();
                startActivity(new Intent(getApplicationContext(), RPMActivity.class));
                break;
//            case R.id.speedBtn:
//                passBluetooth();
//                startActivity(new Intent(getApplicationContext(), SpeedActivity.class));
//                break;
//            case R.id.voltBtn:
//                passBluetooth();
//                startActivity(new Intent(getApplicationContext(), VoltageActivity.class));
//                break;
            case R.id.oilBtn:
                passBluetooth();
                startActivity(new Intent(getApplicationContext(), OilActivity.class));
                break;
            case R.id.multiBtn1:
                passBluetooth();
                startActivity(new Intent(getApplicationContext(), TwoGaugeActivity.class));
                break;
            case R.id.multiBtn2:
                passBluetooth();
                startActivity(new Intent(getApplicationContext(), FourGaugeActivity.class));
                break;
            default:
                break;
        }
    }

    private void passBluetooth() {
        if (!isBLE) {
            PassObject.setObject(mSerialService);
            PassObject.setType(1);

        } else {
            PassObject.setObject(_bluetoothLEService);
            PassObject.setType(2);
        }
    }

    public void onDestroy() {
        super.onDestroy();

        if (mSerialService != null) {
            Log.d(TAG, "onDestroy()");
            mSerialService.stop();
        }
        if (_bluetoothLEService != null) {
            _bluetoothLEService.disconnect();
            _bluetoothLEService.close();
        }
    }

    //TODO: I think the passobject is holding onto a null _bluetoothLeService.

    public void onResume() {
        super.onResume();
        turnOnBluetooth();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        isBLE = !sp.getBoolean("isBluetoothClassic", false);
        if (!debug) {
            if (!isBLE) {
                if (mSerialService != null) {
                    mSerialService.setHandler(mHandler);
                } else {
                    setupBT();
                }
            } else {
                if (isBLEConnected()) {
                    btnConnect.setText("Connected! \n Tap to Disconnect");
                } else { // try to get the BLE service from the calling activity
                    Object obj = PassObject.getObject();
                    if (obj instanceof BluetoothLeService) {
                        _bluetoothLEService = (BluetoothLeService) obj;
                    }

                    if (_bluetoothLEService != null) {
                        _bluetoothLEService.setHandler(_BLEHandler);
                        if (isBLEConnected()) { //OLD: getBLEConnectionState() == BluetoothLeService.STATE_CONNECTED
                            btnConnect.setText("Connected! \n Tap to Disconnect");
                        } else {
                            btnConnect.setText("Connect");
                        }
                    } else { //_bluetoothLeService is null
                        btnConnect.setText("Connect");
                    }
                }
            }
        }
    }

    /*show what's new dialog*/

    private void showWhatsNew() {
        SharedPreferences sharedPref = getSharedPreferences(PRIVATE_PREF, this.MODE_PRIVATE);
        int currentVersionNumber = 0;
        int savedVersionNumber = sharedPref.getInt(VERSION_KEY, 0);

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersionNumber = pi.versionCode;
        } catch (Exception e) {
            //do something
        }

        if (currentVersionNumber > savedVersionNumber) {
            showWhatsNewDialog();
            Editor editor = sharedPref.edit();
            editor.putInt(VERSION_KEY, currentVersionNumber);
            editor.commit();
        }
    }

    private void showWhatsNewDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_whatsnew, null);
        Builder builder = new AlertDialog.Builder(this);
        builder.setView(view).setTitle("Whats New").setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }


    /* classic bluetooth area */

    public int getConnectionState() {
        return mSerialService.getState();
    }

    public void setupBT() {
        Log.d(TAG, "made it to setupBT");
        //Get the bluetooth device adapter, if there is not one, toast.
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (mSerialService == null) {
            mSerialService = new BluetoothSerialService(this, mHandler);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_CONNECT_DEVICE:

                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    //txtView.setText(txtView.getText()+ "\n\n" + device.getName() + "\n" +device.getAddress());
                    mSerialService.connect(device);
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    //If OK (device contains bluetooth connectivity, user did not click "no"):
                    Toast.makeText(getApplicationContext(), "Enabled Bluetooth OK", Toast.LENGTH_SHORT).show();
                } else {
                    //If NOT OK, say so.
                    Toast.makeText(getApplicationContext(), "Bluetooth NOT enabled or not Present", Toast.LENGTH_SHORT).show();
                }
        }
    }


    public void connectDevice() {
        if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        } else if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
            if (mSerialService != null) {
                mSerialService.stop();
            }
            //mSerialService.start(); //--potential error, leaving for now.
//        }else if(getConnectionState() == BluetoothSerialService.STATE_CONNECTING){
//            if(mSerialService != null){
//                mSerialService.stop();
//            }
//            //mSerialService.start(); //-- potential error, leaving for now.
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE1: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothSerialService.STATE_CONNECTED:
                            btnConnect.setClickable(true);
                            btnConnect.setText("Connected! \n Tap to Disconnect");
                            break;
                        case BluetoothSerialService.STATE_CONNECTING:
                            btnConnect.setText("Connecting...");
                            break;
                        case BluetoothSerialService.STATE_LISTEN:
                            btnConnect.setClickable(true);
                            break;
                        case BluetoothSerialService.STATE_NONE:
                            btnConnect.setClickable(true);
                            btnConnect.setText("Connect");
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
//                byte[] writeBuf = (byte[]) msg.obj;
//                // construct a string from the buffer
//                String writeMessage = new String(writeBuf);
                    ////mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MESSAGE_READ:
                    int intReadMessage = 0;

                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    try {
                        intReadMessage = Integer.parseInt(readMessage);
                        intReadMsgPrevious = intReadMessage;
                    } catch (NumberFormatException e) {
                        intReadMessage = intReadMsgPrevious;
                    }

                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    // mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    //                Toast.makeText(getApplicationContext(), "Connected to "
                    //                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    /* Bluetooth LE area */

    // The Handler that gets information back from the BluetoothLeService
    private final Handler _BLEHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BLE_MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE1: " + msg.arg1);
                    switch (msg.arg1) {
                        case BLE_MESSAGE_DISCONNECTED:
                            btnConnect.setText("Connect");
                            break;
                        case BLE_MESSAGE_CONNECTING:
                            btnConnect.setText("Connecting..");
                            break;
                        case BLE_MESSAGE_CONNECTED:
                            btnConnect.setClickable(true);
                            btnConnect.setText("Connected! \n Tap to Disconnect");
                            break;

                    }
            }
        }
    };


    private void connectBLE() {
        if (!isBLEConnected()) {
            Intent serverIntent = new Intent(this, BLEScanActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        } else if (isBLEConnected()) {
            if (_bluetoothLEService != null) {
                Log.d(TAG, "disconnecting BLE...");
                _bluetoothLEService.disconnect();
                _bluetoothLEService.close();
                btnConnect.setText("Connect");
            }
        } else {
            //disconnect so you can reconnect
            _bluetoothLEService.disconnect();
            Intent serverIntent = new Intent(this, BLEScanActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        }
    }

    //deprecated for now.
    private boolean getBLEConnectionState() {
        if (_bluetoothLEService != null) {
            return _bluetoothLEService.getRSSI();
        }
        return false;
    }

    private boolean isBLEConnected() {
        if (_bluetoothLEService != null) {
            return _bluetoothLEService.getRSSI();
        }
        return false;
    }

    private void turnOnBluetooth() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    class GetSensorTimerTask extends java.util.TimerTask {
        private PSensor m_pSensor;

        public GetSensorTimerTask(PSensor _pParent) {
            m_pSensor = _pParent;
        }

        public void run() {
            if (m_pSensor == null) {
                return;
            }
            if (m_pSensor.mSerialService.getState() == BluetoothSerialService.STATE_CONNECTED) {
                byte[] txData = new byte[2];
                txData[0] = 3;
                txData[1] = 4;
                //m_pSensor.mSerialService.initRxState();
                m_pSensor.mSerialService.write(txData);
            }
        }
    }


    public class Map16x1_NTC_ECT extends Map16x1 {
        public Map16x1_NTC_ECT() {
            mapData[0] = 130; //0
            mapData[1] = 65; //16
            mapData[2] = 38; //32
            mapData[3] = 28; //48
            mapData[4] = 18; //64
            mapData[5] = 10; //80
            mapData[6] = 2; //96
            mapData[7] = -6;
            mapData[8] = -14;
            mapData[9] = -22;
            mapData[10] = -30;
            mapData[11] = -38;
            mapData[12] = -46;
            mapData[13] = -54;
            mapData[14] = -62;
            mapData[15] = -62;
            mapData[16] = -62;
        }
    }

    public class Map16x1_NTC_IAT extends Map16x1 {
        public Map16x1_NTC_IAT() {
            mapData[0] = 117; //0
            mapData[1] = 108; //16
            mapData[2] = 99; //32
            mapData[3] = 90; //48
            mapData[4] = 81; //64
            mapData[5] = 72; //80
            mapData[6] = 63; //96
            mapData[7] = 54;
            mapData[8] = 45;
            mapData[9] = 36;
            mapData[10] = 27;
            mapData[11] = 18;
            mapData[12] = 9;
            mapData[13] = 0;
            mapData[14] = -9;
            mapData[15] = -18;
            mapData[16] = -27;
        }
    }

    public class Map16x1_ISV extends Map16x1 {
        public Map16x1_ISV() {
            mapData[0] = 0;
            mapData[1] = 0x23;
            mapData[2] = 0x42;
            mapData[3] = 0x60;
            mapData[4] = 0x7C;
            mapData[5] = 0x93;
            mapData[6] = 0xA7;
            mapData[7] = 0xB7;
            mapData[8] = 0xC6;
            mapData[9] = 0xD1;
            mapData[10] = 0xDB;
            mapData[11] = 0xE4;
            mapData[12] = 0xEB;
            mapData[13] = 0xF1;
            mapData[14] = 0xF7;
            mapData[15] = 0xFB;
            mapData[16] = 0xFB;
        }
    }

    public class Map16x1_Voltage extends Map16x1 {
        public Map16x1_Voltage() {
            mapData[0] = 0;
            mapData[1] = 1.6;
            mapData[2] = 3.2;
            mapData[3] = 4.8;
            mapData[4] = 6.4;
            mapData[5] = 8;
            mapData[6] = 9.6;
            mapData[7] = 11.2;
            mapData[8] = 12.8;
            mapData[9] = 14.3;
            mapData[10] = 15.9;
            mapData[11] = 17.5;
            mapData[12] = 19.1;
            mapData[13] = 20.7;
            mapData[14] = 22.3;
            mapData[15] = 23.9;
            mapData[16] = 25.5;
        }
    }

    public class Map16x1_RPM6500 extends Map16x1 {
        public Map16x1_RPM6500() {
            mapData[0] = 6500;
            mapData[1] = 6000;
            mapData[2] = 5501;
            mapData[3] = 5000;
            mapData[4] = 4500;
            mapData[5] = 4000;
            mapData[6] = 3500;
            mapData[7] = 3200;
            mapData[8] = 2900;
            mapData[9] = 2600;
            mapData[10] = 2300;
            mapData[11] = 1900;
            mapData[12] = 1500;
            mapData[13] = 1200;
            mapData[14] = 900;
            mapData[15] = 700;
            mapData[16] = 500;
        }
    }

    public class Map16x1_RPM7350 extends Map16x1 {
        public Map16x1_RPM7350() {
            mapData[0] = 7350;
            mapData[1] = 6850;
            mapData[2] = 6350;
            mapData[3] = 5850;
            mapData[4] = 5350;
            mapData[5] = 4850;
            mapData[6] = 4350;
            mapData[7] = 3900;
            mapData[8] = 3450;
            mapData[9] = 3050;
            mapData[10] = 2600;
            mapData[11] = 2150;
            mapData[12] = 1700;
            mapData[13] = 1300;
            mapData[14] = 1000;
            mapData[15] = 700;
            mapData[16] = 500;
        }
    }

    public class Sensor_BinaryData {
        public long time;
        public int rpm;
        public float boost;
        public short throttle;
        public float lambda;
        public float lmm;
        public float casetemp;
        public long egt0;
        public long egt1;
        public long egt2;
        public long egt3;
        public long egt4;
        public long egt5;
        public long egt6;
        public long egt7;
        public float batVolt;
        public long vdo_pres1;
        public long vdo_pres2;
        public long vdo_pres3;
        public long vdo_temp1;
        public long vdo_temp2;
        public long vdo_temp3;

        public float speedMD;
        public short gear;
        public short n75;
        public float n75_req_boost;
        public short n75_req_boost_pwm;
        public short flags;
        public float efr_speed_tmp;
        public float efr_speed;
        public float knock;
        public short df_boost_raw;
        public short df_lambda;
        public short df_raw_knock;
        public short df_ect_raw;
        public short df_iat_raw;
        public short df_co_poti;
        public short df_flags;
        public short df_ign_raw;
        public short df_rpm_map;
        public short df_lc_flags;
        public short df_cyl1_knock_retard;
        public short df_cyl1_knock_decay;
        public short df_cyl2_knock_retard;
        public short df_cyl2_knock_decay;
        public short df_cyl3_knock_retard;
        public short df_cyl3_knock_decay;
        public short df_cyl4_knock_retard;
        public short df_cyl4_knock_decay;
        public short df_sci_counter;
        public short df_voltage_raw;
        public long df_inj_time;
        public short df_cold_startup_enrichment;
        public short df_warm_startup_enrichment;
        public short df_ect_enrichment;
        public short df_acceleration_enrichment;
        public short df_counter_startup_enrichment;
        public short df_iat_enrichment;
        public short df_ignition_addon_counter;
        public short df_igniton_addon;
        public short df_ect_injection_addon;
        public short df_isv;
        public long df_rpm_delta_hall;
        public float df_computed_rpm;
        public long df_freq;
        public short df_active_frame;
        public float df_ignition_total_retard;
        public float df_ect;
        public float df_iat;
        public float df_ignition;
        public float df_voltage;


        Map16x1_NTC_ECT dfEctMap = new Map16x1_NTC_ECT();
        Map16x1_NTC_IAT dfIatMap = new Map16x1_NTC_IAT();
        Map16x1_Voltage dfVoltageMap = new Map16x1_Voltage();

        int nCurBuffID = 0;
        boolean bFirst = true;
        private final int BUFFSIZE = 10;

        int[] m_arpm = new int[BUFFSIZE];
        float[] m_aboost = new float[BUFFSIZE];
        short[] m_athrottle = new short[BUFFSIZE];
        float[] m_alambda = new float[BUFFSIZE];
        float[] m_almm = new float[BUFFSIZE];
        float[] m_acasetemp = new float[BUFFSIZE];
        long[] m_aegt0 = new long[BUFFSIZE];
        long[] m_aegt1 = new long[BUFFSIZE];
        long[] m_aegt2 = new long[BUFFSIZE];
        long[] m_aegt3 = new long[BUFFSIZE];
        long[] m_aegt4 = new long[BUFFSIZE];
        long[] m_aegt5 = new long[BUFFSIZE];
        long[] m_aegt6 = new long[BUFFSIZE];
        long[] m_aegt7 = new long[BUFFSIZE];
        float[] m_abatVolt = new float[BUFFSIZE];
        long[] m_avdo_pres1 = new long[BUFFSIZE];
        long[] m_avdo_pres2 = new long[BUFFSIZE];
        long[] m_avdo_pres3 = new long[BUFFSIZE];
        long[] m_avdo_temp1 = new long[BUFFSIZE];
        long[] m_avdo_temp2 = new long[BUFFSIZE];
        long[] m_avdo_temp3 = new long[BUFFSIZE];

        float[] m_aspeed = new float[BUFFSIZE];
        short[] m_agear = new short[BUFFSIZE];
        short[] m_an75 = new short[BUFFSIZE];
        float[] m_an75_req_boost = new float[BUFFSIZE];
        short[] m_an75_req_boost_pwm = new short[BUFFSIZE];
        short[] m_aflags = new short[BUFFSIZE];
        float[] m_aefr_speed = new float[BUFFSIZE];
        float[] m_aknock = new float[BUFFSIZE];
        short[] m_adf_boost_raw = new short[BUFFSIZE];
        short[] m_adf_lambda = new short[BUFFSIZE];
        short[] m_adf_raw_knock = new short[BUFFSIZE];
        short[] m_adf_ect_raw = new short[BUFFSIZE];
        short[] m_adf_iat_raw = new short[BUFFSIZE];
        short[] m_adf_co_poti = new short[BUFFSIZE];
        short[] m_adf_flags = new short[BUFFSIZE];
        short[] m_adf_ign_raw = new short[BUFFSIZE];
        //short[] m_adf_rpm_map = new short[BUFFSIZE];
        short[] m_adf_lc_flags = new short[BUFFSIZE];
        short[] m_adf_cyl1_knock_retard = new short[BUFFSIZE];
        short[] m_adf_cyl1_knock_decay = new short[BUFFSIZE];
        short[] m_adf_cyl2_knock_retard = new short[BUFFSIZE];
        short[] m_adf_cyl2_knock_decay = new short[BUFFSIZE];
        short[] m_adf_cyl3_knock_retard = new short[BUFFSIZE];
        short[] m_adf_cyl3_knock_decay = new short[BUFFSIZE];
        short[] m_adf_cyl4_knock_retard = new short[BUFFSIZE];
        short[] m_adf_cyl4_knock_decay = new short[BUFFSIZE];
        //short[] m_adf_sci_counter = new short[BUFFSIZE];
        short[] m_adf_voltage_raw = new short[BUFFSIZE];
        long[] m_adf_inj_time = new long[BUFFSIZE];
        short[] m_adf_cold_startup_enrichment = new short[BUFFSIZE];
        short[] m_adf_warm_startup_enrichment = new short[BUFFSIZE];
        short[] m_adf_ect_enrichment = new short[BUFFSIZE];
        short[] m_adf_acceleration_enrichment = new short[BUFFSIZE];
        short[] m_adf_counter_startup_enrichment = new short[BUFFSIZE];
        short[] m_adf_iat_enrichment = new short[BUFFSIZE];
        short[] m_adf_ignition_addon_counter = new short[BUFFSIZE];
        short[] m_adf_igniton_addon = new short[BUFFSIZE];
        short[] m_adf_ect_injection_addon = new short[BUFFSIZE];
        short[] m_adf_isv = new short[BUFFSIZE];
        long[] m_adf_rpm_delta_hall = new long[BUFFSIZE];
        float[] m_adf_computed_rpm = new float[BUFFSIZE];
        long[] m_adf_freq = new long[BUFFSIZE];
        short[] m_adf_active_frame = new short[BUFFSIZE];
        float[] m_adf_ignition_total_retard = new float[BUFFSIZE];
        float[] m_adf_ect = new float[BUFFSIZE];
        float[] m_adf_iat = new float[BUFFSIZE];
        float[] m_adf_ignition = new float[BUFFSIZE];
        float[] m_adf_voltage = new float[BUFFSIZE];

        public void parseInput(byte[] rcvData) {
            int base = 2;

            long nTmp = 0;
            float flTmp = 0;
            int i = 0;

            time = Util.byte2long(rcvData[base + 0], rcvData[base + 1], rcvData[base + 2], rcvData[base + 3]);
            //System.out.println(String.format(("Time: %d"), time));
            base += 4;

            m_arpm[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_arpm[i] = m_arpm[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_arpm[i];
            }
            rpm = (int) (nTmp / BUFFSIZE);
            //System.out.println(String.format(("RPM: %d"), rpm));
            base += 2;

            //absolute boost!
            m_aboost[nCurBuffID] = fixed_b100_2double(Util.byte2int(rcvData[base + 0], rcvData[base + 1])) - 1;
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aboost[i] = m_aboost[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_aboost[i];
            }
            boost = limitDigits(flTmp);
            //System.out.println(String.format(("Boost: %.2f"), boost));
            base += 2;

            m_athrottle[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_athrottle[i] = m_athrottle[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_athrottle[i];
            }
            throttle = (short) (nTmp / BUFFSIZE);
            //System.out.println(String.format(("throttle: %d"), throttle));
            base += 1;


            m_alambda[nCurBuffID] = fixed_b100_2double(Util.byte2int(rcvData[base + 0], rcvData[base + 1]));
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_alambda[i] = m_alambda[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_alambda[i];
            }
            lambda = limitDigits(flTmp);
            //System.out.println(String.format(("Lambda: %.2f"), lambda));
            base += 2;

            m_almm[nCurBuffID] = fixed_b100_2double(Util.byte2int(rcvData[base + 0], rcvData[base + 1]));
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_almm[i] = m_almm[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_almm[i];
            }
            lmm = limitDigits(flTmp);
            //System.out.println(String.format(("LMM: %.2f"), lmm));
            base += 2;

            m_acasetemp[nCurBuffID] = fixed_b100_2double(Util.byte2int(rcvData[base + 0], rcvData[base + 1]));
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_acasetemp[i] = m_acasetemp[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_acasetemp[i];
            }
            casetemp = limitDigits(flTmp);
            //System.out.println(String.format(("CaseTemp: %.2f"), casetemp));
            base += 2;

            m_aegt0[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aegt0[i] = m_aegt0[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_aegt0[i];
            }
            egt0 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("ATG0: %d"), egt0));
            base += 2;
            m_aegt1[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aegt1[i] = m_aegt1[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_aegt1[i];
            }
            egt1 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("AGT1: %d"), egt1));
            base += 2;
            m_aegt2[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aegt2[i] = m_aegt2[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_aegt2[i];
            }
            egt2 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("ATG2: %d"), egt2));
            base += 2;
            m_aegt3[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aegt3[i] = m_aegt3[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_aegt3[i];
            }
            egt3 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("EGT3: %d"), egt3));
            base += 2;
            m_aegt4[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aegt4[i] = m_aegt4[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_aegt4[i];
            }
            egt4 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("EGT4: %d"), egt4));
            base += 2;
            m_aegt5[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aegt5[i] = m_aegt5[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_aegt5[i];
            }
            egt5 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("EGT5: %d"), egt5));
            base += 2;
            m_aegt6[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aegt6[i] = m_aegt6[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_aegt6[i];
            }
            egt6 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("EGT6: %d"), egt6));
            base += 2;
            m_aegt7[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aegt7[i] = m_aegt7[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_aegt7[i];
            }
            egt7 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("EGT7: %d"), egt7));
            base += 2;

            m_abatVolt[nCurBuffID] = fixed_b100_2double(Util.byte2int(rcvData[base + 0], rcvData[base + 1]));
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_abatVolt[i] = m_abatVolt[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_abatVolt[i];
            }
            batVolt = limitDigits(flTmp);
            //System.out.println(String.format(("Battery Volt: %.2f"), batVolt));
            base += 2;

            //FIXME pressure / temp ints???
            m_avdo_pres1[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_avdo_pres1[i] = m_avdo_pres1[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_avdo_pres1[i];
            }
            vdo_pres1 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("VDOPres1: %d"), vdo_pres1));
            base += 2;

            m_avdo_pres2[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_avdo_pres2[i] = m_avdo_pres2[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_avdo_pres2[i];
            }
            vdo_pres2 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("VDOPress2: %d"), vdo_pres2));
            base += 2;
            m_avdo_pres3[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_avdo_pres3[i] = m_avdo_pres3[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_avdo_pres3[i];
            }
            vdo_pres3 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("VDOPress3: %d"), vdo_pres3));
            base += 2;
            m_avdo_temp1[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_avdo_temp1[i] = m_avdo_temp1[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_avdo_temp1[i];
            }
            vdo_temp1 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("VDOTemp1: %d"), vdo_temp1));
            base += 2;
            m_avdo_temp2[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_avdo_temp2[i] = m_avdo_temp2[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_avdo_temp2[i];
            }
            vdo_temp2 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("VDOTemp2: %d"), vdo_temp2));
            base += 2;

            m_avdo_temp3[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_avdo_temp3[i] = m_avdo_temp3[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_avdo_temp3[i];
            }
            vdo_temp3 = (nTmp / BUFFSIZE);
            //System.out.println(String.format(("VDOTemp3: %d"), vdo_temp3));
            base += 2;

            //speed = rcvData[base+0] + (rcvData[base+1] << 8);
            m_aspeed[nCurBuffID] = fixed_b100_2double(Util.byte2int(rcvData[base + 0], rcvData[base + 1]));
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aspeed[i] = m_aspeed[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_aspeed[i];
            }
            speedMD = limitDigits(flTmp);
            //System.out.println(String.format(("SpeedMD: %.2f"), speedMD));
            base += 2;

            m_agear[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_agear[i] = m_agear[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_agear[i];
            }
            gear = (short)(nTmp / BUFFSIZE);
            //System.out.println(String.format(("Gear: %d"), gear));
            base += 1;
            m_an75[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_an75[i] = m_an75[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_an75[i];
            }
            n75 = (short)(nTmp / BUFFSIZE);
            //System.out.println(String.format(("N75 Duty: %d"), n75));
            base += 1;
            m_an75_req_boost[nCurBuffID] = fixed_b100_2double(Util.byte2int(rcvData[base + 0], rcvData[base + 1]));
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_an75_req_boost[i] = m_an75_req_boost[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_an75_req_boost[i];
            }
            n75_req_boost = limitDigits(flTmp);
            //System.out.println(String.format(("N75 Req Bst: %.2f"), n75_req_boost));
            base += 2;
            m_an75_req_boost_pwm[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_an75_req_boost_pwm[i] = m_an75_req_boost_pwm[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_an75_req_boost_pwm[i];
            }
            n75_req_boost_pwm = (short)(nTmp / BUFFSIZE);
            //System.out.println(String.format(("N75 Boost PWM: %d"), n75_req_boost_pwm));
            base++;
            m_aflags[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aflags[i] = m_aflags[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_aflags[i];
            }
            flags = (short)(nTmp / BUFFSIZE);
            base += 1;
            efr_speed_tmp = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);

            m_aefr_speed[nCurBuffID] = 0;
            if (efr_speed_tmp == 0xFFFF)
                m_aefr_speed[nCurBuffID] = 0;
            else
                m_aefr_speed[nCurBuffID] = 40000000 / (efr_speed_tmp);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aefr_speed[i] = m_aefr_speed[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_aefr_speed[i];
            }
            efr_speed = (nTmp / BUFFSIZE);
            base += 2;
            m_aknock[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_aknock[i] = m_aknock[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_aknock[i];
            }
            knock = (nTmp / BUFFSIZE);
            base += 2;

            // digifant

            //remember: 68HC11 is big endian!!!
            //avr8 is little endian
            m_adf_boost_raw[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_boost_raw[i] = m_adf_boost_raw[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_boost_raw[i];
            }
            df_boost_raw = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_lambda[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_lambda[i] = m_adf_lambda[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_lambda[i];
            }
            df_lambda = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_raw_knock[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_raw_knock[i] = m_adf_raw_knock[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_raw_knock[i];
            }
            df_raw_knock = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_ect_raw[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_ect_raw[i] = m_adf_ect_raw[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_ect_raw[i];
            }
            df_ect_raw = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_iat_raw[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_iat_raw[i] = m_adf_iat_raw[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_iat_raw[i];
            }
            df_iat_raw = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_co_poti[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_co_poti[i] = m_adf_co_poti[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_co_poti[i];
            }
            df_co_poti = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_flags[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_flags[i] = m_adf_flags[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_flags[i];
            }
            df_flags = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_ign_raw[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_ign_raw[i] = m_adf_ign_raw[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_ign_raw[i];
            }
            df_ign_raw = (short)(nTmp / BUFFSIZE);
            base += 1;
            //changed 2013-9-18
            df_rpm_map = 0;
            m_adf_lc_flags[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_lc_flags[i] = m_adf_lc_flags[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_lc_flags[i];
            }
            df_lc_flags = (short)(nTmp / BUFFSIZE);
            //df_rpm_map = rcvData[base+0];
            base += 1;
            m_adf_cyl1_knock_retard[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_cyl1_knock_retard[i] = m_adf_cyl1_knock_retard[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_cyl1_knock_retard[i];
            }
            df_cyl1_knock_retard = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_cyl1_knock_decay[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_cyl1_knock_decay[i] = m_adf_cyl1_knock_decay[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_cyl1_knock_decay[i];
            }
            df_cyl1_knock_decay = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_cyl2_knock_retard[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_cyl2_knock_retard[i] = m_adf_cyl2_knock_retard[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_cyl2_knock_retard[i];
            }
            df_cyl2_knock_retard = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_cyl2_knock_decay[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_cyl2_knock_decay[i] = m_adf_cyl2_knock_decay[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_cyl2_knock_decay[i];
            }
            df_cyl2_knock_decay = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_cyl3_knock_retard[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_cyl3_knock_retard[i] = m_adf_cyl3_knock_retard[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_cyl3_knock_retard[i];
            }
            df_cyl3_knock_retard = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_cyl3_knock_decay[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_cyl3_knock_decay[i] = m_adf_cyl3_knock_decay[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_cyl3_knock_decay[i];
            }
            df_cyl3_knock_decay = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_cyl4_knock_retard[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_cyl4_knock_retard[i] = m_adf_cyl4_knock_retard[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_cyl4_knock_retard[i];
            }
            df_cyl4_knock_retard = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_cyl4_knock_decay[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_cyl4_knock_decay[i] = m_adf_cyl4_knock_decay[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_cyl4_knock_decay[i];
            }
            df_cyl4_knock_decay = (short)(nTmp / BUFFSIZE);
            base += 1;
            //removed 2013-9-18
            df_sci_counter = 0;
            //df_sci_counter = rcvData[base+0];
            //base +=1;
            m_adf_voltage_raw[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_voltage_raw[i] = m_adf_voltage_raw[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_voltage_raw[i];
            }
            df_voltage_raw = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_inj_time[nCurBuffID] = (Util.byte2int(rcvData[base + 1], rcvData[base + 0])) * 2;
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_inj_time[i] = m_adf_inj_time[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_inj_time[i];
            }
            df_inj_time = (nTmp / BUFFSIZE);
            base += 2;
            m_adf_cold_startup_enrichment[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_cold_startup_enrichment[i] = m_adf_cold_startup_enrichment[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_cold_startup_enrichment[i];
            }
            df_cold_startup_enrichment = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_warm_startup_enrichment[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_warm_startup_enrichment[i] = m_adf_warm_startup_enrichment[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_warm_startup_enrichment[i];
            }
            df_warm_startup_enrichment = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_ect_enrichment[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_ect_enrichment[i] = m_adf_ect_enrichment[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_ect_enrichment[i];
            }
            df_ect_enrichment = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_acceleration_enrichment[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_acceleration_enrichment[i] = m_adf_acceleration_enrichment[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_acceleration_enrichment[i];
            }
            df_acceleration_enrichment = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_counter_startup_enrichment[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_counter_startup_enrichment[i] = m_adf_counter_startup_enrichment[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_counter_startup_enrichment[i];
            }
            df_counter_startup_enrichment = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_iat_enrichment[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_iat_enrichment[i] = m_adf_iat_enrichment[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_iat_enrichment[i];
            }
            df_iat_enrichment = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_ignition_addon_counter[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_ignition_addon_counter[i] = m_adf_ignition_addon_counter[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_ignition_addon_counter[i];
            }
            df_ignition_addon_counter = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_igniton_addon[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_igniton_addon[i] = m_adf_igniton_addon[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_igniton_addon[i];
            }
            df_igniton_addon = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_ect_injection_addon[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_ect_injection_addon[i] = m_adf_ect_injection_addon[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_ect_injection_addon[i];
            }
            df_ect_injection_addon = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_isv[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_isv[i] = m_adf_isv[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_isv[i];
            }
            df_isv = (short)(nTmp / BUFFSIZE);
            base += 1;
            m_adf_rpm_delta_hall[nCurBuffID] = Util.byte2int(rcvData[base + 1], rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_rpm_delta_hall[i] = m_adf_rpm_delta_hall[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_rpm_delta_hall[i];
            }
            df_rpm_delta_hall = (short)(nTmp / BUFFSIZE);
            base += 2;
            m_adf_computed_rpm[nCurBuffID] = 0;
            if (df_rpm_delta_hall > 0) {
                m_adf_computed_rpm[nCurBuffID] = (30000000.0f / df_rpm_delta_hall);
            }
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_computed_rpm[i] = m_adf_computed_rpm[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_adf_computed_rpm[i];
            }
            df_computed_rpm = limitDigits(flTmp);
            //from avr little endian!
            m_adf_freq[nCurBuffID] = Util.byte2int(rcvData[base + 0], rcvData[base + 1]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_freq[i] = m_adf_freq[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_freq[i];
            }
            df_freq = (nTmp / BUFFSIZE);
            base += 2;
            m_adf_active_frame[nCurBuffID] = Util.byte2short(rcvData[base + 0]);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_active_frame[i] = m_adf_active_frame[0];
                }
            }
            nTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                nTmp += m_adf_active_frame[i];
            }
            df_active_frame = (short)(nTmp / BUFFSIZE);
            base += 1;
//            if ( df_active_frame < 255 )
//                df_connected = true;

            m_adf_ignition_total_retard[nCurBuffID] = (float) ((df_cyl1_knock_retard + df_cyl2_knock_retard + df_cyl3_knock_retard + df_cyl4_knock_retard) * 0.351563);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_ignition_total_retard[i] = m_adf_ignition_total_retard[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_adf_ignition_total_retard[i];
            }
            df_ignition_total_retard = limitDigits(flTmp);

            m_adf_ect[nCurBuffID] = (float) (dfEctMap.mapValue(df_ect_raw));
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_ect[i] = m_adf_ect[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_adf_ect[i];
            }
            df_ect = limitDigits(flTmp);

            m_adf_iat[nCurBuffID] = (float) (dfIatMap.mapValue(df_iat_raw));
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_iat[i] = m_adf_iat[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_adf_iat[i];
            }
            df_iat = limitDigits(flTmp);

            m_adf_ignition[nCurBuffID] = (float) ((df_ign_raw * -0.351563) + 73.9);
            if ((df_lc_flags & 3) == 1)
                m_adf_ignition[nCurBuffID] = (float) ((2 * df_ign_raw * -0.351563) + 73.9);
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_ignition[i] = m_adf_ignition[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_adf_ignition[i];
            }
            df_ignition = limitDigits(flTmp);

            m_adf_voltage[nCurBuffID] = (float) (dfVoltageMap.mapValue(df_voltage_raw));
            if (bFirst) {
                for (i = 1; i < BUFFSIZE; i++) {
                    m_adf_voltage[i] = m_adf_voltage[0];
                }
            }
            flTmp = 0;
            for (i = 0; i < BUFFSIZE; i++) {
                flTmp += m_adf_voltage[i];
            }
            df_voltage = limitDigits(flTmp);

            bFirst = false;
            nCurBuffID++;
            nCurBuffID %= BUFFSIZE;
            /*System.out.println(String.format(("Voltage: %f"), df_voltage));

            base = 0;
            System.out.println(String.format(("Rx Data: %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d"), rcvData[base + 0], rcvData[base + 1], rcvData[base + 2], rcvData[base + 3], rcvData[base + 4], rcvData[base + 5], rcvData[base + 6], rcvData[base + 7], rcvData[base + 8], rcvData[base + 9], rcvData[base + 10], rcvData[base + 11], rcvData[base + 12], rcvData[base + 13], rcvData[base + 14], rcvData[base + 15]));
            base =16;
            System.out.println(String.format(("Rx Data: %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d"), rcvData[base + 0], rcvData[base + 1], rcvData[base + 2], rcvData[base + 3], rcvData[base + 4], rcvData[base + 5], rcvData[base + 6], rcvData[base + 7], rcvData[base + 8], rcvData[base + 9], rcvData[base + 10], rcvData[base + 11], rcvData[base + 12], rcvData[base + 13], rcvData[base + 14], rcvData[base + 15]));
            base =32;
            System.out.println(String.format(("Rx Data: %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d"), rcvData[base + 0], rcvData[base + 1], rcvData[base + 2], rcvData[base + 3], rcvData[base + 4], rcvData[base + 5], rcvData[base + 6], rcvData[base + 7], rcvData[base + 8], rcvData[base + 9], rcvData[base + 10], rcvData[base + 11], rcvData[base + 12], rcvData[base + 13], rcvData[base + 14], rcvData[base + 15]));
            base =48;
            System.out.println(String.format(("Rx Data: %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d"), rcvData[base + 0], rcvData[base + 1], rcvData[base + 2], rcvData[base + 3], rcvData[base + 4], rcvData[base + 5], rcvData[base + 6], rcvData[base + 7], rcvData[base + 8], rcvData[base + 9], rcvData[base + 10], rcvData[base + 11], rcvData[base + 12], rcvData[base + 13], rcvData[base + 14], rcvData[base + 15]));*/

        }

        float fixed_b100_2double(int in) {
            float result = in;
            result /= 100.0f;
            return result;
        }

        private float limitDigits(float number) {
            long in = (long)(number);
            in *= 10;
            float result = in;
            result /= 100.0f;
            return result;
        }
    }
}

