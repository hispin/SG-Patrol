package com.sensoguard.detectsensor.fragments

//import com.sensoguard.detectsensor.services.TimerService
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.adapters.CommandAdapter
import com.sensoguard.detectsensor.classes.Command
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.global.*
import com.sensoguard.detectsensor.services.TimerService
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CommandsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CommandsFragment : DialogFragment() {

    private val TRY_CONNECT_INTERVAL = 2.5f
    private val TRY_CONNECT_MAX = 240

    //timer for screen on during connecting
    private var timeoutKeepScreenOn = 16
    private var currentKeepScreenOn = 0

    //private var isConnected: Boolean=false
    private var statusAwake = NONE_AWAKE

    //private var myCommand: Command? = null
    private var sensorsIds = ArrayList<String>()
    private var spSensorsIds: AppCompatSpinner? = null
    private var commandsAdapter: CommandAdapter? = null
    private var rvCommands: RecyclerView? = null
    private var btnConnect: Button? = null
    private var btnDisconnect: Button? = null
    //private var tvTest: TextView? = null

    var selectedSensor: Sensor? = null

    // Animation
    var animBlink: Animation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.getStringArrayList(SENSORS_IDS) != null) {
                sensorsIds.add(resources.getString(R.string.select_sensor))
                sensorsIds.addAll(it.getStringArrayList(SENSORS_IDS)!!)
            }
            //Log.d("", "")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_commands_dialog, container, false)

        initViews(view)
        // load the animation
        animBlink = AnimationUtils.loadAnimation(
            activity,
            R.anim.flickering
        )

        spSensorsIds = view.findViewById(R.id.spSensorsIds)

        //listener for gender selection
        spSensorsIds?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                p1: View?,
                position: Int,
                p3: Long
            ) {
                activity?.sendBroadcast(Intent(STOP_TIMER))
                val item = parent?.getItemAtPosition(position) as String
                getSelectedSensor(item)
                refreshCommandsAdapter()
            }
        }



        if (spSensorsIds != null) {
            val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
                requireActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                sensorsIds
            )
            spSensorsIds?.adapter = adapter

            // Spinner click listener
            //spinner.setOnItemSelectedListener(this)
        }

        return view
    }

    //get the sensor by the selected item
    private fun getSelectedSensor(id: String) {
        val sensors = ArrayList<Sensor>()
        //sensors?.add(Sensor(resources.getString(R.string.id_title),resources.getString(R.string.name_title)))
        val sensorsListStr = getStringInPreference(activity, DETECTORS_LIST_KEY_PREF, ERROR_RESP)

        if (sensorsListStr.equals(ERROR_RESP)) {
            //ArrayList()
        } else {
            sensorsListStr?.let {
                val temp = convertJsonToSensorList(it)
                temp?.let { tmp -> sensors.addAll(tmp) }
            }
        }

        val items = sensors.listIterator()
        while (items.hasNext()) {
            val item = items.next()
            if (item.getId() == id) {
                selectedSensor = item
            }
        }
    }

    //hag
    var count = 0

    private fun initViews(view: View?) {
        //tvTest = view?.findViewById(R.id.tvTest)
        rvCommands = view?.findViewById(R.id.rvCommands)
        btnConnect = view?.findViewById(R.id.btnConnect)
        btnConnect?.setOnClickListener {
            //hag
            //activity?.sendBroadcast(Intent(STOP_READ_DATA_KEY))
            if (statusAwake == NONE_AWAKE) {

                val isConnected = getBooleanInPreference(activity, USB_DEVICE_CONNECT_STATUS, false)
                //if the usb is connected then open dialog of commands
                //hag
                if (isConnected) {
                    //max timer in seconds
                    count = 0
                    //start timer every 2.5 second and stop after 240 seconds
                    sendSetRefTimer(TRY_CONNECT_INTERVAL, TRY_CONNECT_MAX, WAIT_AWAKE, true)
                } else {
                    showToast(activity, resources.getString(R.string.usb_is_disconnect))
                }
            }
        }
        btnDisconnect = view?.findViewById(R.id.btnDisconnect)
        btnDisconnect?.setOnClickListener {
            stopConnection()
        }
    }

    // send set ref timer command
    private fun sendSetRefTimer(
        timerValue: Float,
        maxTimeout: Int,
        status: Int,
        isSendFirstCommand: Boolean
    ) {

        if (spSensorsIds?.selectedItem.toString() == resources.getString(R.string.select_sensor)) {
            showToast(activity, resources.getString(R.string.no_selected_sensor))
        } else {
            try {
                val id = Integer.parseInt(spSensorsIds?.selectedItem.toString())
                val cmdSetRefTimer: IntArray = intArrayOf(2, id, SET_RF_ON_TIMER, 7, 120, 0, 3)
                UserSession.instance.myCommand = Command(
                    resources.getString(R.string.set_ref_timer),
                    cmdSetRefTimer,
                    R.drawable.ic_parameters
                )
                UserSession.instance.myCommand?.maxTimeout = maxTimeout

                //start timer
                startTimerService(true, timerValue, maxTimeout)

                if (UserSession.instance.myCommand != null && isSendFirstCommand) {
                    sendCommand(UserSession.instance.myCommand!!)
                    //after command do not need to send command immediately
                } else if (UserSession.instance.myCommand != null && !isSendFirstCommand) {
                    UserSession.instance.commandContent =
                        UserSession.instance.myCommand?.commandContent!!
                }

                statusAwake = status

                if (statusAwake == WAIT_AWAKE) {
                    btnDisconnect?.visibility = View.VISIBLE
                    setUITryConnect()
                    showToast(requireActivity(), "start screen on")
                    keepScreenOn()
                    //requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                } else if (statusAwake == OK_AWAKE) {
                    setUIConnected()

                }

                //isConnected=true

            } catch (ex: NumberFormatException) {
                showToast(activity, ex.message.toString())
            }
        }

    }

    /**
     * set button as connect
     */
    private fun setUIAsConnect() {
        btnConnect?.text = resources.getString(R.string.connect)
        if (activity != null) {
            btnConnect?.setTextColor(
                ContextCompat.getColor(
                    requireActivity(),
                    R.color.turquoise_blue
                )
            )
        }
        btnConnect?.clearAnimation()
        animBlink?.cancel()
    }


    /**
     * set button as try to connecting
     */
    private fun setUITryConnect() {
        //set try to connect
        if (activity != null) {
            btnConnect?.setTextColor(
                ContextCompat.getColor(
                    requireActivity(),
                    R.color.red
                )
            )
        }
        btnConnect?.text = resources.getString(R.string.try_connect)
        btnConnect?.startAnimation(animBlink)
    }

    /**
     * set button as connected
     */
    private fun setUIConnected() {
        btnConnect?.text = resources.getString(R.string.connected)
        if (activity != null) {
            btnConnect?.setTextColor(
                ContextCompat.getColor(
                    requireActivity(),
                    R.color.green1
                )
            )
        }
        btnConnect?.clearAnimation()
        animBlink?.cancel()
    }

    //start timer
    fun startTimerService(isRepeated: Boolean, timerValue: Float, maxTimeout: Int?) {
        val intent = Intent(requireContext(), TimerService::class.java)
        intent.putExtra(COMMAND_TYPE, resources.getString(R.string.set_ref_timer))
        intent.putExtra(IS_REPEATED, isRepeated)
        intent.putExtra(TIMER_VALUE, timerValue)
        intent.putExtra(MAX_TIMEOUT, maxTimeout)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.startForegroundService(intent)
        } else {
            activity?.startService(intent)
        }
    }


    override fun onStart() {
        super.onStart()
        setFilter()
        //refreshCommandsAdapter()

        //var state = getStringInPreference(activity, "connState", "-1")
        //tvTest?.text = state
    }

    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(usbReceiver)
        clearScreenOn()
    }

    private fun refreshCommandsAdapter() {

        val commands: ArrayList<Command> = ArrayList()

        //commands of seismic
        if (selectedSensor?.getTypeID() == SEISMIC_TYPE) {

            val cmdGetSens: IntArray = intArrayOf(2, -1, 55, 6, 0, 3)

            commands.add(
                Command(
                    resources.getString(R.string.get_sens_level),
                    cmdGetSens,
                    R.drawable.ic_parameters
                )
            )
            //set sens level
            val cmdSetSens: IntArray = intArrayOf(2, -1, 155, 7, -1, -1, 3)

            commands.add(
                Command(
                    resources.getString(R.string.set_sens_level),
                    cmdSetSens,
                    R.drawable.ic_parameters
                )
            )


            val cmdSetSystemTime: IntArray = configureSystemTimeCmd()
            commands.add(
                Command(
                    resources.getString(R.string.set_system_time),
                    cmdSetSystemTime,
                    R.drawable.ic_parameters
                )
            )
        }
        commandsAdapter = CommandAdapter(commands, requireContext()) { command: Command ->


            val isConnected = getBooleanInPreference(activity, USB_DEVICE_CONNECT_STATUS, false)

            //if the usb is connected then open dialog of commands
            if (isConnected) {

                //check if sensor has been responded and it has been awake
                if (statusAwake != OK_AWAKE) {
                    showToast(activity, resources.getString(R.string.no_response_sensor))
                    return@CommandAdapter
                }

                //check if sensor has been responded and it has been awake
                if (UserSession.instance.myCommand?.state == PROCESS_STATE) {
                    showToast(activity, resources.getString(R.string.another_process))
                    return@CommandAdapter
                }

                //check if select sensor
                if (spSensorsIds?.selectedItem.toString() == resources.getString(R.string.select_sensor)) {
                    showToast(activity, resources.getString(R.string.no_selected_sensor))
                } else {
                    UserSession.instance.myCommand = command
                    if (command.commandContent?.size != null
                        && command.commandContent.size > 1
                    ) {

                        //start timer
                        startTimerService(false, 4f, -1)


                        //set the sensor id
                        command.commandContent[1] =
                            Integer.parseInt(spSensorsIds?.selectedItem.toString())

                        //start progress bar
                        UserSession.instance.myCommand?.state = PROCESS_STATE
                        commandsAdapter?.notifyDataSetChanged()


                        sendCommand(command)
                    }
                }
            } else {
                showToast(activity, resources.getString(R.string.usb_is_disconnect))
            }

        }

        rvCommands?.adapter = commandsAdapter
        val layoutManager = LinearLayoutManager(activity)
        rvCommands?.layoutManager = layoutManager

        commandsAdapter?.notifyDataSetChanged()

    }

    /**
     * configure the system time command
     */
    private fun configureSystemTimeCmd(): IntArray {
        val now = Calendar.getInstance()

        val dayInWeekS = now.get(Calendar.DAY_OF_WEEK).toString()
        val dayInMonthS = now.get(Calendar.DAY_OF_MONTH).toString()
        var monthN = now.get(Calendar.MONTH)
        //the month is start with zero
        monthN++
        val monthS = monthN.toString()
        val yearS = now.get(Calendar.YEAR).toString()
        val hourS = now.get(Calendar.HOUR_OF_DAY).toString()
        val minutesS = now.get(Calendar.MINUTE).toString()
        val secondsS = now.get(Calendar.SECOND).toString()

        val dayInWeek = dayInWeekS.toInt(16)
        val dayInMonth = dayInMonthS.toInt(16)
        val month = monthS.toInt(16)
        val year = yearS.substring(yearS.length - 3).toInt(16)
        val hour = hourS.toInt(16)
        val minutes = minutesS.toInt(16)
        val seconds = secondsS.toInt(16)


        return intArrayOf(
            2,
            -1,
            103,
            12,
            dayInWeek,
            dayInMonth,
            month,
            year,
            hour,
            minutes,
            seconds,
            3
        )
    }

    //send rf timer command immediately after other command
    private fun sendRfCmd() {
        val id = Integer.parseInt(spSensorsIds?.selectedItem.toString())
        val cmdSetRefTimer: IntArray = intArrayOf(2, id, SET_RF_ON_TIMER, 7, 45, 0, 3)
        val command = Command(
            resources.getString(R.string.set_ref_timer),
            cmdSetRefTimer,
            R.drawable.ic_parameters
        )
        sendCommand(command)
    }

    //sen command to sensor
    private fun sendCommand(command: Command) {

        UserSession.instance.commandContent = command.commandContent

        activity?.sendBroadcast(Intent(ACTION_SEND_CMD))
    }


    fun collapseExpandTextView(view: View) {
        if (view.visibility == View.GONE) {
            // it's collapsed - expand it
            view.visibility = View.VISIBLE
        } else {
            // it's expanded - collapse it
            view.visibility = View.GONE
        }
    }

    private fun setFilter() {
        val filter = IntentFilter("handle.read.data")
        filter.addAction(ACTION_USB_RESPONSE_CACHE)
        filter.addAction(ACTION_INTERVAL)
        filter.addAction(MAX_TIMER_RESPONSE)
        filter.addAction("test.brod")
        filter.addAction(STOP_READ_DATA_KEY)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity?.registerReceiver(usbReceiver, filter, AppCompatActivity.RECEIVER_NOT_EXPORTED)
        } else {
            activity?.registerReceiver(usbReceiver, filter)
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, inn: Intent) {
            if (inn.action == ACTION_USB_RESPONSE_CACHE) {
                val arr = inn.getIntegerArrayListExtra(USB_CACHE_RESPONSE_KEY)

                //response of get sens command
                if (arr != null && arr.size > 5 && arr[2].toUByte().toInt() == GET_SENS_LEVEL) {
                    if (UserSession.instance.myCommand?.state == PROCESS_STATE) {
                        //stop progress bar
                        UserSession.instance.myCommand?.state = SUCCESS_STATE
                        commandsAdapter?.notifyDataSetChanged()
                        showResponseInDialog(arr[4], arr[5])
                    }
                    //response of set sens command
                } else if (arr != null && arr.size == 7 && arr[2].toUByte()
                        .toInt() == SET_SENS_LEVEL
                ) {
                    if (UserSession.instance.myCommand?.state == PROCESS_STATE) {
                        //stop progress bar
                        UserSession.instance.myCommand?.state = SUCCESS_STATE
                        commandsAdapter?.notifyDataSetChanged()
                    }
                    //accept response from command of RF timer
                } else if (arr != null && arr.size == 7 && arr[2].toUByte()
                        .toInt() == SET_RF_ON_TIMER
                //&& arr[4].toUByte().toInt() == 1
                ) {
                    statusAwake = OK_AWAKE

                    setUIConnected()
                    startTimerService(true, 20f, 100)

                } else if (arr != null && arr.size == 7 && arr[2].toUByte()
                        .toInt() == SET_TIME_SYSTEM
                ) {
                    if (UserSession.instance.myCommand?.state == PROCESS_STATE) {
                        //stop progress bar
                        UserSession.instance.myCommand?.state = SUCCESS_STATE
                        commandsAdapter?.notifyDataSetChanged()
                    }
                    //accept response from command of RF timer
                }
                //time out (no max)
            } else if (inn.action == ACTION_INTERVAL) {
                val commandType = inn.getStringExtra(COMMAND_TYPE)

                //if the interval is belong to the command "set ref timer"
                //do nothing ,ServiceConnectSensor also handle it
                if (commandType != null && commandType == resources.getString(R.string.set_ref_timer)) {

                    //showShortToast(activity, "interval")
                    //do nothing ,ServiceConnectSensor also handle it

                    //accept interval after other commands (timeout, not repeated)
                } else if (UserSession.instance.myCommand?.state == PROCESS_STATE) {
                    //after command
                    //stop progress bar
                    UserSession.instance.myCommand?.state = TIMEOUT_STATE
                    commandsAdapter?.notifyDataSetChanged()
                    //showToast(activity, "timeout")

                    //if the sensor awake then renew the normal Timer RF commands timer
                    if (statusAwake == OK_AWAKE) {
                        sendSetRefTimer(20f, 100, OK_AWAKE, false)
                    }
                } else {
                    //renew the normal Timer RF commands timer
                    if (statusAwake == OK_AWAKE) {
                        sendSetRefTimer(20f, 100, OK_AWAKE, false)
                    }
                }
            } else if (inn.action == MAX_TIMER_RESPONSE) {
                stopConnection()
            } else if (inn.action == STOP_READ_DATA_KEY) {
                stopConnection()
            } else if (inn.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                stopConnection()

            }


            //hag test
//            else if (inn.action == "test.brod") {
//                   //count++
//                   val s=  inn.getIntExtra("size", -1)
//                   val appcode=  inn.getIntExtra("appcode", -1)
//
//                   showToast(activity, "size=$s appcode=$appcode")
//            }
        }
    }

    /**
     * stop connection
     */
    private fun stopConnection() {
        statusAwake = NONE_AWAKE
        btnDisconnect?.visibility = View.GONE
        activity?.sendBroadcast(Intent(STOP_TIMER))
        setUIAsConnect()
        clearScreenOn()
    }

    //show dialog to show response
    private fun showResponseInDialog(carV: Int, intruderV: Int) {

        if (this@CommandsFragment.context != null) {
            val dialog = Dialog(this@CommandsFragment.requireContext())
            dialog.setContentView(R.layout.dialog_command_response)

            dialog.setCancelable(true)


            val tvCarValue = dialog.findViewById<AppCompatTextView>(R.id.tvCarValue)
                val tvIntruderValue = dialog.findViewById<AppCompatTextView>(R.id.tvIntruderValue)
                tvCarValue.text = carV.toString()
                tvIntruderValue.text = intruderV.toString()

                val btnClose = dialog.findViewById<AppCompatButton>(R.id.btnClose)
                btnClose.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.show()
            }
        }

    fun keepScreenOn(rootView: View? = null) {
        requireActivity().window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        rootView?.keepScreenOn = true
    }

    fun clearScreenOn() {
        requireActivity().window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

}
