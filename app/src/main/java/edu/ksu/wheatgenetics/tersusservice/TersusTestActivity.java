package edu.ksu.wheatgenetics.tersusservice;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
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
                ((TextView) findViewById(R.id.tersusOutputTextView)).setText(
                        ((TersusString) intent.getExtras()
                                .get(TersusServiceConstants.TERSUS_OUTPUT)).toString()
                );
            }
            if (intent.hasExtra(TersusServiceConstants.TERSUS_DISCOVERY)) {
                final boolean discovering = (boolean) intent.getExtras()
                        .get(TersusServiceConstants.TERSUS_DISCOVERY);
                final String current = ((TextView) findViewById(R.id.tersusConnectionTextView)).getText().toString();
                ((TextView) findViewById(R.id.tersusConnectionTextView))
                        .setText(discovering ? current + " D" : current);
            }
        }
    }
}
