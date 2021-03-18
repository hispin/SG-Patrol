package com.sensoguard.detectsensor.fragments

//import com.sensoguard.detectsensor.services.TimerService
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.adapters.CommandAdapter
import com.sensoguard.detectsensor.classes.Command
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

    //private var isConnected: Boolean=false
    private var statusAwak = NONE_AWAKE
    private var myCommand: Command? = null
    private var sensorsIds = ArrayList<String>()
    private var spSensorsIds: AppCompatSpinner? = null
    private var commandsAdapter: CommandAdapter? = null
    private var rvCommands: RecyclerView? = null
    private var btnConnect: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.getStringArrayList(SENSORS_IDS) != null) {
                sensorsIds.add(resources.getString(R.string.select_sensor))
                sensorsIds.addAll(it.getStringArrayList(SENSORS_IDS)!!)
            }
            Log.d("", "")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_commands_dialog, container, false)
        initViews(view)
        spSensorsIds = view.findViewById(R.id.spSensorsIds)

        //listener for gender selection
        spSensorsIds?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                activity?.sendBroadcast(Intent(STOP_TIMER))
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

    private fun initViews(view: View?) {
        rvCommands = view?.findViewById(R.id.rvCommands)
        btnConnect = view?.findViewById(R.id.btnConnect)
        btnConnect?.setOnClickListener {
            if (statusAwak == NONE_AWAKE) {

                val isConnected = getBooleanInPreference(activity, USB_DEVICE_CONNECT_STATUS, false)
                //if the usb is connected then open dialog of commands
                if (isConnected) {
                    sendSetRefTimer(3, 30, WAIT_AWAKE)
                } else {
                    showToast(activity, resources.getString(R.string.usb_is_disconnect))
                }


            }
        }
    }

    private fun sendSetRefTimer(timerValue: Int, maxTimeout: Int, status: Int) {

        if (spSensorsIds?.selectedItem.toString() == resources.getString(R.string.select_sensor)) {
            showToast(activity, resources.getString(R.string.no_selected_sensor))
        } else {
            try {
                val id = Integer.parseInt(spSensorsIds?.selectedItem.toString())
                val cmdSetRefTimer: IntArray = intArrayOf(2, id, SET_RF_ON_TIMER, 7, 45, 0, 3)
                myCommand = Command(
                    resources.getString(R.string.set_ref_timer),
                    cmdSetRefTimer,
                    R.drawable.ic_parameters
                )
                myCommand?.maxTimeout = 240

                //start timer every 3 second and stop after 30 seconds
                startTimerService(true, timerValue, maxTimeout)

                if (myCommand != null) {
                    sendCommand(myCommand!!)
                }

                btnConnect?.text = resources.getString(R.string.try_connect)
                statusAwak = status
                //isConnected=true

            } catch (ex: NumberFormatException) {
                showToast(activity, ex.message.toString())
            }
        }

    }

    //start timer
    fun startTimerService(isRepeated: Boolean, timerValue: Int, maxTimeout: Int?) {
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
        refreshCommandsAdapter()
    }

    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(usbReceiver)
    }

    private fun refreshCommandsAdapter() {

        val commands: ArrayList<Command> = ArrayList()

        val cmdGetSens: IntArray = intArrayOf(2, -1, 55, 6, 0, 3)

        commands.add(
            Command(
                resources.getString(R.string.get_sens_level),
                cmdGetSens,
                R.drawable.ic_parameters
            )
        )

        val cmdSetSens: IntArray = intArrayOf(2, -1, 155, 7, -1, -1, 3)

        commands.add(
            Command(
                resources.getString(R.string.set_sens_level),
                cmdSetSens,
                R.drawable.ic_parameters
            )
        )

        commandsAdapter = CommandAdapter(commands, requireContext()) { command: Command ->


            val isConnected = getBooleanInPreference(activity, USB_DEVICE_CONNECT_STATUS, false)

            //if the usb is connected then open dialog of commands
            if (isConnected) {

                //check if sensor has been responded
                if (statusAwak != OK_AWAKE) {
                    showToast(activity, resources.getString(R.string.no_response_sensor))
                    return@CommandAdapter
                }

                //check if select sensor
                if (spSensorsIds?.selectedItem.toString() == resources.getString(R.string.select_sensor)) {
                    showToast(activity, resources.getString(R.string.no_selected_sensor))
                } else {
                    myCommand = command
                    if (command.commandContent?.size != null
                        && command.commandContent.size > 1
                    ) {

                        //start timer
                        startTimerService(false, 4, -1)

//                        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
//                            override fun run() {
//                                sendRfCmd()
//                            }
//                        },1000)

                        //set the sensor id
                        command.commandContent[1] =
                            Integer.parseInt(spSensorsIds?.selectedItem.toString())

                        //start progress bar
                        myCommand?.state = PROCESS_STATE
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
        val inn = Intent(ACTION_SEND_CMD)
        val bnd = Bundle()
        bnd.putIntArray(CURRENT_COMMAND, command.commandContent)
        inn.putExtras(bnd)
        activity?.sendBroadcast(inn)
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
        filter.addAction(ACTION_TIME_OUT)
        filter.addAction(MAX_TIMER_RESPONSE)
        activity?.registerReceiver(usbReceiver, filter)
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, inn: Intent) {
            if (inn.action == ACTION_USB_RESPONSE_CACHE) {
                val arr = inn.getIntegerArrayListExtra(USB_CACHE_RESPONSE_KEY)

                //response of get sens command
                if (arr != null && arr.size > 5 && arr[2].toUByte().toInt() == GET_SENS_LEVEL) {
                    if (myCommand?.state == PROCESS_STATE) {
                        //stop progress bar
                        myCommand?.state = SUCCESS_STATE
                        commandsAdapter?.notifyDataSetChanged()
                        showResponseInDialog(arr[4], arr[5])
                    }
                    //response of set sens command
                } else if (arr != null && arr.size == 7 && arr[2].toUByte()
                        .toInt() == SET_SENS_LEVEL
                ) {
                    if (myCommand?.state == PROCESS_STATE) {
                        //stop progress bar
                        myCommand?.state = SUCCESS_STATE
                        commandsAdapter?.notifyDataSetChanged()
                    }
                    //accept response from command of RF timer
                } else if (arr != null && arr.size == 7 && arr[2].toUByte()
                        .toInt() == SET_RF_ON_TIMER
                ) {
                    statusAwak = OK_AWAKE
                    btnConnect?.text = resources.getString(R.string.disconnect)
                    startTimerService(true, 20, -1)


                    //time out (no max)
                } else if (inn.action == ACTION_TIME_OUT) {
                    val commandType = inn.getStringExtra(COMMAND_TYPE)

                    //if the command is set ref timer
                    if (commandType.equals(resources.getString(R.string.set_ref_timer))) {

                        //showShortToast(activity, "interval")
                        //send another command
                        if (myCommand?.commandName.equals(resources.getString(R.string.set_ref_timer))) {
                            myCommand?.let { sendCommand(it) }
                        }

                    } else if (myCommand?.state == PROCESS_STATE) {

                        //stop progress bar
                        myCommand?.state = TIMEOUT_STATE
                        commandsAdapter?.notifyDataSetChanged()
                        //showToast(activity, "timeout")

                        //renew the normal Timer RF commands timer
                        if (statusAwak == OK_AWAKE) {
                            //startTimerService(true,20,-1)
                            sendSetRefTimer(20, -1, OK_AWAKE)
                        }
                    } else {
                        //renew the normal Timer RF commands timer
                        if (statusAwak == OK_AWAKE) {
                            sendSetRefTimer(20, -1, OK_AWAKE)
                        }
                    }
                } else if (inn.action == MAX_TIMER_RESPONSE) {
                    btnConnect?.text = resources.getString(R.string.connect)
                    statusAwak = NONE_AWAKE
                }
            }
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

    }
}