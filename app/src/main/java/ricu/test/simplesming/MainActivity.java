package ricu.test.simplesming;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.bluetooth.*;
import android.widget.ListView;
import android.widget.TextView;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


public class MainActivity extends Activity  implements BluetoothAdapter.LeScanCallback{
    private String TAG = "MainActivity";
    private String DEVICE = "TXW51";

    private static final UUID DEVICE_INFO_SERVICE           = UUID.fromString("8EDF0100-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID DEVICE_INFO_CHAR_MANUFACTURER = UUID.fromString("8EDF0101-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID DEVICE_INFO_CHAR_MODEL        = UUID.fromString("8EDF0102-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID DEVICE_INFO_CHAR_SERIAL       = UUID.fromString("8EDF0103-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID DEVICE_INFO_CHAR_HW_REV       = UUID.fromString("8EDF0104-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID DEVICE_INFO_CHAR_FW_REV       = UUID.fromString("8EDF0105-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID DEVICE_INFO_CHAR_DEVICE_NAME  = UUID.fromString("8EDF0106-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID DEVICE_INFO_CHAR_SAVE_VALUES  = UUID.fromString("8EDF0107-67E5-DB83-F85B-A1E2AB1C9E7A");

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG  = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID LSM330_SERVICE                = UUID.fromString("8EDF0200-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID LSM330_CHAR_ACC_EN            = UUID.fromString("8EDF0201-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID LSM330_CHAR_GYRO_EN           = UUID.fromString("8EDF0202-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID LSM330_CHAR_TEMP_SAMPLE       = UUID.fromString("8EDF0203-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID LSM330_CHAR_ACC_FSCALE        = UUID.fromString("8EDF0204-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID LSM330_CHAR_GYRO_FSCALE       = UUID.fromString("8EDF0205-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID LSM330_CHAR_ACC_ODR           = UUID.fromString("8EDF0206-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID LSM330_CHAR_GYRO_ODR          = UUID.fromString("8EDF0207-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID LSM330_CHAR_TRIGGER_VAL       = UUID.fromString("8EDF0208-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID LSM330_CHAR_TRIGGER_AXIS      = UUID.fromString("8EDF0209-67E5-DB83-F85B-A1E2AB1C9E7A");

    private static final UUID MEASURE_SERVICE               = UUID.fromString("8EDF0300-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID MEASURE_CHAR_START            = UUID.fromString("8EDF0301-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID MEASURE_CHAR_STOP             = UUID.fromString("8EDF0302-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID MEASURE_CHAR_DURATION         = UUID.fromString("8EDF0303-67E5-DB83-F85B-A1E2AB1C9E7A");
    private static final UUID MEASURE_CHAR_DATASTREAM       = UUID.fromString("8EDF0304-67E5-DB83-F85B-A1E2AB1C9E7A");

    private BluetoothAdapter mBluetooth;

    private BluetoothGatt mConnectedGatt = null;

    private ArrayList<DataPoint> series;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetooth = manager.getAdapter();
        String status = "No BluetoothAdapter";
        if(mBluetooth != null)
        {
            mBluetooth.setName("AndroidBTLe");
            if (mBluetooth.isEnabled()) {
                mBluetooth.startLeScan(this);
            }
            else
            {
                status = "Bluetooth is not Enabled.";
                Log.i(TAG, status);
            }
        }
        else
        {
            Log.i(TAG, status);
        }

        series = new ArrayList<DataPoint>();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Disconnect from any active tag connection
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int firstdevice = 1;
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if(device.getName().compareTo(DEVICE)==0&&firstdevice==1)
        {
            firstdevice=0;
            Log.i(TAG, "New LE Device: " + device.getName() + " @ " + rssi);
            TextView sensortype = (TextView)findViewById(R.id.sensorType);
            sensortype.setText(device.getName() + " " + device.getAddress() + " " + rssi + "dB");
            mBluetooth.stopLeScan(this);
            Log.i(TAG, "connect");
            device.connectGatt(this, true, mGattCallback);
        }
    }

    int mState = 0;

    public void refreshTemp(BluetoothGatt gatt) {
        BluetoothGattCharacteristic characteristic = null;
        switch(mState) {
            case 0:
                characteristic = gatt.getService(LSM330_SERVICE)
                        .getCharacteristic(LSM330_CHAR_TEMP_SAMPLE);
                gatt.readCharacteristic(characteristic);
                break;
        }
    }

    public void startMeasurement(BluetoothGatt gatt)
    {
        BluetoothGattCharacteristic characteristic = null;
        switch(mState) {
            case 0:
                characteristic = gatt.getService(LSM330_SERVICE)
                        .getCharacteristic(LSM330_CHAR_ACC_EN);
                characteristic.setValue(new byte[]{0x01});
                gatt.writeCharacteristic(characteristic);
                break;
            case 1:
                characteristic = gatt.getService(LSM330_SERVICE)
                        .getCharacteristic(LSM330_CHAR_GYRO_EN);
                characteristic.setValue(new byte[]{0x01});
                gatt.writeCharacteristic(characteristic);
                break;
            case 2:
                characteristic = gatt.getService(MEASURE_SERVICE)
                        .getCharacteristic(MEASURE_CHAR_START);
                characteristic.setValue(new byte[]{0x01});
                gatt.writeCharacteristic(characteristic);
                break;
            case 3:
                mState=0;
                Message mymsg = new Message();
                mymsg.obj = gatt;
                mymsg.what = 0;
                mHandler.sendMessageDelayed(mymsg, 500);
                break;
        }
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //Connection established
            if (status == BluetoothGatt.GATT_SUCCESS
                    && newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "connected");
                mConnectedGatt = gatt;
                //Discover services
                Log.i(TAG, "discover services");
                gatt.discoverServices();

            } else if (status == BluetoothGatt.GATT_SUCCESS
                    && newState == BluetoothProfile.STATE_DISCONNECTED) {
                //Handle a disconnect event

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "discovered service");
            //Now we can start reading/writing characteristics
            Log.i(TAG, "start measurement");
            Message mymsg = new Message();
            mymsg.obj = gatt;
            mymsg.what = 1;
            mHandler.sendMessageDelayed(mymsg, 100);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (LSM330_CHAR_ACC_EN.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                Log.i(TAG, "acc enabled: " + data[0]);
                mState++;
            }
            else if (LSM330_CHAR_GYRO_EN.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                Log.i(TAG, "gyro enabled: " + data[0]);
                mState++;
            }
            else if (MEASURE_CHAR_START.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                Log.i(TAG, "start measurement: " + data[0]);
                mState++;
            }
            Message mymsg = new Message();
            mymsg.obj = gatt;
            mymsg.what = 1;
            mHandler.sendMessageDelayed(mymsg, 100);
        }
        int first = 1;
        long last_timestamp = 0;
        double time = 0;
        int cnt = 0;
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //For each read, pass the data up to the UI thread to update the display
            if(LSM330_CHAR_TEMP_SAMPLE.equals(characteristic.getUuid()))
            {
                byte[] data = characteristic.getValue();
                int temp = data[0];
                final long timestamp = System.currentTimeMillis();;
                if(first == 1)
                {
                    first = 0;
                    last_timestamp = timestamp;
                }
                time += (timestamp - last_timestamp)/1000.0;
                Log.i(TAG, "temp: " + temp + " °C " + (int)time + " s");
                DataPoint tempPoint = new DataPoint((int)time,temp);
                last_timestamp = timestamp;
                series.add(tempPoint);
                if(cnt%3==0)
                {
                    Log.i(TAG, "create graph...");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GraphView graph = (GraphView) findViewById(R.id.graph);
                            graph.setTitle("Temperatur");
                            graph.setFitsSystemWindows(true);
                            DataPoint[] tempdata;
                            int size = series.size();
                            int offset = 0;
                            if(size<=100) {
                                tempdata = new DataPoint[size];
                            }
                            else
                            {
                                offset = size - 101;
                                size = 100;
                                tempdata = new DataPoint[size];
                            }
                            for (int i = offset; i < offset + size; i++) {
                                tempdata[i-offset] = series.get(i);
                            }
                            LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(tempdata);
                            graph.removeAllSeries();
                            graph.addSeries(series);
                        }
                    });
                }
                cnt++;
                Message mymsg = new Message();
                mymsg.obj = gatt;
                mymsg.what = 0;
                mHandler.sendMessageDelayed(mymsg, 500);
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case 0:
                    refreshTemp((BluetoothGatt)msg.obj);
                    break;
                case 1:
                    startMeasurement((BluetoothGatt)msg.obj);
                    break;
            }
        }
    };
}
