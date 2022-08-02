package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import biz.borealis.numberpicker.NumberPicker
import biz.borealis.numberpicker.OnValueChangeListener
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.model.GradientColor
import com.github.mikephil.charting.utils.MPPointF
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_profile_settings.*
import ua.cn.stu.navigation.MainActivity.Companion.changeProfile
import ua.cn.stu.navigation.MainActivity.Companion.dataAllCharts
import ua.cn.stu.navigation.MainActivity.Companion.inProfileSettingsFragmentFlag
import ua.cn.stu.navigation.MainActivity.Companion.inputSpeedAllPeriodsMain
import ua.cn.stu.navigation.MainActivity.Companion.periodNamesMain
import ua.cn.stu.navigation.MainActivity.Companion.profileNames
import ua.cn.stu.navigation.MainActivity.Companion.startTimeAllPeriodsMain
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.*
import ua.cn.stu.navigation.databinding.FragmentProfileSettingsBinding
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys
import ua.cn.stu.navigation.recyclers_adapters.ProfileSettingsAdapter
import ua.cn.stu.navigation.rx.RxUpdateMainEvent


class ProfileSettingsFragment : Fragment(), HasCustomTitle, HasReturnAction, HasRenameProfileAction {

    private lateinit var binding: FragmentProfileSettingsBinding
    private lateinit var name: String

    private val maxCountProfiles: Int = 24

    private var periodNames = ArrayList<String>()
    var startTimeAllPeriods = ArrayList<Int>()
    var inputSpeedAllPeriods = ArrayList<Int>()
    private var image: Drawable? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    var settingsAdapter: ProfileSettingsAdapter? = null
    private var timerUnit: CountDownTimer? = null
    private var timerSubunit: CountDownTimer? = null
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        name = profileNames[changeProfile]
    }

    @SuppressLint("CheckResult")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileSettingsBinding.inflate(inflater, container, false)
        image = ResourcesCompat.getDrawable(resources, R.drawable.ic_top_bar_profile_settings, null)

        println("changeProfile=$changeProfile")
        println("periodNamesMain=$periodNamesMain  ${periodNamesMain.size}")
        if (periodNamesMain.size < changeProfile) {
//            navigator().runReadNumBasalProfiles()
//            navigator().runReadBasalProfiles()
        }
        else {
            periodNames = convertArrayStringToList(periodNamesMain[changeProfile])
            startTimeAllPeriods = convertArrayIntToList(startTimeAllPeriodsMain[changeProfile])
            convertArrayIntToList(inputSpeedAllPeriodsMain[changeProfile])
            inputSpeedAllPeriods = convertArrayIntToList(inputSpeedAllPeriodsMain[changeProfile])

            disposables.add(RxUpdateMainEvent.getInstance().backPresedIventSubjectObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (inProfileSettingsFragmentFlag) {
                        println("BACK PRESED!!!")
                        inProfileSettingsFragmentFlag = false
//                        navigator().runWriteBasalProfiles((changeProfile+1), name)
                        disposables.clear()
                    }
                })

            initAdapter(binding.profilePeriodsRv)
            initializedChart(binding.profilePeriodChart)
            createSet(binding.profilePeriodChart, createDataChart())
        }

        return binding.root
    }

    override fun getTitleRes(): String = name
    override fun getReturnAction(): ReturnAction {
        return ReturnAction(
            onReturnAction = {
                onCancelPressed()
            }
        )
    }
    override fun getRenameProfileAction(): RenameProfileAction {
        return RenameProfileAction(
            onRenameProfileAction = { showRenameDialog() }
        )
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun addPeriodProfile() {
        if (startTimeAllPeriods[startTimeAllPeriods.size-1] == 23) {
            Toast.makeText(context, getString(R.string.profile_settings_varning), Toast.LENGTH_SHORT).show()
        } else {
            periodNames.removeLast()
            settingsAdapter?.notifyItemChanged(settingsAdapter!!.itemCount - 1)//adapter!!.itemCount-1)
            periodNames.add("profile new")
            startTimeAllPeriods.add(startTimeAllPeriods[startTimeAllPeriods.size - 1] + 1)
            inputSpeedAllPeriods.add(inputSpeedAllPeriods[inputSpeedAllPeriods.size - 1])
            settingsAdapter?.notifyItemChanged(settingsAdapter!!.itemCount - 1)
            if (periodNames.size < maxCountProfiles) periodNames.add("add")
            settingsAdapter?.notifyItemChanged(settingsAdapter!!.itemCount - 1)
            createSet(profile_period_chart, createDataChart())
            saveAllLists()
        }
//        println("periodNames = $periodNames  size: ${periodNames.size}   periodNames[changeProfile]= ${periodNames[changeProfile]}  size: ${periodNames[changeProfile].size}")
    }
    private fun initAdapter(profile_rv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        profile_rv.layoutManager = linearLayoutManager
        settingsAdapter = ProfileSettingsAdapter(
            periodNames,
            startTimeAllPeriods,
            inputSpeedAllPeriods,
            object : OnProfilePeriodClickListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onClicked(
                name: String,
                selectProfile: Int,
                deleteProfile: Boolean,
                addProfile: Boolean,
                selectTime: Boolean,
                selectSpeed: Boolean
            ) {
                if (name != "add") {
                    if (deleteProfile) {
//                        println("code delete period")
                        periodNames.removeAt(selectProfile)
                        startTimeAllPeriods.removeAt(selectProfile)
                        inputSpeedAllPeriods.removeAt(selectProfile)
                        if (periodNames.count() == (maxCountProfiles - 1)) {
                            if (periodNames.last() != "add") {
                                periodNames.add("add")
                                for (item in selectProfile..periodNames.count()) {
                                    settingsAdapter?.notifyItemChanged(item)
                                }
                            } else { settingsAdapter?.notifyDataSetChanged() }
                        } else { settingsAdapter?.notifyDataSetChanged() }
                        createSet(profile_period_chart, createDataChart())
                        saveAllLists()
                    }
                    if (selectTime) {
//                        println("code time period")
                        showTimeDialog(selectProfile)
                    }
                    if (selectSpeed) {
//                        println("code speed period")
                        showSpeedDialog(selectProfile)
                    }
                } else {
//                    println("code add period")
                    addPeriodProfile()
                }
            }

        })
        profile_rv.adapter = settingsAdapter
    }


    //////////////////////////////////////////////////////////////////////////////
    /**                          работа с диалогами                            **/
    //////////////////////////////////////////////////////////////////////////////
    @SuppressLint("InflateParams")
    private fun showTimeDialog(selectProfile: Int) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_time_period_basal, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val numberPicker = dialogBinding.findViewById<View>(R.id.hour_time_start_period_np) as NumberPicker
//        println("startTimeAllPeriods.size = "+startTimeAllPeriods.size + "    selectProfile = $selectProfile")
        var changeTime = false
        if (selectProfile == (startTimeAllPeriods.size-1)) { numberPicker.max = 23 }
        else { numberPicker.max = startTimeAllPeriods[selectProfile+1]-1 }
        if (selectProfile != 0) numberPicker.min = startTimeAllPeriods[selectProfile-1] + 1
        val oldValue: Int = startTimeAllPeriods[selectProfile]
        numberPicker.isScrollbarFadingEnabled = true
        numberPicker.isHorizontalFadingEdgeEnabled = true
        numberPicker.isVerticalFadingEdgeEnabled = true
        numberPicker.textSizeSelected = convertToSp(44f)
        numberPicker.onValueChangeListener = OnValueChangeListener { value ->
            changeTime = true
            startTimeAllPeriods[selectProfile] = value
            settingsAdapter?.notifyItemChanged(selectProfile)
            createSet(profile_period_chart, createDataChart())
            saveAllLists()
        }


        val cancelBtn = dialogBinding.findViewById<View>(R.id.time_dialog_cancel)
        cancelBtn.setOnClickListener {
            startTimeAllPeriods[selectProfile] = oldValue
            settingsAdapter?.notifyItemChanged(selectProfile)
            createSet(profile_period_chart, createDataChart())
            saveAllLists()
            myDialog.dismiss()
        }

        val yesBtn = dialogBinding.findViewById<View>(R.id.time_dialog_confirm)
        yesBtn.setOnClickListener {
            if (!changeTime) {
                startTimeAllPeriods[selectProfile] = numberPicker.min
                settingsAdapter?.notifyItemChanged(selectProfile)
                createSet(profile_period_chart, createDataChart())
                saveAllLists()
            }
            myDialog.dismiss()
        }
    }
    @SuppressLint("InflateParams")
    private fun showSpeedDialog(selectProfile: Int) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_speed_period_basal, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val oldValue: Int = inputSpeedAllPeriods[selectProfile]
        var unitSpeed: Int
        var subunitSpeed: Int
        val unitPicker = dialogBinding.findViewById<View>(R.id.unit_speed_period_np) as android.widget.NumberPicker
        val subunitPicker = dialogBinding.findViewById<View>(R.id.subunit_speed_period_np) as android.widget.NumberPicker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            unitPicker.textSize = convertToSp(44f)
        }
        unitPicker.minValue = 0
        unitPicker.maxValue = 49
        unitPicker.value = inputSpeedAllPeriods[selectProfile]/100
        unitPicker.setFormatter { i -> String.format("%02d", i) }
        unitPicker.setOnValueChangedListener { _, _, newVal ->
            println("value = $newVal")
            unitSpeed = newVal
            inputSpeedAllPeriods[selectProfile] = (unitSpeed*100)+inputSpeedAllPeriods[selectProfile]%100
            settingsAdapter?.notifyItemChanged(selectProfile)
            if (unitSpeed == 0) { subunitPicker.minValue = 1 }
            else { subunitPicker.minValue = 0 }

            timerUnit?.cancel()
            timerUnit = object : CountDownTimer(500, 1) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    createSet(profile_period_chart, createDataChart())
                    saveAllLists()
                }
            }.start()
        }
//        unitPicker.onValueChangeListener = OnValueChangeListener { value ->
//            changeSpeed = true
//            unitSpeed = value
//            inputSpeedAllPeriods[selectProfile] = (unitSpeed*100)+subunitSpeed
//            settingsAdapter?.notifyItemChanged(selectProfile)
//            createSet(profile_period_chart, createDataChart())
//            saveAllLists()
//            if (unitSpeed == 0) { subunitPicker.min = 1 }
//            else { subunitPicker.min = 0 }
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            subunitPicker.textSize = convertToSp(44f)
        }
        subunitPicker.minValue = 0
        subunitPicker.maxValue = 99
        subunitPicker.value = inputSpeedAllPeriods[selectProfile]%100
        subunitPicker.setFormatter { i -> String.format("%02d", i) }
        subunitPicker.setOnValueChangedListener { _, _, newVal ->
            subunitSpeed = newVal
            inputSpeedAllPeriods[selectProfile] = ((inputSpeedAllPeriods[selectProfile]/100)*100)+subunitSpeed
            settingsAdapter?.notifyItemChanged(selectProfile)

            timerSubunit?.cancel()
            timerSubunit = object : CountDownTimer(500, 1) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    createSet(profile_period_chart, createDataChart())
                    saveAllLists()
                }
            }.start()
        }
//        subunitPicker.onValueChangeListener = OnValueChangeListener { value ->
//            subunitSpeed = value
//            inputSpeedAllPeriods[selectProfile] = (unitSpeed*100)+subunitSpeed
//            settingsAdapter?.notifyItemChanged(selectProfile)
//
//        }


        val cancelBtn = dialogBinding.findViewById<View>(R.id.input_speed_dialog_cancel)
        cancelBtn.setOnClickListener {
            inputSpeedAllPeriods[selectProfile] = oldValue
            settingsAdapter?.notifyItemChanged(selectProfile)
            createSet(profile_period_chart, createDataChart())
            saveAllLists()
            myDialog.dismiss()
        }

        val yesBtn = dialogBinding.findViewById<View>(R.id.input_speed_dialog_confirm)
        yesBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }
    @SuppressLint("InflateParams")
    private fun showRenameDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_rename_profile_basal, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()
        val editText = dialogBinding.findViewById<View>(R.id.rename_profile_dialog_et) as EditText
        editText.setText(name)

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_rename_profile_confirm)
        yesBtn.setOnClickListener {
            name = editText.text.toString()
            profileNames[changeProfile] = name
            setNewTitle(name)
            saveAllLists()
            myDialog.dismiss()
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    /**                          работа с графиками                            **/
    //////////////////////////////////////////////////////////////////////////////
    private fun createSet(chart: BarChart, dataChart: java.util.ArrayList<Int>): BarDataSet {
        val values = ArrayList<BarEntry>()

        val maxVal = dataChart.maxOrNull() ?: 0
        for (i in 0 until 25) {
            val `val` = (dataChart[i]).toFloat()

            if (`val` < maxVal.toFloat()/4) {
                values.add(BarEntry(i*1.0f, `val`))
            } else {
                values.add(BarEntry(i*1.0f, `val`, image))
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
        chart.animateY(700)
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
//        chart.animateY(700)

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
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
    private fun createDataChart() :ArrayList<Int> {
        val dataChart = ArrayList<Int>()
        var plottingData = 1
        dataChart.add(0)
        if (startTimeAllPeriods[0] != 0) plottingData = inputSpeedAllPeriods[inputSpeedAllPeriods.size-1]
        for (numBar in 0..23) {
            for (item in 0 until  startTimeAllPeriods.size) {
                if (numBar == startTimeAllPeriods[item]) {
                    plottingData =  inputSpeedAllPeriods[item]
                }
            }
            dataChart.add(plottingData)
        }
        println("teg dataChart create = $dataChart")
        return dataChart
    }

    private fun onCancelPressed() {
//        navigator().runWriteBasalProfiles((changeProfile+1), name)
        navigator().goBack()
    }
    private fun setNewTitle(newTitle: String) {
        navigator().setNewTitle(newTitle)
    }

    private fun convertToSp(unit: Float): Float {
        val r: Resources = resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            unit,
            r.displayMetrics
        )
    }
    private fun convertArrayStringToList(convertedArray: Array<String>) : ArrayList<String> {
        val result = ArrayList<String>()
        for (i in convertedArray.indices) {
            result.add(convertedArray[i])
        }
        return result
    }
    private fun convertListStringToArray(convertedList: ArrayList<String>): Array<String> {
        val result = Array(convertedList.size) { "" }
        for (i in 0 until convertedList.size) {
            result[i] = convertedList[i]
        }
        return result
    }
    private fun convertArrayIntToList(convertedArray: IntArray) :ArrayList<Int> {
        val result = ArrayList<Int>()
        for (i in convertedArray.indices) {
            result.add(convertedArray[i])
        }
        return result
    }
    private fun convertListIntToArray(convertedList: ArrayList<Int>) :IntArray {
        val result = IntArray(convertedList.size)
        for (i in 0 until convertedList.size) {
            result[i] = convertedList[i]
        }
        return result
    }
    private fun saveAllLists() {
        periodNamesMain[changeProfile] = convertListStringToArray(periodNames)
        startTimeAllPeriodsMain[changeProfile] = convertListIntToArray(startTimeAllPeriods)
        inputSpeedAllPeriodsMain[changeProfile] = convertListIntToArray(inputSpeedAllPeriods)
        dataAllCharts[changeProfile] = createDataChart()
        navigator().saveArrayList(PreferenceKeys.PROFILE_NAMES, profileNames)
        navigator().saveArrayList(PreferenceKeys.DATA_ALL_CHARTS, dataAllCharts)
        navigator().saveArrayStringList(PreferenceKeys.PERIOD_NAMES_MAIN, periodNamesMain)
        navigator().saveIntArrayList(PreferenceKeys.START_TIME_ALL_PERIODS_MAIN, startTimeAllPeriodsMain)
        navigator().saveIntArrayList(PreferenceKeys.INPUT_SPEED_ALL_PERIODS_MAIN, inputSpeedAllPeriodsMain)
    }
}
