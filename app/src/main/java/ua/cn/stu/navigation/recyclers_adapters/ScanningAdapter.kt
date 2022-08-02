package ua.cn.stu.navigation.recyclers_adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ua.cn.stu.navigation.MainActivity.Companion.scanList
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.OnScanClickListener

class ScanningAdapter(private val onScanClickListener: OnScanClickListener
) : RecyclerView.Adapter<ScanningAdapter.ScanViewHolder>() {

    inner class ScanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val selectDeviceBtn: View = view.findViewById(R.id.scan_cell_btn)
        val scanCell: RelativeLayout = view.findViewById(R.id.scan_cell) as RelativeLayout
        val deviceName: TextView = view.findViewById(R.id.scan_cell_tv) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan, parent, false)
        return ScanViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        holder.deviceName.text = scanList[position].getTitle()

        if (scanList[position].getTitle() == "NOT SET!") { holder.scanCell.visibility = View.GONE }
        else { holder.scanCell.visibility = View.VISIBLE }

        holder.selectDeviceBtn.setOnClickListener {
            onScanClickListener.onClicked(scanList[position].getTitle(), scanList[position].getAddr())
        }
    }
    override fun getItemCount(): Int {
        return scanList.size
    }

}