package se.bjarntoft.averagebt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;


/**
 * Created by Andreas Bjärntoft on 2016-04-17.
 */
public class DetectionActivity extends AppCompatActivity implements View.OnClickListener {
    private BluetoothReceiver btReceiver;
    private Boolean btReceiverRegistered;
    private BluetoothAdapter btAdapter;
    private IntentFilter intentFilter;

    // Mätvariabler.
    private String nameBT;
    private int counter;
    private int limitCounter;
    private short rssiLimit;
    private long pauseTime;

    // Gui-komponenter.
    private EditText etName;
    private EditText etLimit;
    private EditText etPause;
    private TextView tvStatusCount;
    private TextView tvStatusLimitCount;
    private ProgressBar spinner;
    private Button btStart;
    private Button btStop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        // Identifierar gui-komponenter.
        etName = (EditText)findViewById(R.id.name);
        etLimit = (EditText)findViewById(R.id.limit);
        etPause = (EditText)findViewById(R.id.pause);
        tvStatusCount = (TextView)findViewById(R.id.counts_status);
        tvStatusLimitCount = (TextView)findViewById(R.id.limitCounts_status);
        spinner = (ProgressBar)findViewById(R.id.progressBar1);
        btStart = (Button)findViewById(R.id.start);
        btStop = (Button)findViewById(R.id.stop);

        //
        makeRunnable(true);

        // Ansluter knappar.
        btStart.setOnClickListener(this);
        btStop.setOnClickListener(this);

        // Definierar bt-filter.
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        // Ansluter bt-objekt.
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btReceiver = new BluetoothReceiver();
        btReceiverRegistered = false;
    }

    @Override
    protected void onDestroy() {
        stopReceiver();

        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            case R.id.average:
                intent = new Intent(this, MainActivity.class);
                this.startActivity(intent);
                return true;
            case R.id.detection:
                intent = new Intent(this, DetectionActivity.class);
                this.startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onClick(View v) {
        if(v == btStart) {
            // Nollställer variabler.
            counter = 0;
            limitCounter = 0;

            // Extraherar bt-variabler.
            nameBT = etName.getText().toString();
            rssiLimit = Short.parseShort(etLimit.getText().toString());
            pauseTime = Long.parseLong(etPause.getText().toString());

            // Startar bt-mätning.
            registerReceiver(btReceiver, intentFilter);
            btReceiverRegistered = true;
            btAdapter.startDiscovery();

            // Uppdaterar gui.
            setCountStatus("0");
            setLimitCountStatus("0");

            makeRunnable(false);
        } else if(v == btStop) {
            // Stoppar bt-mätning.
            stopReceiver();

            // Uppdaterar gui.
            makeRunnable(true);
        }
    }


    private void stopReceiver() {
        if(btReceiverRegistered) {
            unregisterReceiver(btReceiver);
            btReceiverRegistered = false;
        }
    }


    private void setCountStatus(String string) {
        tvStatusCount.setText(string + " st");
    }


    private void setLimitCountStatus(String string) {
        tvStatusLimitCount.setText(string + " st");
    }


    private void makeRunnable(Boolean value) {
        btStart.setEnabled(value);
        btStop.setEnabled(!value);

        if(value) {
            spinner.setVisibility(View.GONE);
        } else {
            spinner.setVisibility(View.VISIBLE);
        }
    }


    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Kontrollerar mottaget anrop.
            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //System.out.println("BT-mätning påbörjas.");
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //System.out.println("BT-mätning avslutad.");

                // Kontrollerar satt paustid.
                if(pauseTime != 0) {
                    // Pausar innan ny mätning.
                    new CountDownTimer(pauseTime, 1000) {
                        public void onTick(long millisUntilFinished) {
                            //System.out.println("Pausar: " + millisUntilFinished / 1000);
                        }

                        public void onFinish() {
                            //System.out.println("Paus klar.");
                            btAdapter.startDiscovery();
                        }
                    }.start();
                } else {
                    btAdapter.startDiscovery();
                }
            } else if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                //System.out.println("BT-enhet funnen");

                // Identifierar funnen BT-enhet.
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                System.out.println(rssi);

                // Kontrollerar enheten.
                if(name != null && name.equals(nameBT)) {
                    counter++;

                    // Uppdaterar gui.
                    setCountStatus(String.valueOf(counter));

                    // Kontrollerar rssi för enheten.
                    if(rssi >= -rssiLimit) {
                        limitCounter++;

                        // Uppdaterar gui.
                        setLimitCountStatus(String.valueOf(limitCounter));
                    }
                }
            }
        }
    }
}
