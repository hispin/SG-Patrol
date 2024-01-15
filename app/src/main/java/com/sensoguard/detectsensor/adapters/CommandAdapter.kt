package com.sensoguard.detectsensor.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
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
import com.sensoguard.detectsensor.global.PROCESS_STATE
import com.sensoguard.detectsensor.global.SUCCESS_STATE
import com.sensoguard.detectsensor.global.TIMEOUT_STATE
import com.sensoguard.detectsensor.global.showToast

class CommandAdapter(
    private var commands: ArrayList<Command>,
    val context: Context,
    var itemClick: (Command) -> Unit
) : RecyclerView.Adapter<CommandAdapter.ViewHolder>() {


    override fun onBindViewHolder(holder: CommandAdapter.ViewHolder, position: Int) {
        holder.bindReservation((commands[position]))
        holder.setIsRecyclable(false)
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
        private var spCarSens: AppCompatSpinner? = null
        private var spIntruderSens: AppCompatSpinner? = null
        private var btnSendCmd: AppCompatButton? = null

        init {
            itemView.setOnClickListener {

                if (commands[adapterPosition].commandName == context.resources.getString(R.string.set_sens_level)) {
                    commands[adapterPosition].isExpand = !commands[adapterPosition].isExpand
                    //Bug fixed:when expand the command ,zero the car and intruder selection (for update ses command)
                    if (commands[adapterPosition].isExpand) {
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
            spCarSens = _itemView.findViewById(R.id.spCarSens)
            //Bug fixed:set the last selection as long as the command of update sens is open
            spCarSens?.setSelection(commands[adapterPosition].sensCar)
            spIntruderSens = _itemView.findViewById(R.id.spIntruderSens)
            //set the last selection as long as the command of update sens is open
            spIntruderSens?.setSelection(commands[adapterPosition].sensIntruder)
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
            } else {
                TransitionManager.beginDelayedTransition(myCardView!!, AutoTransition())
                conExpand?.visibility = View.GONE
            }

            btnSendCmd?.setOnClickListener {
                if (spCarSens?.selectedItem.toString() == "0"
                    && spIntruderSens?.selectedItem.toString() == "0"
                ) {
                    showToast(context, context.getString(R.string.error_zero_sens))
                } else {
                    if (commands[adapterPosition].commandName == context.resources.getString(R.string.set_sens_level)) {
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
                }

            }

        }
    }


}