package com.sensoguard.detectsensor.global

const val CHECK_AVAILABLE_KEY = "find.drivers.command"
const val STOP_READ_DATA_KEY = "stop.data.command"
const val DISCONNECT_USB_PROCESS_KEY = "disconnect.usb.process"

const val HANDLE_READ_DATA_EXCEPTION = "handle.read.data.exception"
const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
const val ACTION_TOGGLE_TEST_MODE = "toggleTestMode"
const val ACTION_SEND_CMD = "action.send.cmd"
const val ACTION_INTERVAL = "action.interval"
const val ACTION_TIME_OUT_MAX = "action.time.out.max"


const val GET_CURRENT_LOCATION_KEY = "handle.get.current.location"
const val GET_CURRENT_SINGLE_LOCATION_KEY = "handle.get.current.single.location"
const val STOP_ALARM_SOUND = "stop.alarm.sound"
const val CURRENT_ITEM_TOP_MENU_KEY = "currentItemTopKey"
const val READ_DATA_KEY = "handle.read.data"
const val READ_DATA_KEY_TEST = "handle.read.data.test"
const val CREATE_ALARM_KEY = "handle.create.alarm"
const val CREATE_ALARM_NOT_DEFINED_KEY = "handle.create.alarm.not.defined"
const val HANDLE_ALARM_KEY = "handle.alarm"
const val RESET_MARKERS_KEY = "resetMarkersKey"
const val IS_VIBRATE_WHEN_ALARM_KEY = "isVibrateWhenAlarm"
const val MAP_SHOW_VIEW_TYPE_KEY = "mapShowViewType"
const val MAP_SHOW_NORMAL_VALUE = 0
const val MAP_SHOW_SATELLITE_VALUE = 1
const val SELECTED_NOTIFICATION_SOUND_KEY = "selectedNotificationSoundKey"
const val IS_NOTIFICATION_SOUND_KEY = "isNotificationSoundKey"
const val IS_SENSOR_NAME_ALWAYS_KEY = "isSensorNameAlwaysKey"
const val ACTIVATION_CODE_KEY = "activationCodeKey"
const val IMEI_KEY = "imeiKey"

const val CURRENT_LANG_KEY_PREF = "currentLangKey"
const val CURRENT_LATITUDE_PREF = "currentLatitudePref"
const val CURRENT_LONGTUDE_PREF = "currentLongtudePref"
const val NO_DATA = "-1"


const val ALARM_FLICKERING_DURATION_KEY = "alarmFlickeringDuration"
const val ALARM_FLICKERING_DURATION_DEFAULT_VALUE_SECONDS = 30L


const val USB_CONNECTION_OFF_UI = "usbConnectionFailed"
const val USB_CONNECTION_ON_UI = "usbConnectionSuccess"

const val CREATE_ALARM_ID_KEY = "CreateAlarmIdKey"
const val CREATE_ALARM_NAME_KEY = "CreateAlarmNameKey"
const val CREATE_ALARM_TYPE_KEY = "CreateAlarmTypeKey"
const val CREATE_ALARM_TYPE_INDEX_KEY = "CreateAlarmTypeIndexKey"
const val CREATE_ALARM_IS_ARMED = "CreateAlarmIsArmedKey"
const val MAP_TYPE_KEY = "mapTypeKey"


const val ERROR_RESP = "-1"

const val SHARED_PREF_FILE_NAME = "SensoGuardPref"
const val DETECTORS_LIST_KEY_PREF = "SensorsList"
const val ALARM_LIST_KEY_PREF = "AlarmsList"

const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0
const val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
const val PERMISSIONS_REQUEST_READ_PHONE_STATE = 2

const val CURRENT_LOCATION = "currentLocation"

const val MAIN_MENU_NUM_ITEM = 4

const val ALARM_CAR = 0//"car"
const val ALARM_INTRUDER = 1//"intruder"
const val ALARM_SENSOR_OFF = 3//"sensor disconnected"
const val ALARM_LOW_BATTERY = "motion"

const val USB_DEVICE_CONNECT_STATUS = "usbDeviceConnect"

const val ACTION_USB_RESPONSE_CACHE = "handle.USB_RESPONSE_CACHE"
const val USB_CACHE_RESPONSE_KEY = "usb_cache_response_key"

const val NONE_VALIDATE_BITS = -1
const val SIX_FOTMAT_BITS = 203
const val TEN_FOTMAT_BITS = 202

//commands
const val SET_RF_ON_TIMER = 158
const val GET_SENS_LEVEL = 55
const val SET_SENS_LEVEL = 155

const val GET_SENS_LEVEL_RESPONSE = "getSensLevelResponse"

const val TABLAYOUT_HEIGHT_DEFAULT = 72

const val USER_NAME_MAIL = "userNameMail1"
const val PASSWORD_MAIL = "passwordMail"
const val SERVER_MAIL = "serverMail"
const val PORT_MAIL = "portMail"
const val RECIPIENT_MAIL = "recipientail"
const val IS_SSL_MAIL = "isSSLMail"
const val IS_FORWARD_ALARM_EMAIL = "isForwardAlarmEmail"

//key to deliver ids of sensors to command dialog
const val SENSORS_IDS = "sensorsIds"

const val CURRENT_COMMAND = "currentCommand"

const val NORMAL_STATE = 0
const val PROCESS_STATE = 1
const val TIMEOUT_STATE = 2
const val SUCCESS_STATE = 3

const val TIMER_VALUE = "timerValue"

//when the timer stop
const val MAX_TIMEOUT = "maxTimeout"
const val STOP_TIMER = "stopTimer"
const val STOP_GENERAL_TIMER = "stopGeneralTimer"

//check usb sw connection key
const val CHECK_USB_CONN_SW = "checkUsbConnSw"

const val MAX_TIMER_RESPONSE = "max_timer_response"
const val COMMAND_TYPE = "commandType"
const val IS_REPEATED = "isRepeated"

const val NONE_AWAKE = 0
const val WAIT_AWAKE = 1
const val OK_AWAKE = 2