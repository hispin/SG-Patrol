package com.sensoguard.detectsensor.fragments


//import android.support.design.widget.FloatingActionButton
//import android.support.v4.app.Fragment
//import android.support.v4.app.FragmentManager
//import android.support.v4.content.ContextCompat
//import android.support.v4.content.ContextCompat.startForegroundService
//import android.support.v7.widget.DividerItemDecoration
//import android.support.v7.widget.LinearLayoutManager
//import android.support.v7.widget.RecyclerView
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.adapters.SensorsDialogAdapter
import com.sensoguard.detectsensor.classes.AlarmSensor
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.controler.ViewModelListener
import com.sensoguard.detectsensor.global.*
import com.sensoguard.detectsensor.interfaces.OnAdapterListener
import com.sensoguard.detectsensor.interfaces.OnFragmentListener
import com.sensoguard.detectsensor.services.ServiceFindLocation
import com.sensoguard.detectsensor.services.ServiceFindSingleLocation
import java.util.*
import kotlin.collections.ArrayList


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MapSensorsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MapSensorsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MapSensorsFragment : Fragment() ,OnMapReadyCallback,OnAdapterListener{

    override fun saveDetector(detector: Sensor) {}


    private var dialog: Dialog?=null
    var sensorsDialogAdapter: SensorsDialogAdapter?=null


    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentListener? = null
    private var mMap : GoogleMap?=null
    private var mMapFragment:SupportMapFragment?=null
    private val TAG = "MapSensorsFragment"
    private var mCenterLatLong: LatLng? = null
    @Volatile
    private var myLocate: LatLng? = null
    private var currentLongitude: Double?=null
    private var currentLatitude: Double?=null
    private var fbRefresh: FloatingActionButton?=null
    private var fbTest:FloatingActionButton?=null
    private var fbChangeMapType:FloatingActionButton?=null
    private var mapType=GoogleMap.MAP_TYPE_SATELLITE
    private var flickering: Animation?=null
    //private var ibOfflineMap:AppCompatImageButton?=null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        //start listener to timer
        startTimerListener()
    }

    //start listener to timer
    private fun startTimerListener() {
        activity?.let {
            ViewModelProviders.of(it).get(ViewModelListener::class.java)
                .startCurrentCalendarListener()?.observe(
                    this,
                    androidx.lifecycle.Observer { calendar ->

                        //Log.d("testTimer","tick in MapSensorsFragment")
                        //if there is no alarm in process then shut down the timer
                        if (UserSession.instance.alarmSensors == null || UserSession.instance.alarmSensors?.isEmpty()!!) {
                            activity?.let { act ->
                                ViewModelProviders.of(act).get(ViewModelListener::class.java)
                                    .shutDownTimer()
                            }
                            //stopPlayingAlarm()
                            //refresh markers
                            showMarkers()
                        } else {
                            //remove all the time out sensors alarm and show them with regular sensor marker
                            replaceSensorAlarmTimeOutToSensorMarker()
                        }

                    })

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.fragment_map_detects, container, false)

        fbRefresh = view.findViewById(R.id.fbRefresh1)
        fbRefresh?.setOnClickListener {
            //gotoMyLocation()
            gotoMySingleLocation()
        }

        fbTest = view.findViewById(R.id.fbTest1)
        fbTest?.setOnClickListener {
            showTestEventDialog()
        }

//        ibOfflineMap=view.findViewById(R.id.ibOfflineMap)
//        ibOfflineMap?.setOnClickListener {
//            setUpMap()
//        }

        //toggle the type of the map
        fbChangeMapType = view.findViewById(R.id.fbChangeMapType)
        fbChangeMapType?.setOnClickListener {
            if (mMap?.mapType == GoogleMap.MAP_TYPE_NORMAL) {
                mapType = GoogleMap.MAP_TYPE_SATELLITE
            } else {
                mapType = GoogleMap.MAP_TYPE_NORMAL
            }
            mMap?.mapType = mapType
        }

        mMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        mMapFragment?.getMapAsync(this)



        return view
    }


    override fun onPause() {
        super.onPause()
        activity?.stopService(Intent(context, ServiceFindLocation::class.java))
        //Log.d("ServiceFindLocation"," map onPause")
    }

    override fun onResume() {
        super.onResume()
        initMapType()

        //load map
        //if (mMap == null) {
            val listener: OnMapReadyCallback = this
            if (isAdded) {
                //create map fragment
                Log.d(TAG, "run")
                mMapFragment = SupportMapFragment()
                mMapFragment?.let {
                    val fm: FragmentManager = childFragmentManager
                    fm.beginTransaction()
                        .replace(R.id.map, it).commit()
                    it.getMapAsync(listener)
                }

            }
    }


    //configureActivation map type
    private fun initMapType() {
        val _mapType = getIntInPreference(activity, MAP_SHOW_VIEW_TYPE_KEY, -1)
        Log.d("testMapView", "_mapType:$_mapType")
        if(_mapType==MAP_SHOW_NORMAL_VALUE){
            mapType= GoogleMap.MAP_TYPE_NORMAL
        }else if(_mapType== MAP_SHOW_SATELLITE_VALUE){
            mapType= GoogleMap.MAP_TYPE_SATELLITE
        }

        //when the map is already loaded
        try {
            mMap?.mapType = mapType
        } catch (ex: Exception) {
        }
    }

    //when the app start rotate ,then invoke this method and save the map type
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MAP_TYPE_KEY, mapType)
    }


    //when the app complete rotate ,then invoke this method and restore the map type
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        if (savedInstanceState != null && savedInstanceState.containsKey(MAP_TYPE_KEY)) {
            mapType = savedInstanceState.getInt(MAP_TYPE_KEY)
            mMap?.mapType = mapType
        }
        super.onViewStateRestored(savedInstanceState)
    }

    //get current location from gps
    private fun gotoMyLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.startForegroundService(Intent(context, ServiceFindLocation::class.java))
        }else{
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

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap

        mMap?.clear()

        //set satellite as default
        mMap?.mapType = mapType

        //set the location of the current touching map
        mMap?.setOnMapLongClickListener { arg0 ->
            //bug fixed: crash when accept alarm
            if (arg0 != null) {
                currentLongitude = arg0.longitude
                currentLatitude = arg0.latitude
                showDialogSensorsList()
            } else {
                Toast.makeText(activity, "error in location1", Toast.LENGTH_LONG).show()
            }
        }

        mMap?.setOnCameraMoveListener {
            val cp = mMap?.cameraPosition
            mCenterLatLong = cp?.target

            //remove the action of set current location as current central location
            //mCenterLatLong?.let { setMyLocate(it) }
        }

        //go to last location
        val location = initFindLocation()

        if (location == null) {
            val latitude = getStringInPreference(activity, CURRENT_LATITUDE_PREF, "-1")
            val longtude = getStringInPreference(activity, CURRENT_LONGTUDE_PREF, "-1")

            if (!latitude.equals("-1") && !longtude.equals("-1")) {
                try {
                    val lat = latitude?.toDouble()
                    val lon = longtude?.toDouble()

                } catch (ex: NumberFormatException) {
                }
            }
        }

        //set last location if exist
        location?.let { myLocate = LatLng(it.latitude, it.longitude) }

        showLocation(location)

        //Log.d("ServiceFindLocation","map start location")
        gotoMyLocation()
    }


    // move the camera to ic_mark location
    private fun showMyLocationMarker(location: Location?) {

        if (location != null) {
            setMyLocate(LatLng(location.latitude, location.longitude))
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
//            showMyLocationNarker(it)}
//            //mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocate, 15.0f))
//            showMarkers()
//            //fillSensorsMarkers()
        }

    }

    // move the camera to ic_mark location
    private fun showLocation(location: Location?) {

        if (location != null) {
            setMyLocate(LatLng(location.latitude, location.longitude))
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
        myLocate?.let{
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocate, 15.0f))
            showMarkers()
            //fillSensorsMarkers()
        }

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

    //execute vibrate
    private fun playVibrate() {

        val isVibrateWhenAlarm = getBooleanInPreference(activity, IS_VIBRATE_WHEN_ALARM_KEY, true)
        if(isVibrateWhenAlarm){
            // Get instance of Vibrator from current Context
           val vibrator =   activity?.getSystemService(Context.VIBRATOR_SERVICE)  as Vibrator

            // Vibrate for 200 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        1000,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibrator.vibrate(1000)
            }

         }

    }

    //create timeout for reset sensor to regular icon and cancel the alarm icon
    private fun startTimer() {

        Log.d("testTimer", "start timer")

        activity?.let {
            ViewModelProviders.of(it).get(ViewModelListener::class.java).startTimer()
        }

    }


    //show all markers
    fun showMarkers(){

        mMap?.clear()

        //show current location marker
        showCurrentLocationMarker()

        //get sensors from locally
        val sensorsArr= activity?.let { getSensorsFromLocally(it) }

        //for
        val iteratorList=sensorsArr?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if(sensorItem.getLatitude()!=null
                && sensorItem.getLongtitude()!=null){

                val sensorAlarm=getSensorAlarmBySensor(sensorItem)

                if(sensorAlarm!=null){

                    //if time out then remove the sensor from alarm list
                    if(isSensorAlarmTimeout(sensorAlarm)){
                        UserSession.instance.alarmSensors?.remove(sensorAlarm)
                        showSensorMarker(sensorItem)
                    }else{
                        //save the marker for update after timeout
                        sensorAlarm.marker = showSensorAlarmMarker(sensorItem, sensorAlarm.type)
                    }

                }else{
                    //show sensor marker
                    showSensorMarker(sensorItem)
                }

            }
        }
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

        val iteratorList=UserSession.instance.alarmSensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if(sensorItem.alarmSensorId == sensor.getId()){
                return sensorItem
            }
        }
        return null
    }


   //show marker of sensor alarm
   private fun showSensorAlarmMarker(sensorItem: Sensor, type: String): Marker? {

       if(mMap==null){
           return null
       }

       //set icon according to type alarm
       val alarmTypeIcon =
           when (type) {
               ALARM_CAR -> context?.let { con ->
                   convertBitmapToBitmapDiscriptor(
                       con,
                       R.drawable.ic_alarm_car
                   )
               }
               ALARM_INTRUDER -> context?.let { con ->
                   convertBitmapToBitmapDiscriptor(
                       con,
                       R.drawable.ic_alarm_intruder
                   )
               }
               ALARM_SENSOR_OFF -> context?.let { con ->
                   convertBitmapToBitmapDiscriptor(
                       con,
                       R.drawable.ic_alarm_sensor_off
                   )
               }
               //ALARM_LOW_BATTERY->context?.let { con -> convertBitmapToBitmapDiscriptor(con,R.drawable.ic_alarm_low_battery)}
               else -> context?.let { con ->
                   convertBitmapToBitmapDiscriptor(
                       con,
                       R.drawable.ic_sensor_alarm
                   )
               }
           }


       val loc:LatLng? = LatLng(sensorItem.getLatitude()!!, sensorItem.getLongtitude()!!)

       mMap!!.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener {
           override fun onMarkerClick(marker: Marker): Boolean {
               marker.showInfoWindow()
               return true
           }
       })

       if(loc!=null) {
           return mMap?.addMarker(
               MarkerOptions()
                   .position(loc)
                   .draggable(true)
                   .icon(context?.let { alarmTypeIcon })
                   .title("Id:" + sensorItem.getId() + "  Name:" + sensorItem.getName())

           )
       }
       return null
   }

    var myLocatioMarker: Marker? = null

    //show marker of current location
    private fun showCurrentLocationMarker() {

        if (mMap == null) {
            return
        }

        if (myLocate == null) {
            return
        }

        myLocatioMarker?.remove()

        myLocatioMarker = mMap?.addMarker(
            myLocate?.let {
                MarkerOptions()
                    .position(it)
                    .draggable(true)
                    .icon(context?.let { con ->
                        convertBitmapToBitmapDiscriptor(
                            con,
                            R.drawable.ic_my_locate
                        )
                    })
            }
        )
   }

    //show marker of sensor
    private fun showSensorMarker(sensorItem: Sensor) {

        if (mMap == null || sensorItem == null) {
            return
        }

        val loc=LatLng(sensorItem.getLatitude()!!, sensorItem.getLongtitude()!!)

        mMap!!.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker): Boolean {
                marker.showInfoWindow()
                return true
            }
        })

        if (sensorItem.isArmed()) {

            mMap?.addMarker(
                MarkerOptions()
                    .position(loc)
                    .draggable(true)
                    .icon(context?.let { con ->
                        convertBitmapToBitmapDiscriptor(
                            con,
                            R.drawable.ic_sensor_item
                        )
                    }).title("Id:" + sensorItem.getId() + "  Name:" + sensorItem.getName())
                //.snippet("type:motion")
            )
        } else {
            mMap?.addMarker(
                MarkerOptions()
                    .position(loc)
                    .draggable(true)
                    .icon(context?.let { con ->
                        convertBitmapToBitmapDiscriptor(
                            con,
                            R.drawable.ic_sensor_item_disable
                        )
                    }).title("Id:" + sensorItem.getId() + "  Name:" + sensorItem.getName())
            )

        }
    }

    //show marker of sensor
    private fun showSensorMarker(marker: Marker, isSensorArmed: Boolean, sensorId: String) {
        if (marker == null || mMap == null) {
            return
        }

        mMap!!.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener {

            override fun onMarkerClick(marker: Marker): Boolean {
                marker.showInfoWindow()
                return true
            }
        })

        if (isSensorArmed) {
            marker.setIcon(context?.let { con ->
                convertBitmapToBitmapDiscriptor(
                    con,
                    R.drawable.ic_sensor_item
                )
            })
        } else {
            marker.setIcon(context?.let { con ->
                convertBitmapToBitmapDiscriptor(
                    con,
                    R.drawable.ic_sensor_item_disable
                )
            })
        }
    }

    //check it there is any sensor alarm which is not time out
    private fun isAnySensorAlarmNotTimeOut():Boolean{
        val iteratorList=UserSession.instance.alarmSensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if(!isSensorAlarmTimeout(sensorItem)){
                return true
            }
        }
        return false
    }


    //remove all the time out sensors alarm and show them with regular sensor marker
    private fun replaceSensorAlarmTimeOutToSensorMarker(){
        val iteratorList=UserSession.instance.alarmSensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if(isSensorAlarmTimeout(sensorItem)){
                //show regular sensor marker
                sensorItem.marker?.let {
                    showSensorMarker(
                        it,
                        sensorItem.isSensorArmed,
                        sensorItem.alarmSensorId
                    )
                }
                //remove the sensor alarm because it timeout
                iteratorList.remove()
            }
        }
    }

    private var rington:Ringtone?=null

    private fun playAlarmSound() {

        val isNotificationSound = getBooleanInPreference(activity, IS_NOTIFICATION_SOUND_KEY, true)
        if(!isNotificationSound){
            return
        }

        val selectedSound = getStringInPreference(activity, SELECTED_NOTIFICATION_SOUND_KEY, "-1")

        if(!selectedSound.equals("-1")) {

            try {
                val uri = Uri.parse(selectedSound)

                if (rington != null && rington!!.isPlaying) {
                    //if the sound it is already played,
                    rington?.stop()
                    Handler().postDelayed({
                        rington = RingtoneManager.getRingtone(activity, uri)
                        rington?.play()
                    }, 1000)
                } else {
                    rington = RingtoneManager.getRingtone(activity, uri)
                    rington?.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }


    private fun setMyLocate(myLocate: LatLng) {
        this.myLocate = myLocate
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
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnAdapterListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        //remove all callbacks event timeout(relevant to the type of icon)
        Handler().removeCallbacksAndMessages(null)
    }


   companion object {
                /**
                 * Use this factory method to create a new instance of
                 * this fragment using the provided parameters.
                 *
                 * @param param1 Parameter 1.
                 * @param param2 Parameter 2.
                 * @return A new instance of fragment MapSensorsFragment.
                 */
                // TODO: Rename and change types and number of parameters
                @JvmStatic
                fun newInstance(param1: String, param2: String) =
                    MapSensorsFragment().apply {
                        arguments = Bundle().apply {
                            putString(ARG_PARAM1, param1)
                            putString(ARG_PARAM2, param2)
                        }
                    }
            }


   private fun showDialogSensorsList() {

                //TODO to separate the adapters


                val sensors = activity?.let { getSensorsFromLocally(it) }

                if(dialog!=null && dialog?.isShowing!!){
                    sensorsDialogAdapter?.setDetects(sensors)
                    sensorsDialogAdapter?.notifyDataSetChanged()
                    return
                }

                sensorsDialogAdapter=activity?.let { adapter ->
                    sensors?.let { arr ->
                        SensorsDialogAdapter(arr, adapter, this) { _ ->

                        }
                    }
                }

                //create dialog
                dialog = this.context?.let{Dialog(it)}
                //set layout custom
                dialog?.setContentView(R.layout.dialog_list_detectors)

                val width = (resources.displayMetrics.widthPixels*0.90).toInt()
                val height = (resources.displayMetrics.heightPixels*0.75).toInt()
                dialog?.window?.setLayout(width, height)

                val rvDetector = dialog?.findViewById<RecyclerView>(R.id.rvDetector)
                val btnSaveLocateSensor= dialog?.findViewById<Button>(R.id.btnSaveLocateSensor)
                btnSaveLocateSensor?.setOnClickListener{
                    SensorsDialogAdapter.selectedSensor

                    currentLatitude?.let { SensorsDialogAdapter.selectedSensor?.setLatitude(it) }
                    currentLongitude?.let { SensorsDialogAdapter.selectedSensor?.setLongtitude(it) }

                    SensorsDialogAdapter.selectedSensor?.let { sensor -> saveLatLongDetector(sensor) }
                    dialog?.dismiss()
                    showMarkers()
                    //fillSensorsMarkers()
                }

       val itemDecorator = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
       itemDecorator.setDrawable(
           ContextCompat.getDrawable(
               context!!,
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

   private fun showTestEventDialog() {


                //create dialog
                val dialog = MapDetectsFragment@this.context?.let{Dialog(it)}
                //set layout custom
                dialog?.setContentView(R.layout.test_dialog)

                val etid=dialog?.findViewById<EditText>(R.id.etId)
                val etType=dialog?.findViewById<EditText>(R.id.etType)
                val btnOk=dialog?.findViewById<Button>(R.id.btnOk)

                //Bug fixed:Fatal Exception: java.lang.NumberFormatException
                //Invalid int: "1 "
                btnOk?.setOnClickListener{


                    if( etid!=null &&  validIsEmpty(etid)
                        && etType!=null &&  validIsEmpty(etType)) {
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

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, inn: Intent) {
            //accept currentAlarm
            if (inn.action == CREATE_ALARM_KEY) {

                val alarmSensorId = inn.getStringExtra(CREATE_ALARM_ID_KEY)
                val type = inn.getStringExtra(CREATE_ALARM_TYPE_KEY)
                val isArmed = inn.getBooleanExtra(CREATE_ALARM_IS_ARMED, false)

                //prevent duplicate alarm at the same sensor at the same time
                alarmSensorId?.let { removeSensorAlarmById(it) }

                //add alarm process to queue
                alarmSensorId.let {
                    it?.let { it1 ->
                        type?.let { it2 ->
                            AlarmSensor(
                                it1,
                                Calendar.getInstance(),
                                it2,
                                isArmed
                            )
                        }
                    }
                }?.let { UserSession.instance.alarmSensors?.add(it) }

//                        Toast.makeText(activity, "start sound", Toast.LENGTH_LONG).show()
//                        playAlarmSound()
//
//                        playVibrate()
//
//                        startTimer()

                showMarkers()


                    }else if(inn.action == GET_CURRENT_LOCATION_KEY){
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
                    }else if(inn.action == RESET_MARKERS_KEY){
                        //fillSensorsMarkers()
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



    override fun onDestroy() {
                super.onDestroy()
                activity?.unregisterReceiver(usbReceiver)
            }

    private var locationManager: LocationManager? = null
    //    private var criteria: Criteria? = null
//    private var currentApiVersion: Int = 0
    private fun initFindLocation(): Location? {
                locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//                criteria = Criteria()
//                criteria?.accuracy = Criteria.ACCURACY_FINE
//                currentApiVersion = Build.VERSION.SDK_INT

            if (context?.let {
                    ContextCompat.checkSelfPermission(
                        it,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } == PackageManager.PERMISSION_GRANTED
            ) {

                return locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                //return locationManager?.getLastKnownLocation(locationManager?.getBestProvider(criteria, false))
            }


            return null
            }

    //rename the sensor name ,from the adapter
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

//    private fun setUpMap() {
//        mMap!!.mapType = GoogleMap.MAP_TYPE_NONE
//        if(ibOfflineMap!=null)
//            mMap!!.addTileOverlay(TileOverlayOptions().tileProvider(CustomMapTileProvider(ibOfflineMap,requireContext())))
//        //val upd: CameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(LAT, LON), ZOOM)
//        //mMap!!.moveCamera(upd)
//    }


}


