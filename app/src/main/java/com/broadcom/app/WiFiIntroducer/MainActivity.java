/******************************************************************************
 *
 *  Copyright (C) 2013-2014 Cypress Semiconductor
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
package com.broadcom.app.WiFiIntroducer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.broadcom.app.DevicePicker.DeviceListFragment.Callback;
import com.broadcom.app.DevicePicker.DevicePickerFragment;
import com.broadcom.app.WiFiIntroducer.GattUtils.RequestQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main activity for the the Hello Client application
 */
@SuppressLint("LongLogTag")
public class MainActivity extends Activity implements OnClickListener, Callback,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = Constants.TAG_PREFIX + "MainActivity";
    private static final String FRAGMENT_DEVICE_PICKER = "DevicePickerDialog";

    /**
     * Callback object that the LE Gatt service calls to report callback events that occur
     */
    private class GattCallback extends BluetoothGattCallback {

        /**
         * Callback invoked by Android framework and a LE connection state
         * change occurs
         */
        @SuppressLint("LongLogTag")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(MainActivity.TAG, "onConnectionStateChange(): address=="
                    + gatt.getDevice().getAddress() + ", status = " + status + ", state="
                    + newState);
            boolean isConnected = (newState == BluetoothAdapter.STATE_CONNECTED);

            boolean isOk = (status == 0);
            if (isConnected && isOk) {
                // Discover services, and return connection state = connected
                // after services discovered
                isOk = gatt.discoverServices();
                if (isOk) {
                    Log.d(MainActivity.TAG, "discoverServices ok");
                    return;
                }
            }

            // If we got here, this is a disconnect with or without error
            // close gatt connection
            if (!isOk) {
                Log.d(MainActivity.TAG, "discoverServices failed");
                gatt.close();
            }
            processConnectionStateChanged(false, !isOk);
        }

        /**
         * Callback invoked by Android framework when LE service discovery
         * completes
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered: address=="
                    + gatt.getDevice().getAddress() + ", status = " + status);

            if (status != 0) {
                // Error occurred. close the ocnnection and return a
                // disconnected status
                gatt.close();
                try {
                    processConnectionStateChanged(false, true);
                } catch (Throwable t) {
                    Log.e(TAG, "error", t);
                }
            } else {
                try {
                    processConnectionStateChanged(true, false);
                } catch (Throwable t) {
                    Log.e(TAG, "error", t);
                }
            }
        }

        /**
         * Callback invoked by Android framework when a characteristic read
         * completes
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            //Log.d(TAG, "onCharacteristicRead: address=="
            //        + gatt.getDevice().getAddress() + ", status = " + status);

            if (characteristic == null)
                Log.d(TAG, "characteristic == null");

            if (status == 0) {
                try {
                    processCharacteristicRead(characteristic);
                } catch (Throwable t) {
                    Log.e(TAG, "error", t);
                }
            }
            //Log.d(TAG, "status: " + status + ", read next");
            mRequestQueue.next();
        }

        /**
         * Callback invoked by Android framework when a descriptor read
         * completes
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
            //Log.d(TAG, "onDescriptorRead: address=="
            //        + gatt.getDevice().getAddress() + ", status = " + status);

            if (status == 0) {
                try {
                    processDescriptorRead(descriptor);
                } catch (Throwable t) {
                    Log.e(TAG, "error", t);
                }
            }
            mRequestQueue.next();// Execute the next queued request, if
            // any
        }

        /**
         * Callback invoked by Android framework when a characteristic
         * notification occurs
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //Log.d(TAG, "onCharacteristicChanged: address==" + gatt.getDevice().getAddress());

            try {
                processCharacteristicNotification(characteristic);
            } catch (Throwable t) {
                Log.e(TAG, "error", t);
            }
        }

        /**
         * Callback invoked by Android framework when a descriptor write
         * completes
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            Log.d(TAG, "onDescriptorWrite: address=="
                    + gatt.getDevice().getAddress() + ", status = " + status);

            if (status == 0) {
                try {
                    processDescriptorWrite(descriptor);
                } catch (Throwable t) {
                    Log.e(TAG, "error", t);
                }
            }

            mRequestQueue.next();// Execute the next queued request, if any

        }

        /**
         * Callback invoked by Android framework when a characteristic write
         * completes
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite: address=="
                    + gatt.getDevice().getAddress() + ", status = " + status);

            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == 0) {
                try {
                    processCharacteristicWrite(characteristic);
                } catch (Throwable t) {
                    Log.e(TAG, "error", t);
                }
            }

            mRequestQueue.next();// Execute the next queued request, if any
        }
    }

    // UI Components
    // Device picker components
    private LinearLayout mButtonSelectDevice; // Button to start device picker
    private DevicePickerFragment mDevicePicker;
    private TextView mTextDeviceName; // Displays device's name
    private TextView mTextDeviceAddress; // Displays device's address

    // Connection components
    private Button mButtonConnect; // Button to connect to a device
    private Button mButtonDisconnect; // Button to connect from a device
    private TextView mTextConnectionState; // Displays current connection state

    private final GattCallback mGattCallback = new GattCallback();
    private final RequestQueue mRequestQueue = GattUtils.createRequestQueue();
    private BluetoothAdapter mBtAdapter;
    private BluetoothDevice mPickedDevice;
    private BluetoothGatt mPickedDeviceGatt;
    private boolean mPickedDeviceIsConnected;
    private boolean mSyncNotificationSetting;

    //Wifi service components
    private ListView mWifiListView = null;
    private ArrayAdapter<String> mWifiAdapter = null;
    private ArrayList<String> mWifiList = new ArrayList<String>();
    private EditText mSsidEditText = null;
    private EditText mPasswordEditText = null;
    private Switch mShowPasswordSwitch = null;
    private Button mConnectWifiBtn = null;

    /**
     * Helper function to show a toast notification message
     *
     * @param msg
     */
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Check Bluetooth is available and enabled, and initialize Bluetooth
     * adapter
     *
     * @return
     */
    private boolean checkAndInitBluetooth() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
            return false;
        }
        return true;
    }

    /**
     * Initialize the device picker
     *
     * @return
     */
    private void initDevicePicker() {
        mDevicePicker = DevicePickerFragment.createDialog(this, null, true);
    }

    /**
     * Cleanup the device picker
     */
    private void cleanupDevicePicker() {
        if (mDevicePicker != null) {
            mDevicePicker = null;
        }
    }

    private void closeDevice() {
        if (mPickedDeviceGatt != null) {
            mPickedDeviceGatt.close();
            mPickedDeviceGatt = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check bluetooth is available. If not, exit
        if (!checkAndInitBluetooth()) {
            showMessage(getString(R.string.error_bluetooth_not_available));
            finish();
            return;
        }

        // Initialize the UI components, and register a listeners
        setContentView(R.layout.main);

        // Load device picker components
        mButtonSelectDevice = (LinearLayout) findViewById(R.id.btn_selectdevice);
        mButtonSelectDevice.setOnClickListener(this);
        mTextDeviceName = (TextView) findViewById(R.id.deviceName);
        mTextDeviceAddress = (TextView) findViewById(R.id.deviceAddress);
        mTextConnectionState = (TextView) findViewById(R.id.connectionState);

        // Load connection components
        mButtonConnect = (Button) findViewById(R.id.btn_connect);
        mButtonConnect.setOnClickListener(this);
        mButtonDisconnect = (Button) findViewById(R.id.btn_disconnect);
        mButtonDisconnect.setOnClickListener(this);

        //Load wifi service components
        mWifiListView = (ListView) findViewById(R.id.wifi_list_view);
        mSsidEditText = (EditText) findViewById(R.id.ssid_edit_text);
        mPasswordEditText = (EditText) findViewById(R.id.password_edit_text);
        if (mPasswordEditText != null)
            mPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT );
        else
            Log.e(TAG, "mPasswordEditText is null");
        mShowPasswordSwitch = (Switch) findViewById(R.id.show_password_switch);
        mShowPasswordSwitch.setOnCheckedChangeListener(this);
        mConnectWifiBtn = (Button) findViewById(R.id.connect_wifi_btn);
        mConnectWifiBtn.setOnClickListener(this);

        initWifi();

        // Initialize the device picker UI fragment
        initDevicePicker();

        // refresh the UI component states
        updateWidgets();


    }

    @Override
    public void onResume(){
        Log.d(TAG, "onResume");
        super.onResume();
        initWifi();
    }

    private void initWifi() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiManager.isWifiEnabled() && (TextUtils.isEmpty(mSsidEditText.getText())
                || !wifiInfo.getSSID().equals(mSsidEditText.getText().toString()))) {
            //wifiManager.setWifiEnabled(true);
            mSsidEditText.setText(Utils.removeQuote(wifiInfo.getSSID()));
        }
    }

    /**
     * Updates the UI widgets based on the latest connection state
     */
    private void updateWidgets() {
        if (mPickedDevice == null) {
            // No devices selected: set initial state
            mButtonConnect.setEnabled(false);
            mButtonDisconnect.setEnabled(false);
            mButtonSelectDevice.setEnabled(true);
            mTextDeviceName.setText(R.string.no_device);
            mTextDeviceAddress.setText("");
        } else {
            // Device picked, always set the connect/disconnect buttons enabled
            mButtonConnect.setEnabled(true);
            mButtonDisconnect.setEnabled(true);

            if (mPickedDeviceIsConnected) {
                // Set resources when connected

                // Disable selecting new device when connected
                mButtonSelectDevice.setEnabled(false);

                // Set the connection state status
                mTextConnectionState.setText(getString(R.string.connected));

                mConnectWifiBtn.setEnabled(true);
            } else {
                // Update resources when disconnected

                // Enable selecting new device when connected
                mButtonSelectDevice.setEnabled(true);

                // Set the connection state status
                mTextConnectionState.setText(getString(R.string.disconnected));

                mConnectWifiBtn.setEnabled(false);
            }
        }
    }

    @Override
    public void onDestroy() {
        closeDevice();
        cleanupDevicePicker();
        super.onDestroy();
    }

    /**
     * Callback invoked when buttons/switches clicked
     */
    @Override
    public void onClick(View v) {
        if (v == mButtonSelectDevice) {
            // Start the device selector
            mDevicePicker.show(getFragmentManager(), FRAGMENT_DEVICE_PICKER);
        } else if (v == mButtonConnect) {
            // Start device connection
            connect();
        } else if (v == mButtonDisconnect) {
            // Start device disconnect
            disconnect();
        } else if (v == mConnectWifiBtn) {
            if (!Utils.verifyWifiPwd(mPasswordEditText.getText().toString())) {
                showMessage("请正确填写WIFI密码");
                return;
            }

            writeWifiCharacteristic();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(!buttonView.isPressed()){
            return;
        } else {
            switch (buttonView.getId()) {
                case R.id.show_password_switch:
                    if (isChecked)
                        mPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    else
                        mPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT );
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Callback invoked when a device was picked from the device picker
     *
     * @param device
     */
    @Override
    public void onDevicePicked(BluetoothDevice device) {
        Log.d(TAG, "onDevicePicked: " + device == null ? "" : device.getAddress());
        // Close any outstanding connections to remote devices
        closeDevice();

        // Get the remote device object
        String address = device.getAddress();
        mPickedDevice = mBtAdapter.getRemoteDevice(address);

        // Get the name
        String name = mPickedDevice.getName();
        if (name == null || name.isEmpty()) {
            name = address;
        }

        // Set UI resources
        mTextDeviceName.setText(name);
        mTextDeviceAddress.setText(address);
        // Update the connect widget
        mButtonConnect.setEnabled(true);
        mButtonDisconnect.setEnabled(true);
    }

    /**
     * Callback invoked when a devicepicker was dismissed without a device
     * picked
     */
    @Override
    public void onDevicePickError() {
        Log.d(TAG, "onDevicePickError");
    }

    /**
     * Callback invoked when a devicepicker encountered an unexpected error
     */
    @Override
    public void onDevicePickCancelled() {
        Log.d(TAG, "onDevicePickCancelled");
    }

    /**
     * Connect to the picked device
     */
    private void connect() {
        if (mPickedDevice == null) {
            showMessage(getString(R.string.error_connect, mPickedDevice.getName(),
                    mPickedDevice.getAddress()));
            return;
        }

        mPickedDeviceGatt = mPickedDevice.connectGatt(this, false, mGattCallback);

        if (mPickedDeviceGatt == null) {
            showMessage(getString(R.string.error_connect, mPickedDevice.getName(),
                    mPickedDevice.getAddress()));
        }
    }

    /**
     * Disconnects the picked device
     */
    private void disconnect() {
        if (mPickedDeviceGatt != null) {
            mPickedDeviceGatt.disconnect();
            closeDevice();
        }
    }

    /**
     * Called when a gatt connection state changes. This function updates the UI
     */
    private void processConnectionStateChanged(final boolean isConnected, final boolean hasError) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (hasError) {
                    showMessage(getString(R.string.error_connect, mPickedDevice.getName(),
                            mPickedDevice.getAddress()));
                }
                mPickedDeviceIsConnected = isConnected;
                updateWidgets();

                // Refresh the device information
                if (mPickedDeviceIsConnected) {
                    mSyncNotificationSetting = true;
                    readEverything();
                }
            }
        });

    }

    private void printfAllCharacteristics() {
        List<BluetoothGattService> serviceList = mPickedDeviceGatt.getServices();
        Log.d(TAG, "list.size: " + serviceList.size());
        for (BluetoothGattService service : serviceList) {
            Log.d(TAG, "service uuid: " + service.getUuid().toString().trim().toUpperCase());
            Log.d(TAG, "includedServices size:" + service.getIncludedServices().size());

            List<BluetoothGattCharacteristic> cList = service.getCharacteristics();
            Log.d(TAG, "cList.size: " + cList.size());
            for (BluetoothGattCharacteristic characteristic : cList) {
                Log.d(TAG, "characteristic uuid: " + characteristic.getUuid().toString().trim().toUpperCase());
                int permission = characteristic.getPermissions();
                Log.d(TAG, "permission: " + Utils.getCharPermission(permission));

                int property = characteristic.getProperties();
                Log.d(TAG, "property: " + Utils.getCharPropertie(property));

                byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    Log.d(TAG, "data.length:" + data.length);
                    Log.d(TAG, "characteristic.getValue():" + new String(data));
                } else {
                    Log.d(TAG, "characteristic.getValue() == null");
                }

                List<BluetoothGattDescriptor> dList = characteristic.getDescriptors();
                Log.d(TAG, "dList.size: " + dList.size());
                for (BluetoothGattDescriptor descriptor : dList)
                    Log.d(TAG, "descriptor uuid: " + descriptor.getUuid().toString().trim().toUpperCase());
            }
        }
    }

    @SuppressLint("LongLogTag")
    private void writeWifiCharacteristic() {
        BluetoothGattCharacteristic characteristic = null;

        String ssid = mSsidEditText.getText().toString();
        Log.d(TAG, "ssid: " + ssid);
        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt, Constants.WIFI_SERVICE_UUID,
                Constants.WiFiIntroducerConfigNWSSIDUUID);
        //mPickedDeviceGatt.setCharacteristicNotification(characteristic, true);
        characteristic.setValue(ssid.getBytes());
        mRequestQueue.addWriteCharacteristic(mPickedDeviceGatt, characteristic);

        String password = mPasswordEditText.getText().toString();
        Log.d(TAG, "password: " + password);
        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt, Constants.WIFI_SERVICE_UUID,
                Constants.WiFiIntroducerConfigNWPassphraseUUID);
        //mPickedDeviceGatt.setCharacteristicNotification(characteristic, true);
        characteristic.setValue(password.getBytes());
        mRequestQueue.addWriteCharacteristic(mPickedDeviceGatt, characteristic);

        String checkdata = "liguang09897865";
        Log.d(TAG, "checkdata: " + checkdata);
        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt, Constants.WIFI_SERVICE_UUID,
                Constants.WiFiIntroducerConfigCheckDataUUID);
        //mPickedDeviceGatt.setCharacteristicNotification(characteristic, true);
        characteristic.setValue(checkdata.getBytes());
        mRequestQueue.addWriteCharacteristic(mPickedDeviceGatt, characteristic);

        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt, Constants.WIFI_SERVICE_UUID,
                Constants.WiFiIntroducerConfigCharNotifyValueUUID);
        BluetoothGattDescriptor descriptor = GattUtils.getDescriptor(mPickedDeviceGatt,
                Constants.WIFI_SERVICE_UUID, Constants.WiFiIntroducerConfigCharNotifyValueUUID,
                Constants.CLIENT_CONFIG_DESCRIPTOR_UUID);
        mPickedDeviceGatt.setCharacteristicNotification(characteristic, true);
        //具有NOTIFY|INDICATE 属性，根据属性设置相应的值，这里先默认设置为ENABLE_NOTIFICATION_VALUE, tiantian
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mRequestQueue.addWriteDescriptor(mPickedDeviceGatt, descriptor);

        mRequestQueue.execute();
    }

    private void readWifiSetupNetCharacteristics() {
        Log.d(TAG, "readWifiSetupNetCharacteristics");
        BluetoothGattCharacteristic characteristic = null;

        BluetoothGattDescriptor descriptor = GattUtils.getDescriptor(mPickedDeviceGatt, Constants.WIFI_SERVICE_UUID,
                Constants.WiFiIntroducerConfigCharNotifyValueUUID, Constants.CLIENT_CONFIG_DESCRIPTOR_UUID);
        mRequestQueue.addReadDescriptor(mPickedDeviceGatt, descriptor);

        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt,
                Constants.WIFI_SERVICE_UUID, Constants.WiFiIntroducerConfigNWSecurityUUID);
        mRequestQueue.addReadCharacteristic(mPickedDeviceGatt, characteristic);

        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt,
                Constants.WIFI_SERVICE_UUID, Constants.WiFiIntroducerConfigNWSSIDUUID);
        mRequestQueue.addReadCharacteristic(mPickedDeviceGatt, characteristic);

        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt,
                Constants.WIFI_SERVICE_UUID, Constants.WiFiIntroducerConfigNWPassphraseUUID);
        mRequestQueue.addReadCharacteristic(mPickedDeviceGatt, characteristic);

        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt,
                Constants.WIFI_SERVICE_UUID, Constants.WiFiIntroducerConfigCheckDataUUID);
        mRequestQueue.addReadCharacteristic(mPickedDeviceGatt, characteristic);

        mRequestQueue.execute();
    }

    private void readDeviceNameCharacteristics() {
        Log.d(TAG, "readDeviceNameCharacteristics");
        BluetoothGattCharacteristic characteristic = null;

        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt,
                Constants.GAP_SERVICE_UUID, Constants.DEVICE_NAME_UUID);
        mRequestQueue.addReadCharacteristic(mPickedDeviceGatt, characteristic);

        mRequestQueue.execute();
    }

    /**
     * Reads the device info characteristics and updates the UI components
     */
    private void readDeviceInfoCharacteristics() {
        // Get all readable characteristics and descriptors of interest and add
        // request to a request queue

        BluetoothGattCharacteristic characteristic = null;

        // Get model number
        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt,
                Constants.DEVICE_INFO_SERVICE_UUID, Constants.MODEL_NUMBER_UUID);
        mRequestQueue.addReadCharacteristic(mPickedDeviceGatt, characteristic);

        // Get manufacturer name
        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt,
                Constants.DEVICE_INFO_SERVICE_UUID, Constants.MANUFACTURER_NAME_UUID);
        mRequestQueue.addReadCharacteristic(mPickedDeviceGatt, characteristic);

        // Get system Id
        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt,
                Constants.DEVICE_INFO_SERVICE_UUID, Constants.SYSTEM_ID_UUID);
        mRequestQueue.addReadCharacteristic(mPickedDeviceGatt, characteristic);
        mRequestQueue.execute();
    }

    /**
     * Reads the battery characteristics and updates the UI components
     */
    private void readBatteryCharacteristic() {
        // Get all readable characteristics and descriptors of interest and add
        // request to a request queue

        BluetoothGattCharacteristic characteristic = null;
        // Get battery level
        characteristic = GattUtils.getCharacteristic(mPickedDeviceGatt,
                Constants.BATTERY_SERVICE_UUID, Constants.BATTERY_LEVEL_UUID);
        mRequestQueue.addReadCharacteristic(mPickedDeviceGatt, characteristic);
        mRequestQueue.execute();
    }

    /**
     * Read every characteristic on the device
     */
    private void readEverything() {
        printfAllCharacteristics();
        //readDeviceInfoCharacteristics();
        //readBatteryCharacteristic();
        readDeviceNameCharacteristics();
        readWifiSetupNetCharacteristics();
    }

    /**
     * Callback invoked by the Android framework when a read characteristic
     * successfully completes
     *
     * @param characteristic
     */
    private void processCharacteristicRead(final BluetoothGattCharacteristic characteristic) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                UUID uuid = characteristic.getUuid();
                Log.d(TAG, "read callback uuid: " + uuid);
                if (Constants.MANUFACTURER_NAME_UUID.equals(uuid)) {
                    Log.d(TAG, "MANUFACTURER_NAME_UUID");
                    Log.d(TAG, characteristic.getStringValue(0));
                } else if (Constants.MODEL_NUMBER_UUID.equals(uuid)) {
                    Log.d(TAG, "MODEL_NUMBER_UUID");
                    Log.d(TAG, characteristic.getStringValue(0));
                } else if (Constants.SYSTEM_ID_UUID.equals(uuid)) {
                    Log.d(TAG, "SYSTEM_ID_UUID");
                    byte[] systemIdBytes = characteristic.getValue();
                    if (systemIdBytes != null && systemIdBytes.length > 0) {
                        long systemIdLong = GattUtils.unsignedBytesToLong(systemIdBytes, 8, 0);
                        long manuId = 0xFFFFFFFFFFL & systemIdLong; // 40bits
                        long orgId = 0xFFFFFFL & (systemIdLong >> 40);
                        String manuIdString = String.format("%010X", manuId);
                        String orgIdString = String.format("%06X", orgId);
                        Log.d(TAG, orgIdString + " " + manuIdString);
                    }
                } else if (Constants.BATTERY_LEVEL_UUID.equals(uuid)) {
                    Log.d(TAG, "BATTERY_LEVEL_UUID");
                    int batteryLevel = characteristic.getIntValue(
                            BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    Log.d(TAG, String.valueOf(batteryLevel));
                } else if (Constants.WiFiIntroducerConfigCharNotifyValueUUID.equals(uuid)){
                    Log.d(TAG, "WiFiIntroducerConfigCharNotifyValueUUID");
                    Log.d(TAG, "characteristic.getValue(): " + characteristic.getIntValue(
                            BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                } else if (Constants.WiFiIntroducerConfigNWSecurityUUID.equals(uuid)){
                    Log.d(TAG, "WiFiIntroducerConfigNWSecurityUUID");
                    Log.d(TAG, "characteristic.getValue(): " + characteristic.getIntValue(
                            BluetoothGattCharacteristic.FORMAT_UINT8, 0));
                } else if (Constants.WiFiIntroducerConfigNWSSIDUUID.equals(uuid)){
                    Log.d(TAG, "WiFiIntroducerConfigNWSSIDUUID");
                    Log.d(TAG, "characteristic.getValue(): " + characteristic.getStringValue(0));
                } else if (Constants.WiFiIntroducerConfigNWPassphraseUUID.equals(uuid)){
                    Log.d(TAG, "WiFiIntroducerConfigNWPassphraseUUID");
                    Log.d(TAG, "characteristic.getValue(): " + characteristic.getStringValue(0));
                } else if (Constants.WiFiIntroducerConfigCheckDataUUID.equals(uuid)){
                    Log.d(TAG, "WiFiIntroducerConfigCheckDataUUID");
                    Log.d(TAG, "characteristic.getValue(): " + characteristic.getStringValue(0));
                } else if (Constants.DEVICE_NAME_UUID.equals(uuid)) {
                    Log.d(TAG, "DEVICE_NAME_UUID");
                    Log.d(TAG, "characteristic.getValue(): " + characteristic.getStringValue(0));
                }
            }
        });
    }

    /**
     * Callback invoked by the Android framework when a read descriptor
     * successfully completes
     *
     * @param descriptor
     */
    private void processDescriptorRead(final BluetoothGattDescriptor descriptor) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                UUID uuid = descriptor.getUuid();
                if (Constants.CLIENT_CONFIG_DESCRIPTOR_UUID.equals(uuid)) {
                    byte[] descriptorBytes = descriptor.getValue();
                    int notificationState = 0;
                    notificationState = (int) GattUtils.unsignedBytesToLong(descriptorBytes, 2, 0);
                    Log.d(TAG, "notificationState = " + notificationState);
                    if (notificationState >= 0 && notificationState <= 2) {
                        // Temporarily disable the spinner listener when we set
                        // the selection
                    }
                    if (mSyncNotificationSetting && notificationState > 0) {
                        // On initial connection, mSyncNotificationSetting is
                        // set to indicate that we should set characteristic
                        // notification if the descriptor is set
                        Log.d(TAG, "setCharacteristicNotification true ");
                        mSyncNotificationSetting = false;
                        BluetoothGattCharacteristic notifyCharacteristic = GattUtils
                                .getCharacteristic(mPickedDeviceGatt, Constants.WIFI_SERVICE_UUID,
                                        Constants.WiFiIntroducerConfigCharNotifyValueUUID);
                        mPickedDeviceGatt.setCharacteristicNotification(notifyCharacteristic, true);
                    }
                }
            }
        });
    }

    /**
     * Callback invoked by the Android framework when a write descriptor
     * successfully completes
     *
     * @param descriptor
     */
    private void processDescriptorWrite(final BluetoothGattDescriptor descriptor) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                //readEverything(); //tiantian
            }
        });
    }

    /**
     * Callback invoked by the Android framework when a write characteristic
     * successfully completes
     *
     * @param characteristic
     */
    private void processCharacteristicWrite(final BluetoothGattCharacteristic characteristic) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                //readEverything(); //tiantian
            }
        });
    }

    /**
     * Callback invoked by the Android framework when a characteristic
     * notification is received
     *
     * @param characteristic
     */
    private void processCharacteristicNotification(final BluetoothGattCharacteristic characteristic) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String s = characteristic.getStringValue(0);
                Log.d(TAG, "processCharacteristicNotification: " + s);
            }
        });
    }

}
