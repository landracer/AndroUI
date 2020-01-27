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

public class FourGaugeActivity extends Activity implements Runnable{
    GaugeBuilder analogGauge1;
    GaugeBuilder analogGauge2;
    GaugeBuilder analogGauge3;
    GaugeBuilder analogGauge4;
    GaugeBuilder analogGauge5;
    GaugeBuilder analogGauge6;
    GaugeBuilder analogGauge7;
    GaugeBuilder analogGauge8;
    GaugeBuilder analogGauge9;
    GaugeBuilder analogGauge10;
    GaugeBuilder analogGauge11;
    GaugeBuilder analogGauge12;
    GaugeBuilder analogGauge13;
    GaugeBuilder analogGauge14;
    MultiGauges  multiGauge1;
    MultiGauges  multiGauge2;
    MultiGauges  multiGauge3;
    MultiGauges  multiGauge4;
    MultiGauges  multiGauge5;
    MultiGauges  multiGauge6;
    MultiGauges  multiGauge7;
    MultiGauges  multiGauge8;
    MultiGauges  multiGauge9;
    MultiGauges  multiGauge10;
    MultiGauges  multiGauge11;
    MultiGauges  multiGauge12;
    MultiGauges  multiGauge13;
    MultiGauges  multiGauge14;
    MultiGauges  multiGaugeVolts;
    ImageButton  btnOne;
    ImageButton  btnTwo;
    Typeface     typeFaceDigital;
    TextView     txtViewDigital;
    TextView     txtViewDigital2;
    TextView     txtViewDigital3;
    TextView     txtViewDigital4;
    TextView     txtViewDigital5;
    TextView     txtViewDigital6;
    TextView     txtViewDigital7;
    TextView     txtViewDigital8;
    TextView     txtViewDigital9;
    TextView     txtViewDigital10;
    TextView     txtViewDigital11;
    TextView     txtViewDigital12;
    TextView     txtViewVolts;
    TextView     txtViewVoltsText;
    float        batvolt;
    int          digitalToken;
    String       currentMsg;
    Thread       thread;
    boolean      isBLE;

    boolean  paused;
    Context  context;
    float   boost;
    float   lambda;
    float   vdo_temp1;
    float   vdo_pres1;
    float   egt0;
    float   egt1;
    float   egt2;
    float   egt3;
    float   egt4;
    float   egt5;
    float   egt6;
    float   egt7;
    float   rpm;
    float   speed;


    //Prefs vars
    View    root;
    boolean showAnalog; //Display the analog gauge or not.
    boolean showDigital; //Display the digital gauge or not.
    boolean showNightMode; //Change background to black.
    boolean showVoltMeter;


    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ         = 2;
    public static final int MESSAGE_WRITE        = 3;
    public static final int MESSAGE_DEVICE_NAME  = 4;
    public static final int MESSAGE_TOAST        = 5;

    //Test

    // Key names received from the BluetoothChatService Handler
    public static final String TOAST        = "toast";
    private static final int BOOST_TOKEN    = 1;
    private static final int WIDEBAND_TOKEN = 2;
    private static final int TEMP_TOKEN     = 3;
    private static final int OIL_TOKEN      = 4;
    private static final int VOLT_TOKEN     = 0;
    private static final int EGT0_TOKEN     = 5;
    private static final int EGT1_TOKEN     = 6;
    private static final int EGT2_TOKEN     = 7;
    private static final int EGT3_TOKEN     = 8;
    private static final int EGT4_TOKEN     = 9;
    private static final int EGT5_TOKEN     = 10;
    private static final int EGT6_TOKEN     = 11;
    private static final int EGT7_TOKEN     = 12;
    private static final int RPM_TOKEN      = 13;
    private static final int SPEED_TOKEN    = 14;


    //Bluetooth types
    private static final int CLASSIC_TYPE = 1;
    private static final int BLE_TYPE     = 2;

    BluetoothSerialService mSerialService;
    BluetoothLeService _bluetoothLeService;
    private static Handler workerHandler;

    @Override

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gauge_layout_4);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefsInit(); //Load up the preferences.
        context = this;

        //Instantiate the GaugeBuilder.
        analogGauge1    = (GaugeBuilder) findViewById(R.id.analogGauge);
        analogGauge2    = (GaugeBuilder) findViewById(R.id.analogGauge2);
        analogGauge3    = (GaugeBuilder) findViewById(R.id.analogGauge3);
        analogGauge4    = (GaugeBuilder) findViewById(R.id.analogGauge4);
        analogGauge5    = (GaugeBuilder) findViewById(R.id.EGT1);
        analogGauge6    = (GaugeBuilder) findViewById(R.id.EGT2);
        analogGauge7    = (GaugeBuilder) findViewById(R.id.EGT3);
        analogGauge8    = (GaugeBuilder) findViewById(R.id.EGT4);
        analogGauge9    = (GaugeBuilder) findViewById(R.id.EGT5);
        analogGauge10   = (GaugeBuilder) findViewById(R.id.EGT6);
        analogGauge11   = (GaugeBuilder) findViewById(R.id.EGT7);
        analogGauge12   = (GaugeBuilder) findViewById(R.id.EGT8);
        analogGauge13   = (GaugeBuilder) findViewById(R.id.RPM);
        analogGauge14   = (GaugeBuilder) findViewById(R.id.SPEED);
        multiGauge1     = new MultiGauges(context);
        multiGauge2     = new MultiGauges(context);
        multiGauge3     = new MultiGauges(context);
        multiGauge4     = new MultiGauges(context);
        multiGauge5     = new MultiGauges(context);
        multiGauge6     = new MultiGauges(context);
        multiGauge7     = new MultiGauges(context);
        multiGauge8     = new MultiGauges(context);
        multiGauge9     = new MultiGauges(context);
        multiGauge10    = new MultiGauges(context);
        multiGauge11    = new MultiGauges(context);
        multiGauge12    = new MultiGauges(context);
        multiGauge13    = new MultiGauges(context);
        multiGauge14    = new MultiGauges(context);
        multiGaugeVolts = new MultiGauges(context);
        txtViewDigital  = (TextView) findViewById(R.id.txtViewDigital);
        txtViewDigital2 = (TextView) findViewById(R.id.txtViewDigital2);
        txtViewDigital3 = (TextView) findViewById(R.id.txtViewDigital3);
        txtViewDigital4 = (TextView) findViewById(R.id.txtViewDigital4);
        txtViewDigital5 = (TextView) findViewById(R.id.txtViewDigital5);
        txtViewDigital6 = (TextView) findViewById(R.id.txtViewDigital6);
        txtViewDigital7 = (TextView) findViewById(R.id.txtViewDigital7);
        txtViewDigital8 = (TextView) findViewById(R.id.txtViewDigital8);
        txtViewDigital9 = (TextView) findViewById(R.id.txtViewDigital9);
        txtViewDigital10 = (TextView) findViewById(R.id.txtViewDigital10);
        txtViewDigital11 = (TextView) findViewById(R.id.txtViewDigital11);
        txtViewDigital12 = (TextView) findViewById(R.id.txtViewDigital12);
        txtViewVolts    = (TextView) findViewById(R.id.txtViewVolts);
        txtViewVoltsText= (TextView) findViewById(R.id.txtViewVoltsText);
        btnOne          = (ImageButton) findViewById(R.id.btnOne);
        btnTwo          = (ImageButton) findViewById(R.id.btnTwo);
        typeFaceDigital = Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");
        digitalToken    = 1;

        //Set the font of the digital.
        txtViewDigital.setTypeface(typeFaceDigital);
        txtViewDigital2.setTypeface(typeFaceDigital);
        txtViewDigital3.setTypeface(typeFaceDigital);
        txtViewDigital4.setTypeface(typeFaceDigital);
        txtViewDigital5.setTypeface(typeFaceDigital);
        txtViewDigital6.setTypeface(typeFaceDigital);
        txtViewDigital7.setTypeface(typeFaceDigital);
        txtViewDigital8.setTypeface(typeFaceDigital);
        txtViewDigital9.setTypeface(typeFaceDigital);
        txtViewDigital10.setTypeface(typeFaceDigital);
        txtViewDigital11.setTypeface(typeFaceDigital);
        txtViewDigital12.setTypeface(typeFaceDigital);
        txtViewVolts.setTypeface(typeFaceDigital);
        txtViewVoltsText.setTypeface(typeFaceDigital);
        txtViewDigital.setText("0.00");
        txtViewDigital2.setText("0.00");
        txtViewDigital3.setText("0.00");
        txtViewDigital4.setText("0.00");
        txtViewDigital5.setText("0.00");
        txtViewDigital6.setText("0.00");
        txtViewDigital7.setText("0.00");
        txtViewDigital8.setText("0.00");
        txtViewDigital9.setText("0.00");
        txtViewDigital10.setText("0.00");
        txtViewDigital11.setText("0.00");
        txtViewDigital12.setText("0.00");

        //Setup gauge 1
        multiGauge1.setAnalogGauge(analogGauge1);
        multiGauge1.buildGauge(BOOST_TOKEN);

        //Check if the gauge uses negative numbers or not.
        if(analogGauge1.getAbsoluteNumbers()){ 
            txtViewDigital.setText(Double.toString(Math.abs(multiGauge1.getMinValue())));
        }else{
            txtViewDigital.setText(Double.toString(multiGauge1.getMinValue()));
        }

        //Setup gauge 2
        multiGauge2.setAnalogGauge(analogGauge2);
        multiGauge2.buildGauge(WIDEBAND_TOKEN);
        txtViewDigital2.setText(Double.toString(multiGauge2.getSensorMaxValue()));

        //Setup gauge 3
        multiGauge3.setAnalogGauge(analogGauge3);
        multiGauge3.buildGauge(TEMP_TOKEN);
        txtViewDigital3.setText(Double.toString(multiGauge3.getSensorMaxValue()));

        //Setup gauge 4
        multiGauge4.setAnalogGauge(analogGauge4);
        multiGauge4.buildGauge(OIL_TOKEN);
        txtViewDigital4.setText(Double.toString(multiGauge4.getSensorMaxValue()));

        //Setup gauge 5
        multiGauge5.setAnalogGauge(analogGauge5);
        multiGauge5.buildGauge(EGT0_TOKEN);
        txtViewDigital5.setText(Double.toString(multiGauge5.getSensorMaxValue()));

        //Setup gauge 6
        multiGauge6.setAnalogGauge(analogGauge6);
        multiGauge6.buildGauge(EGT1_TOKEN);
        txtViewDigital6.setText(Double.toString(multiGauge6.getSensorMaxValue()));

        //Setup gauge 7
        multiGauge7.setAnalogGauge(analogGauge7);
        multiGauge7.buildGauge(EGT2_TOKEN);
        txtViewDigital7.setText(Double.toString(multiGauge7.getSensorMaxValue()));

        //Setup gauge 8
        multiGauge8.setAnalogGauge(analogGauge8);
        multiGauge8.buildGauge(EGT3_TOKEN);
        txtViewDigital8.setText(Double.toString(multiGauge8.getSensorMaxValue()));

        //Setup gauge 9
        multiGauge9.setAnalogGauge(analogGauge9);
        multiGauge9.buildGauge(EGT4_TOKEN);
        txtViewDigital9.setText(Double.toString(multiGauge9.getSensorMaxValue()));

        //Setup gauge 10
        multiGauge10.setAnalogGauge(analogGauge10);
        multiGauge10.buildGauge(EGT5_TOKEN);
        txtViewDigital10.setText(Double.toString(multiGauge10.getSensorMaxValue()));

        //Setup gauge 11
        multiGauge11.setAnalogGauge(analogGauge11);
        multiGauge11.buildGauge(EGT6_TOKEN);
        txtViewDigital11.setText(Double.toString(multiGauge11.getSensorMaxValue()));

        //Setup gauge 12
        multiGauge12.setAnalogGauge(analogGauge12);
        multiGauge12.buildGauge(EGT7_TOKEN);
        txtViewDigital12.setText(Double.toString(multiGauge12.getSensorMaxValue()));

        //Setup gauge 13
        multiGauge13.setAnalogGauge(analogGauge13);
        multiGauge13.buildGauge(RPM_TOKEN);
        txtViewDigital12.setText(Double.toString(multiGauge13.getSensorMaxValue()));

        //Setup gauge 14
        multiGauge14.setAnalogGauge(analogGauge14);
        multiGauge14.buildGauge(SPEED_TOKEN);
        txtViewDigital12.setText(Double.toString(multiGauge14.getSensorMaxValue()));

        //Setup voltmeter
        multiGaugeVolts.buildGauge(VOLT_TOKEN);


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

        Thread thread = new Thread(FourGaugeActivity.this);
        thread.start();

        if(!showAnalog){
            ((ViewManager)analogGauge1.getParent()).removeView(analogGauge1); //Remove analog gauge
            ((ViewManager)analogGauge2.getParent()).removeView(analogGauge2); //Remove analog gauge
            ((ViewManager)analogGauge3.getParent()).removeView(analogGauge3); //Remove analog gauge
            ((ViewManager)analogGauge4.getParent()).removeView(analogGauge4);
            ((ViewManager)analogGauge5.getParent()).removeView(analogGauge5);
            ((ViewManager)analogGauge6.getParent()).removeView(analogGauge6);
            ((ViewManager)analogGauge7.getParent()).removeView(analogGauge7);
            ((ViewManager)analogGauge8.getParent()).removeView(analogGauge8);
            ((ViewManager)analogGauge9.getParent()).removeView(analogGauge9);
            ((ViewManager)analogGauge10.getParent()).removeView(analogGauge10);
            ((ViewManager)analogGauge11.getParent()).removeView(analogGauge11);
            ((ViewManager)analogGauge12.getParent()).removeView(analogGauge12);
            ((ViewManager)analogGauge13.getParent()).removeView(analogGauge13);
            ((ViewManager)analogGauge14.getParent()).removeView(analogGauge14);//Remove analog gauge
        }
        if(!showDigital){
            ((ViewManager)txtViewDigital.getParent()).removeView(txtViewDigital); //Remove digital gauge
            ((ViewManager)txtViewDigital2.getParent()).removeView(txtViewDigital2); //Remove digital gauge
            ((ViewManager)txtViewDigital3.getParent()).removeView(txtViewDigital3); //Remove digital gauge
            ((ViewManager)txtViewDigital4.getParent()).removeView(txtViewDigital4); //Remove digital gauge
        }
        if(showNightMode){
            root = btnOne.getRootView(); //Get root layer view.
            root.setBackgroundColor(getResources().getColor(R.color.black)); //Set background color to black.
        }
        if(!showVoltMeter){
            root = btnOne.getRootView(); //Get root layer view.
            ((ViewManager)txtViewVolts.getParent()).removeView(txtViewVolts);
            ((ViewManager)txtViewVoltsText.getParent()).removeView(txtViewVoltsText);
        }

    }

    ///Handles the data being sent back from the BluetoothSerialService class.
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(!paused){
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
 //               String readMessage;
 //               try {
//                    readMessage = new String(readBuf, 0, msg.arg1);
//                } catch (NullPointerException e) {
 //                   readMessage = "0";
 //               }
                //Redraw the needle to the correct value.
 //               currentMsg = readMessage;

                Message workerMsg = workerHandler.obtainMessage(msg.what, readBuf);
                workerMsg.sendToTarget();
                updateGauges();
            }

        }
    };

    //Worker thread handling
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
                        PSensor.sensorData.parseInput((byte[])msg.obj);//parseInput((byte[])msg.obj);
                        multiGauge1.handleSensor(PSensor.sensorData.boost);
                        multiGauge2.handleSensor(PSensor.sensorData.lambda);
                        multiGauge3.handleSensor(PSensor.sensorData.vdo_temp1);
                        multiGauge4.handleSensor(PSensor.sensorData.vdo_pres1);
                        multiGaugeVolts.handleSensor(PSensor.sensorData.batVolt);
                        multiGauge5.handleSensor(PSensor.sensorData.egt0);
                        multiGauge6.handleSensor(PSensor.sensorData.egt1);
                        multiGauge7.handleSensor(PSensor.sensorData.egt2);
                        multiGauge8.handleSensor(PSensor.sensorData.egt3);
                        multiGauge9.handleSensor(PSensor.sensorData.egt4);
                        multiGauge10.handleSensor(PSensor.sensorData.egt5);
                        multiGauge11.handleSensor(PSensor.sensorData.egt6);
                        multiGauge12.handleSensor(PSensor.sensorData.egt7);
                        multiGauge13.handleSensor(PSensor.sensorData.rpm);
                        multiGauge14.handleSensor(PSensor.sensorData.speed);
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
        if(!paused){
            analogGauge1.setValue(multiGauge1.getCurrentGaugeValue());
            analogGauge2.setValue(multiGauge2.getCurrentGaugeValue());
            analogGauge3.setValue(multiGauge3.getCurrentGaugeValue());
            analogGauge4.setValue(multiGauge4.getCurrentGaugeValue());
            analogGauge5.setValue(multiGauge5.getCurrentGaugeValue());
            analogGauge6.setValue(multiGauge6.getCurrentGaugeValue());
            analogGauge7.setValue(multiGauge7.getCurrentGaugeValue());
            analogGauge8.setValue(multiGauge8.getCurrentGaugeValue());
            analogGauge9.setValue(multiGauge9.getCurrentGaugeValue());
            analogGauge10.setValue(multiGauge10.getCurrentGaugeValue());
            analogGauge11.setValue(multiGauge11.getCurrentGaugeValue());
            analogGauge12.setValue(multiGauge12.getCurrentGaugeValue());
            analogGauge13.setValue(multiGauge13.getCurrentGaugeValue());
            analogGauge14.setValue(multiGauge14.getCurrentGaugeValue());


            txtViewDigital.setText(Float.toString(Math.abs(multiGauge1.getCurrentGaugeValue())));
            txtViewDigital2.setText(Float.toString(multiGauge2.getCurrentGaugeValue()));
            txtViewDigital3.setText(Float.toString(multiGauge3.getCurrentGaugeValue()));
            txtViewDigital4.setText(Float.toString(multiGauge4.getCurrentGaugeValue()));

            txtViewDigital5.setText(Float.toString(Math.abs(multiGauge5.getCurrentGaugeValue())));
            txtViewDigital6.setText(Float.toString(multiGauge6.getCurrentGaugeValue()));
            txtViewDigital7.setText(Float.toString(multiGauge7.getCurrentGaugeValue()));
            txtViewDigital8.setText(Float.toString(multiGauge8.getCurrentGaugeValue()));

            txtViewDigital9.setText(Float.toString(Math.abs(multiGauge9.getCurrentGaugeValue())));
            txtViewDigital10.setText(Float.toString(multiGauge10.getCurrentGaugeValue()));
            txtViewDigital11.setText(Float.toString(multiGauge11.getCurrentGaugeValue()));
            txtViewDigital12.setText(Float.toString(multiGauge12.getCurrentGaugeValue()));

            txtViewVolts.setText(Float.toString(Math.abs(multiGaugeVolts.getCurrentGaugeValue())));
        }
    }

//    private void parseInput(byte[] sValue){
//        //String[] tokens=sValue.split(":,;"); //split the input into an array.
//
//        try {
//            //Get current tokens for this gauge activity, cast as float.
//            calBoost = Float.valueOf(tokens[BOOST_TOKEN].toString());
//            calLambda 	= Float.valueOf(tokens[WIDEBAND_TOKEN].toString());
//            VDOTemp1 	= Float.valueOf(tokens[TEMP_TOKEN].toString());
//            VDOPres1 	= Float.valueOf(tokens[OIL_TOKEN].toString());
//            batvolt  = Float.valueOf(tokens[VOLT_TOKEN].toString());//Get volt token value, cast as float.
//            calEgt0  = Float.valueOf(tokens[EGT0_TOKEN].toString());
//            EGT1SValue  = Float.valueOf(tokens[EGT1_TOKEN].toString());
//            EGT2SValue  = Float.valueOf(tokens[EGT2_TOKEN].toString());
//            EGT3SValue  = Float.valueOf(tokens[EGT3_TOKEN].toString());
//            EGT4SValue  = Float.valueOf(tokens[EGT4_TOKEN].toString());
//            EGT5SValue  = Float.valueOf(tokens[EGT5_TOKEN].toString());
//            EGT6SValue  = Float.valueOf(tokens[EGT6_TOKEN].toString());
//            EGT7SValue  = Float.valueOf(tokens[EGT7_TOKEN].toString());
//            RPMSValue  = Float.valueOf(tokens[RPM_TOKEN].toString());
//            SPEEDSValue  = Float.valueOf(tokens[SPEED_TOKEN].toString());
//
//
//
//        } catch (NumberFormatException f) {
//            calBoost = 0;
//            calLambda   = 0;
//            VDOTemp1 	= 0;
//            VDOPres1	= 0;
//            batvolt  = 0f;
//            calEgt0  = 0;
//            EGT1SValue  = 0;
//            EGT2SValue  = 0;
//            EGT3SValue  = 0;
//            EGT4SValue  = 0;
//            EGT5SValue  = 0;
//            EGT6SValue  = 0;
//            EGT7SValue  = 0;
//            RPMSValue   = 0;
//            SPEEDSValue = 0;
//
//
//        } catch (ArrayIndexOutOfBoundsException e){
//            calBoost = 0;
//            calLambda 	= 0;
//            VDOTemp1 	= 0;
//            VDOPres1	= 0;
//            batvolt  = 0f;
//            calEgt0  = 0;
//            EGT1SValue  = 0;
//            EGT2SValue  = 0;
//            EGT3SValue  = 0;
//            EGT4SValue  = 0;
//            EGT5SValue  = 0;
//            EGT6SValue  = 0;
//            EGT7SValue  = 0;
//            RPMSValue   = 0;
//            SPEEDSValue = 0;
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
        Intent chartIntent = new Intent(this, QuadChartActivity.class);
        startActivity(chartIntent);
    }

    //Button one handling.
    public void buttonOneClick(View v){   
        //Reset the max value.
        multiGauge1.setSensorMaxValue(multiGauge1.getMinValue());
        multiGauge2.setSensorMaxValue(multiGauge2.getMinValue());
        multiGauge3.setSensorMaxValue(multiGauge3.getMinValue());
        multiGauge4.setSensorMaxValue(multiGauge4.getMinValue());
        multiGauge5.setSensorMaxValue(multiGauge5.getMinValue());
        multiGauge6.setSensorMaxValue(multiGauge6.getMinValue());
        multiGauge7.setSensorMaxValue(multiGauge7.getMinValue());
        multiGauge8.setSensorMaxValue(multiGauge8.getMinValue());
        multiGauge9.setSensorMaxValue(multiGauge9.getMinValue());
        multiGauge10.setSensorMaxValue(multiGauge10.getMinValue());
        multiGauge11.setSensorMaxValue(multiGauge11.getMinValue());
        multiGauge12.setSensorMaxValue(multiGauge12.getMinValue());
        multiGauge13.setSensorMaxValue(multiGauge13.getMinValue());
        multiGauge14.setSensorMaxValue(multiGauge14.getMinValue());
        //multiGauge3.setSensorMaxValue(multiGauge3.getMinValue());
       // multiGauge4.setSensorMaxValue(multiGauge4.getMinValue());
        multiGaugeVolts.setSensorMaxValue(multiGaugeVolts.getMinValue());
        paused = false;
        btnTwo.setBackgroundResource(Color.TRANSPARENT);
        Toast.makeText(getApplicationContext(), "Max value reset", Toast.LENGTH_SHORT).show();
    }

    //Button two handling.
    public void buttonTwoClick(View v){
        if(!paused){
            paused = true;

            //set the gauge/digital to the max value captured so far.
            txtViewDigital.setText(Double.toString(Math.abs(multiGauge1.getSensorMaxValue())));
            txtViewDigital2.setText(Double.toString(multiGauge2.getSensorMaxValue()));
            txtViewDigital3.setText(Double.toString(multiGauge3.getSensorMaxValue()));
            txtViewDigital4.setText(Double.toString(multiGauge4.getSensorMaxValue()));
            txtViewDigital5.setText(Double.toString(multiGauge5.getSensorMaxValue()));
            txtViewDigital6.setText(Double.toString(multiGauge6.getSensorMaxValue()));
            txtViewDigital7.setText(Double.toString(multiGauge7.getSensorMaxValue()));
            txtViewDigital8.setText(Double.toString(multiGauge8.getSensorMaxValue()));
            txtViewDigital9.setText(Double.toString(multiGauge9.getSensorMaxValue()));
            txtViewDigital10.setText(Double.toString(multiGauge10.getSensorMaxValue()));
            txtViewDigital11.setText(Double.toString(multiGauge11.getSensorMaxValue()));
            txtViewDigital12.setText(Double.toString(multiGauge12.getSensorMaxValue()));
        //  txtViewDigital13.setText(Double.toString(multiGauge13.getSensorMaxValue()));
        //  txtViewDigital14.setText(Double.toString(multiGauge14.getSensorMaxValue()));

            analogGauge1.setValue((float)multiGauge1.getSensorMaxValue());
            analogGauge2.setValue((float)multiGauge2.getSensorMaxValue());
            analogGauge3.setValue((float)multiGauge3.getSensorMaxValue());
            analogGauge4.setValue((float)multiGauge4.getSensorMaxValue());
            analogGauge5.setValue((float)multiGauge5.getSensorMaxValue());
            analogGauge6.setValue((float)multiGauge6.getSensorMaxValue());
            analogGauge7.setValue((float)multiGauge7.getSensorMaxValue());
            analogGauge8.setValue((float)multiGauge8.getSensorMaxValue());
            analogGauge9.setValue((float)multiGauge9.getSensorMaxValue());
            analogGauge10.setValue((float)multiGauge10.getSensorMaxValue());
            analogGauge11.setValue((float)multiGauge11.getSensorMaxValue());
            analogGauge12.setValue((float)multiGauge12.getSensorMaxValue());
            analogGauge13.setValue((float)multiGauge13.getSensorMaxValue());
            analogGauge14.setValue((float)multiGauge14.getSensorMaxValue());


            txtViewVolts.setText(Double.toString(multiGaugeVolts.getSensorMaxValue()));

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
        Thread thread = new Thread(FourGaugeActivity.this);
        thread.start();
        analogGauge1.invalidate();
        analogGauge2.invalidate();
        analogGauge3.invalidate();
        analogGauge4.invalidate();
        analogGauge5.invalidate();
        analogGauge6.invalidate();
        analogGauge7.invalidate();
        analogGauge8.invalidate();
        analogGauge9.invalidate();
        analogGauge10.invalidate();
        analogGauge11.invalidate();
        analogGauge12.invalidate();
        analogGauge13.invalidate();
        analogGauge14.invalidate();
        //analogGauge3.invalidate();
        //analogGauge4.invalidate();

    }

    public void prefsInit(){
        SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
        showAnalog = sp.getBoolean("showAnalog", true);
        showDigital = sp.getBoolean("showDigital", true);
        showNightMode = sp.getBoolean("showNightMode", false);
        showVoltMeter = sp.getBoolean("showVoltMeter", true);
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
