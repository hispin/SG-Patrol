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
const val START_ALARM_SOUND = "start.alarm.sound"
const val STOP_ALARM_SOUND = "stop.alarm.sound"
const val CURRENT_ITEM_TOP_MENU_KEY = "currentItemTopKey"
const val READ_DATA_KEY = "handle.read.data"
const val READ_DATA_KEY_TEST = "handle.read.data.test"
const val CREATE_ALARM_KEY = "handle.create.alarm"
const val MEDIA_WORKER = "mediaWorker"
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
const val UPDATE_MEDIA = "updateMedia"

const val CURRENT_LANG_KEY_PREF = "currentLangKey"
const val CURRENT_LATITUDE_PREF = "currentLatitudePref"
const val CURRENT_LONGTUDE_PREF = "currentLongtudePref"
const val NO_DATA = "-1"


const val ALARM_FLICKERING_DURATION_KEY = "alarmFlickeringDuration"
const val ALARM_FLICKERING_DURATION_DEFAULT_VALUE_SECONDS = 3L


const val USB_DEVICES_EMPTY = "usbDevicesEmpty"
const val USB_DEVICES_NOT_EMPTY = "usbDevicesNotEmpty"

const val CREATE_ALARM_ID_KEY = "CreateAlarmIdKey"
const val CREATE_ALARM_NAME_KEY = "CreateAlarmNameKey"
const val CREATE_ALARM_TYPE_KEY = "CreateAlarmTypeKey"
const val CREATE_ALARM_TYPE_INDEX_KEY = "CreateAlarmTypeIndexKey"
const val CREATE_ALARM_IS_ARMED = "CreateAlarmIsArmedKey"
const val MAP_TYPE_KEY = "mapTypeKey"
const val SENSOR_TYPE_INDEX_KEY = "SensorTypeId"
const val SET_SENS_CAR_VALUE = "set_sens_car_value_command"
const val SET_SENS_INTRUDER_VALUE = "set_sens_intruder_value_command"
const val SET_SNR_CAR_VALUE = "set_snr_car_value_command"
const val SET_SNR_INTRUDER_VALUE = "set_snr_intruder_value_command"
const val SET_LOGIC_CAR_COUNT_SEISMIC_VALUE = "setLogicCarCountSeismicValue"
const val SET_LOGIC_CAR_DURATION_SEISMIC_VALUE = "setLogicCarDurationSeismicValue"
const val SET_LOGIC_INTRUDER_COUNT_SEISMIC_VALUE = "setLogicIntruderCountSeismicValue"
const val SET_LOGIC_INTRUDER_DURATION_SEISMIC_VALUE = "setLogicIntruderDurationSeismicValue"
const val SET_LOGIC_SUSPEND_SEISMIC_VALUE = "setLogicSuspendSeismicValue"
const val SET_MIN_POWER_SEISMIC_VALUE = "setMinPowerSeismicValue"
const val SET_MIN_POWER_VIB_VALUE = "setMinPowerVibValue"
const val SET_LOGIC_COUNT_RADAR_VALUE = "setLogicCountRadarValue"
const val SET_LOGIC_DURATION_RADAR_VALUE = "setLogicDurationRadarValue"
const val SET_LOGIC_SUSPEND_RADAR_VALUE = "setLogicSuspendRadarValue"
const val SET_LOGIC_COUNT_PIR_VALUE = "setLogicCountPirValue"
const val SET_LOGIC_DURATION_PIR_VALUE = "setLogicDurationPirValue"
const val SET_LOGIC_SUSPEND_PIR_VALUE = "setLogicSuspendPirValue"
const val SET_MIN_POWER_PIR_VALUE = "setMinPowerPirValue"
const val SET_MIN_POWER_RADAR_VALUE = "setMinPowerRadarValue"
const val SET_LOGIC_COUNT_VIB_VALUE = "setLogicCountVibValue"
const val SET_LOGIC_DURATION_VIB_VALUE = "setLogicDurationVibValue"
const val SET_LOGIC_SUSPEND_VIB_VALUE = "setLogicSuspendVibValue"



const val ERROR_RESP = "-1"

const val SHARED_PREF_FILE_NAME = "SensoGuardPref"
const val DETECTORS_LIST_KEY_PREF = "SensorsList"
const val ALARM_LIST_KEY_PREF = "AlarmsList"
const val LAST_ALARM_SENSOR_ID_PREF = "last_alarm_sensor_id_pref"
const val LAST_ALARM_TIME_PREF = "last_alarm_time_pref"
const val COUNTER_ALARM_SENSOR_ID_PREF = "counter_alarm_sensor_id_pref"
const val LAST_ATTACH_TIME_PREF = "last_attach_time_pref"

const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0
const val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
const val PERMISSIONS_REQUEST_READ_PHONE_STATE = 2

const val CURRENT_LOCATION = "currentLocation"

const val MAIN_MENU_NUM_ITEM = 4

const val ALARM_CAR = 0//"car"
const val ALARM_INTRUDER = 1//"intruder"
const val ALARM_MOTION = 2//"motion"
const val ALARM_SENSOR_OFF = 3//"sensor disconnected"
const val ALARM_KEEP_ALIVE = 4
const val ALARM_LOW_BATTERY = 5
const val ALARM_DUAL_TECH = 6

const val USB_DEVICE_CONNECT_STATUS = "usbDeviceConnect"

const val ACTION_USB_RESPONSE_CACHE = "handle.USB_RESPONSE_CACHE"
const val USB_CACHE_RESPONSE_KEY = "usb_cache_response_key"

const val NONE_VALIDATE_BITS = -1
const val SIX_SEVEN_FOTMAT_BITS = 203
const val TEN_FOTMAT_BITS = 202

//commands
const val SET_RF_ON_TIMER = 158
const val GET_SENS_LEVEL = 55
const val SET_SENS_LEVEL = 155
const val SET_TIME_SYSTEM = 103
const val SET_SNR_SYSTEM = 153
const val GET_SNR_SYSTEM = 53
const val SET_LOGIC_PARAM = 151
const val GET_LOGIC_PARAM = 51
const val SET_MIN_POWER = 156
const val GET_MIN_POWER = 56

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
const val SENSORS_TYPES = "sensorsTypes"

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
const val DISCONNECTED_INTERNET_SENSOR = "disconnected_internet_sensor"

const val NONE_AWAKE = 0
const val WAIT_AWAKE = 1
const val OK_AWAKE = 2

const val SEISMIC_TYPE = 0L
const val PIR_TYPE = 1L
const val RADAR_TYPE = 2L
const val VIBRATION_TYPE = 3L
//////////

const val DETECT_ALARM_KEY = "handle.detect.alarm"
const val ADD_ATTACHED_PHOTOS_KEY = "add.attached.photos"
const val RESULT_VALIDATION_EMAIL_ACTION = "result.validation.email"
const val ERROR_RESULT_VALIDATION_EMAIL_ACTION = "error_result.validation.email"
const val LOGIN_COMPLETE_KEY = "login.complete"


const val ALARM_DISPLAY_KEY = "alarmDisplay"
const val LOGIN_TYPE_KEY = "loginType"
const val AZURE = "azure"
const val AMAZON = "amazon"

const val ERROR_VALIDATION_EMAIL_MSG_KEY = "errorValidationEmailMessageKey"

const val REGISTER_ID_KEY = "registrationID"


const val VALIDATION_EMAIL_RESULT = "validationEmailResult"

const val TOKEN_AMAZON_KEY_PREF = "tokenAmazonKeyPref"


const val USB_CONNECTION_FAILED = "usbConnectionFailed"

const val AMAZON_PRECESS_TYPE_KEY = "AmazonProcessTypeKey"
const val AMAZON_PRECESS_DIALOG_VALUE = "AmazonProcessDialogValue"
const val AMAZON_PRECESS_WITH_USER_VALUE = "AmazonProcessWithUserValue"


const val IS_EMAIL_CONFIG_PREF_KEY = "isEmailConfigPrefKey"

const val CAMERA_KEY = "cameraKey"
const val EMAIL_ACCOUNT_KEY = "emailAccount"

const val TAGS_KEY = "tagsKey"
const val USER_INFO_AZURE_KEY = "userInfoAzure"
const val USER_INFO_AMAZON_KEY = "userInfoAmazon"

const val SORT_TYPE_KEY = "sortType"
const val SORT_BY_SYSTEM_KEY = 1
const val SORT_BY_DATETIME_KEY = 2
const val ACTION_TYPE_KEY = "actionType"
const val ACTION_PICTURE_KEY = 1
const val ACTION_VIDEO_KEY = 2

const val IMAGE_PATH_KEY = "imagePathKey"
const val IMAGE_TIME_KEY = "imageTimeKey"

const val ALARM_OTHER = "other"

const val TARGET_CAMERA_EXTRA_SETTING_REQUEST_CODE = "targetCameraExtra"
const val TAKE_PICTURE_REQUEST_CODE = 2
const val SORT_BY_SYSTEM_REQUEST_CODE = "sortBySystem"
const val SORT_PICK_DATE_TIME_REQUEST_CODE = "sortPickDateTime"
const val RESULT_CODE = "resultCode"
const val FROM_CALENDAR = "fromCalendar"
const val TO_CALENDAR = "toCalendar"
const val REQUEST_KEY = "requestKey"


const val CHANNEL_NAME = "newAlarmDetected"
const val CHANNEL_ID = "1.0"

const val IS_MYSCREENACTIVITY_FOREGROUND = "isMyScreenActivityForeground"
const val IS_LOAD_APP = "isLoadApp"
const val HUNTER_LOG = "hunterLog"

const val NO_SORTED = 0
const val DATE_SORTED = 1
const val CAMERA_SORTED = 2

const val AMAZONE_POST_LOGIN_RESULT_SUCCESS = "amazonLoginSuccess"
const val AMAZONE_POST_LOGIN_RESULT_FAILED = "amazonLoginFailed"

const val AZURA_POST_RESULT_OK = "1"
const val AZURA_POST_RESULT_UNHUTHORIZED = "-1"
const val AZURA_POST_RESULT_NO_USER = "-2"
const val AZURA_POST_RESULT_USER_NO_ACTIVE = "-3"
const val AZURA_POST_RESULT_ERROR_NO_DATA = "-99"

const val LAST_DATE_ALARM = "lastDateAlarm"

const val HOUR_OFFSET = 3

const val PWA_URL = "https://outwatch.sensoguard.com/pwa"//"https://outwatchpwa.sensoguard.com")

const val IS_SETTINGS_NOTIFICATION_LAUNCHER = "isSettingsNotificationLauncher"

6
const val TEST_CODE = "Pr6/d+q5iK3e4hWjcW31I0VQ7ylHvqDHsazj0MIhcvA="
/////////