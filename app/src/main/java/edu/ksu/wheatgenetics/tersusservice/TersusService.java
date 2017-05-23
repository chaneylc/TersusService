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

public class TersusService extends Service {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mDevice;

    public TersusService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {

            discover(true);

        }

        final IntentFilter bt_intent = new IntentFilter();
        bt_intent.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, bt_intent);

        return super.onStartCommand(intent, flags, startId);
    }

    public void discover(boolean flag) {

        if (flag) mBluetoothAdapter.startDiscovery();
        else mBluetoothAdapter.cancelDiscovery();

        LocalBroadcastManager.getInstance(TersusService.this).sendBroadcast(
                new Intent(TersusServiceConstants.BROADCAST_TERSUS_DISCOVERY)
                        .putExtra(TersusServiceConstants.TERSUS_DISCOVERY, flag)
        );
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

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device != null && device.getName() != null && device.getAddress() != null) {

                    String deviceName = device.getName();
                    //String deviceHardwareAddress = device.getAddress();

                    if (deviceName.startsWith("HB")) {//default name for Tersus BT, maybe add this as setting

                       /* if (android.os.Build.VERSION.SDK_INT >= 19) {

                            //the following two lines only work if the app is installed in system/priv-apps
                            //which requires manual installation, the two lines automatically pair with the Tersus
                            //otherwise the user must enter the default pin number when prompted
                            /** device.setPin("1234".getBytes());
                             device.setPairingConfirmation(true);
                        }/*/
                        try {

                            Method method = device.getClass().getMethod("createBond", (Class[]) null);
                            method.invoke(device, (Object[]) null);

                            LocalBroadcastManager.getInstance(TersusService.this).sendBroadcast(
                                    new Intent(TersusServiceConstants.BROADCAST_TERSUS_CONNECTION)
                                            .putExtra(TersusServiceConstants.TERSUS_CONNECTION, true)
                            );

                            //connect thread creats rfcomm connection, and cancels bt discovery
                            new ConnectThread(device).start();

                            // Toast.makeText(MainActivity.this, "Successfully paired with Tersus.", Toast.LENGTH_LONG).show();

                        } catch (Exception e) {

                            // Toast.makeText(MainActivity.this, "Something went wrong connecting to Tersus.", Toast.LENGTH_LONG).show();

                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };

    /* Thread that creates hexTronik socket and given a paired HB device */
    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
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
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            discover(false);

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    discover(true);
                } catch (IOException closeException) {
                    Log.e("CONNECT THREAD: RUN", "Could not close the client socket", closeException);
                }
                return;
            }

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
                Log.e("CONNECTED THREAD: input", "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("CONECTED THREAD: output", "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
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

                    LocalBroadcastManager.getInstance(TersusService.this).sendBroadcast(
                            new Intent(TersusServiceConstants.BROADCAST_TERSUS_CONNECTION)
                                    .putExtra(TersusServiceConstants.TERSUS_CONNECTION, false)
                    );
                    discover(true);

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
