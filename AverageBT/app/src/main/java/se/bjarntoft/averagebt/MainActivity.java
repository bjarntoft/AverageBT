package se.bjarntoft.averagebt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;


/**
 * Created by Andreas Bjärntoft on 2016-04-10.
 */
public class MainActivity extends AppCompatActivity implements OnClickListener {
    private BluetoothReceiver btReceiver;
    private BluetoothAdapter btAdapter;
    private IntentFilter intentFilter;

    // Mätvariabler.
    private String nameBT;
    private int counter;
    private int counterLimit;
    private long pauseTime;
    private short totalRSSI;

    // Gui-komponenter.
    private EditText etName;
    private EditText etCount;
    private EditText etPause;
    private TextView tvStatusCount;
    private TextView tvStatusRSSI;
    private ProgressBar spinner;
    private Button btStart;
    private Button btStop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Identifierar gui-komponenter.
        etName = (EditText)findViewById(R.id.name);
        etCount = (EditText)findViewById(R.id.counts);
        etPause = (EditText)findViewById(R.id.pause);
        tvStatusCount = (TextView)findViewById(R.id.counts_status);
        tvStatusRSSI = (TextView)findViewById(R.id.rssi_status);
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
    }

    @Override
    protected void onDestroy() {
        stopReceiver();

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if(v == btStart) {
            // Nollställer variabler.
            counter = 0;
            totalRSSI = 0;

            // Extraherar bt-variabler.
            nameBT = etName.getText().toString();
            counterLimit = Integer.parseInt(etCount.getText().toString());
            pauseTime = Long.parseLong(etPause.getText().toString());

            // Startar bt-mätning.
            registerReceiver(btReceiver, intentFilter);
            btAdapter.startDiscovery();

            // Uppdaterar gui.
            setCountStatus("0");
            setRSSIStatus("0");

            makeRunnable(false);
        } else if(v == btStop) {
            // Stoppar bt-mätning.
            stopReceiver();

            // Uppdaterar gui.
            makeRunnable(true);
        }
    }


    private void stopReceiver() {
        unregisterReceiver(btReceiver);
    }


    private void setCountStatus(String string) {
        tvStatusCount.setText(string + " st");
    }


    private void setRSSIStatus(String string) {
        tvStatusRSSI.setText(string + " dBm");
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

                // Kontrollerar antalet genomförda mätning samt satt paustid.
                if(counter < counterLimit && pauseTime != 0) {
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
                } else if(counter < counterLimit && pauseTime == 0) {
                    btAdapter.startDiscovery();
                } else {
                    // Uppdatera gui.
                    makeRunnable(true);

                    // Stoppat bt-mätning.
                    stopReceiver();
                }
            } else if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                //System.out.println("BT-enhet funnen");

                // Identifierar funnen BT-enhet.
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();

                // Kontrollerar enheten.
                if(name != null && name.equals(nameBT)) {
                    counter++;

                    // Beräknar RSSI.
                    short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    totalRSSI += rssi;

                    //System.out.println("Mätning: " + rssi);
                    //System.out.println("Räknare: " + counter);
                    //System.out.println("Medelvärde: " + totalRSSI / counter);

                    setCountStatus(String.valueOf(counter));
                    setRSSIStatus(String.valueOf(totalRSSI / counter));
                }

                // Avbryter mätning.
                if(counter == counterLimit) {
                    btAdapter.cancelDiscovery();
                }
            }
        }
    }
}
