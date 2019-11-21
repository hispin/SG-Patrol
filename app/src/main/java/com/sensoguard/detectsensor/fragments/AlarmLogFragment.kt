package com.sensoguard.detectsensor.fragments

//import android.support.v4.app.Fragment

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.adapters.AlarmAdapter
import com.sensoguard.detectsensor.classes.Alarm
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.global.*
import com.sensoguard.detectsensor.interfaces.OnAdapterListener
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [HistoryWarningFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [HistoryWarningFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class HistoryWarningFragment : Fragment(), OnAdapterListener {
    override fun saveDetector(detector: Sensor) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveNameSensor(detector: Sensor) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var alarms:ArrayList<Alarm>?=null
    private var rvAlarm:RecyclerView?=null
    private var alarmAdapter: AlarmAdapter?=null
    private var btnCsv: Button?=null

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
            ALARM_LIST_KEY_PREF -> {
                refreshAlarmsFromPref()
            }
        }
    }

    private fun initAlarmsAdapter() {
        alarms= ArrayList()
        //alarms?.add(Alarm("ID", "NAME", "TYPE", "TIME", false, -1))
        val itemDecorator= DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        itemDecorator.setDrawable(ContextCompat.getDrawable(context!!, R.drawable.divider)!!)
        rvAlarm?.addItemDecoration(itemDecorator)

        alarmAdapter=activity?.let { adapter ->
            alarms?.let { arr ->
                AlarmAdapter(arr, adapter,HistoryWarningFragment@this) { _ ->

                }
            }
        }
        rvAlarm?.adapter=alarmAdapter
        val layoutManager= LinearLayoutManager(activity)
        rvAlarm?.layoutManager=layoutManager

        alarmAdapter?.notifyDataSetChanged()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        var view=inflater.inflate(R.layout.fragment_alarm_log, container, false)

        rvAlarm=view.findViewById(R.id.rvAlarm)

        btnCsv=view.findViewById(R.id.btnCsv)

        btnCsv?.setOnClickListener{
            val alarms=populateAlarmsFromLocally()
            //val csvFile=CsvFile()
            HistoryWarningFragment@this.context?.let {
                    it1 ->

                    val alarmsStr=alarmsListToCsvFile(alarms, it1)
                    if(writeCsvFile(alarmsStr)){
                        activity?.let { it2 -> shareCsv(it2) }
                        //Toast.makeText(context,"success",Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(context,"failed",Toast.LENGTH_SHORT).show()
                    }


            }

        }

        // Inflate the layout for this fragment
        return view
    }


    override fun onStart() {
        super.onStart()

        activity?.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE)?.registerOnSharedPreferenceChangeListener(appStateChangeListener)

        initAlarmsAdapter()

        refreshAlarmsFromPref()

    }

    override fun onStop() {
        super.onStop()
        activity?.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE)?.unregisterOnSharedPreferenceChangeListener(appStateChangeListener)
    }

    private fun refreshAlarmsFromPref(){
        alarms=ArrayList()
        //create title
//        alarms?.add(Alarm(
//            resources.getString(R.string.id_title),
//            resources.getString(R.string.name_title),
//            resources.getString(R.string.type_title),
//            resources.getString(R.string.time_title),
//            false,
//            -1
//        ))
        val _alarms=populateAlarmsFromLocally()
        _alarms?.let { alarms?.addAll(it) }

        alarmAdapter?.setDetects(alarms)
        alarmAdapter?.notifyDataSetChanged()
    }

    //get the alarms from locally
    private fun populateAlarmsFromLocally(): ArrayList<Alarm>?  {
        val alarms: ArrayList<Alarm>?
        val alarmListStr= getStringInPreference(context, ALARM_LIST_KEY_PREF, ERROR_RESP)

        alarms = if(alarmListStr.equals(ERROR_RESP)){
            ArrayList()
        }else {
            alarmListStr?.let { convertJsonToAlarmList(it) }
        }
        return alarms
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
         * @return A new instance of fragment HistoryWarningFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HistoryWarningFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

}
