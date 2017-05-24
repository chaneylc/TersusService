package edu.ksu.wheatgenetics.tersusservice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class TersusService extends Service {

    private Timer pairTimer = new Timer("hexTronik Pair", true);
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mDevice;

    public TersusService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //begin pairing timer
        resetPairedTimer();

        return super.onStartCommand(intent, flags, startId);
    }

    private void findPairedBTDevice() {

        if (mBluetoothAdapter != null) {

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (!pairedDevices.isEmpty()) {
                for (BluetoothDevice bd : pairedDevices) {
                    //TODO change this with settings name
                    if (bd.getName().startsWith("HB")) {
                        mDevice = bd;
                        try {
                            new ConnectThread(mDevice).start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private class PairTask extends TimerTask {

        @Override
        public void run() {

            findPairedBTDevice();
        }
    }

    private void resetPairedTimer() {

        pairTimer.purge();
        pairTimer.cancel();
        pairTimer = new Timer("hexTronik Pair", true);
        pairTimer.schedule(new PairTask(), 1000, 5000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message input) {

            switch (input.what) {
                case TersusServiceConstants.MESSAGE_READ:
                    final TersusString ts = new TersusString((byte[]) input.obj);
                    if (!ts.toString().isEmpty()) {

                        LocalBroadcastManager.getInstance(TersusService.this).sendBroadcast(
                                new Intent(TersusServiceConstants.BROADCAST_TERSUS_OUTPUT)
                                        .putExtra(TersusServiceConstants.TERSUS_OUTPUT, ts)
                        );
                    }
                    break;
            }
        }
    };

    /* Thread that creates hexTronik socket and given a paired HB device */
    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mDevice = mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
            } catch (IOException e) {
                Log.e("CONNECT THREAD", "Socket's create() method failed", e);
                resetPairedTimer();
            }

            mmSocket = tmp;
        }

        public void run() {

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    resetPairedTimer();
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("CONNECT THREAD: RUN", "Could not close the client socket", closeException);
                }
                return;
            }


            //cancel the pair timer
            pairTimer.cancel();
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            new ConnectedThread(mmSocket).start();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("CONNECT THREAD : CANCEL", "Could not close the client socket", e);
            }
        }
    }

    /* thread that communicates over the hexTronik socket created by ConnectThread */
    /* sends data to the Service handler */
    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e("CONNECTED THREAD: in", "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("CONNECTED THREAD: out", "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            //broadcast that the device is connected
            LocalBroadcastManager.getInstance(TersusService.this).sendBroadcast(
                    new Intent(TersusServiceConstants.BROADCAST_TERSUS_CONNECTION)
                            .putExtra(TersusServiceConstants.TERSUS_CONNECTION, true)
            );
        }

        public void run() {

            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);

                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            TersusServiceConstants.MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();

                } catch (IOException e) {

                    Log.d("CONNECTED THREAD: run", "Input stream was disconnected", e);

                    resetPairedTimer();

                    LocalBroadcastManager.getInstance(TersusService.this).sendBroadcast(
                            new Intent(TersusServiceConstants.BROADCAST_TERSUS_CONNECTION)
                                    .putExtra(TersusServiceConstants.TERSUS_CONNECTION, false)
                    );

                    break;
                }
            }
        }

        // Can this be used to program Tersus...? e.g write("log com1 gpgg ontime 0.2".getBytes())
      /*  public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e("CONNECTED THREAD : w", "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }*/

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("CONNECTED : cancel", "Could not close the connect socket", e);
            }
        }
    }
}
