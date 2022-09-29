package ua.cn.stu.navigation.fragments


import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
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
import ua.cn.stu.navigation.contract.DisconnectionAction
import ua.cn.stu.navigation.contract.HasDisconnectionAction
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentBmsBinding
import java.util.*
import kotlin.math.roundToInt


class BMSFragment : Fragment(), OnChartValueSelectedListener, HasDisconnectionAction {
    private lateinit var binding: FragmentBmsBinding
    private var scale = 0f
    private var leftMargin = 0
    private var maxLimit = 4.23f
    private var minLimit = 3.74f

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentBmsBinding.inflate(inflater, container, false)
        navigator().showBottomNavigationMenu(true)
        println("BMS fragment started")
        scale = resources.displayMetrics.density
        leftMargin = ((binding.cellHighlightIv.layoutParams as MarginLayoutParams).leftMargin/scale).roundToInt()+5

        initializedChart(binding.batteryChart)
        createSet(binding.batteryChart, scalingOfInputData(createFakeDataChart(), maxLimit, minLimit))
        setProgressBigBaterry(createFakeDataChart())
        binding.deltaVoltageLimitTv.text = "Δ ${maxLimit - minLimit} V"
        binding.maxVoltageLimitTv.text = "$maxLimit V"
        binding.minVoltageLimitTv.text = "$minLimit V"

        return binding.root
    }

    //////////////////////////////////////////////////////////////////////////////
    /**                          работа с графиком                            **/
    //////////////////////////////////////////////////////////////////////////////
    private fun createSet(chart: BarChart, dataChart: ArrayList<Float>): BarDataSet {
        val values = ArrayList<BarEntry>()

        if (dataChart.count() >= createFakeDataChart().size) {
            for (i in 0 until createFakeDataChart().size) {
                val `val` = dataChart[i]

                values.add(BarEntry(i * 1.0f, `val`))
            }
        }

        val set = BarDataSet(values, "Data Set")
        set.colors = createColorsList(scalingOfInputData(createFakeDataChart(), maxLimit, minLimit))
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
        //TODO изменить нижний лимит и проверить как это повлияет на график без изменения датасета
        chart.axisLeft.mAxisMinimum = 0f
        chart.axisLeft.textColor = Color.TRANSPARENT

        chart.axisRight.setDrawGridLines(false)
        chart.axisRight.setDrawAxisLine(false)
        chart.axisRight.textColor = Color.TRANSPARENT
    }

    private fun createFakeDataChart() :ArrayList<Float> {
        val dataChart = ArrayList<Float>()
        dataChart.add(10f)
        dataChart.add(7.36f)
        dataChart.add(3.74f)
        dataChart.add(3.80f)
        dataChart.add(3.85f)
        dataChart.add(3.90f)
        dataChart.add(3.95f)
        dataChart.add(4.00f)
        dataChart.add(4.05f)
        dataChart.add(4.10f)
        dataChart.add(4.15f)
        dataChart.add(4.20f)
        dataChart.add(4.23f)
        dataChart.add(6f)
        dataChart.add(4.20f)
        dataChart.add(4.15f)
        dataChart.add(4.10f)
        dataChart.add(4.05f)
        dataChart.add(4.00f)
        dataChart.add(3.95f)
        dataChart.add(3.90f)
        dataChart.add(3.85f)
        dataChart.add(1.82f)//3.80f)
        dataChart.add(0.1f) //24
        return dataChart
    }

    private fun scalingOfInputData (inputData: ArrayList<Float>, maxLimit: Float, minLimit: Float): ArrayList<Float> {
        val outputData = ArrayList<Float>()
        val maxV = Collections.max(inputData)
        val iter: MutableIterator<Float> = inputData.iterator()

        while (iter.hasNext()) {
            val scalablePoint = iter.next()
            if ((scalablePoint > minLimit) && (scalablePoint < maxLimit)) {
                val percentScale: Float = (maxLimit - minLimit)/100
                val percentageValue: Float = (scalablePoint-minLimit)/percentScale
                val calculatedValue: Float = percentageValue*0.08f+1
                outputData.add(calculatedValue)
//                System.err.println("scalablePoint: $scalablePoint  -->  calculatedValue: $calculatedValue")
            }
            if (scalablePoint <= minLimit) {
                val percentScale: Float = minLimit/100
                val percentageValue: Float = scalablePoint/percentScale
                val calculatedValue: Float = percentageValue*0.01f
                outputData.add(calculatedValue)
//                System.err.println("scalablePoint: $scalablePoint  -->  calculatedValue: $calculatedValue")
            }
            if (scalablePoint >= maxLimit) {
                val percentScale: Float = (maxV-maxLimit)/100
                val percentageValue: Float = (scalablePoint-maxLimit)/percentScale
                val calculatedValue: Float = percentageValue*0.01f+9
                outputData.add(calculatedValue)
//                System.err.println("scalablePoint: $scalablePoint  -->  calculatedValue: $calculatedValue")
            }
        }

        // Точки максимумма и минимума, чтобы масштаб графика был соответствующим преобразованию
        outputData.add(0f)
        outputData.add(10f)
        return outputData
    }

    private fun createColorsList(scaledCellsVoltage: ArrayList<Float>): List<Int> {
        val output = mutableListOf<Int>()

        for(cellVoltage in scaledCellsVoltage){
            if (cellVoltage > 6.333f && cellVoltage <= 9f) {
                output.add(Color.rgb(61, 255, 88))
                System.err.println("cellVoltage: $cellVoltage  green")
            }
            if (cellVoltage in 3.666f..6.333f) {
                output.add(Color.rgb(253, 192, 48))
                System.err.println("cellVoltage: $cellVoltage  yellow")
            }
            if (cellVoltage < 3.666f || cellVoltage > 9f) {
                output.add(Color.rgb(225, 28, 41))
                System.err.println("cellVoltage: $cellVoltage  red")
            }
        }
        output[output.size-2] = Color.TRANSPARENT
        output[output.size-1] = Color.TRANSPARENT

        return output
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun setProgressBigBaterry(inputData: ArrayList<Float>) {
        val maxV = Collections.max(inputData)
        var summ = 0f
        for (i in 0 until inputData.size) summ += inputData[i]
        val percent = ((summ/inputData.size)/(maxV/100)).toInt()
        System.err.println("maxV: $maxV   summ:$summ    (summ/inputData.size):${(summ/inputData.size)}   percent: $percent")


        binding.batteryIndicatorSb.progress = percent
        binding.batteryAverageVoltageTv.text = String.format("%.2f", summ/inputData.size)
        binding.percentTv.text = percent.toString()

        //делаем сикбар некликабельным
        binding.batteryIndicatorSb.setOnTouchListener { _, _ -> true }
    }

    private val onValueSelectedRectF = RectF()
    @SuppressLint("SetTextI18n")
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        if ( ((h?.x)?.toInt()?.plus(1))!! <= 24) {
            binding.cellHighlightIv.visibility = View.VISIBLE
            binding.overflowCellHighlightIv.visibility = View.GONE
            binding.darkeningTopAndBottomCellHighlightIv.visibility = View.VISIBLE
            binding.voltageNumberTv.visibility = View.VISIBLE
            binding.resistanceNumberTv.visibility = View.VISIBLE
            binding.capacityNumberTv.visibility = View.VISIBLE
            binding.voltageUnitTv.visibility = View.VISIBLE
            binding.resistanceUnitTv.visibility = View.VISIBLE
            binding.capacityUnitTv.visibility = View.VISIBLE

            if (createFakeDataChart()[(h.x).toInt()] > maxLimit || createFakeDataChart()[(h.x).toInt()] <= minLimit) {
                binding.darkeningTopAndBottomCellHighlightIv.visibility = View.GONE
                binding.cellHighlightIv.visibility = View.GONE
                binding.overflowCellHighlightIv.visibility = View.VISIBLE
            }

            val bounds: RectF = onValueSelectedRectF
            binding.batteryChart.getBarBounds(e as BarEntry?, bounds)
            binding.titleCellInfoTv.text = "CELL  # " + ((h.x).toInt().plus(1))
            binding.voltageNumberTv.text =
                (h.x).toInt().let { createFakeDataChart()[it] }.toString()
            binding.resistanceNumberTv.text =
                (createFakeDataChart()[(h.x).toInt()].toInt() * 10).toString()
            binding.capacityNumberTv.text =
                ((createFakeDataChart()[(h.x).toInt()] * 10).toInt() * 10).toString()


            if (binding.cellHighlightIv.layoutParams is MarginLayoutParams) {
                val p = binding.cellHighlightIv.layoutParams as MarginLayoutParams
                p.setMargins(leftMargin + (bounds.left).toInt(), 0, 0, 0)
                binding.cellHighlightIv.requestLayout()
            }
            if (binding.overflowCellHighlightIv.layoutParams is MarginLayoutParams) {
                val p = binding.overflowCellHighlightIv.layoutParams as MarginLayoutParams
                p.setMargins(leftMargin + (bounds.left).toInt(), 0, 0, 0)
                binding.overflowCellHighlightIv.requestLayout()
            }

            System.err.println("Margin left: ${(bounds.left/scale).toInt()}  Highlight: ${(h.x).toInt()}")
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onNothingSelected() {
        binding.cellHighlightIv.visibility = View.GONE
        binding.overflowCellHighlightIv.visibility = View.GONE
        binding.darkeningTopAndBottomCellHighlightIv.visibility = View.GONE
        binding.voltageNumberTv.visibility = View.GONE
        binding.resistanceNumberTv.visibility = View.GONE
        binding.capacityNumberTv.visibility = View.GONE

        binding.voltageUnitTv.visibility = View.GONE
        binding.resistanceUnitTv.visibility = View.GONE
        binding.capacityUnitTv.visibility = View.GONE

        binding.titleCellInfoTv.text  = "SELECT CELL"

    }

    override fun getDisconnectionAction(): DisconnectionAction {
        return DisconnectionAction(
            onDisconnectionAction = {
                showDisconnectionDialog()
            }
        )
    }

    private fun showDisconnectionDialog() {
        navigator().showDisconnectDialog()
    }
}