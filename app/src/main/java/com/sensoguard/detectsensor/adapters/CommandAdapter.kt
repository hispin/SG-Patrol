package com.sensoguard.detectsensor.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.classes.Command
import com.sensoguard.detectsensor.global.NORMAL_STATE
import com.sensoguard.detectsensor.global.PIR_TYPE
import com.sensoguard.detectsensor.global.PROCESS_STATE
import com.sensoguard.detectsensor.global.RADAR_TYPE
import com.sensoguard.detectsensor.global.SEISMIC_TYPE
import com.sensoguard.detectsensor.global.SUCCESS_STATE
import com.sensoguard.detectsensor.global.TIMEOUT_STATE
import com.sensoguard.detectsensor.global.VIBRATION_TYPE
import com.sensoguard.detectsensor.global.showToast

class CommandAdapter(
    private var commands: ArrayList<Command>,
    val context: Context,
    var itemClick: (Command) -> Unit
) : RecyclerView.Adapter<CommandAdapter.ViewHolder>() {

    var myRv: RecyclerView? = null
    var myPos: Int? = null


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        myRv = recyclerView
    }

    override fun onBindViewHolder(holder: CommandAdapter.ViewHolder, position: Int) {
        holder.bindReservation((commands[position]))
        holder.setIsRecyclable(false)
        myPos = position
    }

    override fun getItemCount(): Int {
        return this.commands.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): CommandAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_command, parent, false)


        return ViewHolder(view, itemClick)
    }

    fun setCommands(_detectors: ArrayList<Command>?) {
        _detectors?.let { commands = it }
        //TODO how to define with this
    }

    inner class ViewHolder(
        private val _itemView: View,
        private val itemClick: (Command) -> Unit
    ) :
        RecyclerView.ViewHolder(_itemView) {

        private var tvCommandTitle: TextView? = null
        private var ivIcon: AppCompatImageView? = null
        private var pbTimer: ProgressBar? = null
        private var ivTimeout: ImageView? = null
        private var conExpand: ConstraintLayout? = null
        private var myCardView: CardView? = null
        private var tvSelectCar: TextView? = null
        private var spCarSens: AppCompatSpinner? = null
        private var etCarSnr: AppCompatEditText? = null
        private var tvSelectIntruder: TextView? = null
        private var spIntruderSens: AppCompatSpinner? = null
        private var etIntruderSnr: AppCompatEditText? = null
        private var btnSendCmd: AppCompatButton? = null
        private var llCarLogicParam: LinearLayout? = null
        private var llIntruderLogicParam: LinearLayout? = null
        private var etSeconds: AppCompatEditText? = null
        private var etCarCount: AppCompatEditText? = null
        private var etCarDuration: AppCompatEditText? = null
        private var etIntruderCount: AppCompatEditText? = null
        private var etIntruderDuration: AppCompatEditText? = null


        init {
            itemView.setOnClickListener {

                if (commands[adapterPosition].commandName == context.resources.getString(R.string.set_sens_level)
                    || commands[adapterPosition].commandName == context.resources.getString(R.string.set_snr)
                    || commands[adapterPosition].commandName == context.resources.getString(R.string.set_logic_param)
                    || commands[adapterPosition].commandName == context.resources.getString(R.string.set_min_power)
                ) {
                    commands[adapterPosition].isExpand = !commands[adapterPosition].isExpand
                    //Bug fixed:when expand the command ,zero the car and intruder selection (for update ses command)
                    if (commands[adapterPosition].isExpand
                        && commands[adapterPosition].commandName == context.resources.getString(R.string.set_sens_level)
                    ) {
                        commands[adapterPosition].sensCar = 4
                        commands[adapterPosition].sensIntruder = 4
                    }
                    notifyDataSetChanged()
                } else if (adapterPosition >= 0) {
                    itemClick.invoke(commands[adapterPosition])
                }
            }
        }


        fun bindReservation(command: Command) {
            //tvId = _itemView.findViewById(R.id.tvId)
            tvCommandTitle = _itemView.findViewById(R.id.tvCommandTitle)
            tvCommandTitle?.text = command.commandName
            ivIcon = _itemView.findViewById(R.id.ivIcon)
            pbTimer = _itemView.findViewById(R.id.pbTimer)
            ivTimeout = _itemView.findViewById(R.id.ivTimeout)
            conExpand = _itemView.findViewById(R.id.conExpand)
            myCardView = _itemView.findViewById(R.id.myCardView)

            tvSelectCar = _itemView.findViewById(R.id.tvSelectCar)
            spCarSens = _itemView.findViewById(R.id.spCarSens)
            etCarSnr = _itemView.findViewById(R.id.etCarSnr)
            //Bug fixed:set the last selection as long as the command of update sens is open

            tvSelectIntruder = _itemView.findViewById(R.id.tvSelectIntruder)
            spIntruderSens = _itemView.findViewById(R.id.spIntruderSens)
            etIntruderSnr = _itemView.findViewById(R.id.etIntruderSnr)

            llCarLogicParam = _itemView.findViewById(R.id.llCarLogicParam)
            llIntruderLogicParam = _itemView.findViewById(R.id.llIntruderLogicParam)
            etSeconds = _itemView.findViewById(R.id.etSeconds)
            etCarCount = _itemView.findViewById(R.id.etCarCount)
            etCarDuration = _itemView.findViewById(R.id.etCarDuration)
            etIntruderCount = _itemView.findViewById(R.id.etIntruderCount)
            etIntruderDuration = _itemView.findViewById(R.id.etIntruderDuration)

            //set the last selection as long as the command of update sens is open

            if (commands[adapterPosition].commandName == context.resources.getString(R.string.set_sens_level)) {
                spCarSens?.setSelection(commands[adapterPosition].sensCar)
                spIntruderSens?.setSelection(commands[adapterPosition].sensIntruder)
            }

            btnSendCmd = _itemView.findViewById(R.id.btnSendCmd)


            if (command.icId != -1) {
                ivIcon?.setImageDrawable(
                    ContextCompat.getDrawable(
                        context, command.icId
                    )
                )
            }
            //show progress bar during sending command
            if (command.state == PROCESS_STATE) {
                pbTimer?.visibility = View.VISIBLE
            } else {
                pbTimer?.visibility = View.INVISIBLE
            }
            //show x when time out without response
            if (command.state == TIMEOUT_STATE) {
                ivTimeout?.setBackgroundResource(R.drawable.ic_command_timeout)
                ivTimeout?.visibility = View.VISIBLE
                command.state = NORMAL_STATE
            } else if (command.state == SUCCESS_STATE) {
                ivTimeout?.setBackgroundResource(R.drawable.ic_command_success)
                ivTimeout?.visibility = View.VISIBLE
                command.state = NORMAL_STATE
            } else {
                ivTimeout?.visibility = View.INVISIBLE
            }

            if (command.isExpand) {
                TransitionManager.beginDelayedTransition(myCardView!!, AutoTransition())
                conExpand?.visibility = View.VISIBLE
                when (command.commandName) {
                    context.resources.getString(R.string.set_sens_level) -> {
                        spCarSens?.visibility = View.VISIBLE
                        etCarSnr?.visibility = View.GONE
                        etCarSnr?.focusable = View.NOT_FOCUSABLE
                        etCarSnr?.isFocusableInTouchMode = false
                        spIntruderSens?.visibility = View.VISIBLE
                        etIntruderSnr?.visibility = View.GONE
                        etIntruderSnr?.focusable = View.NOT_FOCUSABLE
                        etIntruderSnr?.isFocusableInTouchMode = false
                        llCarLogicParam?.visibility = View.GONE
                        llIntruderLogicParam?.visibility = View.GONE
                        etSeconds?.visibility = View.GONE
                        etSeconds?.focusable = View.NOT_FOCUSABLE
                        etSeconds?.isFocusableInTouchMode = false
                        tvSelectCar?.text = context.resources.getString(R.string.car)
                        tvSelectIntruder?.text =
                            context.resources.getString(R.string.intruder)
                    }

                    context.resources.getString(R.string.set_snr) -> {
                        spCarSens?.visibility = View.GONE
                        etCarSnr?.visibility = View.VISIBLE
                        etCarSnr?.focusable = View.FOCUSABLE
                        etCarSnr?.isFocusableInTouchMode = true
                        spIntruderSens?.visibility = View.GONE
                        etIntruderSnr?.visibility = View.VISIBLE
                        etIntruderSnr?.focusable = View.FOCUSABLE
                        etIntruderSnr?.isFocusableInTouchMode = true
                        llCarLogicParam?.visibility = View.GONE
                        llIntruderLogicParam?.visibility = View.GONE
                        etSeconds?.visibility = View.GONE
                        etSeconds?.focusable = View.NOT_FOCUSABLE
                        etSeconds?.isFocusableInTouchMode = false
                        tvSelectCar?.text = context.resources.getString(R.string.car)
                        tvSelectIntruder?.text =
                            context.resources.getString(R.string.intruder)

                    }

                    context.resources.getString(R.string.set_logic_param) -> {
                        spCarSens?.visibility = View.GONE
                        etCarSnr?.visibility = View.GONE
                        etCarSnr?.focusable = View.NOT_FOCUSABLE
                        etCarSnr?.isFocusableInTouchMode = false
                        spIntruderSens?.visibility = View.GONE
                        etIntruderSnr?.visibility = View.GONE
                        etIntruderSnr?.focusable = View.NOT_FOCUSABLE
                        etIntruderSnr?.isFocusableInTouchMode = false
                        llIntruderLogicParam?.visibility = View.VISIBLE
                        etSeconds?.visibility = View.VISIBLE
                        etSeconds?.focusable = View.FOCUSABLE
                        etSeconds?.isFocusableInTouchMode = true

                        if (command.sensorType == SEISMIC_TYPE) {
                            llCarLogicParam?.visibility = View.VISIBLE
                            tvSelectCar?.text =
                                context.resources.getString(R.string.car)
                            tvSelectIntruder?.text =
                                context.resources.getString(R.string.intruder)
                        } else if (command.sensorType == RADAR_TYPE
                            || command.sensorType == PIR_TYPE
                            || command.sensorType == VIBRATION_TYPE
                        ) {
                            llCarLogicParam?.visibility = View.GONE
                            tvSelectCar?.text =
                                context.resources.getString(R.string.select_logic_param)
                            tvSelectIntruder?.text = ""
                        }


                    }
                    context.resources.getString(R.string.set_min_power) -> {
                        spCarSens?.visibility = View.GONE
                        etCarSnr?.visibility = View.VISIBLE
                        etCarSnr?.focusable = View.FOCUSABLE
                        etCarSnr?.isFocusableInTouchMode = true
                        spIntruderSens?.visibility = View.GONE
                        etIntruderSnr?.visibility = View.GONE
                        etIntruderSnr?.focusable = View.NOT_FOCUSABLE
                        etIntruderSnr?.isFocusableInTouchMode = false
                        llCarLogicParam?.visibility = View.GONE
                        llIntruderLogicParam?.visibility = View.GONE
                        etSeconds?.visibility = View.GONE
                        etSeconds?.focusable = View.NOT_FOCUSABLE
                        etSeconds?.isFocusableInTouchMode = false
                        tvSelectCar?.text =
                            context.resources.getString(R.string.min_power)
                        tvSelectIntruder?.visibility = View.GONE
                    }
                }
            } else {
                TransitionManager.beginDelayedTransition(myCardView!!, AutoTransition())
                conExpand?.visibility = View.GONE
            }

            btnSendCmd?.setOnClickListener {

                if (myPos != null) {
                    myRv?.post(Runnable { myRv?.smoothScrollToPosition(myPos!! + 1) })
                    //myRv?.scrollToPosition(myPos!! + 1)
                }

                if (command.commandName == context.resources.getString(R.string.set_sens_level)
                    && spCarSens?.selectedItem.toString() == "0"
                    && spIntruderSens?.selectedItem.toString() == "0"
                ) {
                    showToast(context, context.getString(R.string.error_zero_sens))
                } else if (command.commandName == context.resources.getString(R.string.set_snr)
                    && etCarSnr?.text.toString() == "0"
                    && etIntruderSnr?.text.toString() == "0"
                ) {

                } else {
                    when (commands[adapterPosition].commandName) {
                        context.resources.getString(R.string.set_sens_level) -> {
                            commands[adapterPosition].sensCar =
                                spCarSens?.selectedItem.toString().toInt()
                            commands[adapterPosition].sensIntruder =
                                spIntruderSens?.selectedItem.toString().toInt()
                            commands[adapterPosition].commandContent?.set(
                                4,
                                spCarSens?.selectedItem.toString().toInt()
                            )
                            commands[adapterPosition].commandContent?.set(
                                5,
                                spIntruderSens?.selectedItem.toString().toInt()
                            )
                            itemClick.invoke(commands[adapterPosition])
                        }

                        context.resources.getString(R.string.set_snr) -> {

                            if (validIsEmpty(etCarSnr) && validIsEmpty(etIntruderSnr)) {
                                var carSrn = etCarSnr?.text.toString().toInt()
                                var intruderSrn: Float = etIntruderSnr?.text.toString().toFloat()
                                var intruderFirst: Int = intruderSrn.toInt()
                                var intruderSecond: Float = 10 * (intruderSrn - intruderFirst)
                                commands[adapterPosition].snrCar = carSrn
                                commands[adapterPosition].snrIntruder = intruderSrn
                                commands[adapterPosition].commandContent?.set(
                                    4,
                                    intruderFirst
                                )
                                commands[adapterPosition].commandContent?.set(
                                    5,
                                    intruderSecond.toInt()
                                )
                                commands[adapterPosition].commandContent?.set(
                                    6,
                                    carSrn
                                )
                                itemClick.invoke(commands[adapterPosition])
                            }
                        }
                        context.resources.getString(R.string.set_logic_param) -> {
                            if (command.sensorType == SEISMIC_TYPE) {
                                if (validIsEmpty(etCarCount) && validIsEmpty(etIntruderCount)
                                    && validIsEmpty(etCarDuration) && validIsEmpty(
                                        etIntruderDuration
                                    )
                                    && validIsEmpty(etSeconds)
                                ) {
                                    var countCar = etCarCount?.text.toString().toInt()
                                    var countIntruder = etIntruderCount?.text.toString().toInt()
                                    var durationCar = etCarDuration?.text.toString().toInt()
                                    var durationIntruder =
                                        etIntruderDuration?.text.toString().toInt()
                                    var seconds = etSeconds?.text.toString().toInt()
                                    commands[adapterPosition].logicCountCar = countCar
                                    commands[adapterPosition].logicdurationCar = durationCar
                                    commands[adapterPosition].logicCountIntruder = countIntruder
                                    commands[adapterPosition].logicdurationIntruder =
                                        durationIntruder
                                    commands[adapterPosition].logicSecomds = seconds


                                    commands[adapterPosition].commandContent?.set(
                                        4,
                                        countCar
                                    )
                                    commands[adapterPosition].commandContent?.set(
                                        5,
                                        durationCar
                                    )
                                    commands[adapterPosition].commandContent?.set(
                                        6,
                                        countIntruder
                                    )
                                    commands[adapterPosition].commandContent?.set(
                                        7,
                                        durationIntruder
                                    )
                                    commands[adapterPosition].commandContent?.set(
                                        8,
                                        seconds
                                    )
                                    itemClick.invoke(commands[adapterPosition])
                                }
                            } else if (command.sensorType == RADAR_TYPE
                                || command.sensorType == PIR_TYPE
                            ) {

                                if (validIsEmpty(etSeconds) && validIsEmpty(etIntruderCount)
                                    && validIsEmpty(etIntruderDuration)
                                ) {

                                    var seconds = etSeconds?.text.toString().toInt()
                                    if (seconds < 256 && seconds < 15) {
                                        etSeconds?.error =
                                            context.getString(R.string.value_out_of_range_15_256)
                                    } else {
                                        var countIntruder = etIntruderCount?.text.toString().toInt()
                                        var durationIntruder =
                                            etIntruderDuration?.text.toString().toInt()

                                        commands[adapterPosition].logicCountIntruder = countIntruder
                                        commands[adapterPosition].logicdurationIntruder =
                                            durationIntruder
                                        commands[adapterPosition].logicSecomds = seconds


                                        commands[adapterPosition].commandContent?.set(
                                            6,
                                            countIntruder
                                        )
                                        commands[adapterPosition].commandContent?.set(
                                            7,
                                            durationIntruder
                                        )
                                        commands[adapterPosition].commandContent?.set(
                                            8,
                                            seconds
                                        )
                                        itemClick.invoke(commands[adapterPosition])
                                    }
                                }
                                //vibration
                            } else if (command.sensorType == VIBRATION_TYPE
                            ) {

                                if (validIsEmpty(etSeconds) && validIsEmpty(etIntruderCount)
                                    && validIsEmpty(etIntruderDuration)
                                ) {

                                    var seconds = etSeconds?.text.toString().toInt()

                                    var countIntruder = etIntruderCount?.text.toString().toInt()
                                    var durationIntruder =
                                        etIntruderDuration?.text.toString().toInt()

                                    commands[adapterPosition].logicCountIntruder = countIntruder
                                    commands[adapterPosition].logicdurationIntruder =
                                        durationIntruder
                                    commands[adapterPosition].logicSecomds = seconds


                                    commands[adapterPosition].commandContent?.set(
                                        4,
                                        countIntruder
                                    )
                                    commands[adapterPosition].commandContent?.set(
                                        5,
                                        durationIntruder
                                    )
                                    commands[adapterPosition].commandContent?.set(
                                        8,
                                        seconds
                                    )
                                    itemClick.invoke(commands[adapterPosition])
                                }

                            }
                        }
                        //set min power
                        context.resources.getString(R.string.set_min_power) -> {


                            if (command.sensorType == SEISMIC_TYPE
                                || command.sensorType == VIBRATION_TYPE
                            ) {
                                if (validIsEmpty(etCarSnr)) {
                                    var minPower = etCarSnr?.text.toString().toInt()
                                    if (minPower <= 256) {
                                        commands[adapterPosition].minPower = minPower
                                        commands[adapterPosition].commandContent?.set(
                                            4,
                                            minPower
                                        )
                                        commands[adapterPosition].commandContent?.set(
                                            5,
                                            0
                                        )
                                    } else {
                                        val promote = minPower / 256
                                        val reminder = minPower % 256
                                        commands[adapterPosition].commandContent?.set(
                                            4,
                                            reminder
                                        )
                                        commands[adapterPosition].commandContent?.set(
                                            5,
                                            promote
                                        )
                                    }
                                    itemClick.invoke(commands[adapterPosition])
                                }
                            } else if (command.sensorType == RADAR_TYPE
                                || command.sensorType == PIR_TYPE
                            ) {
                                if (validIsEmpty(etCarSnr)) {
                                    var minPower = etCarSnr?.text.toString().toInt()
                                    if (minPower in 0..7) {
                                        commands[adapterPosition].minPower = minPower
                                        commands[adapterPosition].commandContent?.set(
                                            4,
                                            minPower
                                        )
                                        itemClick.invoke(commands[adapterPosition])
                                    } else {
                                        etCarSnr?.error =
                                            context.getString(R.string.value_out_of_range)
                                    }
                                }
                            }
                        }
                    }
                }

            }

        }
    }

    //check if the field of edit text is empty
    private fun validIsEmpty(editText: AppCompatEditText?): Boolean {
        var isValid = true

        if (editText?.text.isNullOrBlank()) {
            editText?.error = context.resources.getString(R.string.empty_field_error)
            isValid = false
        }

        return isValid
    }

}