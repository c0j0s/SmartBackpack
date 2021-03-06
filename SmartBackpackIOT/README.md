# SmartBackpackIOT
Services written in Python for Raspberry Pi to handle sensor data and communication with Android App.

## Software Architecture
![software architecture](https://github.com/c0j0s/SmartBackpack/blob/master/Documentations/2_iot_software_architecture.jpeg)

## Components of SmartBackpackIOT
The BT, Sensor and Service monitor processes will start-up automatically on boot.

__BT Service:__  
A Bluetooth service that is responsible for communicating with the Android companion app.

__Sensor Service:__  
Responsible for handling sensor readings.

__Service Monitor:__  
A monitoring service to ensure the BT and Sensor services are always online. restart services if any is found dead.

__Data storage:__  
redis is used for short-term real time sensor readings.  
holding_zone for long-term sensor readings in SQL format.

__Config.json:__  
A configuration file that controls the behaviour of BT and Sensor Server.  
e.g. debug mode, buzzer and led toggle, reading interval control.  

# SmartBackpackIOT Specifications
## Runtime Environment
Environment:  
- Python3
- Redis
	
Python packages: (install through pip3)  
- GrovePi
- Pybluez
- Redis
	
## Development Tools
The following are some of the tools used for development, feel free to use other alternative as long as it is capable of performing the development task.  

__Development cycle:__
```
code -> sync to iot device -> activate
```
__Visual studio code__ is used for coding the scripts  
__WinSCP__ is used to sync the scripts to the iot device  
__Bitvise SSH Client__ is used to send bash command to the device to activate the scripts  

Raspberry Pi login credentials
```
user: pi
password: root
```
__Getting network IP of iot device for ssh/sFtp connection:__  

__locally for initial setup__  
The device is connected to the school WIFI automatically on boot, the credentials used is school WIFI login credentials.  

To change the WIFI/update WIFI credentials, connect the device to an external monitor, keyboard and mouse, use either GUI(enter startx to switch to GUI mode if you boot into command line mode) or bash command to change the WIFI configs.

Now you can retrieve the IP address of the device using ifconfig command.

__remotely__  
Once the WIFI connection is ready, you can retrieve the address using the Bluetooth. To do so please download a Bluetooth terminal app in your mobile phone, here __Serial Bluetooth__ is used for Android.

1. connect the iot device
2. send following command as a string:
```
{"function_code":"43000","data":"","end_code":"EOT"}
```
3. the terminal should output the response with IP address in the data body

## Start Services Manually
1. Change to service folder directory
```sh
$ cd /home/pi/SmartbackpackIOT
```
2. Execute following command to start service scripts 
```sh
#for production
$ sudo service SBP_Sensor_Server start
$ sudo service SBP_BT_Server start
$ sudo service SBP_Service_Monitor start

#for debugging
$ Python3 SBP_Sensor_Server.py
$ sudo Python3 SBP_BT_Server.py
$ Python3 SBP_Service_Monitor.py
```

Both method works however starting service method will not output any print log.  
Instead print logs will be recorded in dedicated log file under /log folder

### About service auto start-up on boot
Start-up script is stored under utils folder, a symbolic link was created in the linux /lib/systemd/system/<Service name>, therefore you can directly modify the scripts in the utils folder.

Please refer to Method 4 - SYSTEMD:  
https://www.dexterindustries.com/howto/run-a-program-on-your-raspberry-pi-at-startup/

### Regarding the clock of iot device
As raspberry pi does not keep track of time when it is powered off, the clock will have to be manually set to the correct timing.
If possible, network time can be used to sync the clock whenever it is online, but little time is invested to solve this issue.

__Update clock manually__  
```ssh
$ sudo date -s "01 JAN 2019 00:00:00"
```
This will be needed if you need the iot to record sensor data with correct timing

## Pairing mobile with IOT device
The Mobile app will connect with the backpack automatically, however, for first time usage, you have to pair it manually in the settings app of your mobile phone.

If your mobile phone could not detect the iot device, please check if the iot device Bluetooth discoverable is on, otherwise you can use bluetoothctl to pair the devices manually.
```sh
$ sudo bluetoothctl
$ pair <phone Bluetooth address>
```
refer to official bluetoothctl documentations for all the commands

## Config.json Specifications
The following field are values that will affect how the services run, other fields not stated does not impact the services in anyway.  
```json
{
    "device":{
        "below contains the digital port number of grovepi connected with the sensor"
        "sensors":{
            "particle": "/dev/ttyAMA0",
            "temp_hum": 8,
            "button": 5
        },
        "actuators":{
            "led_green": 4, 
            "led_blue": 3,
            "led_red": 2,
            "buzzer": 5
        }
    },
    "env":{
        "redis":{
            "host":"localhost",
            "port":"6379",
            "db":"0"
        },
    },
    "settings":{
        "debug":true,
        "Sensor_Server_Settings":{
            "CONFIG_ENABLE_BUZZER":0,
            "CONFIG_ENABLE_LED":1,
            "MINUTES_TO_RECORD_DATA":0.1,
            "SECONDS_TO_UPDATE_DATA":5
        }
    }
}
```

## Bluetooth Commands
### Bluetooth communication command syntax
This Bluetooth communication syntax is develop to handle various propose features for interacting with companion apps.  
As the serial communication transmits with raw strings, complex commands would not be possible without a standard protocol shared by both components, as such the following protocol was developed.

The entire transmission string in __JSON__ object format consist of 3 main parts:  
```JSON
{
    "function_code":"",
    "data":"",
    "end_code":"",
    "debug":"debug info [optional]"
}
```
__Function code__  
The first item is reserved for function identifier code
```JSON
{
    "function_code":"00001",
}
```
#### IOT Device Function Codes:  
| Function Code  | Description                        | Enum      |
|:-------------- |:-----------------------------------|:--------- |
| 00000 | terminate Bluetooth connections             | DISCONNECT |
| 10000 | restart device                              | REBOOT_DEVICE |
| 10500 | shutdown device                             |  |
| 11000 | restart sensor server                       | RESTART_SENSOR_SERVICE|
| 11500 | get the status of sensor server             | GET_SENSOR_STATUS|
| 12000 | restart Bluetooth server [NOT IMPLEMENTED]  | RESTART_BLUETOOTH_SERVICE|
| 12500 | get the status of Bluetooth server          | GET_BLUETOOTH_STATUS|
| 30000 | get real time sensor reading                | GET_SENSOR_DATA|
| 31000 | set user preferences                        | CHANGE_DEVICE_SETTINGS|
| 32000 | get holding_zone data                       | SYNC_HOLDING_ZONE |
| 32500 | flush holding_zone                          | FLUSH_HOLDING_ZONE |
| 41000 | toggle server debug mode                    | TOOGLE_DEBUG |
| 42000 | execute custom shell commands               | EXE_SH |
| 43000 | get network IP address                      | GET_NETWORK_IP |
| 44000 | activate the buzzer for 1 second            | BUZZER_TEST |

__Data__  
The second item is reserved for data body  
```JSON
{
    "data":{},
}
```

Holding zone synchronisation syntax:  
```JSON
{
    "data":{
        "RECORDED_ON":"HUMIDITY;TEMPERATURE;PM2_5;PM10;PREDICTED_COMFORT_LEVEL;ALERT_TRIGGERED",
    },
}
```

__End Status Code__  
The third item is reserved for transmission ending status, the end status code will indicate the mobile app when is the transmission of data completed and therefore continue with other tasks.   
```JSON
{
    "end_code":"EOT"
}
```

#### Proposed status codes:  
| Status Code | Description                    |
| ----|----------------------------------------|
| EOT | end of transmission |
| MSE | maintain session, more data transmitting |
| ERR | error occurred, transmission terminated and services schedule for reboot |

# Project Folder Structure
### Main Service Scripts:  
- SBP_BT_Server.py  
- SBP_Sensor_Server.py  
- SBP_Service_Monitor.py  

### Data and configuration storage:  
- config.json  
- holding_zone/

### Project Structures
/lib  
contains sensor libraries and wrapper classes for front-end development.

- Common  
Contains static functions that is commonly used in various places

- HPMA115S0  
Object for interacting with air particle sensor
- SBP_BT_Command_Manager  
Wrapper class for handling complex Bluetooth communication syntax

- SBP_Buzzer  
Object class for controlling buzzer

- SBP_Display  
Object class for controlling with the lcd display

- SBP_LED_Controller  
Wrapper class for controlling a cluster of led groups

- SBP_LED  
Object for controlling individual leds

- SBP_Redis_Wrapper  
Wrapper class for interacting with redis


/log  
contains empty file for logging, require setup in `/etc/rsyslog.d/rsyslog.conf`.

/src  
contains reference source for configuration.

/utils  
contains scripts to setup the runtime environment.

/unitTesting  
for testing purposes.

/holding_zone  
for holding zone files

# Backpack Embedding
__Items:__
- Iot Container
- Power Bank
- Supporting Structures(cardboards)

![mounting_guide](https://github.com/c0j0s/SmartBackpack/blob/master/Documentations/Others/EmbeddingGuide/0_instruction_overview.png)

__Attaching:__
1. first align the leds, temperature and humidity sensor with the cut outs, push gently to expose the leds.
2. connect the air particle sensor pins with the external pins from the iot container, use the flaps for guidance when connecting the pins.
3. align the container with the velcro pads in the backpack and attach it firmly.
4. [optional] insert the bottom and side supporting structures to support the containers weight
5. attach power bank to the usb power supply cable and the device should automatically start.

__Detaching:__
1. disconnect the power supply and air quality sensor pins
2. [optional] remove the bottom and side supporting structures
3. push led into the backpacks to avoid damaging the led pins
4. detach the velcros and pull the container out

# Troubleshooting 
- __If the air particle sensor is not running__  
Ensure the pins are connected correctly and firmly with the iot container external pin connectors or GPIO pins.

| air particle sensor | Raspberry pi gpio |
| ------------------- | ----------------- |
| pin 2 | pin 4  (5V power) |
| pin 6 | pin 8  (UART_TXD) |
| pin 7 | pin 10 (UART_RXD) |
| pin 8 | pin 6  (Ground)   |

- __If the sensor service lit up for initial testing and did not lit up for normal runtime__  
Ensure sensors are connected correctly, the led will not lit if the air quality, temperature & humidity sensor does not provide values to the sensor service.

- __If the sensors are connected, the sensor service did not lit up__
Ensure if the led setting in device config is turn on, otherwise restart the service manually using command line.