package ua.cn.stu.navigation.fragments


import android.R.attr.left
import android.R.attr.right
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentBmsBinding
import kotlin.math.roundToInt


class BMSFragment : Fragment(), OnChartValueSelectedListener {
    private lateinit var binding: FragmentBmsBinding
    private var scale = 0f
    private var leftMargin = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentBmsBinding.inflate(inflater, container, false)
        navigator().showBottomNavigationMenu(true)
        println("BMS fragment started")
        scale = resources.displayMetrics.density
        leftMargin = ((binding.cellHighlightIv.layoutParams as MarginLayoutParams).leftMargin/scale).roundToInt()+5

        initializedChart(binding.batteryChart)
        createSet(binding.batteryChart, createFakeDataChart())

        return binding.root
    }

    //////////////////////////////////////////////////////////////////////////////
    /**                          работа с графиком                            **/
    //////////////////////////////////////////////////////////////////////////////
    private fun createSet(chart: BarChart, dataChart: ArrayList<Float>): BarDataSet {
        val values = ArrayList<BarEntry>()

        if (dataChart.count() >= 24) {
            for (i in 0 until 24) {
                val `val` = dataChart[i]

                values.add(BarEntry(i * 1.0f, `val`))
            }
        }

        val set = BarDataSet(values, "Data Set")
        set.colors = createColorsList(createFakeDataChart())
        set.highLightAlpha = 1
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
        chart.setTouchEnabled(true)
        chart.setOnChartValueSelectedListener(this)
        chart.isDragEnabled = true
        chart.isDragDecelerationEnabled = true
        chart.setScaleEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setPinchZoom(true)
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.getHighlightByTouchPoint(1f, 1f)
        val data = BarData()
        chart.data = data
        chart.legend.isEnabled = false
        chart.description.textColor = Color.TRANSPARENT

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(false)
        chart.xAxis.gridLineWidth = 5f
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
        dataChart.add(3.85f)
        dataChart.add(4f)
        dataChart.add(3.95f)
        dataChart.add(4.15f)
        dataChart.add(4.19f)
        dataChart.add(4.23f)
        dataChart.add(3.85f)
        dataChart.add(4.07f)
        dataChart.add(3.83f)
        dataChart.add(3.88f)
        dataChart.add(3.85f)
        dataChart.add(4.23f)
        dataChart.add(3.98f)
        dataChart.add(3.85f)
        dataChart.add(4.23f)
        dataChart.add(4.04f)
        dataChart.add(3.85f)
        dataChart.add(3.89f)
        dataChart.add(4.2f)
        dataChart.add(3.85f)
        dataChart.add(3.83f)
        dataChart.add(3.74f)
        return dataChart
    }


    private fun createColorsList(cellsVoltage: ArrayList<Float>): List<Int> {
        val output = mutableListOf<Int>()

        for(cellVoltage in cellsVoltage){
            if (cellVoltage < 3.985f) { output.add(Color.rgb(61, 255, 88)) }
            if (cellVoltage > 3.985f && cellVoltage < 4.13f) { output.add(Color.rgb(253, 192, 48)) }
            if (cellVoltage > 4.13f) { output.add(Color.rgb(225, 28, 41)) }
        }

        return output
    }

    private val onValueSelectedRectF = RectF()
    @SuppressLint("SetTextI18n")
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        binding.cellHighlightIv.visibility = View.VISIBLE
        binding.voltageNumberTv.visibility = View.VISIBLE
        binding.resistanceNumberTv.visibility = View.VISIBLE
        binding.capacityNumberTv.visibility = View.VISIBLE
        binding.voltageUnitTv.visibility = View.VISIBLE
        binding.resistanceUnitTv.visibility = View.VISIBLE
        binding.capacityUnitTv.visibility = View.VISIBLE

        val bounds: RectF = onValueSelectedRectF
        binding.batteryChart.getBarBounds(e as BarEntry?, bounds)
        binding.titleCellInfoTv.text = "CELL  # " + ((h?.x)?.toInt()?.plus(1))
        binding.voltageNumberTv.text = (h?.x)?.toInt()?.let { createFakeDataChart()[it] }.toString()
        binding.resistanceNumberTv.text = (createFakeDataChart()[(h?.x)?.toInt()!!].toInt()*10).toString()
        binding.capacityNumberTv.text = ((createFakeDataChart()[(h.x).toInt()]*10).toInt()*10).toString()



        if (binding.cellHighlightIv.layoutParams is MarginLayoutParams) {
            val p = binding.cellHighlightIv.layoutParams as MarginLayoutParams
            p.setMargins(leftMargin+(bounds.left).toInt(), 0, 0, 0)
            binding.cellHighlightIv.requestLayout()
        }

        System.err.println("Margin left: ${(bounds.left/scale).toInt()}  Highlight: ${(h?.x)?.toInt()}")
    }

    override fun onNothingSelected() {
        binding.cellHighlightIv.visibility = View.GONE
        binding.voltageNumberTv.visibility = View.GONE
        binding.resistanceNumberTv.visibility = View.GONE
        binding.capacityNumberTv.visibility = View.GONE

        binding.voltageUnitTv.visibility = View.GONE
        binding.resistanceUnitTv.visibility = View.GONE
        binding.capacityUnitTv.visibility = View.GONE

        binding.titleCellInfoTv.text  = "SELECT CELL"

    }
}