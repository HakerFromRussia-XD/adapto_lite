package ua.cn.stu.navigation.recyclers_adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.model.GradientColor
import com.github.mikephil.charting.utils.MPPointF
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.OnProfileClickListener

class ProfileAdapter(private val profileNames: ArrayList<String>,
                     private val dataCharts: ArrayList<ArrayList<Int>>,
                     private val image: Drawable,
                     private val onProfileClickListener: OnProfileClickListener
) : RecyclerView.Adapter<ProfileAdapter.BasalProfileViewHolder>() {

    inner class BasalProfileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val selectProfileBtn: View = view.findViewById(R.id.select_profile_btn)
        val deleteProfileBtn: View = view.findViewById(R.id.delete_profile_btn)
        val addProfileBtn: View = view.findViewById(R.id.add_profile_btn)
        val titleProfileView: View = view.findViewById(R.id.title_profile_view)
        val cell: CardView = view.findViewById(R.id.cv) as CardView
        val profileCell: ConstraintLayout = view.findViewById(R.id.profile_cell) as ConstraintLayout
        val addCell: ConstraintLayout = view.findViewById(R.id.add_cell) as ConstraintLayout
        val deleteProfileImage: ImageView = view.findViewById(R.id.delete_profile_iv) as ImageView
        val iconProfileImage: ImageView = view.findViewById(R.id.chart_icon_iv) as ImageView
        val profileChart: BarChart = view.findViewById(R.id.profile_chart) as BarChart
        val nameProfile: TextView = view.findViewById(R.id.name_profile_tv) as TextView
        val noteProfile: TextView = view.findViewById(R.id.note_profile_tv) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasalProfileViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return BasalProfileViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BasalProfileViewHolder, position: Int) {
        println("onBindViewHolder selectedProfile = " + MainActivity.selectedProfile)
        val profileName = profileNames[position]

        if (profileName != "add") {
            val dataChart = dataCharts[position]
//            println("1 dataChart[position] = $dataChart [[$position]]")
            holder.profileCell.visibility = View.VISIBLE
            holder.addCell.visibility = View.GONE
            holder.cell.setCardBackgroundColor(Color.WHITE)
            holder.cell.cardElevation = 5F
            holder.nameProfile.text = profileName
            if (position == MainActivity.selectedProfile) {
                holder.nameProfile.text = (holder.nameProfile.text as String) + " (active)"
                holder.deleteProfileBtn.visibility = View.GONE
                holder.deleteProfileImage.visibility = View.GONE
                holder.titleProfileView.setBackgroundColor(Color.rgb(242, 200, 0))
                holder.iconProfileImage.setColorFilter(Color.WHITE)
                holder.nameProfile.setTextColor(Color.WHITE)
            } else {
                holder.iconProfileImage.setColorFilter(Color.BLACK)
                holder.nameProfile.setTextColor(Color.BLACK)
                holder.nameProfile.text = (holder.nameProfile.text as String) + " (inactive)"
                holder.titleProfileView.setBackgroundColor(Color.rgb(229, 229, 234))
                holder.deleteProfileImage.visibility = View.VISIBLE
                holder.deleteProfileBtn.visibility = View.VISIBLE
                holder.deleteProfileBtn.setOnClickListener {
                    onProfileClickListener.onClicked(profileName, position, deleteProfile = true, addProfile = false)
                }
            }

            holder.noteProfile.text = holder.itemView.context.getString(R.string.note_basal_profile, (dataCharts[position].sum().toFloat()/100))
            holder.selectProfileBtn.setOnClickListener {
                onProfileClickListener.onClicked(profileName, position, deleteProfile = false, addProfile = false)
            }

            initializedChart(holder.profileChart)
            createSet(holder.profileChart, dataChart)
        } else {
            holder.profileCell.visibility = View.GONE
            holder.addCell.visibility = View.VISIBLE
            holder.cell.setCardBackgroundColor(Color.TRANSPARENT)
            holder.cell.cardElevation = 0F
            holder.addProfileBtn.setOnClickListener {
                onProfileClickListener.onClicked(profileName, position, deleteProfile = false, addProfile = true)
            }
        }
    }
    override fun getItemCount(): Int {
        return profileNames.size
    }


    //////////////////////////////////////////////////////////////////////////////
    /**                          работа с графиками                            **/
    //////////////////////////////////////////////////////////////////////////////
    private fun createSet(chart: BarChart, dataChart: ArrayList<Int>): BarDataSet {
        val values = ArrayList<BarEntry>()

//        dataChart.maxOrNull()
//        println("0 dataChart = $dataChart")
        val maxVal: Int = dataChart.maxOrNull()!!.toInt()
        if (dataChart.count() >= 25) {
            for (i in 0 until 25) {
                val `val` = (dataChart[i]).toFloat()

                if (`val` < maxVal.toFloat() / 4) {
                    values.add(BarEntry(i * 1.0f, `val`))
                } else {
                    values.add(BarEntry(i * 1.0f, `val`, image))
                }
            }
        }

        val set = BarDataSet(values, "Data Set")
        set.iconsOffset = MPPointF(0F, 5F)
        set.valueTextColor = Color.TRANSPARENT
        val startColor4 = Color.rgb(163, 254, 124)
        val endColor3 = Color.rgb(110, 217, 64)
        val myMyGradient = GradientColor(startColor4, endColor3)
        val gradientFills: MutableList<GradientColor> = java.util.ArrayList<GradientColor>()
        gradientFills.add(myMyGradient)
        set.gradientColors = gradientFills

        val dataSets = java.util.ArrayList<IBarDataSet>()
        dataSets.add(set)

        val data = BarData(dataSets)
        chart.data = data
        chart.setFitBars(true)
        chart.invalidate()
        return set
    }
    private fun initializedChart(chart: BarChart) {
        chart.contentDescription
        chart.setTouchEnabled(false)
        chart.isDragEnabled = false
        chart.isDragDecelerationEnabled = false
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setPinchZoom(false)
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.getHighlightByTouchPoint(1f, 1f)
        val data = BarData()
        chart.data = data
        chart.legend.isEnabled = false
        chart.description.textColor = Color.TRANSPARENT
        chart.animateY(700)

        chart.xAxis.position = XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.xAxis.textColor = Color.TRANSPARENT

        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisLeft.textColor = Color.TRANSPARENT

        chart.axisRight.setDrawGridLines(false)
        chart.axisRight.setDrawAxisLine(false)
        chart.axisRight.textColor = Color.TRANSPARENT
    }
}