package com.sensoguard.detectsensor.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.adapters.AlarmAdapter
import com.sensoguard.detectsensor.classes.Alarm
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.classes.SystemSort
import com.sensoguard.detectsensor.global.ALARM_LIST_KEY_PREF
import com.sensoguard.detectsensor.global.CAMERA_KEY
import com.sensoguard.detectsensor.global.CAMERA_SORTED
import com.sensoguard.detectsensor.global.DATE_SORTED
import com.sensoguard.detectsensor.global.ERROR_RESP
import com.sensoguard.detectsensor.global.FROM_CALENDAR
import com.sensoguard.detectsensor.global.HANDLE_ALARM_KEY
import com.sensoguard.detectsensor.global.NO_SORTED
import com.sensoguard.detectsensor.global.RESULT_CODE
import com.sensoguard.detectsensor.global.SORT_BY_DATETIME_KEY
import com.sensoguard.detectsensor.global.SORT_BY_SYSTEM_KEY
import com.sensoguard.detectsensor.global.SORT_BY_SYSTEM_REQUEST_CODE
import com.sensoguard.detectsensor.global.SORT_PICK_DATE_TIME_REQUEST_CODE
import com.sensoguard.detectsensor.global.SORT_TYPE_KEY
import com.sensoguard.detectsensor.global.TO_CALENDAR
import com.sensoguard.detectsensor.global.alarmsListToCsvFile
import com.sensoguard.detectsensor.global.convertJsonToAlarmList
import com.sensoguard.detectsensor.global.convertJsonToSystemSortList
import com.sensoguard.detectsensor.global.convertToAlarmsGson
import com.sensoguard.detectsensor.global.getStringFromCalendar
import com.sensoguard.detectsensor.global.getStringInPreference
import com.sensoguard.detectsensor.global.setStringInPreference
import com.sensoguard.detectsensor.global.shareCsv
import com.sensoguard.detectsensor.global.storeAlarmsToLocally
import com.sensoguard.detectsensor.global.writeCsvFile
import com.sensoguard.detectsensor.interfaces.OnAdapterListener
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AlarmsLogFragment : ParentFragment(), OnAdapterListener {

    override fun saveSensors(detector: Sensor) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveNameSensor(detector: Sensor) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var myAlarms: ArrayList<Alarm>? = null
    private var rvAlarm: RecyclerView? = null
    private var alarmAdapter: AlarmAdapter? = null
    private var btnCsv: Button? = null
    private var btnFilterSystem: Button? = null
    private var btnFilterDateTime: Button? = null
    private var cbIsSelected: CheckBox? = null
    private var mySortedAlarms: ArrayList<Alarm>? = null
    var mySortedCameras: ArrayList<SystemSort>? = null
    var fromCalendar: Calendar? = null
    var toCalendar: Calendar? = null
    private var typeOfSorted: Int = NO_SORTED
    private var tvReset: TextView? = null
    private var ibDeleteSelectedItems: ImageButton? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }


    private fun initAlarmsAdapter() {
        myAlarms = ArrayList()
        //alarms?.add(Alarm("ID", "NAME", "TYPE", "TIME", false, -1))
        val itemDecorator = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
        rvAlarm?.addItemDecoration(itemDecorator)

        alarmAdapter = activity?.let { adapter ->
            myAlarms?.let { arr ->
                AlarmAdapter(arr, adapter, this) { _ ->

                }
            }
        }
        rvAlarm?.adapter = alarmAdapter
        val layoutManager = LinearLayoutManager(activity)
        rvAlarm?.layoutManager = layoutManager

        alarmAdapter?.notifyDataSetChanged()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_alarm_log, container, false)

        rvAlarm = view.findViewById(R.id.rvAlarm)

        btnCsv = view.findViewById(R.id.btnCsv)

        btnCsv?.setOnClickListener {
            val alarms: ArrayList<Alarm>? =
                if (typeOfSorted == DATE_SORTED || typeOfSorted == CAMERA_SORTED) {
                    mySortedAlarms
                } else {
                    myAlarms
                }

            this.context?.let { it1 ->

                val alarmsStr = alarmsListToCsvFile(alarms, it1)
                if (activity!=null && writeCsvFile(alarmsStr, requireActivity())) {
                    activity?.let { it2 -> shareCsv(it2) }
                    //Toast.makeText(context,"success",Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "failed", Toast.LENGTH_SHORT).show()
                }


            }

        }

        ibDeleteSelectedItems = view.findViewById(R.id.ibDeleteSelectedItems)
        ibDeleteSelectedItems?.setOnClickListener {

            var alarmCounter = 0
            if (typeOfSorted == DATE_SORTED || typeOfSorted == CAMERA_SORTED) {
                mySortedAlarms?.let { it1 -> alarmCounter = getCountItemSelected(it1) }
            } else {
                myAlarms?.let { it1 -> alarmCounter = getCountItemSelected(it1) }
            }
            if (alarmCounter > 0) {
                showDeleteDialog(alarmCounter)
            } else {
                Toast.makeText(
                    activity,
                    resources.getString(R.string.no_selected_alarms),
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }

        cbIsSelected = view.findViewById(R.id.cbIsSelected)
        cbIsSelected?.setOnCheckedChangeListener { _, isChecked ->
            if (typeOfSorted == DATE_SORTED || typeOfSorted == CAMERA_SORTED) {
                mySortedAlarms?.let { toggleItemSelected(it, isChecked) }
            } else {
                myAlarms?.let { toggleItemSelected(it, isChecked) }
            }
        }

        btnFilterSystem = view.findViewById(R.id.btnFilterSystem)
        btnFilterSystem?.setOnClickListener {

            //clear if selected
            clearSelection()

            this@AlarmsLogFragment.context?.let { it1 ->
                ContextCompat.getColor(
                    it1, R.color.green2
                )
            }?.let { it2 -> it.setBackgroundColor(it2) }

            this@AlarmsLogFragment.context?.let { it1 ->
                ContextCompat.getColor(
                    it1, R.color.white
                )
            }?.let { it2 -> (it as Button).setTextColor(it2) }

            btnFilterSystem?.isEnabled = false
            btnFilterDateTime?.isEnabled = false
            btnCsv?.visibility = View.GONE

            openSortByType(SORT_BY_SYSTEM_KEY, SORT_BY_SYSTEM_REQUEST_CODE)
        }
        btnFilterDateTime = view.findViewById(R.id.btnFilterDateTime)
        btnFilterDateTime?.setOnClickListener {

            //clear if selected
            clearSelection()

            this@AlarmsLogFragment.context?.let { it1 ->
                ContextCompat.getColor(
                    it1, R.color.green2
                )
            }?.let { it2 -> it.setBackgroundColor(it2) }

            this@AlarmsLogFragment.context?.let { it1 ->
                ContextCompat.getColor(
                    it1, R.color.white
                )
            }?.let { it2 -> (it as Button).setTextColor(it2) }

            btnFilterSystem?.isEnabled = false
            btnFilterDateTime?.isEnabled = false
            btnCsv?.visibility = View.GONE

            openSortByType(SORT_BY_DATETIME_KEY, SORT_PICK_DATE_TIME_REQUEST_CODE)
        }

        tvReset = view.findViewById(R.id.tvReset)
        if (tvReset != null) {
            Linkify.addLinks(tvReset!!, Linkify.WEB_URLS)
        }
        tvReset//tvReset?.movementMethod = LinkMovementMethod.getInstance()
            ?.setOnClickListener {
                typeOfSorted = NO_SORTED
                refreshAlarmsFromPref()
            }
        typeOfSorted = NO_SORTED


        // Inflate the layout for this fragment
        return view
    }


    override fun onStart() {
        super.onStart()

        setFilter()

        initAlarmsAdapter()

        refreshAlarmsFromPref()

    }

    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(usbReceiver)
    }

    private fun refreshAlarmsFromPref() {
        myAlarms = ArrayList()

        myAlarms = populateAlarmsFromLocally()
        //_alarms?.let { myAlarms?.addAll(it) }


        myAlarms?.let { myAlarms ->
            this.myAlarms = ArrayList(myAlarms.sortedWith(compareByDescending { it.timeInMillis }))
            //myAlarms?.let { alarms?.addAll(it) }

            when (typeOfSorted) {
                DATE_SORTED -> {
                    sortByDateAlarm()
                    alarmAdapter?.setDetects(mySortedAlarms)
                    alarmAdapter?.notifyDataSetChanged()
                }

                CAMERA_SORTED -> {
                    sortByCamerasAlarm()
                    alarmAdapter?.setDetects(mySortedAlarms)
                    alarmAdapter?.notifyDataSetChanged()
                }

                else -> {
                    alarmAdapter?.setDetects(this.myAlarms)
                    alarmAdapter?.notifyDataSetChanged()
                }
            }
        }
    }

    //get the alarms from locally
    private fun populateAlarmsFromLocally(): ArrayList<Alarm>? {
        val alarms: ArrayList<Alarm>?
        val alarmListStr = getStringInPreference(context, ALARM_LIST_KEY_PREF, ERROR_RESP)

        alarms = if (alarmListStr.equals(ERROR_RESP)) {
            ArrayList()
        } else {
            alarmListStr?.let { convertJsonToAlarmList(it) }
        }
        return alarms
    }

    //store the detectors to locally
    private fun storeAlarmsToLocally(alarms: ArrayList<Alarm>) {
        // sort the list of events by date in descending
        val alarms = ArrayList(alarms.sortedWith(compareByDescending { it.timeInMillis }))
        if (alarms != null) {
            val alarmsJsonStr = convertToAlarmsGson(alarms)
            setStringInPreference(activity, ALARM_LIST_KEY_PREF, alarmsJsonStr)
        }
    }


    private fun setFilter() {
        val filter = IntentFilter(HANDLE_ALARM_KEY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity?.registerReceiver(usbReceiver, filter, AppCompatActivity.RECEIVER_EXPORTED)
        } else {
            activity?.registerReceiver(usbReceiver, filter)
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, inn: Intent) {
            //accept currentAlarm
            if (inn.action == HANDLE_ALARM_KEY) {
                refreshAlarmsFromPref()
            }
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        //listener?.onFragmentInteraction(uri)
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AlarmLogFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AlarmsLogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    //open fragment dialog to sort the list of alarm log
    private fun openSortByType(type: Int, requestCode: String) {

        val fr = SystemSortDialogFragment()

        //deliver selected camera to continue add data
        //val cameraStr = convertToGson(camera)
        val bdl = Bundle()
        bdl.putInt(SORT_TYPE_KEY, type)
        fr.arguments = bdl
        //fr.setTargetFragment(this, requestCode)
        val fm = parentFragmentManager


        // In the parent fragment
        parentFragmentManager.setFragmentResultListener(
            requestCode,
            viewLifecycleOwner
        ) { key, bundle ->
            val resultCode = bundle.getInt(RESULT_CODE)

            setUIAfterSorting()

            if (requestCode == SORT_BY_SYSTEM_REQUEST_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    val mySysSortStr =
                        bundle.getString(CAMERA_KEY)//intent?.extras?.getString(CAMERA_KEY, null)
                    mySysSortStr?.let {
                        mySortedCameras = convertJsonToSystemSortList(mySysSortStr)
                    }
                    if (mySortedCameras != null) {
                        typeOfSorted = CAMERA_SORTED
                        refreshAlarmsFromPref()
                    }
                }
            } else if (requestCode == SORT_PICK_DATE_TIME_REQUEST_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    //get the start date and end date for sorting
                    try {
                        fromCalendar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            bundle.getSerializable(FROM_CALENDAR, Calendar::class.java)
                        } else {
                            bundle.getSerializable(FROM_CALENDAR) as Calendar
                        }
                        //Bug fixed: remove UTC:
                        // fromCalendar?.add(Calendar.HOUR, HOUR_OFFSET)//to sort UTC
                        toCalendar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            bundle.getSerializable(TO_CALENDAR, Calendar::class.java)
                        } else {
                            bundle.getSerializable(TO_CALENDAR) as Calendar
                        }
                        //Bug fixed: remove UTC:
                        // toCalendar?.add(Calendar.HOUR, HOUR_OFFSET)//to sort UTC
                        //toCalendar?.timeZone=TimeZone.getTimeZone("GMT+3")
                        if (fromCalendar != null && toCalendar != null) {
                            typeOfSorted = DATE_SORTED
                            refreshAlarmsFromPref()
                        }
                        activity?.let { it1 ->
                            getStringFromCalendar(
                                fromCalendar!!,
                                "dd/MM/yy kk:mm:ss",
                                it1
                            )
                        }
                        activity?.let { it1 ->
                            getStringFromCalendar(
                                toCalendar!!,
                                "dd/MM/yy kk:mm:ss",
                                it1
                            )
                        }
                        //Log.d("testCalendar", fromDateStr)
                        //Log.d("testCalendar", toDateStr)
                        //Log.d("testCalendar", getOffsetHour().toString())
                    } catch (ex: Exception) {
                        Toast.makeText(
                            activity,
                            resources.getString(R.string.error),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            }
            // Handle the result
        }

        val fragmentTransaction = fm.beginTransaction()
        fragmentTransaction.add(R.id.flSortBySystemCamera, fr)
        fragmentTransaction.commit()
    }

    /**
     * set UI after sorting
     */
    private fun setUIAfterSorting() {
        btnCsv?.visibility = View.VISIBLE
        //btnDeleteAll?.visibility = View.VISIBLE
        btnFilterSystem?.isEnabled = true
        btnFilterDateTime?.isEnabled = true

        //change the color of the button
        this@AlarmsLogFragment.context?.let { it1 ->
            ContextCompat.getColor(
                it1, R.color.gray11
            )
        }?.let { it2 -> btnFilterSystem?.setBackgroundColor(it2) }


        this@AlarmsLogFragment.context?.let { it1 ->
            ContextCompat.getColor(
                it1, R.color.black
            )
        }?.let { it2 -> (btnFilterSystem as Button).setTextColor(it2) }

        //change the color of the button
        this@AlarmsLogFragment.context?.let { it1 ->
            ContextCompat.getColor(
                it1, R.color.gray11
            )
        }?.let { it2 -> btnFilterDateTime?.setBackgroundColor(it2) }

        this@AlarmsLogFragment.context?.let { it1 ->
            ContextCompat.getColor(
                it1, R.color.black
            )
        }?.let { it2 -> (btnFilterDateTime as Button).setTextColor(it2) }

    }

    //clear the selection of the sorted and normal array
    private fun clearSelection() {
        cbIsSelected?.isChecked = false
        mySortedAlarms?.let { toggleItemSelected(it, false) }
        myAlarms?.let { toggleItemSelected(it, false) }
    }

    //toggle selected/unselected alarms
    private fun toggleItemSelected(alarms: ArrayList<Alarm>, isSelected: Boolean) {
        val iteratorList = alarms.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val item = iteratorList.next()
            item.isReadyToDelete = isSelected
            alarmAdapter?.setDetects(alarms)
            alarmAdapter?.notifyDataSetChanged()
        }
    }

    //sort the alarm by date time
    private fun sortByDateAlarm() {
        if (toCalendar == null || fromCalendar == null) {
            return
        }
        mySortedAlarms = ArrayList()
        val iteratorList = myAlarms?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val item = iteratorList.next()
            if (item.timeInMillis != null
                && item.timeInMillis!! <= toCalendar!!.timeInMillis
                && item.timeInMillis!! >= fromCalendar!!.timeInMillis
            )
                mySortedAlarms?.add(item)
        }
    }

    //sort the alarm by sorter camera
    private fun sortByCamerasAlarm() {
        if (mySortedCameras == null) {
            return
        }

        mySortedAlarms = ArrayList()
        val iteratorList = myAlarms?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val item = iteratorList.next()
            if (isAlarmSorted(item, mySortedCameras))
                mySortedAlarms?.add(item)
        }
    }

    //check if the the alarm is sorted
    private fun isAlarmSorted(
        itemP: Alarm,
        mySystemSort: ArrayList<SystemSort>?
    ): Boolean {

        val iteratorList = mySystemSort?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val item = iteratorList.next()
            if (itemP.name.equals(item.cameraName) && item.isSorted != null && item.isSorted!!) {
                return true
            }
        }
        return false
    }

    //get the counter of selected alarms
    private fun getCountItemSelected(alarms: ArrayList<Alarm>): Int {
        val iteratorList = alarms.listIterator()
        var counter = 0
        while (iteratorList != null && iteratorList.hasNext()) {
            val item = iteratorList.next()
            if (item.isReadyToDelete) {
                counter++
            }

        }
        return counter
    }

    //show dialog before delete alarms
    private fun showDeleteDialog(alarmCounter: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(alarmCounter.toString() + " " + requireContext().resources.getString(R.string.selected_alarms))
        val yes = requireContext().resources.getString(R.string.yes)
        val no = requireContext().resources.getString(R.string.no)
        builder.setMessage(requireContext().resources.getString(R.string.do_you_realy_want_delete_selected_alarm))
            .setCancelable(false)
        builder.setPositiveButton(yes) { dialog, which ->

            //delete from main array and also from sort array
            myAlarms?.let { deleteItemSelected(it) }
            mySortedAlarms?.let { deleteItemSelected(it) }
            //save the changing in shared preference
            if (context != null && myAlarms != null) {
                myAlarms?.let { storeAlarmsToLocally(it, requireContext()) }
            }

            clearSelection()

            refreshAlarmsFromPref()

            dialog.dismiss()
        }


        // Display a negative button on alert dialog
        builder.setNegativeButton(no) { dialog, which ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    //delete the selected alarms
    private fun deleteItemSelected(alarms: ArrayList<Alarm>): Int {
        val iteratorList = alarms.listIterator()
        var counter = 0
        while (iteratorList != null && iteratorList.hasNext()) {
            val item = iteratorList.next()
            if (item.isReadyToDelete) {
                iteratorList.remove()
                counter++
            }

        }
        return counter
    }
}
