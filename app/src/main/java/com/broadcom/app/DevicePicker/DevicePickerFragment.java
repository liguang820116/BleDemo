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
package com.broadcom.app.DevicePicker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.util.Log;

import com.broadcom.app.DevicePicker.DeviceListFragment.Callback;
import com.broadcom.app.WiFiIntroducer.Constants;
import com.broadcom.app.WiFiIntroducer.R;

import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.Manifest;
import android.widget.Toast;

import java.util.Collection;

/**
 * Wrapper fragment to wrap the Device Picker device list in a Fragment
 */
public class DevicePickerFragment extends DialogFragment implements DeviceListFragment.Callback,
        android.view.View.OnClickListener {
    private static final String TAG = Constants.TAG_PREFIX + "DevicePickerFragment";

    public static DevicePickerFragment createDialog(Callback callback, String dialogTitle,
            boolean startScanning) {
        DevicePickerFragment f = new DevicePickerFragment();
        f.mTitle = dialogTitle;
        f.mCallback = callback;
        f.mStartScanning = startScanning;
        f.setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogTheme);

        return f;
    }

    private String mTitle;
    private Callback mCallback;
    private Button mScanButton;
    private boolean mIsScanning;
    private boolean mStartScanning;
    private DeviceListFragment mDevicePickerFragment;

    private void setScanState(boolean isScanning) {
        if (isScanning) {
            mScanButton.setText(R.string.devicepicker_menu_stop);
        } else {
            mScanButton.setText(R.string.devicepicker_menu_scan);
        }
        mIsScanning = isScanning;

    }

    private void initDevicePickerFragment() {
        FragmentManager mgr = getFragmentManager();
        mDevicePickerFragment = (DeviceListFragment) mgr.findFragmentById(R.id.device_picker_id);
        mDevicePickerFragment.setCallback(this);

    }

    private void scan() {
        if (!mIsScanning) {
            setScanState(true);
            mDevicePickerFragment.scan(true);
        }
    }

    private void stopScan() {
        if (mIsScanning) {
            setScanState(false);
            mDevicePickerFragment.scan(false);
        }
    }
    @SuppressLint("LongLogTag")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity appContext = getActivity();
        View view = appContext.getLayoutInflater().inflate(R.layout.devicepicker_layout, null);
        mScanButton = (Button) view.findViewById(R.id.scan_button);
        mScanButton.setOnClickListener(this);
        initDevicePickerFragment();
        AlertDialog.Builder builder = new AlertDialog.Builder(appContext);
        builder.setTitle(mTitle != null ? mTitle : getActivity().getString(
                R.string.devicepicker_default_title));
                
        if (isLocationOpen(appContext)) {
            Log.d(TAG, "Location opened");
            
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    //请求权限
                    ActivityCompat.requestPermissions(appContext,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION},
                            1);
                    if (ActivityCompat.shouldShowRequestPermissionRationale(appContext,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        //判断是否需要解释
                        Toast.makeText(appContext, "open bluetooth permission", Toast.LENGTH_SHORT).show();
                    }
                }

        } else {
            Log.d(TAG, "Location not opened, need opened");
            Intent enableLocate = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(enableLocate, 1);
        }
        
        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onClick(View v) {
        boolean isScanning = !mIsScanning;
        setScanState(isScanning);
        mDevicePickerFragment.scan(isScanning);
    }

    @Override
    public void onDevicePicked(BluetoothDevice device) {
        if (mCallback != null) {
            mCallback.onDevicePicked(device);
        }
        dismiss();
    }

    @Override
    public void onDevicePickCancelled() {
        if (mCallback != null) {
            mCallback.onDevicePickCancelled();
        }

    }

    @Override
    public void onDevicePickError() {
        if (mCallback != null) {
            mCallback.onDevicePickError();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStartScanning) {
            scan();
        } else {
            stopScan();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopScan();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mDevicePickerFragment != null) {
            mDevicePickerFragment.getFragmentManager().beginTransaction()
                    .remove(mDevicePickerFragment).commit();
        }
    }

    /**
     * Add a collection of devices to the list of devices excluded from the
     * device picker
     *
     */
    public void addExcludedDevices(Collection<String> deviceAddresses) {
        mDevicePickerFragment.addExcludedDevices(deviceAddresses);
    }

    /**
     * Add a device to the list of devices excluded from the device picker
     *
     * @param deviceAddress
     */
    public void addExcludedDevice(String deviceAddress) {
        mDevicePickerFragment.addExcludedDevice(deviceAddress);
    }

    /**
     * Remove the device from the list of devices excluded from the device
     * picker
     *
     */
    public void removeExcludedDevice(String address) {
        mDevicePickerFragment.removeExcludedDevice(address);
    }

    /**
     * Clear the list of devices excluded from the device picker
     *
     */
    public void clearExcludedDevices() {
        mDevicePickerFragment.clearExcludedDevices();
    }
        
        
     /**
     *判断位置信息是否开启
     * @param context
     * @return
     */
    public static boolean isLocationOpen(final Context context){
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        //gps定位
        boolean isGpsProvider = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        //网络定位
        boolean isNetWorkProvider = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return isGpsProvider|| isNetWorkProvider;
    }

}