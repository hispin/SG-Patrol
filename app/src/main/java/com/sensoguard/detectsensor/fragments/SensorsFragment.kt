package com.sensoguard.detectsensor.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.adapters.SensorsAdapter
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.global.*
import com.sensoguard.detectsensor.interfaces.OnAdapterListener
import com.sensoguard.detectsensor.interfaces.OnFragmentListener


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SensorsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SensorsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SensorsFragment : ParentFragment(), OnAdapterListener {


    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    //private var listener: OnAdapterListener? = null
    var tvShowLogs: TextView? = null
    var bs: StringBuilder? = null

    //private var floatAddSensor: FloatingActionButton? = null
    private var ibSendCommand: ImageButton? = null
    //private var floatSendCommand: FloatingActionButton? = null

    private var sensors: ArrayList<Sensor>? = null
    private var rvSensor: RecyclerView? = null
    private var sensorsAdapter: SensorsAdapter? = null
    private val listenerPref: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var listener: OnFragmentListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnAdapterListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    //create shared preference handler change
    private val appStateChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            DETECTORS_LIST_KEY_PREF -> {
                refreshSensorsFromPref()
            }
        }
    }



    private fun initSensorsAdapter() {

        sensors = ArrayList()

        //sensors?.add(Sensor(resources.getString(R.string.id_title),resources.getString(R.string.name_title)))

        val itemDecorator = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
        rvSensor?.addItemDecoration(itemDecorator)

        sensorsAdapter = activity?.let { adapter ->
            sensors?.let { arr ->
                SensorsAdapter(arr, adapter, this) { _sensor ->
                    showEditSensorDialog(_sensor)
                }
            }
        }
        rvSensor?.adapter = sensorsAdapter
        val layoutManager= LinearLayoutManager(activity)
        rvSensor?.layoutManager=layoutManager

        sensorsAdapter?.notifyDataSetChanged()



    }

    //show dialog of add a new detector
    private fun showDialog() {

        var numSensorsRequest:Int?=null

        //ask before delete extra sensors
        fun askBeforeDeleteExtraSensor() {
            val dialog= AlertDialog.Builder(activity)
                //set message, title, and icon
                .setTitle(activity?.resources?.getString(R.string.remove_extra_sensors)).setMessage(
                    activity?.resources?.getString(
                        R.string.content_delete_extra_sensor
                    )
                ).setIcon(
                    android.R.drawable.ic_menu_delete

                )

                .setPositiveButton(activity?.resources?.getString(R.string.yes)) { dialog, _ ->

                    //remove extra sensors
                    if(numSensorsRequest!=null) {
                        val items=sensors?.listIterator()
                        while (items != null && items.hasNext()) {
                            val item = items.next()

                            val id=item.getId()
                            try {
                                if (id.toInt() > numSensorsRequest!!) {
                                    items.remove()
                                }
                            } catch (ex: NumberFormatException) {
                                //do nothing
                            }
                        }
                    }

                    sensors?.let { sen -> storeSensorsToLocally(sen, requireActivity()) }
                    dialog.dismiss()
                }


                .setNegativeButton(activity?.resources?.getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
                .create()
            dialog.show()

        }


        if(activity==null){
            return
        }

        val dialog = Dialog(requireActivity())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_new_sensor)

        val etId = dialog.findViewById(R.id.etId) as EditText


        val btnOk = dialog.findViewById(R.id.btnOk) as Button

        btnOk.setOnClickListener {

            if(validIsEmpty(etId) && activity!=null) {


                val sensors= populateSensorsFromLocally()


                try {
                    numSensorsRequest = etId.text.toString().toInt()
                } catch (ex: NumberFormatException) {
                    Toast.makeText(this.context, "exception ${ex.message}", Toast.LENGTH_LONG)
                        .show()
                    return@setOnClickListener
                }

                if(numSensorsRequest!=null && numSensorsRequest!! >254) {
                    Toast.makeText(
                        this.context,
                        resources.getString(R.string.invalid_mum_sensors),
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }


                if(numSensorsRequest!=null) {
                    //add numSensors sensors
                    for (sensorId in 1 until numSensorsRequest!! + 1) {
                        //add it just if not exist
                        if (sensors?.let { it1 -> !isIdExist(it1, sensorId.toString()) }!!) {
                            sensors.add(Sensor(sensorId.toString()))
                        }
                    }
                }

                //check if the request of sensors number is smaller then the number of exist
                if(sensors?.size!=null
                    && numSensorsRequest!=null
                    && numSensorsRequest!! < sensors.size){
                    askBeforeDeleteExtraSensor()
                }else if(activity!=null) {
                    sensors?.let { sen -> storeSensorsToLocally(sen, requireActivity()) }
                }

                dialog.dismiss()
            }
        }

        val btnCancel = dialog.findViewById(R.id.btncn) as Button
        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun isIdExist(detectorsArr: ArrayList<Sensor>, id: String): Boolean {
        for (item in detectorsArr) {
            if (item.getId() == id) {
                return true
            }
        }
        return false
    }

    private fun saveNameDetector(detector: Sensor) {
        val detectorsArr = populateSensorsFromLocally()
        if (detectorsArr != null) {

            val iteratorList = detectorsArr.listIterator()
            while (iteratorList != null && iteratorList.hasNext()) {
                val detectorItem = iteratorList.next()
                if (detectorItem.getId() == detector.getId()) {
                    detector.getName()?.let { detectorItem.setName(it) }
                    detector.getType()?.let { detectorItem.setType(it) }
                    detector.getTypeID()?.let { detectorItem.setTypeID(it) }
                }
            }

        }
        detectorsArr?.let { activity?.let { context -> storeSensorsToLocally(it, context) } }
    }

    //get the sensors from locally
    private fun populateSensorsFromLocally(): ArrayList<Sensor>?  {
        val detectors: ArrayList<Sensor>?
        val detectorListStr = getStringInPreference(activity, DETECTORS_LIST_KEY_PREF, ERROR_RESP)

        detectors = if(detectorListStr.equals(ERROR_RESP)){
            ArrayList()
        }else {
            detectorListStr?.let { convertJsonToSensorList(it) }
        }
        return detectors
    }


    private fun validIsEmpty(editText: EditText): Boolean {
        var isValid = true

        if (editText.text.isNullOrBlank()) {
            editText.error = resources.getString(R.string.empty_field_error)
            isValid = false
        }

        return isValid
    }

    //save sensors in locally
    override fun saveSensors(sensor: Sensor) {
        val sensorsArr = populateSensorsFromLocally()
        if (sensorsArr != null) {

            val iteratorList = sensorsArr.listIterator()
            while (iteratorList != null && iteratorList.hasNext()) {
                var sensorItem = iteratorList.next()
                if (sensorItem.getId() == sensor.getId()) {
                    sensor.getName()?.let { sensorItem.setName(it) }
                    sensor.getId().let { sensorItem.setId(it) }
                    sensor.getType().let { sensorItem.setType(it) }
                    sensor.getTypeID().let { sensorItem.setTypeID(it) }
                    sensor.isArmed().let { sensorItem.setArm(it) }
                    sensor.getLatitude().let { sensorItem.setLatitude(it) }
                    sensor.getLongtitude().let { sensorItem.setLongtitude(it) }
                }
            }

        }
        sensorsArr?.let { activity?.let { context -> storeSensorsToLocally(it, context) } }
    }

    //ic_edit name (maybe come from adapter)
    override fun saveNameSensor(detector: Sensor){
        saveNameDetector(detector)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_sensors, container, false)
        tvShowLogs = view.findViewById(R.id.tvShowLogs)
        rvSensor = view.findViewById(R.id.rvDetector)


        ibSendCommand = view.findViewById(R.id.ibSendCommand)
        ibSendCommand?.setOnClickListener {
            val isConnected = getBooleanInPreference(activity, USB_DEVICE_CONNECT_STATUS, false)
            //if the usb is connected then open dialog of commands
            //if (isConnected) {
            openCommands()
//            } else {
//                showToast(activity, resources.getString(R.string.usb_is_disconnect))
//            }
        }
        bs = StringBuilder()
        return view
    }

    //trigger also when changing tabs
    override fun onPause() {
        super.onPause()
        activity?.sendBroadcast(Intent(STOP_TIMER))
    }


    override fun onStart() {
        super.onStart()

        activity?.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE)
            ?.registerOnSharedPreferenceChangeListener(
                appStateChangeListener
            )

        setFilter()

        initSensorsAdapter()

        refreshSensorsFromPref()

    }

    private fun refreshSensorsFromPref(){
        sensors= ArrayList()
        //sensors?.add(Sensor(resources.getString(R.string.id_title),resources.getString(R.string.name_title)))
        val detectorListStr = getStringInPreference(activity, DETECTORS_LIST_KEY_PREF, ERROR_RESP)

        if(detectorListStr.equals(ERROR_RESP)){
            //ArrayList()
        }else {

            detectorListStr?.let {
                val temp=convertJsonToSensorList(it)
                temp?.let { tmp -> sensors?.addAll(tmp) } }
        }

        sensorsAdapter?.setSensors(sensors)
        sensorsAdapter?.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(usbReceiver)
        activity?.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE)
            ?.unregisterOnSharedPreferenceChangeListener(
                appStateChangeListener
            )
    }


    private fun setFilter() {
        val filter = IntentFilter("handle.read.data")
        //filter.addAction(ACTION_USB_RESPONSE_CACHE)
        activity?.registerReceiver(usbReceiver, filter)
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, inn: Intent) {
            if (inn.action == "handle.read.data") {

                //TODO return the code
                val bit = inn.getIntegerArrayListExtra("data")

                if (bit != null) {
                    for (item in bit) {
                        bs?.append("  ${item.toUByte()}")
                    }
                }
                bs?.append("\n")
                tvShowLogs?.text = bs.toString()

            }
//            else if(inn.action == ACTION_USB_RESPONSE_CACHE){
//                val arr = inn.getIntegerArrayListExtra(USB_CACHE_RESPONSE_KEY)
//
//                var msg=""
//                val iteratorList = arr?.listIterator()
//                while (iteratorList != null && iteratorList.hasNext()) {
//                    val item = iteratorList.next()
//                    msg+=item.toUByte().toString()+" "
//                }
//
//                showToast(
//                    activity,
//                    msg
//                )
//            }
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MainUartFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SensorsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    //open fragment dialog to make commands
    private fun openCommands() {

        val sensorsIds = getSensorsIds()

        val fr = CommandsFragment()

        if (sensorsIds.size > 0) {
            val bnd = Bundle()
            bnd.putStringArrayList(SENSORS_IDS, sensorsIds)
            fr.arguments = bnd
        }
//        //deliver selected camera to continue add data
//
        val fm = activity?.supportFragmentManager
//        fm?.addOnBackStackChangedListener {
//            //if the dialog is close then the add button is visible
//            if (fm.backStackEntryCount == 0) {
//                floatSendCommand?.visibility = View.VISIBLE
//            } else {
//                floatSendCommand?.visibility = View.INVISIBLE
//            }
//        }
        val fragmentTransaction = fm?.beginTransaction()
        fragmentTransaction?.addToBackStack(fr.tag)
        fragmentTransaction?.add(R.id.flCommands, fr, "CommandsFragment")
        fragmentTransaction?.commit()
    }

    //get sensors id
    private fun getSensorsIds(): ArrayList<String> {
        val sensorsIds = ArrayList<String>()
        val iteratorList = sensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            sensorsIds.add(sensorItem.getId())
        }
        return sensorsIds
    }

    //show dialog to edit sensor info
    private fun showEditSensorDialog(sensor: Sensor) {

        if (this.context != null) {
            val dialog = Dialog(this.requireContext())
            dialog.setContentView(R.layout.custom_dialog_edit_sensor)

            dialog.setCancelable(true)

            val etName = dialog.findViewById<AppCompatEditText>(R.id.etName)
            val spSensorsType = dialog.findViewById<AppCompatSpinner>(R.id.spSensorsType)

//            val values = resources.getStringArray(R.array.sensor_type)//arrayOf(resources.getString(R.string.seismic),resources.getString(R.string.pir),resources.getString(R.string.radar),resources.getString(R.string.vibration))//arrayOf("Seismic","PIR","Radar","Vibration")//resources.getStringArray(R.array.sensor_type)
//
//            val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
//                requireActivity(), android.R.layout.simple_spinner_item, values
//            )
//            spSensorsType.adapter = spinnerArrayAdapter

            //show the current type in spinner
            sensor.getTypeID()?.let { spSensorsType.setSelection(it.toInt()) }

            etName.setText(sensor.getName())
            //etType.setText(sensor.getType())

            val btnOK = dialog.findViewById<AppCompatButton>(R.id.btnOK)
            btnOK.setOnClickListener {

                sensor.setName(etName.text.toString())
                sensor.setType(spSensorsType.selectedItem.toString())
                //set the position of selected item as type id
                sensor.setTypeID(spSensorsType.selectedItemId)

                //save in local the changes of the sensors
                saveSensors(sensor)

                //refresh the list view
                sensorsAdapter?.notifyDataSetChanged()



                dialog.dismiss()

            }
            val btnCancel = dialog.findViewById<AppCompatButton>(R.id.btnCancel)
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }


    }
}
