package com.rAtTrax.AndroUI;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.ViewManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class TwoGaugeActivity extends Activity implements Runnable{
    GaugeBuilder analogGauge1;
    GaugeBuilder analogGauge2;
    MultiGauges  multiGauge1;
    MultiGauges  multiGauge2;
    TextView     txtViewDigital;
    TextView     txtViewDigital2;
    TextView     textViewGS3;
    TextView     textViewGSspeed1;
    ImageButton  btnOne;
    ImageButton  btnTwo;
    Typeface     typeFaceDigital;
    String       currentMsg;
    double       speed;


    float   flt;
    int     minValue; //gauge min.
    int     maxValue; //gauge max.
    double  sensorMinValue; //the lowest value that has been sent from the arduino.
    double  sensorMaxValue; //the highest value that has been sent from the arduino.
    boolean paused;
    Context context;
    float   boostSValue;
    float   wbSValue;
    float   tempSValue;
    float   oilSValue;
    boolean isBLE;

    //Prefs vars
    View    root;
    boolean showAnalog; //Display the analog gauge or not.
    boolean showDigital; //Display the digital gauge or not.
    boolean showNightMode; //Change background to black.
    String  gaugeOnePref;
    String  gaugeTwoPref;
    int     currentTokenOne = 1;
    int     currentTokenTwo = 2;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ         = 2;
    public static final int MESSAGE_WRITE        = 3;
    public static final int MESSAGE_DEVICE_NAME  = 4;
    public static final int MESSAGE_TOAST        = 5;


    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME  = "device_name";
    public static final String TOAST        = "toast";
    private static final int BOOST_TOKEN    = 1;
    private static final int WIDEBAND_TOKEN = 2;
    private static final int TEMP_TOKEN     = 3;
    private static final int OIL_TOKEN      = 4;

    //Bluetooth types
    private static final int CLASSIC_TYPE = 1;
    private static final int BLE_TYPE     = 2;

    BluetoothSerialService mSerialService;
    BluetoothLeService _bluetoothLeService;
    private static Handler workerHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.gauge_layout_2_landscape);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefsInit(); //Load up the preferences.
        context = this;

        //Instantiate the GaugeBuilder.
        analogGauge1    = (GaugeBuilder) findViewById(R.id.analogGauge);
        analogGauge2    = (GaugeBuilder) findViewById(R.id.analogGauge2);
        multiGauge1     = new MultiGauges(context);
        multiGauge2     = new MultiGauges(context);
        txtViewDigital  = (TextView) findViewById(R.id.txtViewDigital);
        txtViewDigital2 = (TextView) findViewById(R.id.txtViewDigital2);
        textViewGS3 = (TextView) findViewById(R.id.textViewGS3);
        textViewGSspeed1 = (TextView) findViewById(R.id.textViewGSspeed1);
        btnOne          = (ImageButton) findViewById(R.id.btnOne);
        btnTwo          = (ImageButton) findViewById(R.id.btnTwo);  
        typeFaceDigital = Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");

        //Set the font of the digital.
        txtViewDigital.setTypeface(typeFaceDigital);
        txtViewDigital2.setTypeface(typeFaceDigital);
        textViewGS3.setTypeface(typeFaceDigital);
        textViewGSspeed1.setTypeface(typeFaceDigital);

        txtViewDigital.setText("0.00");
        txtViewDigital2.setText("0.00");

        //Setup gauge 1
        multiGauge1.setAnalogGauge(analogGauge1);
        multiGauge1.buildGauge(currentTokenOne);

        //Check if the gauge uses negative numbers or not.
        if(analogGauge1.getAbsoluteNumbers()){ 
            txtViewDigital.setText(Float.toString(Math.abs(multiGauge1.getMinValue())));
        }else{
            txtViewDigital.setText(Float.toString(multiGauge1.getMinValue()));
        }

        //Setup gauge 2
        multiGauge2.setAnalogGauge(analogGauge2);
        multiGauge2.buildGauge(currentTokenTwo);

        //Check if the gauge uses negative numbers or not.
        if(analogGauge2.getAbsoluteNumbers()){
            txtViewDigital2.setText(Float.toString(Math.abs(multiGauge2.getMinValue())));
        }else{
            txtViewDigital2.setText(Float.toString(multiGauge2.getMinValue()));
        }

        //Get the mSerialService/BLE service object from the UI activity.
        Object obj = PassObject.getObject();
        int _bluetoothType = PassObject.getType();

        isBLE = false;
        if(_bluetoothType == CLASSIC_TYPE){
            isBLE = false;
        }else if(_bluetoothType == BLE_TYPE){
            isBLE = true;
        }

        //Assign it to global mSerialService variable in this activity.
        if(!isBLE) {
            mSerialService = (BluetoothSerialService) obj;
        }else{
            _bluetoothLeService = (BluetoothLeService) obj;
        }

        //Check if the serial service object is null - assign the handler.
        if(mSerialService != null && !isBLE){
            //Update the BluetoothSerialService instance's handler to this activities.
            mSerialService.setHandler(mHandler);
        }

        if(_bluetoothLeService != null && isBLE){
            _bluetoothLeService.setHandler(mHandler);
        }

        Thread thread = new Thread(TwoGaugeActivity.this);
        thread.start();

        if(!showAnalog){
            ((ViewManager)analogGauge1.getParent()).removeView(analogGauge1); //Remove analog gauge
            ((ViewManager)analogGauge2.getParent()).removeView(analogGauge2); //Remove analog gauge
        }
        if(!showDigital){
            ((ViewManager)txtViewDigital.getParent()).removeView(txtViewDigital); //Remove digital gauge
            ((ViewManager)txtViewDigital2.getParent()).removeView(txtViewDigital2); //Remove digital gauge
        }
        if(showNightMode){
            root = btnOne.getRootView();  //Get root layer view.
            root.setBackgroundColor(getResources().getColor(R.color.black)); //Set background color to black.
        }

    }

    //Handles the data being sent back from the BluetoothSerialService class.
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(!paused){
                byte[] readBuf = (byte[]) msg.obj;
//                // construct a string from the valid bytes in the buffer
//                String readMessage;
//                try {
//                    readMessage = new String(readBuf, 0, msg.arg1);
//                } catch (NullPointerException e) {
//                    readMessage = "0";
//                }
//                //Redraw the needle to the correct value.
//                currentMsg = readMessage;
//
//                //Log.d("Boost HERE", readMessage);

                Message workerMsg = workerHandler.obtainMessage(msg.what, readBuf);
                workerMsg.sendToTarget();
                updateGauges();
            }
        }
    };
    public void run(){
        Looper.prepare();
        workerHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                switch (msg.what) {
                    case PSensor.MESSAGE_STATE_CHANGE:
                        break;
                    case PSensor.MESSAGE_WRITE:
                        break;
                    case PSensor.MESSAGE_READ:
                        PSensor.sensorData.parseInput((byte[])msg.obj);//parseInput((String)msg.obj);
                        switch(currentTokenOne){
                            case 1:
                                multiGauge1.handleSensor(PSensor.sensorData.boost);
                                break;
                            case 2:
                                multiGauge1.handleSensor(PSensor.sensorData.lambda);
                                break;
                            case 3:
                                multiGauge1.handleSensor(PSensor.sensorData.vdo_temp1);
                                break;
                            case 4:
                                multiGauge1.handleSensor(PSensor.sensorData.vdo_pres1);
                                break;
                            default:
                                break;
                        }

                        switch(currentTokenTwo){
                            case 1:
                                multiGauge2.handleSensor(PSensor.sensorData.boost);
                                break;
                            case 2:
                                multiGauge2.handleSensor(PSensor.sensorData.lambda);
                                break;
                            case 3:
                                multiGauge2.handleSensor(PSensor.sensorData.vdo_temp1);
                                break;
                            case 4:
                                multiGauge2.handleSensor(PSensor.sensorData.vdo_pres1);
                                break;
                            default:
                                break;
                        }
                        break;
                    case PSensor.MESSAGE_DEVICE_NAME:
                        break;
                    case PSensor.MESSAGE_TOAST:
                        break;
                    default:
                        break;
                }
            }
        };
        Looper.loop();
    }

    public void updateGauges(){
        analogGauge1.setValue(multiGauge1.getCurrentGaugeValue());
        analogGauge2.setValue(multiGauge2.getCurrentGaugeValue());

        if(analogGauge1.getAbsoluteNumbers()){ 
            txtViewDigital.setText(Float.toString(Math.abs(multiGauge1.getCurrentGaugeValue())));
        }else{
            txtViewDigital.setText(Float.toString(multiGauge1.getCurrentGaugeValue()));
        }
        if(analogGauge2.getAbsoluteNumbers()){ 
            txtViewDigital2.setText(Float.toString(Math.abs(multiGauge2.getCurrentGaugeValue())));
        }else{
            txtViewDigital2.setText(Float.toString(multiGauge2.getCurrentGaugeValue()));

            TinyDB tinyDB = new TinyDB(getApplicationContext());
            //if (speed != null);
            speed = 0.0;
            speed = tinyDB.getDouble("GPSspeed", speed);
            textViewGS3.setText(String.format("%.2f", speed));
        }

    }

//    private void parseInput(String sValue){
//        String[] tokens=sValue.split(","); //split the input into an array.
//
//        try {
//            //Get current tokens for this gauge activity, cast as float.
//            boostSValue = Float.valueOf(tokens[BOOST_TOKEN].toString());
//            wbSValue 	= Float.valueOf(tokens[WIDEBAND_TOKEN].toString());
//            tempSValue 	= Float.valueOf(tokens[TEMP_TOKEN].toString());
//            oilSValue 	= Float.valueOf(tokens[OIL_TOKEN].toString());
//        } catch (NumberFormatException e) {
//            boostSValue = 0;
//            wbSValue 	= 0;
//            tempSValue 	= 0;
//            oilSValue	= 0;
//        } catch (ArrayIndexOutOfBoundsException e){
//            boostSValue = 0;
//            wbSValue 	= 0;
//            tempSValue 	= 0;
//            oilSValue	= 0;
//        }
//    }

    //Activity transfer handling
    public void goHome(View v){
        PassObject.setObject(mSerialService);
        onBackPressed();
        finish();
    }

    @Override
    public void onBackPressed(){
        paused = true;
        passObject();
        super.onBackPressed();
    }
    
    //chart/gauge display click handling
    public void buttonDisplayClick(View v){
        paused = true;
        //workerHandler.getLooper().quit();
        passObject();
        Intent chartIntent = new Intent(this, DualChartActivity.class);
        startActivity(chartIntent);
    }

    //Button one handling.
    public void buttonOneClick(View v){   
        //Reset the max value.
        multiGauge1.setSensorMaxValue(multiGauge1.getMinValue());
        multiGauge2.setSensorMaxValue(multiGauge2.getMinValue());
        paused = false;
        btnTwo.setBackgroundResource(Color.TRANSPARENT);
        Toast.makeText(getApplicationContext(), "Max value reset", Toast.LENGTH_SHORT).show();
    }

    //Button two handling.
    public void buttonTwoClick(View v){
        if(!paused){
            paused = true;
            //set the gauge/digital to the max value captured so far for two seconds.
            if(analogGauge1.getAbsoluteNumbers()){ 
                txtViewDigital.setText(Double.toString(Math.abs(multiGauge1.getSensorMaxValue())));
            }else{
                txtViewDigital.setText(Double.toString(multiGauge1.getSensorMaxValue()));
            }
            if(analogGauge2.getAbsoluteNumbers()){
                txtViewDigital2.setText(Double.toString(Math.abs(multiGauge2.getSensorMaxValue())));
            }else{
                txtViewDigital2.setText(Double.toString(multiGauge2.getSensorMaxValue()));
            }

            analogGauge1.setValue((float)multiGauge1.getSensorMaxValue());
            analogGauge2.setValue((float)multiGauge2.getSensorMaxValue());
            btnTwo.setBackgroundResource(R.drawable.btn_bg_pressed);
        }else{
            paused = false;
            btnTwo.setBackgroundResource(Color.TRANSPARENT);
        }
    }

    protected void onPause(){
        super.onPause();
    }

    protected void onResume(){
        super.onResume();
        Thread thread = new Thread(TwoGaugeActivity.this);
        thread.start();
        analogGauge1.invalidate();
        analogGauge2.invalidate();
    }

    public void prefsInit(){
        SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
        showAnalog = sp.getBoolean("showAnalog", true);
        showDigital = sp.getBoolean("showDigital", true);
        showNightMode = sp.getBoolean("showNightMode", false);
        gaugeOnePref = sp.getString("multiGaugeOne", "boost");
        gaugeTwoPref = sp.getString("multiGaugeTwo", "wb");

        if(gaugeOnePref.equals("Boost")){currentTokenOne = BOOST_TOKEN;}else 
            if(gaugeOnePref.equals("Wideband O2")){currentTokenOne = WIDEBAND_TOKEN;}else 
                if(gaugeOnePref.equals("Temperature")){currentTokenOne = TEMP_TOKEN;}else 
                    if(gaugeOnePref.equals("Oil Pressure")){currentTokenOne = OIL_TOKEN;}
        if(gaugeTwoPref.equals("Boost")){currentTokenTwo = BOOST_TOKEN;}else
            if(gaugeTwoPref.equals("Wideband O2")){currentTokenTwo = WIDEBAND_TOKEN;}else
                if(gaugeTwoPref.equals("Temperature")){currentTokenTwo = TEMP_TOKEN;}else
                    if(gaugeTwoPref.equals("Oil Pressure")){currentTokenTwo = OIL_TOKEN;}
    }

    private void passObject(){
        if(!isBLE){
            PassObject.setObject(mSerialService);
            PassObject.setType(1);
        }else{
            PassObject.setObject(_bluetoothLeService);
            PassObject.setType(2);
        }
    }
}
