package ua.cn.stu.navigation.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.utils.MPPointF
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentBmsBinding

class BMSFragment : Fragment() {
    private lateinit var binding: FragmentBmsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentBmsBinding.inflate(inflater, container, false)
        navigator().showBottomNavigationMenu(true)
        println("BMS fragment started")

        initializedChart(binding.batteryChart)
        createSet(binding.batteryChart, createFakeDataChart())

        return binding.root
    }

    //////////////////////////////////////////////////////////////////////////////
    /**                          работа с графиком                            **/
    //////////////////////////////////////////////////////////////////////////////
    private fun createSet(chart: BarChart, dataChart: ArrayList<Float>): BarDataSet {
        val values = ArrayList<BarEntry>()

        if (dataChart.count() >= 25) {
            for (i in 0 until 25) {
                val `val` = dataChart[i]

                values.add(BarEntry(i * 1.0f, `val`))
            }
        }

        val set = BarDataSet(values, "Data Set")
        set.colors = createColorsList(createFakeDataChart())
        set.iconsOffset = MPPointF(0F, 5F)
        set.valueTextColor = Color.TRANSPARENT

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

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.xAxis.textColor = Color.TRANSPARENT

        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisLeft.mAxisMinimum = 3.75f
        chart.axisLeft.textColor = Color.TRANSPARENT

        chart.axisRight.setDrawGridLines(false)
        chart.axisRight.setDrawAxisLine(false)
        chart.axisRight.textColor = Color.TRANSPARENT
    }

    private fun createFakeDataChart() :ArrayList<Float> {
        val dataChart = ArrayList<Float>()
        dataChart.add(4.23f)
        dataChart.add(4.23f)
        dataChart.add(3.75f)
        dataChart.add(4f)
        dataChart.add(3.95f)
        dataChart.add(4.15f)
        dataChart.add(4.19f)
        dataChart.add(4.23f)
        dataChart.add(3.75f)
        dataChart.add(4.07f)
        dataChart.add(3.83f)
        dataChart.add(3.88f)
        dataChart.add(3.75f)
        dataChart.add(4.23f)
        dataChart.add(3.98f)
        dataChart.add(3.75f)
        dataChart.add(4.23f)
        dataChart.add(4.04f)
        dataChart.add(3.75f)
        dataChart.add(3.89f)
        dataChart.add(4.2f)
        dataChart.add(3.75f)
        dataChart.add(3.78f)
        dataChart.add(3.75f)
        dataChart.add(3.85f)
        return dataChart
    }


    private fun createColorsList(cellsVoltage: ArrayList<Float>): List<Int> {
        val output = mutableListOf<Int>()

        for(cellVoltage in cellsVoltage){
            println("cellVoltage $cellVoltage")
            if (cellVoltage < 3.985f) { output.add(Color.rgb(61, 255, 88)) }
            if (cellVoltage > 3.985f && cellVoltage < 4.13f) { output.add(Color.rgb(253, 192, 48)) }
            if (cellVoltage > 4.13f) { output.add(Color.rgb(225, 28, 41)) }
        }

        return output
    }
}
