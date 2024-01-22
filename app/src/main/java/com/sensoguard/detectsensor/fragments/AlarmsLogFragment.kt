package com.sensoguard.detectsensor.fragments

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.sensoguard.detectsensor.global.ALARM_LIST_KEY_PREF
import com.sensoguard.detectsensor.global.ERROR_RESP
import com.sensoguard.detectsensor.global.HANDLE_ALARM_KEY
import com.sensoguard.detectsensor.global.alarmsListToCsvFile
import com.sensoguard.detectsensor.global.convertJsonToAlarmList
import com.sensoguard.detectsensor.global.convertToAlarmsGson
import com.sensoguard.detectsensor.global.getStringInPreference
import com.sensoguard.detectsensor.global.setStringInPreference
import com.sensoguard.detectsensor.global.shareCsv
import com.sensoguard.detectsensor.global.writeCsvFile
import com.sensoguard.detectsensor.interfaces.OnAdapterListener

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
    private var alarms: ArrayList<Alarm>? = null
    private var rvAlarm: RecyclerView? = null
    private var alarmAdapter: AlarmAdapter? = null
    private var btnCsv: Button? = null
    private var btnDeleteAll: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }


    private fun initAlarmsAdapter() {
        alarms = ArrayList()
        //alarms?.add(Alarm("ID", "NAME", "TYPE", "TIME", false, -1))
        val itemDecorator = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
        rvAlarm?.addItemDecoration(itemDecorator)

        alarmAdapter = activity?.let { adapter ->
            alarms?.let { arr ->
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
            val alarms = populateAlarmsFromLocally()
            //val csvFile=CsvFile()
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
        btnDeleteAll = view.findViewById(R.id.btnDeleteAll)
        btnDeleteAll?.setOnClickListener {

            showDeleteDialog()

        }

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
        alarms = ArrayList()

        val _alarms = populateAlarmsFromLocally()
        _alarms?.let { alarms?.addAll(it) }

        alarmAdapter?.setDetects(alarms)
        alarmAdapter?.notifyDataSetChanged()
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

    //show dialog before delete alarms log
    private fun showDeleteDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(requireContext().resources.getString(R.string.delete_all))
        val yes = requireContext().resources.getString(R.string.yes)
        val no = requireContext().resources.getString(R.string.no)
        builder.setMessage(requireContext().resources.getString(R.string.do_you_really_want_delete_all_alarm))
            .setCancelable(false)
        builder.setPositiveButton(yes) { dialog, which ->

            //remove all alarms log
            alarms = populateAlarmsFromLocally()
            alarms?.clear()
            alarms?.let { alarms -> storeAlarmsToLocally(alarms) }
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


    private fun setFilter() {
        val filter = IntentFilter(HANDLE_ALARM_KEY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity?.registerReceiver(usbReceiver, filter, AppCompatActivity.RECEIVER_NOT_EXPORTED)
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
}
