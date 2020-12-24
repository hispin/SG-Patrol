package com.sensoguard.detectsensor.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.activities.DownloadOfflineTilesActivity
import com.sensoguard.detectsensor.adapters.GeneralItemMenuAdapter
import com.sensoguard.detectsensor.classes.GeneralItemMenu
import com.sensoguard.detectsensor.classes.LanguageManager
import com.sensoguard.detectsensor.classes.Sensor
import com.sensoguard.detectsensor.global.*
import com.sensoguard.detectsensor.interfaces.CallToParentInterface
import com.sensoguard.detectsensor.interfaces.OnFragmentListener


open class ConfigurationFragment : ParentFragment(), CallToParentInterface {


    private var listPopupWindow: ListPopupWindow? = null
    private var generalItemMenuAdapter: GeneralItemMenuAdapter? = null
    private var etSensorValue: AppCompatEditText? = null
    private var btnSaveSensors: AppCompatButton? = null
    private var togChangeAlarmVibrate: ToggleButton? = null
    private var ibSatelliteMode: AppCompatButton? = null
    private var ibNormalMode: AppCompatButton? = null
    private var etAlarmFlickerValue: AppCompatEditText? = null
    private var btnSaveFlicker: AppCompatButton? = null
    private var constAlarmSound: ConstraintLayout? = null
    private var txtAlarmSoundValue: TextView? = null
    private var togChangeAlarmSound: ToggleButton? = null
    private var btnDefault: AppCompatButton? = null

    //private var ivSelectLanguage: AppCompatImageView?=null
    private var constLangView: ConstraintLayout? = null
    private var languageValue: TextView? = null
    private var listener: OnFragmentListener? = null
    private var btnSaveOffline: AppCompatButton? = null
    private var togIsSensorAlwaysShow: ToggleButton? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnAdapterListener")
        }
    }

    //(activity,ALARM_FLICKERING_DURATION_KEY,ALARM_FLICKERING_DURATION_DEFAULT_VALUE_SECONDS)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_configuration, container, false)

        etSensorValue = view.findViewById(R.id.etSensorValue)
        val currentNumSensors = getCurrentNumSensorsFromLocally()
        if (currentNumSensors != null) {
            etSensorValue?.setText(currentNumSensors.toString())
        }

        btnSaveSensors = view.findViewById(R.id.btnSaveSensors)
        btnSaveSensors?.setOnClickListener {
            addSensors()
        }

        togChangeAlarmVibrate = view.findViewById(R.id.togChangeAlarmVibrate)
        togChangeAlarmVibrate?.isChecked =
            getBooleanInPreference(activity, IS_VIBRATE_WHEN_ALARM_KEY, true)
        togChangeAlarmVibrate?.setOnCheckedChangeListener { buttonView, isChecked ->
            //update the status of the alarm vibrate : on/off
            setBooleanInPreference(activity, IS_VIBRATE_WHEN_ALARM_KEY, isChecked)
        }

        ibSatelliteMode = view.findViewById(R.id.ibSatelliteMode)
        ibSatelliteMode?.setOnClickListener {
            setMapSatellite()
        }

        ibNormalMode = view.findViewById(R.id.ibNormalMode)
        ibNormalMode?.setOnClickListener {
            setMapNormal()
        }
        val mapType = getIntInPreference(activity, MAP_SHOW_VIEW_TYPE_KEY, -1)
        if (mapType == MAP_SHOW_NORMAL_VALUE) {
            setMapNormal()
        } else if (mapType == MAP_SHOW_SATELLITE_VALUE) {
            setMapSatellite()
        }

        etAlarmFlickerValue = view.findViewById(R.id.etAlarmFlickerValue)
        etAlarmFlickerValue?.setText(
            getLongInPreference(
                activity,
                ALARM_FLICKERING_DURATION_KEY,
                -1L
            ).toString()
        )

        btnSaveFlicker = view.findViewById(R.id.btnSaveFlicker)
        //update the time flickering
        btnSaveFlicker?.setOnClickListener {
            try {
                val timeFlicker = etAlarmFlickerValue?.text.toString().toLong()
                setLongInPreference(activity, ALARM_FLICKERING_DURATION_KEY, timeFlicker)
                Toast.makeText(
                    activity,
                    resources.getString(com.sensoguard.detectsensor.R.string.time_flickering_save_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (ex: NumberFormatException) {
            }

        }

        constAlarmSound = view.findViewById(R.id.constAlarmSound)
        constAlarmSound?.setOnClickListener {
            openSoundsMenu()
        }
        txtAlarmSoundValue = view.findViewById(R.id.txtAlarmSoundValue)

        var title = getSelectedNotificationSound()
        txtAlarmSoundValue?.text = title

        togChangeAlarmSound = view.findViewById(R.id.togChangeAlarmSound)
        togChangeAlarmSound?.isChecked =
            getBooleanInPreference(activity, IS_NOTIFICATION_SOUND_KEY, true)
        togChangeAlarmSound?.setOnCheckedChangeListener { buttonView, isChecked ->
            //update the status of the alarm vibrate : on/off
            setBooleanInPreference(activity, IS_NOTIFICATION_SOUND_KEY, isChecked)
        }

        togIsSensorAlwaysShow = view.findViewById(R.id.togIsSensorAlwaysShow)
        togIsSensorAlwaysShow?.isChecked =
            getBooleanInPreference(activity, IS_SENSOR_NAME_ALWAYS_KEY, false)
        togIsSensorAlwaysShow?.setOnCheckedChangeListener { buttonView, isChecked ->
            //update the status of the alarm vibrate : on/off
            setBooleanInPreference(activity, IS_SENSOR_NAME_ALWAYS_KEY, isChecked)
        }

        btnDefault = view.findViewById(R.id.btnDefault)
        btnDefault?.setOnClickListener {
            //return the alarm to default sound
            val packageName = "android.resource://${activity?.packageName}/raw/alarm_sound"
            val uri = Uri.parse(packageName)
            setStringInPreference(activity, SELECTED_NOTIFICATION_SOUND_KEY, uri.toString())
            title = getSelectedNotificationSound()
            txtAlarmSoundValue?.text = title
        }

//        ivSelectLanguage=view.findViewById(R.id.ivSelectLanguage)
//        ivSelectLanguage?.setOnClickListener{
//            showPopupList(it)
//        }

        constLangView = view.findViewById(R.id.constLangView)
        constLangView?.setOnClickListener {
            showPopupList(it)
        }

        languageValue = view.findViewById(R.id.languageValue)

        btnSaveOffline = view.findViewById(R.id.btnSaveOffline)
        btnSaveOffline?.setOnClickListener {
            startActivity(
                Intent(
                    requireActivity(),
                    DownloadOfflineTilesActivity::class.java
                )
            )
        }


        return view
    }

    override fun onStart() {
        super.onStart()
        //set the current language (or selected language or language of device)
        val generalItemMenu = LanguageManager.getCurrentLang(GeneralItemMenu.selectedItem)
        if (generalItemMenu != null) {
            showCurrentLanguage(generalItemMenu)
        }
    }

    //get selected notification sound from locally
    private fun getSelectedNotificationSound(): String? {
        val selectedSound =getStringInPreference(activity,SELECTED_NOTIFICATION_SOUND_KEY,"-1")
        if(!selectedSound.equals("-1")) {
            val uri=Uri.parse(selectedSound)
            uri?.let {
                val ringtone = RingtoneManager.getRingtone(activity, uri)
                return ringtone.getTitle(activity)
            }
        }
        return null
    }

    //open menu of notification sounds
    private fun openSoundsMenu(){
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
        this.startActivityForResult(intent, 5)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == 5) {
            val uri = intent!!.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(activity, uri)
                val title = ringtone.getTitle(activity)
                txtAlarmSoundValue?.text = title
                setStringInPreference(activity, SELECTED_NOTIFICATION_SOUND_KEY, uri.toString())
            } else {
                txtAlarmSoundValue?.text = resources.getString(com.sensoguard.detectsensor.R.string.no_selected_sound)
            }
        }
    }

    private fun setMapSatellite() {
        ibNormalMode?.isEnabled = true
        ibSatelliteMode?.isEnabled = false
        setIntInPreference(activity,MAP_SHOW_VIEW_TYPE_KEY, MAP_SHOW_SATELLITE_VALUE)
    }


    private fun setMapNormal() {
        ibNormalMode?.isEnabled = false
        ibSatelliteMode?.isEnabled = true
        setIntInPreference(activity,MAP_SHOW_VIEW_TYPE_KEY, MAP_SHOW_NORMAL_VALUE)
    }

    //get the current size of sensors
    private fun getCurrentNumSensorsFromLocally(): Int? {
        val sensors= activity?.let { getSensorsFromLocally(it) }
        return sensors?.size
    }


    //add sensors according to the number that get from user
    private fun addSensors(){

        var numSensorsRequest:Int?=null

        val sensors= activity?.let { getSensorsFromLocally(it) }

        //check if id is already exist
        fun isIdExist(sensorsArr:ArrayList<Sensor>, id:String):Boolean{
            for(item in sensorsArr){
                if(item.getId() == id){
                    return true
                }
            }
            return false
        }

        //ask before delete extra sensors
        fun askBeforeDeleteExtraSensor() {
            val dialog= AlertDialog.Builder(activity)
                //set message, title, and icon
                .setTitle(activity?.resources?.getString(com.sensoguard.detectsensor.R.string.remove_extra_sensors)).setMessage(activity?.resources?.getString(
                    com.sensoguard.detectsensor.R.string.content_delete_extra_sensor)).setIcon(android.R.drawable.ic_menu_delete

                )

                .setPositiveButton(activity?.resources?.getString(com.sensoguard.detectsensor.R.string.yes)) { dialog, _ ->

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
                            }catch(ex:NumberFormatException){
                                //do nothing
                            }
                        }
                        Toast.makeText(activity,resources.getString(R.string.sensors_save_successfully),Toast.LENGTH_SHORT).show()
                    }

                    sensors?.let { sen -> storeSensorsToLocally(sen, requireActivity()) }
                    dialog.dismiss()
                }


                .setNegativeButton(activity?.resources?.getString(com.sensoguard.detectsensor.R.string.no)) {
                        dialog, _ -> dialog.dismiss() }.create()
            dialog.show()

        }


        try{
            numSensorsRequest=etSensorValue?.text.toString().toInt()
        }catch (ex: NumberFormatException){
            Toast.makeText(this.context, "exception ${ex.message}", Toast.LENGTH_LONG).show()
            return
        }

        if(numSensorsRequest!=null
            && numSensorsRequest >254){
            Toast.makeText(this.context, resources.getString(com.sensoguard.detectsensor.R.string.invalid_mum_sensors), Toast.LENGTH_LONG).show()
            return
        }


        if(numSensorsRequest!=null) {
            //add numSensors sensors
            for (sensorId in 1 until numSensorsRequest + 1) {
                //add it just if not exist
                if (sensors?.let { it1 -> !isIdExist(it1, sensorId.toString()) }!!) {
                    sensors.add(Sensor(sensorId.toString()))
                }
            }
        }

        //check if the request of sensors number is smaller then the number of exist
        if(sensors?.size!=null
            && numSensorsRequest!=null
            && numSensorsRequest < sensors.size){
            askBeforeDeleteExtraSensor()
        }else if(activity!=null) {
            sensors?.let { sen -> storeSensorsToLocally(sen, requireActivity()) }
            Toast.makeText(activity,resources.getString(R.string.sensors_save_successfully),Toast.LENGTH_SHORT).show()
        }

    }

    private fun showPopupList(anchorView: View) {
        if (LanguageManager.languagesItems != null && LanguageManager.languagesItems.size > 0) {
            generalItemMenuAdapter = GeneralItemMenuAdapter(
                activity,
                R.layout.row_general_item_menu,
                createLanguagesItemsDeliver(LanguageManager.languagesItems),
                this
            )
            listPopupWindow = context?.let { ListPopupWindow(it) }
            listPopupWindow?.isModal = true
            listPopupWindow?.animationStyle = R.style.winPopupAnimation
            listPopupWindow?.setAdapter(generalItemMenuAdapter)
            listPopupWindow?.anchorView = anchorView
            listPopupWindow?.width = getScreenWidth(activity) * 2 / 3
            listPopupWindow?.show()
        } else {
            Toast.makeText(activity,resources.getString(R.string.error),Toast.LENGTH_SHORT).show()
        }
    }

    //To prevent two identical list with the same address and depends on each other
    private fun createLanguagesItemsDeliver(items: java.util.ArrayList<GeneralItemMenu>): java.util.ArrayList<GeneralItemMenu> {
        val newItems = java.util.ArrayList<GeneralItemMenu>()
        newItems.addAll(items)
        return newItems
    }

    //select language for app
    override fun selectedItem(item: Any) {
        setStringInPreference(
            activity,
            CURRENT_LANG_KEY_PREF,
            GeneralItemMenu.selectedItem
        )
        if (item is GeneralItemMenu) {
            if (listPopupWindow != null && listPopupWindow!!.isShowing) {
                listPopupWindow?.dismiss()
                if (item != null) {
                    showCurrentLanguage(item)
                }
                listener?.updateLanguage()

            }
        }
    }
    //show current language in menu
    private fun showCurrentLanguage(generalItemMenu: GeneralItemMenu?) {
        if (generalItemMenu != null) {
            languageValue?.text = generalItemMenu.title
//            ibLangSelect.setImageDrawable(
//                ContextCompat.getDrawable(
//                    getMyActivity(),
//                    generalItemMenu.iconLarge
//                )
//            )
        }
    }
}