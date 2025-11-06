package com.sensoguard.detectsensor.adapters


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.classes.SystemSort


class SystemSortDialogAdapter(
    private var systems: ArrayList<SystemSort>,
    val context: Context,
    var itemClick: (SystemSort) -> Unit
) : RecyclerView.Adapter<SystemSortDialogAdapter.ViewHolder>() {


    companion object;


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.bindReservation((systems[position]))
        holder.setIsRecyclable(false)
    }

    override fun getItemCount(): Int {
        return this.systems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_system_filter_dialog, parent, false)



        return ViewHolder(view, itemClick)
    }

    fun setDetects(_detectors: ArrayList<SystemSort>?) {
        _detectors?.let { systems = it }
        //TODO how to define with this
    }

    inner class ViewHolder(
        private val _itemView: View,
        private val itemClick: (SystemSort) -> Unit
    ) :
        RecyclerView.ViewHolder(_itemView) {
        //private var tvId: TextView? = null
        private var tvName: TextView? = null
        private var togIsSelected: ToggleButton? = null


//        init {
//            itemView.setOnClickListener {
//                //itemClick.invoke(sensors[adapterPosition])
//            }
//        }


        fun bindReservation(systemSort: SystemSort) {
            //tvId = _itemView.findViewById(R.id.tvId)
            tvName = _itemView.findViewById(R.id.tvName)

            togIsSelected = _itemView.findViewById(R.id.togIsSelected)


            togIsSelected?.setOnCheckedChangeListener { _, isChecked ->
                systemSort.isSorted = isChecked
            }


            //tvId?.text = systemSort.getId()
            tvName?.text = systemSort.cameraName

        }
    }
}