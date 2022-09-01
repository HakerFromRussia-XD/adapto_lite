package ua.cn.stu.navigation.recyclers_adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ua.cn.stu.navigation.MainActivity.Companion.statList
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.OnStatClickListener

class StatisticAdapter(private val onStatClickListener: OnStatClickListener
) : RecyclerView.Adapter<StatisticAdapter.StatViewHolder>() {

    inner class StatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val selectDeviceBtn: View = view.findViewById(R.id.scan_cell_btn)
        val deviceName: TextView = view.findViewById(R.id.scan_cell_tv) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan, parent, false)
        return StatViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        holder.deviceName.text = statList[position]

        holder.selectDeviceBtn.setOnClickListener {
            onStatClickListener.onClicked(statList[position])
        }
    }
    override fun getItemCount(): Int {
        return statList.size
    }

}