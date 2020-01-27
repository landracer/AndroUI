package com.rAtTrax.AndroUI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class BlueToothTrace extends Activity implements Runnable{

    ImageButton             btnOne;
    ImageButton             btnTwo;
    TextView                traceOut;
    boolean                 paused;
    Thread                  thread;
    boolean                 isBLE;
    BluetoothSerialService  mSerialService;
    BluetoothLeService      _bluetoothLeService;
    private static Handler  workerHandler;

    //Bluetooth types
    private static final int CLASSIC_TYPE = 1;
    private static final int BLE_TYPE     = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.blue_tooth_trace);

        paused          = false;
        btnOne          = (ImageButton) findViewById(R.id.btnOne);
        btnTwo          = (ImageButton) findViewById(R.id.btnTwo);
        traceOut        = (TextView)    findViewById(R.id.traceOut);

        traceOut.setMovementMethod(new ScrollingMovementMethod());

        //Get the mSerialService object from the UI activity.
        Object obj = PassObject.getObject();
        int _bluetoothType = PassObject.getType();

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

        thread = new Thread(BlueToothTrace.this);
        thread.start();
    }

    //Handles the data being sent back from the BluetoothSerialService class.
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(!paused){
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage;
                try {
                    readMessage = new String(readBuf, 0, msg.arg1);
                } catch (NullPointerException e) {
                    readMessage = "0";
                }

                Message workerMsg = workerHandler.obtainMessage(1, readMessage);
                workerMsg.sendToTarget();
            }
        }
    };

    @Override
    public void run(){
        Looper.prepare();
        workerHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                textOut((String) msg.obj);
            }
        };
        Looper.loop();
    }

    public void textOut(String msg) {
        final String str = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                traceOut.append(str + "\n");
            }
        });
    }

    //Button one handling.
    public void buttonOneClick(View v){
        //Reset the max value.
        paused = false;
        btnTwo.setBackgroundResource(Color.TRANSPARENT);
    }

    //Button two handling.
    public void buttonTwoClick(View v){
        if(!paused){
            paused = true;
            btnTwo.setBackgroundResource(R.drawable.btn_bg_pressed);
        }else{
            paused = false;
            btnTwo.setBackgroundResource(Color.TRANSPARENT);
        }
    }

    //Activity transfer handling
    public void goHome(View v){
        onBackPressed();
        finish();
    }

    @Override
    public void onBackPressed(){
        paused = true;
        passObject();
        super.onBackPressed();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        thread = new Thread(BlueToothTrace.this);
        thread.start();
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

