/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rAtTrax.AndroUI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothSerialService {
    // Debugging
    private static final String TAG = "BluetoothReadService";
    private static final boolean D = false;


    private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;


    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public static final int MD_FRAMEBEGIN = 2;
    public static final int MD_FRAMEEND = 3;
    public static final int MD_STATUS_RECEIVING = 1;
    public static final int MD_STATUS_FRAME_COMPLETE = 2;
    public static final int MD_STATUS_FRAMEERROR = 3;
    public static final int MD_STATUS_WAITING_FOR_TAG = 4;

    public static final int MD_MAXFRAME_SIZE = 97;
    //87 bytes MD2 data plus digifant data
    public static final int MD_SERIALOUT_BINARY_TAG = 95;
    public static final int MD_SERIALOUT_BINARY_BOOSTPID_TAG = 69;
    public static final int MD_SERIALOUT_BINARY_CONFIG_TAG = 99;
    public static final int MD_SERIALOUT_BINARY_TAG_GEAR_RATIO_6G = 17;

    //STX tag=24 gearX mode serial 16 bytes map ETX
    public static final int MD_SERIALOUT_BINARY_TAG_N75_DUTY_MAP = 22;
    public static final int MD_SERIALOUT_BINARY_TAG_N75_SETPOINT_MAP = 38;
    public static final int MD_SERIALOUT_BINARY_TAG_ACK = 4;
    public static final int MD_SERIALOUT_BINARY_TAG_N75_PARAMS = 23;

    private int status;
    private int index;
    private int discarded_frames;
    private int framelength;
    private byte[] m_abtRcvData;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothSerialService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        m_abtRcvData = new byte[MD_MAXFRAME_SIZE];
        status = MD_STATUS_FRAME_COMPLETE;
        index = 0;
        discarded_frames = 0;
        framelength = 0;
    }

    public void setHandler(Handler inHandler) {
        mHandler = inHandler;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(PSensor.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
        Log.d("BL", "setState from start()");
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.interrupt();
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
        Log.d("BL", "setState from connect(BluetoothDevice device)");
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(PSensor.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(PSensor.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
        Log.d("BL", "connected(BluetoothSocket socket, BluetoothDevice device)");
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.interrupt();
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.interrupt();
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
        Log.d("BL", "setState from stop()");
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        if (isRxAvailable()) {
            return;
        }
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    public boolean isRxAvailable() {
        return mConnectedThread.bRxAvailable;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_NONE);
        Log.d("BL", "setState from connectionFailed()");

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(PSensor.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(PSensor.TOAST, "Unable to connect.");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(PSensor.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(PSensor.TOAST, "Disconnected from bluetooth.");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
        Log.d("BL", "setState from connectionLost()");
    }

    public void initRxState() {
        status = MD_STATUS_FRAME_COMPLETE;
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(SerialPortServiceClass_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
                setState(STATE_NONE);
                Log.d("BL", "setState from connectThread(BluetoothDevice device)");
            }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                //BluetoothSerialService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothSerialService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                synchronized (this) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;


        public boolean bRxAvailable = false;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            int i, j;
            //New Shit:
            final byte delimiter = 10; //This is the ASCII code for a newline character
            int readBufferPosition = 0;

            // Keep listening to the InputStream while connected
            while (true) {
                if (Thread.interrupted()) return;
                int bytesAvailable = 0;

                try {
                    bytesAvailable = mmInStream.available();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                bRxAvailable = false;
                if (bytesAvailable > 0) {
                    bRxAvailable = true;
                    byte[] packetBytes = new byte[bytesAvailable];
                    try {
                        mmInStream.read(packetBytes);
                    } catch (IOException e) {
                        connectionLost();
                        e.printStackTrace();
                        bytesAvailable = 0;
                        break;
                    }

                    for (i = 0; i < bytesAvailable; i++) {
                        byte d = packetBytes[i];
                        switch (status) {
                            case MD_STATUS_FRAMEERROR:
                            case MD_STATUS_FRAME_COMPLETE:
                                //new frame --> check for start char
                                index = 0;
                                /*for (j = 0; j < (MD_MAXFRAME_SIZE - 1); j++) {
                                    m_abtRcvData[j] = (byte) 0x00;
                                }*/

                                if (d != MD_FRAMEBEGIN) {
                                    //skip it
                                } else {
                                    status = MD_STATUS_WAITING_FOR_TAG;
                                    m_abtRcvData[index] = d;
                                    index++;
                                }
                                break;

                            case MD_STATUS_WAITING_FOR_TAG:
                                if (d == MD_SERIALOUT_BINARY_TAG || d == MD_SERIALOUT_BINARY_BOOSTPID_TAG
                                        || d == MD_SERIALOUT_BINARY_TAG_N75_DUTY_MAP || d == MD_SERIALOUT_BINARY_TAG_N75_SETPOINT_MAP
                                        || d == MD_SERIALOUT_BINARY_TAG_ACK || d == MD_SERIALOUT_BINARY_TAG_N75_PARAMS
                                        || d == MD_SERIALOUT_BINARY_TAG_GEAR_RATIO_6G) {
                                    status = MD_STATUS_RECEIVING;
                                    framelength = d;
                                    m_abtRcvData[index] = d;
                                    index++;
                                } else {
//                                    qDebug() << "(WARN) expected tag but did not get one! d=" << d;
                                    framelength = 0;
                                }
                                break;

                            case MD_STATUS_RECEIVING:
                                if (index == framelength - 1) {
                                    //last char -> check for end char
                                    if (d != MD_FRAMEEND) {
//                                        discarded_frames++;
//                                        if ( discarded_frames % 100 == 0 )
//                                            qDebug() << "(WARN) frame discarded! expected framelength=" << framelength << " #discarded frames=" << discarded_frames << " d=" << d << " data=" << sdata->toHex();
                                        status = MD_STATUS_FRAMEERROR;
                                    } else {
                                        m_abtRcvData[index] = d;
                                        //frame complete!
                                        status = MD_STATUS_FRAME_COMPLETE;

                                        //do sth with it!
                                        mHandler.obtainMessage(PSensor.MESSAGE_READ, index, -1, m_abtRcvData).sendToTarget();
                                        index = 0;
                                    }
                                } else {
                                    m_abtRcvData[index] = d;
                                    index++;
                                }
                                break;
                        }
                    }
//                        if(b == delimiter){
//                            byte[] encodedBytes = new byte[readBufferPosition];
//                            readBufferPosition = 0;
//                            mHandler.obtainMessage(PSensor.MESSAGE_READ, encodedBytes.length, -1, buffer).sendToTarget();
//
//                            //temp
//                            String readMessage = new String(buffer, 0, encodedBytes.length);
//                            Log.i("FROM BT Classic", readMessage);
//
//                        }else{
//                            //Getting a few index out of bounds errors here, adding handling.
//                            if(readBufferPosition<1023) {
//                                buffer[readBufferPosition++] = b;
//                            }
//                        }
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(PSensor.MESSAGE_WRITE, buffer.length, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                synchronized (this) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
                setState(STATE_NONE);
                Log.d("BL", "setState from cancel()");
            }
        }
    }
}