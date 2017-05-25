package edu.ksu.wheatgenetics.tersusservice;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class TersusTestActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    private Mode mCurrentMode = Mode.NORMAL;
    private SparseArray<Double> baseCartCoords;

    private enum Mode { AVG_BASE, RTK, NORMAL }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tersus_test);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        baseCartCoords = new SparseArray<>();

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

                mCurrentMode = Mode.AVG_BASE;
                //TODO add dialog box to replace this Toast
                //add check box to not ask again, generalize to use in other places

                Toast.makeText(TersusTestActivity.this, "Ensure bluetooth is plugged into COM2 of base tersus precis!", Toast.LENGTH_LONG).show();

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

               configureInputDialog();

            }
        });
    }

    public void configureInputDialog() {

        final String retValue;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input configuration for Tersus");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCurrentMode = Mode.NORMAL;

                final String command = input.getText().toString();
                if (!command.isEmpty()) {
                    final Intent i = new Intent(TersusServiceConstants.TERSUS_COMMAND);
                    i.putExtra(TersusServiceConstants.TERSUS_COMMAND_STRING, command);
                    sendBroadcast(i);
                    ((TextView) findViewById(R.id.topTextView))
                            .setText(command);
                }
            }
        });

        builder.show();
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
                final TextView connectText = (TextView) findViewById(R.id.tersusConnectionTextView);
                if (connected) {
                    connectText.setTextColor(Color.GREEN);
                    connectText.setText(R.string.tersus_connected);
                } else {
                    connectText.setTextColor(Color.RED);
                    connectText.setText(R.string.tersus_disconnected);
                }
            }
           if (intent.hasExtra(TersusServiceConstants.TERSUS_OUTPUT)) {

               //start DFA for print mode

               switch (mCurrentMode) {
                   case NORMAL:
                       ((TextView) findViewById(R.id.midTextView))
                               .setText(((TersusString) intent.getExtras()
                                       .get(TersusServiceConstants.TERSUS_OUTPUT)).toString());
                       break;
                   case AVG_BASE:

                       //parse the lat/lng string
                       final TersusString ts = intent.getParcelableExtra(TersusServiceConstants.TERSUS_OUTPUT);

                       if (!ts.getLongitude().isEmpty() && !ts.getLatitude().isEmpty()) {

                           //nmea lat/lng values are in DDMM.MMMM format
                           final String[] latTokens = ts.getLatitude().split(".");
                           final String[] lngTokens = ts.getLongitude().split(".");

                           if (latTokens.length == 2 && lngTokens.length == 2) {

                               final double latRads = Math.toRadians(Double.valueOf(latTokens[0].substring(0, 2))
                                       + Double.valueOf(latTokens[0].substring(2) + "." + latTokens[1]) / 60.0);

                               final double lngRads = Math.toRadians(Double.valueOf(lngTokens[0].substring(0, 2))
                                       + Double.valueOf(lngTokens[0].substring(2) + "." + lngTokens[1]) / 60.0);

                               //convert to XYZ
                               //add as flattened array
                               //X = cos(lat) * cos(lng)
                               baseCartCoords.setValueAt(baseCartCoords.size(), Math.cos(latRads) * Math.cos(lngRads));
                               //Y = cos(lat) * sin(lng)
                               baseCartCoords.setValueAt(baseCartCoords.size(), Math.cos(latRads) * Math.sin(lngRads));
                               //Z = sin(lat)
                               baseCartCoords.setValueAt(baseCartCoords.size(), Math.sin(latRads));

                               //ensure baseCartCoords is a multiple of 3
                               if (baseCartCoords.size() % 3 == 0) {

                                   double avgX = baseCartCoords.get(baseCartCoords.keyAt(0));
                                   double avgY = baseCartCoords.get(baseCartCoords.keyAt(1));
                                   double avgZ = baseCartCoords.get(baseCartCoords.keyAt(2));
                                   double n = (double) (baseCartCoords.size() % 3);

                                   if (n > 1) {

                                       avgX /= n;
                                       avgY /= n;
                                       avgZ /= n;

                                       //start averaging at index 3 (the second set of coordinates)
                                       for (int i = 3; i < baseCartCoords.size(); i = i + 3) {
                                           avgX += baseCartCoords.get(baseCartCoords.keyAt(i)) / n;
                                           avgY += baseCartCoords.get(baseCartCoords.keyAt(i+1)) / n;
                                           avgZ += baseCartCoords.get(baseCartCoords.keyAt(i+2)) / n;
                                       }
                                   }

                                   final double hyp = Math.sqrt(Math.pow(avgX, 2) + Math.pow(avgY, 2));
                                   final double lat = Math.atan2(avgZ, hyp);
                                   final double lng = Math.atan2(avgY, avgX);

                                   ((TextView) findViewById(R.id.midTextView)).setText(lat + ", " + lng);
                               }
                           }
                       }
                       break;
               }
            }
        }
    }
}
