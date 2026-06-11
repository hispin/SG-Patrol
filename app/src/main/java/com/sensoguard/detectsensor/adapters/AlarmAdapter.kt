package com.sensoguard.detectsensor.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.classes.Alarm
import com.sensoguard.detectsensor.global.getStrDateTimeByMilliSeconds
import com.sensoguard.detectsensor.interfaces.OnAdapterListener

class AlarmAdapter (private var alarms: ArrayList<Alarm>, val context: Context, val onAdapterListener: OnAdapterListener, var itemClick: (Alarm) -> Unit) : RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindReservation((alarms[position]))
        holder.setIsRecyclable(false)
    }

    override fun getItemCount(): Int {
        return this.alarms.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.alarm_item, parent, false)


        return ViewHolder(view, itemClick)
    }

    fun setDetects(_alarm: ArrayList<Alarm>?) {
        _alarm?.let { alarms = it }
        //TODO how to define with this
    }

    inner class ViewHolder(private val _itemView: View, private val itemClick: (Alarm) -> Unit) :
        RecyclerView.ViewHolder(_itemView) {
        private var tvId:TextView? = null
        private var tvName:TextView?=null
        private var tvDate:TextView? = null
        private var tvType: TextView? = null


        init {
            itemView.setOnClickListener {
                itemClick.invoke(alarms[adapterPosition])
            }
            itemView.setOnLongClickListener {

                //toggle the status of ready to delete
                alarms[bindingAdapterPosition].isReadyToDelete =
                    !alarms[bindingAdapterPosition].isReadyToDelete
                notifyItemChanged(bindingAdapterPosition, alarms[bindingAdapterPosition])

                return@setOnLongClickListener false

            }
        }


        fun bindReservation(alarm: Alarm) {
            tvId = _itemView.findViewById(R.id.tvId)
            tvName = _itemView.findViewById(R.id.tvName)
            tvDate = _itemView.findViewById(R.id.tvDate)
            tvType = _itemView.findViewById(R.id.tvType)


            if (alarm.isArmed != null
                && alarm.isArmed!!
                && alarm.isLocallyDefined != null
                && alarm.isLocallyDefined
            ) {
                tvName?.setTextColor(ContextCompat.getColor(context, R.color.red))
                tvDate?.setTextColor(ContextCompat.getColor(context, R.color.red))
                tvId?.setTextColor(ContextCompat.getColor(context, R.color.red))
                tvType?.setTextColor(ContextCompat.getColor(context, R.color.red))
            } else {
                tvName?.setTextColor(ContextCompat.getColor(context, R.color.black))
                tvDate?.setTextColor(ContextCompat.getColor(context, R.color.black))
                tvId?.setTextColor(ContextCompat.getColor(context, R.color.black))
                tvType?.setTextColor(ContextCompat.getColor(context, R.color.black))
            }


            if (alarm.timeInMillis != null) {
                val tmp = getStrDateTimeByMilliSeconds(
                    alarm.timeInMillis!!,
                    "dd/MM/yy",
                    context
                ) + "\n" + alarm.timeInMillis?.let {
                    getStrDateTimeByMilliSeconds(
                        it,
                        "kk:mm:ss",
                        context
                    )
                }
                tvDate?.text = tmp
            }


            tvId?.text = alarm.id

            val str = alarm.id + "\n" + "NET " + alarm.subnet + "(" + alarm.originId + ")"
            tvId?.text = str



            tvType?.text = alarm.type
            tvName?.text = alarm.name

            //set selected/unselected
            if (alarm.isReadyToDelete) {
                itemView.alpha = 0.2F
            } else {
                itemView.alpha = 1.0F
            }

        }
    }
}