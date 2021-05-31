package com.sensoguard.detectsensor.fragments

import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.get
import com.mapbox.mapboxsdk.style.layers.Property.*
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.adapters.SensorsDialogAdapter
import com.sensoguard.detectsensor.classes.AlarmSensor
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.controler.ViewModelListener
import com.sensoguard.detectsensor.global.*
import com.sensoguard.detectsensor.interfaces.OnAdapterListener
import com.sensoguard.detectsensor.services.ServiceFindLocation
import com.sensoguard.detectsensor.services.ServiceFindSingleLocation
import kotlinx.android.synthetic.main.fragment_map_detects.*
import kotlinx.android.synthetic.main.on_off_connect_device.*
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MapmobFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapmobFragment : ParentFragment(), OnAdapterListener, MapboxMap.OnMoveListener {
    private var popup: PopupWindow? = null
    private var currentLocationMarker: Feature? = null
    private var markersList: ArrayList<Feature>? = null
    private var symbolOption: SymbolOptions? = null
    private var markerViewManager: MarkerViewManager? = null

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var mapView: MapView? = null
    private var mapType = Style.OUTDOORS
    private var myLocate: LatLng? = null
    private var loadedMapStyle: Style? = null

    private var fbRefresh: FloatingActionButton? = null
    private var fbTest: FloatingActionButton? = null

    //private var btnDownloadMaps:AppCompatButton?=null

    private var myMapboxMap: MapboxMap? = null
    private var myOfflineRegion: OfflineRegion? = null

    private var currentLongitude: Double? = null
    private var currentLatitude: Double? = null
    private var mCenterLatLong: LatLng? = null

    val TAG = "MapmobFragment"

    private var dialog: Dialog? = null
    var sensorsDialogAdapter: SensorsDialogAdapter? = null

    private val SOURCE_ID = "SOURCE_ID"
    private val CURRENT_LOC_SOURCE = "current_loc_source"
    private val LAYER_ID = "LAYER_ID"

    private val ICON_PROPERTY: String = "ICON_PROPERTY"
    private val BLUE_ICON_ID = "BLUE_ICON_ID"
    private val GREEN_ICON_ID = "GREEN_ICON_ID"
    private val GRAY_ICON_ID = "GRAY_ICON_ID"
    private val RED_ICON_ID = "RED_ICON_ID"
    private val CAR_ICON_ID = "CAR_ICON_ID"
    private val INTRUDER_ICON_ID = "INTRUDER_ICON_ID"
    private val SENSOR_OFF_ICON_ID = "SENSOR_OFF_ICON_ID"
    private val PIR_ICON_ID = "PIR_ICON_ID"
    private val RADAR_ICON_ID = "RADAR_ICON_ID"
    private val VIBRATION_ICON_ID = "VIBRATION_ICON_ID"

    private var locationManager: LocationManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        startTimerListener()
    }

    //start listener to timer
    private fun startTimerListener() {
        activity?.let {
            ViewModelProviders.of(it).get(ViewModelListener::class.java)
                .startCurrentCalendarListener()?.observe(
                    this,
                    { calendar ->

                        //Log.d("testTimer","tick in MapSensorsFragment")
                        //if there is no alarm in process then shut down the timer
                        if (UserSession.instance.alarmSensors == null || UserSession.instance.alarmSensors?.isEmpty()!!) {
                            activity?.let { act ->
                                ViewModelProviders.of(act).get(ViewModelListener::class.java)
                                    .shutDownTimer()
                            }
                            //showMarkers()
                        }
//                        else {
//                            //remove all the time out sensors alarm and show them with regular sensor marker
//                            //replaceSensorAlarmTimeOutToSensorMarker()
//                            showMarkers()
//                        }
                        //if the
                        showMarkers()

                    })

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Mapbox.getInstance(requireActivity(), getString(R.string.mapbox_access_token))
        val view = inflater.inflate(R.layout.fragment_mapmob, container, false)

        mapView = view.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)


        fbRefresh = view.findViewById(R.id.fbRefresh1)
        fbRefresh?.setOnClickListener {
            gotoMySingleLocation()
        }

        fbTest = view.findViewById(R.id.fbTest1)
        fbTest?.setOnClickListener {
            showTestEventDialog()
        }


        initMapType()

        return view
    }

    //get last location
    private fun initFindLocation(): Location? {
        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager


        if (context?.let {
                ContextCompat.checkSelfPermission(
                    it,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            } == PackageManager.PERMISSION_GRANTED
        ) {

            return locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }


        return null
    }

    // move the camera to ic_mark location
    private fun showLocation(location: Location?) {

        if (location != null) {
            setMyLocate(
                LatLng(
                    location.latitude,
                    location.longitude
                )
            )
        } else {

            myLocate = getLastLocationLocally()

            if (myLocate == null) {
                //set default location (london)
                myLocate = LatLng(51.509865, -0.118092)
                //set default location (london) if there is no last location
                setMyLocate(LatLng(51.509865, -0.118092))
            }
        }
        //add marker at the focus of the map
        myLocate?.let {
            //load the camera
            if (myLocate != null && myLocate?.latitude != null &&
                myLocate?.longitude != null
            ) {

                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(myLocate?.latitude!!, myLocate?.longitude!!))
                    .zoom(15.0)
                    .tilt(20.0)
                    .build()


                // Move camera to new position
                myMapboxMap!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

                showMarkers()

            }

        }

    }

    //show all markers
    fun showMarkers() {

        //clear the markers
        markersList = ArrayList<Feature>()

        //show current location marker
        showCurrentLocationMarker()

        //get sensors from locally
        val sensorsArr = activity?.let { getSensorsFromLocally(it) }

        //for
        val iteratorList = sensorsArr?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if (sensorItem.getLatitude() != null
                && sensorItem.getLongtitude() != null
            ) {

                val sensorAlarm = getSensorAlarmBySensor(sensorItem)

                if (sensorAlarm != null) {

                    //if time out then remove the sensor from alarm list
                    if (isSensorAlarmTimeout(sensorAlarm)) {
                        UserSession.instance.alarmSensors?.remove(sensorAlarm)
                        showSensorMarker(sensorItem)
                    } else {
                        //save the marker for update after timeout
                        sensorAlarm.markerFeature = showSensorAlarmMarker(
                            sensorItem,
                            sensorAlarm.type,
                            sensorAlarm.typeIdx
                        )
                    }

                } else {
                    //show sensor marker
                    showSensorMarker(sensorItem)
                }

            }
        }
    }


    //get current location from gps
    private fun gotoMyLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.startForegroundService(Intent(context, ServiceFindLocation::class.java))
        } else {
            activity?.startService(Intent(context, ServiceFindLocation::class.java))
        }
    }

    //get current location from gps
    private fun gotoMySingleLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.startForegroundService(Intent(context, ServiceFindSingleLocation::class.java))
        } else {
            activity?.startService(Intent(context, ServiceFindSingleLocation::class.java))
        }
    }

    //for test :enter manually alarm
    private fun showTestEventDialog() {

        //create dialog
        val dialog = MapDetectsFragment@ this.context?.let { Dialog(it) }
        //set layout custom
        dialog?.setContentView(R.layout.test_dialog)

        val etid = dialog?.findViewById<EditText>(R.id.etId)
        val etType = dialog?.findViewById<EditText>(R.id.etType)
        val btnOk = dialog?.findViewById<Button>(R.id.btnOk)

        //Bug fixed:Fatal Exception: java.lang.NumberFormatException
        //Invalid int: "1 "
        btnOk?.setOnClickListener {


            if (etid != null && validIsEmpty(etid)
                && etType != null && validIsEmpty(etType)
            ) {
                val arr = ArrayList<Int>()
                arr.add(0, 2)
                arr.add(1, etid.text.toString().toInt())
                arr.add(2, 202)
                arr.add(3, 10)
                arr.add(4, 0)
                arr.add(5, etType.text.toString().toInt())
                arr.add(6, 0)
                arr.add(7, 0)
                arr.add(8, 0)
                arr.add(9, 3)
                val inn = Intent(READ_DATA_KEY_TEST)
                inn.putExtra("data", arr)
                context?.sendBroadcast(Intent(inn))
                dialog.dismiss()
            }
        }

        dialog?.show()
    }

    private fun validIsEmpty(editText: EditText): Boolean {
        var isValid = true

        if (editText.text.isNullOrBlank()) {
            editText.error =
                resources.getString(com.sensoguard.detectsensor.R.string.empty_field_error)
            isValid = false
        }

        return isValid
    }

    //show marker of sensor
    private fun showSensorMarker(sensorItem: Sensor) {

        if (mapView == null || sensorItem == null) {
            return
        }

        val loc = LatLng(
            sensorItem.getLatitude()!!,
            sensorItem.getLongtitude()!!
        )


        if (sensorItem.isArmed()) {

            addMarker(
                loc,
                GREEN_ICON_ID,
                sensorItem.getName(),
                sensorItem.getType()
            )

        } else {
            addMarker(
                loc,
                GRAY_ICON_ID,
                sensorItem.getName(),
                sensorItem.getType()
            )

        }
    }

    //show marker of sensor alarm
    private fun showSensorAlarmMarker(sensorItem: Sensor, type: String, typeIdx: Int?): Feature? {

        if (mapView == null) {
            return null
        }

        val loc: LatLng? =
            LatLng(
                sensorItem.getLatitude()!!,
                sensorItem.getLongtitude()!!
            )

        var alarmTypeIcon: Feature? = null

        //car ,intruder and off are relevant when type = seismic
        if (sensorItem.getTypeID() == SEISMIC_TYPE) {
            //set icon according to type alarm
            alarmTypeIcon =
                when (typeIdx) {
                    ALARM_CAR -> {
                        loc?.let { addMarker(it, CAR_ICON_ID, sensorItem.getName(), type) }
                    }
                    ALARM_INTRUDER -> {
                        loc?.let { addMarker(it, INTRUDER_ICON_ID, sensorItem.getName(), type) }
                    }
                    ALARM_SENSOR_OFF -> {
                        loc?.let { addMarker(it, SENSOR_OFF_ICON_ID, sensorItem.getName(), type) }
                    }
                    //ALARM_LOW_BATTERY->context?.let { con -> convertBitmapToBitmapDiscriptor(con,R.drawable.ic_alarm_low_battery)}
                    else -> {
                        loc?.let { addMarker(it, RED_ICON_ID, sensorItem.getName(), type) }
                    }
                }
        } else {
            alarmTypeIcon =
                when (sensorItem.getTypeID()) {
                    PIR_TYPE -> loc?.let {
                        addMarker(
                            it,
                            PIR_ICON_ID,
                            sensorItem.getName(),
                            sensorItem.getType()
                        )
                    }
                    RADAR_TYPE -> loc?.let {
                        addMarker(
                            it,
                            RADAR_ICON_ID,
                            sensorItem.getName(),
                            sensorItem.getType()
                        )
                    }
                    VIBRATION_TYPE -> loc?.let {
                        addMarker(
                            it,
                            VIBRATION_ICON_ID,
                            sensorItem.getName(),
                            sensorItem.getType()
                        )
                    }
                    else -> {
                        loc?.let {
                            addMarker(
                                it,
                                RED_ICON_ID,
                                sensorItem.getName(),
                                sensorItem.getType()
                            )
                        }
                    }
                }
        }


        return alarmTypeIcon
    }

    //check if the alarm sensor is in duration
    private fun isSensorAlarmTimeout(alarmProcess: AlarmSensor?): Boolean {

        val timeout = getLongInPreference(
            activity,
            ALARM_FLICKERING_DURATION_KEY,
            ALARM_FLICKERING_DURATION_DEFAULT_VALUE_SECONDS
        )
        val futureTimeout = timeout?.let { alarmProcess?.alarmTime?.timeInMillis?.plus(it * 1000) }

        if (futureTimeout != null) {
            val calendar = Calendar.getInstance()
            return when {
                futureTimeout < calendar.timeInMillis -> true
                else -> false
            }
        }
        return true
    }

    //remove alarm sensor if exist
    private fun removeSensorAlarmById(alarmId: String) {

        val iteratorList = UserSession.instance.alarmSensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if (sensorItem.alarmSensorId == alarmId) {
                iteratorList.remove()
            }
        }
    }

    //check if the sensor is in alarm process
    private fun getSensorAlarmBySensor(sensor: Sensor): AlarmSensor? {

        val iteratorList = UserSession.instance.alarmSensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if (sensorItem.alarmSensorId == sensor.getId()) {
                return sensorItem
            }
        }
        return null
    }

    var mySymbolCurrLocation: Symbol? = null

    //show marker of current location
    private fun showCurrentLocationMarker() {

        if (mapView == null) {
            return
        }

        if (myLocate == null) {
            return
        }


        if (myLocate != null) {
            currentLocationMarker = addMarker(
                myLocate!!,
                BLUE_ICON_ID,
                "myLocate",
                ""
            )
        }

    }

    //refresh markers
    private fun refreshMarkers() {

        if (markersList != null && markersList?.size!! > 0) {
            myMapboxMap?.setStyle(Style.Builder()
                .fromUri(mapType)

                // Add the SymbolLayer icon image to the map style
                .withImage(
                    GREEN_ICON_ID, BitmapFactory.decodeResource(
                        requireActivity().resources, R.drawable.ic_sensor_item
                    )
                )
                .withImage(
                    BLUE_ICON_ID, BitmapFactory.decodeResource(
                        requireActivity().resources, R.drawable.ic_my_locate
                    )
                )
                .withImage(
                    GRAY_ICON_ID, BitmapFactory.decodeResource(
                        requireActivity().resources, R.drawable.ic_sensor_item_disable
                    )
                )
                .withImage(
                    CAR_ICON_ID, BitmapFactory.decodeResource(
                        requireActivity().resources, R.drawable.ic_alarm_car
                    )
                )
                .withImage(
                    INTRUDER_ICON_ID, BitmapFactory.decodeResource(
                        requireActivity().resources, R.drawable.ic_alarm_intruder
                    )
                )
                .withImage(
                    SENSOR_OFF_ICON_ID, BitmapFactory.decodeResource(
                        requireActivity().resources, R.drawable.ic_alarm_sensor_off
                    )
                )
                .withImage(
                    PIR_ICON_ID, BitmapFactory.decodeResource(
                        requireActivity().resources, R.drawable.ic_pir
                    )
                )
                .withImage(
                    RADAR_ICON_ID, BitmapFactory.decodeResource(
                        requireActivity().resources, R.drawable.ic_radar
                    )
                )
                .withImage(
                    VIBRATION_ICON_ID, BitmapFactory.decodeResource(
                        requireActivity().resources, R.drawable.ic_vibration
                    )
                )
                .withImage(
                    RED_ICON_ID, BitmapFactory.decodeResource(
                        requireActivity().resources, R.drawable.ic_sensor_alarm
                    )
                )

                // Adding a GeoJson source for the SymbolLayer icons.
                .withSource(
                    GeoJsonSource(
                        SOURCE_ID,
                        FeatureCollection.fromFeatures(markersList!!)
                    )
                )

                // Adding the actual SymbolLayer to the map style. An offset is added that the bottom of the red
                // marker icon gets fixed to the coordinate, rather than the middle of the icon being fixed to
                // the coordinate point. This is offset is not always needed and is dependent on the image
                // that you use for the SymbolLayer icon.
                .withLayer(
                    SymbolLayer(LAYER_ID, SOURCE_ID)
                        .withProperties(
                            iconImage(
                                Expression.match(
                                    Expression.get(ICON_PROPERTY),
                                    Expression.literal(GREEN_ICON_ID),
                                    Expression.stop(GRAY_ICON_ID, GRAY_ICON_ID),
                                    Expression.stop(BLUE_ICON_ID, BLUE_ICON_ID),
                                    Expression.stop(GREEN_ICON_ID, GREEN_ICON_ID),
                                    Expression.stop(CAR_ICON_ID, CAR_ICON_ID),
                                    Expression.stop(INTRUDER_ICON_ID, INTRUDER_ICON_ID),
                                    Expression.stop(SENSOR_OFF_ICON_ID, SENSOR_OFF_ICON_ID),
                                    Expression.stop(PIR_ICON_ID, PIR_ICON_ID),
                                    Expression.stop(RADAR_ICON_ID, RADAR_ICON_ID),
                                    Expression.stop(VIBRATION_ICON_ID, VIBRATION_ICON_ID),
                                    Expression.stop(RED_ICON_ID, RED_ICON_ID)
                                )
                            ),
                            iconAllowOverlap(true),
                            iconIgnorePlacement(true)
                        )
                ), Style.OnStyleLoaded {
            })
        }//end checking the array
    }


    //add marker
    private fun addMarker(
        location: LatLng,
        iconId: String,
        cameraName: String?,
        type: String?
    ): Feature? {

        val feature = Feature.fromGeometry(
            Point.fromLngLat(location.longitude, location.latitude)
        )


        val isSensorAlwaysShow = getBooleanInPreference(activity, IS_SENSOR_NAME_ALWAYS_KEY, false)
        if (!cameraName.equals("myLocate") && isSensorAlwaysShow) {
            feature.addStringProperty(PROPERTY_NAME, cameraName)
        }
        feature.addStringProperty(PROPERTY_NAME_WIN, cameraName)
        feature.addStringProperty(PROPERTY_SENSOR_TYPE, type)

        feature.addStringProperty(ICON_PROPERTY, iconId)

        markersList?.add(
            feature
        )

        if (markersList != null && markersList?.size!! > 0) {
            myMapboxMap?.setStyle(
                Style.Builder()
                    .fromUri(mapType)//"mapbox://styles/mapbox/cjf4m44iw0uza2spb3q0a7s41")

                    // Add the SymbolLayer icon image to the map style
                    .withImage(
                        GREEN_ICON_ID, BitmapFactory.decodeResource(
                            requireActivity().resources, R.drawable.ic_sensor_item
                        )
                    )
                    .withImage(
                        BLUE_ICON_ID, BitmapFactory.decodeResource(
                            requireActivity().resources, R.drawable.ic_my_locate
                        )
                    )
                    .withImage(
                        GRAY_ICON_ID, BitmapFactory.decodeResource(
                            requireActivity().resources, R.drawable.ic_sensor_item_disable
                        )
                    )
                    .withImage(
                        CAR_ICON_ID, BitmapFactory.decodeResource(
                            requireActivity().resources, R.drawable.ic_alarm_car
                        )
                    )
                    .withImage(
                        INTRUDER_ICON_ID, BitmapFactory.decodeResource(
                            requireActivity().resources, R.drawable.ic_alarm_intruder
                        )
                    )
                    .withImage(
                        SENSOR_OFF_ICON_ID, BitmapFactory.decodeResource(
                            requireActivity().resources, R.drawable.ic_alarm_sensor_off
                        )
                    )
                    .withImage(
                        PIR_ICON_ID, BitmapFactory.decodeResource(
                            requireActivity().resources, R.drawable.ic_pir
                        )
                    )
                    .withImage(
                        RADAR_ICON_ID, BitmapFactory.decodeResource(
                            requireActivity().resources, R.drawable.ic_radar
                        )
                    )
                    .withImage(
                        VIBRATION_ICON_ID, BitmapFactory.decodeResource(
                            requireActivity().resources, R.drawable.ic_vibration
                        )
                    )
                    .withImage(
                        RED_ICON_ID, BitmapFactory.decodeResource(
                            requireActivity().resources, R.drawable.ic_sensor_alarm
                        )
                    )

                    // Adding a GeoJson source for the SymbolLayer icons.
                    .withSource(
                        GeoJsonSource(
                            SOURCE_ID,
                            FeatureCollection.fromFeatures(markersList!!)
                        )
                    )

// Adding the actual SymbolLayer to the map style. An offset is added that the bottom of the red
// marker icon gets fixed to the coordinate, rather than the middle of the icon being fixed to
// the coordinate point. This is offset is not always needed and is dependent on the image
// that you use for the SymbolLayer icon.
                    .withLayer(
                        SymbolLayer(LAYER_ID, SOURCE_ID)
                            .withProperties(
                                iconImage(
                                    Expression.match(
                                        Expression.get(ICON_PROPERTY),
                                        Expression.literal(GREEN_ICON_ID),
                                        Expression.stop(GRAY_ICON_ID, GRAY_ICON_ID),
                                        Expression.stop(BLUE_ICON_ID, BLUE_ICON_ID),
                                        Expression.stop(GREEN_ICON_ID, GREEN_ICON_ID),
                                        Expression.stop(CAR_ICON_ID, CAR_ICON_ID),
                                        Expression.stop(INTRUDER_ICON_ID, INTRUDER_ICON_ID),
                                        Expression.stop(SENSOR_OFF_ICON_ID, SENSOR_OFF_ICON_ID),
                                        Expression.stop(PIR_ICON_ID, PIR_ICON_ID),
                                        Expression.stop(RADAR_ICON_ID, RADAR_ICON_ID),
                                        Expression.stop(VIBRATION_ICON_ID, VIBRATION_ICON_ID),
                                        Expression.stop(RED_ICON_ID, RED_ICON_ID)
                                    )
                                ),
                                iconAllowOverlap(true),
                                iconIgnorePlacement(true),
                                textOffset(FloatArray(2) { 0f;-2.5f }.toTypedArray()),
                                textIgnorePlacement(true),
                                textAllowOverlap(true),
                                textHaloColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.white
                                    )
                                ),
                                textHaloWidth(2f),
                                textVariableAnchor(Array(4) { TEXT_ANCHOR_TOP }),//; TEXT_ANCHOR_BOTTOM; TEXT_ANCHOR_LEFT; TEXT_ANCHOR_RIGHT}),
                                //textJustify(TEXT_JUSTIFY_AUTO),
                                textField(Expression.concat(get(PROPERTY_NAME)))
                            )
                    ), Style.OnStyleLoaded {
                })
        }//end checking the array
        return feature
    }


    //configureActivation map type
    private fun initMapType() {
        val _mapType = getIntInPreference(activity, MAP_SHOW_VIEW_TYPE_KEY, -1)
        //Log.d("testMapView", "_mapType:$_mapType")
        if (_mapType == MAP_SHOW_NORMAL_VALUE) {
            mapType = Style.OUTDOORS
        } else if (_mapType == MAP_SHOW_SATELLITE_VALUE) {
            mapType = Style.SATELLITE
        }

    }

    private fun setMyLocate(myLocate: LatLng) {
        this.myLocate = myLocate
    }

    //get last location from shared preference
    private fun getLastLocationLocally(): LatLng? {
        val latitude = getStringInPreference(activity, CURRENT_LATITUDE_PREF, "-1")
        val longtude = getStringInPreference(activity, CURRENT_LONGTUDE_PREF, "-1")
        var lat: Double? = null
        var lon: Double? = null

        if (!latitude.equals("-1") && !longtude.equals("-1")) {
            try {
                lat = latitude?.toDouble()
                lon = longtude?.toDouble()

            } catch (ex: NumberFormatException) {
            }
        }
        if (lat != null && lon != null) {
            return LatLng(lat, lon)
        }
        return null
    }

    //remove all the time out sensors alarm and show them with regular sensor marker
    private fun replaceSensorAlarmTimeOutToSensorMarker() {
        val iteratorList = UserSession.instance.alarmSensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if (isSensorAlarmTimeout(sensorItem)) {
                //show regular sensor marker
                sensorItem.markerFeature?.let {
                    showSensorMarker(
                        it,
                        sensorItem.isSensorArmed
                    )
                }
                //remove the sensor alarm because it timeout
                iteratorList.remove()
            }
        }
    }

    //show marker of sensor
    private fun showSensorMarker(
        markerFeature: Feature,
        isSensorArmed: Boolean

    ) {
        if (markerFeature == null || mapView == null) {
            return
        }


        if (isSensorArmed) {
            markerFeature.addStringProperty(ICON_PROPERTY, GREEN_ICON_ID)
        } else {
            markerFeature.addStringProperty(ICON_PROPERTY, GRAY_ICON_ID)
        }
        refreshMarkers()
    }

    private fun showDialogSensorsList() {

        //TODO to separate the adapters

        val sensors = activity?.let { getSensorsFromLocally(it) }

        if (dialog != null && dialog?.isShowing!!) {
            sensorsDialogAdapter?.setDetects(sensors)
            sensorsDialogAdapter?.notifyDataSetChanged()
            return
        }

        sensorsDialogAdapter = activity?.let { adapter ->
            sensors?.let { arr ->
                SensorsDialogAdapter(arr, adapter, this) { _ ->

                }
            }
        }

        //create dialog
        dialog = this.context?.let { Dialog(it) }
        //set layout custom
        dialog?.setContentView(R.layout.dialog_list_detectors)

        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.75).toInt()
        dialog?.window?.setLayout(width, height)

        val rvDetector = dialog?.findViewById<RecyclerView>(R.id.rvDetector)
        val btnSaveLocateSensor = dialog?.findViewById<Button>(R.id.btnSaveLocateSensor)
        btnSaveLocateSensor?.setOnClickListener {
            SensorsDialogAdapter.selectedSensor

            currentLatitude?.let { SensorsDialogAdapter.selectedSensor?.setLatitude(it) }
            currentLongitude?.let { SensorsDialogAdapter.selectedSensor?.setLongtitude(it) }

            SensorsDialogAdapter.selectedSensor?.let { sensor -> saveLatLongDetector(sensor) }
            dialog?.dismiss()
            showMarkers()

        }

        val itemDecorator = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.divider
            )!!
        )
        rvDetector?.addItemDecoration(itemDecorator)

        sensorsDialogAdapter?.itemClick = { detector ->

        }

        // Add some item here to show the list.
        rvDetector?.adapter = sensorsDialogAdapter
        val mLayoutManager = LinearLayoutManager(context)
        rvDetector?.layoutManager = mLayoutManager
        dialog?.show()
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MapmobFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MapmobFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun setFilter() {
        val filter = IntentFilter(CREATE_ALARM_KEY)
        filter.addAction(RESET_MARKERS_KEY)
        filter.addAction(GET_CURRENT_LOCATION_KEY)
        filter.addAction(GET_CURRENT_SINGLE_LOCATION_KEY)
        filter.addAction(STOP_ALARM_SOUND)
        filter.addAction(ACTION_TOGGLE_TEST_MODE)
        activity?.registerReceiver(usbReceiver, filter)
    }

    override fun onStart() {
        super.onStart()
        setFilter()
        initMapType()
        mapView?.onStart()
    }


    override fun onResume() {
        super.onResume()

        mapView?.onResume()

        //load map
        if (isAdded) {
            mapView?.getMapAsync {
                mapView?.getMapAsync { mapboxMap ->
                    mapboxMap.uiSettings.isCompassEnabled = true
                    mapboxMap.uiSettings.setCompassFadeFacingNorth(false)
                    mapboxMap.setStyle(mapType) {

                        loadedMapStyle = it
                        loadedMapStyle?.addSource(GeoJsonSource("source-id"))
                        myMapboxMap = mapboxMap

                        myMapboxMap?.addOnMapClickListener { point ->

                            val result = handleClickIcon(
                                mapboxMap.projection.toScreenLocation(point),
                                point
                            )
                            result
                        }

                        //detect map dragging
                        mapboxMap.addOnMoveListener(this)

                        myMapboxMap?.addOnMapLongClickListener { point ->
                            currentLongitude = point.longitude
                            currentLatitude = point.latitude
                            showDialogSensorsList()
                            true
                        }

                        //for markers
                        markerViewManager = MarkerViewManager(mapView, myMapboxMap)


                        //go to last location
                        val location = initFindLocation()


                        //set last location if exist
                        location?.let {
                            myLocate =
                                LatLng(it.latitude, it.longitude)
                        }

                        showLocation(location)

                        gotoMyLocation()
                    }
                }
            }

        }
    }


    // move the camera to ic_mark location
    private fun showMyLocationMarker(location: Location?) {

        if (location != null) {
            setMyLocate(
                LatLng(
                    location.latitude,
                    location.longitude
                )
            )
        } else {

            myLocate = getLastLocationLocally()

            if (myLocate == null) {
                //set default location (london)
                myLocate = LatLng(51.509865, -0.118092)
                //set default location (london) if there is no last location
                setMyLocate(LatLng(51.509865, -0.118092))
            }
        }
        //add marker at the focus of the map
        myLocate?.let {
            //show current location marker
            showCurrentLocationMarker()

        }

    }

    private fun saveLatLongDetector(sensor: Sensor) {
        val sensorsArr = activity?.let { getSensorsFromLocally(it) }
        if (sensorsArr != null) {

            val iteratorList = sensorsArr.listIterator()
            while (iteratorList != null && iteratorList.hasNext()) {
                val sensorItem = iteratorList.next()
                if (sensorItem.getId() == sensor.getId()) {
                    sensor.getLatitude()?.let { sensorItem.setLatitude(it) }
                    sensor.getLongtitude()?.let { sensorItem.setLongtitude(it) }
                }
            }

        }
        sensorsArr?.let { activity?.let { context -> storeSensorsToLocally(it, context) } }
    }


    override fun onPause() {
        super.onPause()
        popup?.dismiss()
        mapView?.onPause()
        activity?.stopService(Intent(context, ServiceFindLocation::class.java))
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
        activity?.unregisterReceiver(usbReceiver)
    }

    //save the name of the sensor
    override fun saveNameSensor(detector: Sensor) {
        val detectorsArr = activity?.let { getSensorsFromLocally(it) }
        if (detectorsArr != null) {

            val iteratorList = detectorsArr.listIterator()
            while (iteratorList != null && iteratorList.hasNext()) {
                val detectorItem = iteratorList.next()
                if (detectorItem.getId() == detector.getId()) {
                    detector.getName()?.let { detectorItem.setName(it) }
                }
            }

        }
        detectorsArr?.let { activity?.let { context -> storeSensorsToLocally(it, context) } }
        showDialogSensorsList()
    }

    override fun saveSensors(detector: Sensor) {}

    //reciever
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, inn: Intent) {
            //accept currentAlarm
            if (inn.action == CREATE_ALARM_KEY) {

                val alarmSensorId = inn.getStringExtra(CREATE_ALARM_ID_KEY)
                val type = inn.getStringExtra(CREATE_ALARM_TYPE_KEY)
                val typeIdx = inn.getIntExtra(CREATE_ALARM_TYPE_INDEX_KEY, -1)
                val isArmed = inn.getBooleanExtra(CREATE_ALARM_IS_ARMED, false)

//                //prevent duplicate alarm at the same sensor at the same time
//                alarmSensorId?.let { removeSensorAlarmById(it) }
//
//                //add alarm process to queue
//                val alarmSensor = alarmSensorId.let {
//                    it?.let { it1 ->
//                        type?.let { it2 ->
//                            AlarmSensor(
//                                it1,
//                                Calendar.getInstance(),
//                                it2,
//                                isArmed
//                            )
//                        }
//                    }
//                }
//                alarmSensor?.typeIdx = typeIdx
//                alarmSensor?.let { UserSession.instance.alarmSensors?.add(it) }
                showMarkers()

            } else if (inn.action == GET_CURRENT_LOCATION_KEY) {
                val location: Location? = inn.getParcelableExtra(CURRENT_LOCATION)
                if (location != null) {
                    //save locally the current location
                    setStringInPreference(
                        activity,
                        CURRENT_LATITUDE_PREF,
                        location.latitude.toString()
                    )
                    setStringInPreference(
                        activity,
                        CURRENT_LONGTUDE_PREF,
                        location.longitude.toString()
                    )
                    //clear the current marker
                    markersList?.remove(currentLocationMarker)
                    showMyLocationMarker(location)
                    //showLocation(location)
                } else {
                    Toast.makeText(activity, "error in location2", Toast.LENGTH_LONG).show()
                }
            } else if (inn.action == GET_CURRENT_SINGLE_LOCATION_KEY) {
                val location: Location? = inn.getParcelableExtra(CURRENT_LOCATION)
                if (location != null) {
                    //save locally the current location
                    setStringInPreference(
                        activity,
                        CURRENT_LATITUDE_PREF,
                        location.latitude.toString()
                    )
                    setStringInPreference(
                        activity,
                        CURRENT_LONGTUDE_PREF,
                        location.longitude.toString()
                    )
                    showLocation(location)
                } else {
                    Toast.makeText(activity, "error in location2", Toast.LENGTH_LONG).show()
                }
            } else if (inn.action == RESET_MARKERS_KEY) {
                showMarkers()
            } else if (inn.action == STOP_ALARM_SOUND) {
                //stopPlayingAlarm()
            } else if (inn.action == ACTION_TOGGLE_TEST_MODE) {
                if (fbTest?.visibility == View.VISIBLE) {
                    fbTest?.visibility = View.GONE
                } else {
                    fbTest?.visibility = View.VISIBLE
                }
            }

        }
    }

    private val PROPERTY_NAME = "name"
    private val PROPERTY_NAME_WIN = "name_win"
    private val PROPERTY_SENSOR_TYPE = "sensor_type"

    private fun handleClickIcon(screenPoint: PointF, point: LatLng): Boolean {
        if (myMapboxMap != null) {
            val features: List<Feature> = myMapboxMap!!.queryRenderedFeatures(screenPoint, LAYER_ID)
            if (features.isNotEmpty()) {
                val cameraName = features[0].getStringProperty(PROPERTY_NAME_WIN)
                val sensorType = features[0].getStringProperty(PROPERTY_SENSOR_TYPE)
                //if(cameraName!=null && cameraName != "") {
                showPopup(requireActivity(), screenPoint, cameraName, sensorType)
                //}

                return true
            } else {
                if (popup != null)
                    popup?.dismiss()
                return false
            }
        }
        return false
    }


    //popup with camera info
    private fun showPopup(
        context: Activity,
        pointF: PointF,
        cameraName: String,
        sensorType: String
    ) {

        //when press on icon of current location
        if (cameraName == "myLocate")
            return

        // Inflate the popup_layout.xml
        //val viewGroup = context.findViewById<View>(R.id.popup) as LinearLayout
        val layoutInflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout: View = layoutInflater.inflate(R.layout.popup_marker, null)

        // Creating the PopupWindow
        if (popup != null)
            popup?.dismiss()

        popup = PopupWindow(context)
        popup?.contentView = layout

        popup?.isFocusable = false


        //disregard the tab layout height
        val offsetY = dpToPx(TABLAYOUT_HEIGHT_DEFAULT, requireActivity())


        // Displaying the popup at the specified location, + offsets.
        popup?.showAtLocation(
            layout,
            Gravity.NO_GRAVITY,
            pointF.x.toInt(),
            pointF.y.toInt() + offsetY
        )

        // Getting a reference to Close button, and close the popup when clicked.
        val tvCameraName = layout.findViewById<TextView>(R.id.tvCameraName)
        tvCameraName.text = cameraName

        val tvCameraType = layout.findViewById<TextView>(R.id.tvCameraType)
        tvCameraType.text = sensorType//cameraName
    }

    //to dismiss info popup when
    override fun onMoveBegin(detector: MoveGestureDetector) {
        if (popup != null)
            popup?.dismiss()
    }

    override fun onMove(detector: MoveGestureDetector) {}

    override fun onMoveEnd(detector: MoveGestureDetector) {}


}


