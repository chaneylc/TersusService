package edu.ksu.wheatgenetics.tersusservice;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class TersusTestActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tersus_test);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(TersusServiceConstants.BROADCAST_TERSUS_CONNECTION);
        filter.addAction(TersusServiceConstants.BROADCAST_TERSUS_DISCOVERY);
        filter.addAction(TersusServiceConstants.BROADCAST_TERSUS_OUTPUT);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new ResponseReceiver(),
                filter
        );

        checkLocationPermission();

        ((Button) findViewById(R.id.avgBaseButton)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                final String command = "log com2 gpgga ontime 1\r\n";
                final Intent i = new Intent(TersusServiceConstants.TERSUS_COMMAND);
                i.putExtra(TersusServiceConstants.TERSUS_COMMAND_STRING, command);
                sendBroadcast(i);
                ((TextView) findViewById(R.id.topTextView))
                        .setText(command);
            }
        });

        ((Button) findViewById(R.id.configureButton)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                final String command = ((EditText) findViewById(R.id.configureEditText))
                        .getText().toString();
                if (!command.isEmpty()) {
                    final Intent i = new Intent(TersusServiceConstants.TERSUS_COMMAND);
                    i.putExtra(TersusServiceConstants.TERSUS_COMMAND_STRING, command);
                    sendBroadcast(i);
                    ((TextView) findViewById(R.id.topTextView))
                            .setText(command);
                }

            }
        });
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        stopService(new Intent(this, TersusService.class));
    }

    protected void checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    TersusServiceConstants.REQUEST_COARSE_LOCATION);
        } else connectTersus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == TersusServiceConstants.REQUEST_COARSE_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectTersus();
            } else {
                //TODO re-request
            }
        }
    }


    private void connectTersus() {

        if (mBluetoothAdapter != null) {

            if (!mBluetoothAdapter.isEnabled()) {

                final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, TersusServiceConstants.REQUEST_ENABLE_BT);

            }

            startService(new Intent(this, TersusService.class));

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == TersusServiceConstants.REQUEST_ENABLE_BT) {

            if (resultCode == RESULT_OK) {


            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }

    private class ResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra(TersusServiceConstants.TERSUS_CONNECTION)) {
                final boolean connected = ((boolean) intent.getExtras()
                        .get(TersusServiceConstants.TERSUS_CONNECTION));
                ((TextView) findViewById(R.id.tersusConnectionTextView))
                        .setText(connected ? "TersusOnline" : "TersusOffline");

            }
           if (intent.hasExtra(TersusServiceConstants.TERSUS_OUTPUT)) {
                ((TextView) findViewById(R.id.midTextView)).setText(
                        ((TersusString) intent.getExtras()
                                .get(TersusServiceConstants.TERSUS_OUTPUT)).toString()
                );
            }
        }
    }
}
