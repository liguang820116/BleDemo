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

import java.util.UUID;

/**
 * Contains the UUID of services, characteristics, and descriptors
 */
public class Constants {
    public static final String TAG_PREFIX = "WiFiIntroducer."; //used for debugging

    /**
     * UUID of Wifi Service
     */
    public static final UUID WIFI_SERVICE_UUID = UUID
            .fromString("1B7E8251-2877-41C3-B46E-CF057C562045");

    /**
     * UUID of wifi set up net characteristic
     */
    public static final UUID WiFiIntroducerConfigCharNotifyValueUUID = UUID
            .fromString("8AC32D3f-5CB9-4D44-BEC2-EE689169F638");

    public static final UUID WiFiIntroducerConfigNWSecurityUUID = UUID
            .fromString("CAC2ABA4-EDBB-4C4A-BBAF-0A84A5CD9323");

    public static final UUID WiFiIntroducerConfigNWSSIDUUID = UUID
            .fromString("ACA0EF7C-EEAA-48AD-9508-19A6CEF6B768");

    public static final UUID WiFiIntroducerConfigNWPassphraseUUID = UUID
            .fromString("40B7DE33-93E4-4C8B-A876-D833B415ASD");

    public static final UUID WiFiIntroducerConfigCheckDataUUID = UUID
            .fromString("40B7DE33-93E4-4C8B-A876-D833B415C765");

    /**
     * UUID of Gatt Service
     */
    public static final UUID GATT_SERVICE_UUID = UUID
            .fromString("00001801-0000-1000-8000-00805F9B34FB");

    //Charateristtic
    public static final UUID SERVICE_CHANGE_UUID = UUID
            .fromString("00002A05-0000-1000-8000-00805F9B34FB");

    /**
     * UUID of Gap Service
     */
    public static final UUID GAP_SERVICE_UUID = UUID
            .fromString("00001800-0000-1000-8000-00805F9B34FB");

    //Charateristtic
    public static final UUID DEVICE_NAME_UUID = UUID
            .fromString("00002A00-0000-1000-8000-00805F9B34FB");

    public static final UUID APPEARANCE_UUID = UUID
            .fromString("00002A01-0000-1000-8000-00805F9B34FB");

    public static final UUID PERI_CONN_PARAM_UUID = UUID
            .fromString("00002A04-0000-1000-8000-00805F9B34FB");

    public static final UUID EQUIPMENT_APPEARANCE_UUID = UUID
            .fromString("00002AA6-0000-1000-8000-00805F9B34FB");

    /**
     * UUID of the client configuration descriptor
     */
    public static final UUID CLIENT_CONFIG_DESCRIPTOR_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * UUID of battery service
     */
    public static final UUID BATTERY_SERVICE_UUID = UUID
            .fromString("0000180F-0000-1000-8000-00805f9b34fb");

    /**
     * UUID of battery level characteristic
     */
    public static final UUID BATTERY_LEVEL_UUID = UUID
            .fromString("00002a19-0000-1000-8000-00805f9b34fb");

    /**
     * UUID of device information service
     */
    public static final UUID DEVICE_INFO_SERVICE_UUID = UUID
            .fromString("0000180A-0000-1000-8000-00805f9b34fb");

    /**
     * UUID of manufacturer name characteristic
     */
    public static final UUID MANUFACTURER_NAME_UUID = UUID
            .fromString("00002A29-0000-1000-8000-00805f9b34fb");
    /**
     * UUID of model number characteristic
     */
    public static final UUID MODEL_NUMBER_UUID = UUID
            .fromString("00002A24-0000-1000-8000-00805f9b34fb");

    /**
     * UUID of system id characteristic
     */
    public static final UUID SYSTEM_ID_UUID = UUID
            .fromString("00002A23-0000-1000-8000-00805f9b34fb");

}
