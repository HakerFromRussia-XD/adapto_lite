package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import io.reactivex.android.schedulers.AndroidSchedulers
import ua.cn.stu.navigation.MainActivity.Companion.param1
import ua.cn.stu.navigation.MainActivity.Companion.param2
import ua.cn.stu.navigation.MainActivity.Companion.param3
import ua.cn.stu.navigation.MainActivity.Companion.param4
import ua.cn.stu.navigation.MainActivity.Companion.param5
import ua.cn.stu.navigation.MainActivity.Companion.param6
import ua.cn.stu.navigation.MainActivity.Companion.param7
import ua.cn.stu.navigation.MainActivity.Companion.param8
import ua.cn.stu.navigation.MainActivity.Companion.param9
import ua.cn.stu.navigation.MainActivity.Companion.param10
import ua.cn.stu.navigation.MainActivity.Companion.param11
import ua.cn.stu.navigation.MainActivity.Companion.param12
import ua.cn.stu.navigation.MainActivity.Companion.param13
import ua.cn.stu.navigation.MainActivity.Companion.param14
import ua.cn.stu.navigation.MainActivity.Companion.param15
import ua.cn.stu.navigation.MainActivity.Companion.param16
import ua.cn.stu.navigation.MainActivity.Companion.param17
import ua.cn.stu.navigation.MainActivity.Companion.param18
import ua.cn.stu.navigation.MainActivity.Companion.param19
import ua.cn.stu.navigation.MainActivity.Companion.param20
import ua.cn.stu.navigation.MainActivity.Companion.param21
import ua.cn.stu.navigation.MainActivity.Companion.param22
import ua.cn.stu.navigation.MainActivity.Companion.param23
import ua.cn.stu.navigation.MainActivity.Companion.param24
import ua.cn.stu.navigation.MainActivity.Companion.param25
import ua.cn.stu.navigation.MainActivity.Companion.param26
import ua.cn.stu.navigation.MainActivity.Companion.param27
import ua.cn.stu.navigation.MainActivity.Companion.param28
import ua.cn.stu.navigation.MainActivity.Companion.param29
import ua.cn.stu.navigation.MainActivity.Companion.param30

import ua.cn.stu.navigation.MainActivity.Companion.param1name
import ua.cn.stu.navigation.MainActivity.Companion.param2name
import ua.cn.stu.navigation.MainActivity.Companion.param3name
import ua.cn.stu.navigation.MainActivity.Companion.param4name
import ua.cn.stu.navigation.MainActivity.Companion.param5name
import ua.cn.stu.navigation.MainActivity.Companion.param6name
import ua.cn.stu.navigation.MainActivity.Companion.param7name
import ua.cn.stu.navigation.MainActivity.Companion.param8name
import ua.cn.stu.navigation.MainActivity.Companion.param9name
import ua.cn.stu.navigation.MainActivity.Companion.param10name
import ua.cn.stu.navigation.MainActivity.Companion.param11name
import ua.cn.stu.navigation.MainActivity.Companion.param12name
import ua.cn.stu.navigation.MainActivity.Companion.param13name
import ua.cn.stu.navigation.MainActivity.Companion.param14name
import ua.cn.stu.navigation.MainActivity.Companion.param15name
import ua.cn.stu.navigation.MainActivity.Companion.param16name
import ua.cn.stu.navigation.MainActivity.Companion.param17name
import ua.cn.stu.navigation.MainActivity.Companion.param18name
import ua.cn.stu.navigation.MainActivity.Companion.param19name
import ua.cn.stu.navigation.MainActivity.Companion.param20name
import ua.cn.stu.navigation.MainActivity.Companion.param21name
import ua.cn.stu.navigation.MainActivity.Companion.param22name
import ua.cn.stu.navigation.MainActivity.Companion.param23name
import ua.cn.stu.navigation.MainActivity.Companion.param24name
import ua.cn.stu.navigation.MainActivity.Companion.param25name
import ua.cn.stu.navigation.MainActivity.Companion.param26name
import ua.cn.stu.navigation.MainActivity.Companion.param27name
import ua.cn.stu.navigation.MainActivity.Companion.param28name
import ua.cn.stu.navigation.MainActivity.Companion.param29name
import ua.cn.stu.navigation.MainActivity.Companion.param30name
import ua.cn.stu.navigation.contract.DisconnectionAction
import ua.cn.stu.navigation.contract.HasDisconnectionAction

import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentDebugBinding
import ua.cn.stu.navigation.rx.RxUpdateMainEvent

class DebugFragment : Fragment(), HasDisconnectionAction {
    private lateinit var binding: FragmentDebugBinding


    @SuppressLint("CheckResult")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDebugBinding.inflate(inflater, container, false)
        navigator().showBottomNavigationMenu(true)
        println("Debug fragment started")

        RxUpdateMainEvent.getInstance().debugFragmentObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { updateUI() }

        return binding.root
    }

    private fun updateUI() {
//        System.err.println("-->    updateUI  param1:$param1    param1name:$param1name")
        binding.param1.text = param1.value
        binding.val1.text = param1name.value
        binding.param2.text = param2.value
        binding.val2.text = param2name.value
        binding.param3.text = param3.value
        binding.val3.text = param3name.value
        binding.param4.text = param4.value
        binding.val4.text = param4name.value
        binding.param5.text = param5.value
        binding.val5.text = param5name.value
        binding.param6.text = param6.value
        binding.val6.text = param6name.value
        binding.param7.text = param7.value
        binding.val7.text = param7name.value
        binding.param8.text = param8.value
        binding.val8.text = param8name.value
        binding.param9.text = param9.value
        binding.val9.text = param9name.value

        binding.param10.text = param10.value
        binding.val10.text = param10name.value
        binding.param11.text = param11.value
        binding.val11.text = param11name.value
        binding.param12.text = param12.value
        binding.val12.text = param12name.value
        binding.param13.text = param13.value
        binding.val13.text = param13name.value
        binding.param14.text = param14.value
        binding.val14.text = param14name.value
        binding.param15.text = param15.value
        binding.val15.text = param15name.value
        binding.param16.text = param16.value
        binding.val16.text = param16name.value
        binding.param17.text = param17.value
        binding.val17.text = param17name.value
        binding.param18.text = param18.value
        binding.val18.text = param18name.value
        binding.param19.text = param19.value
        binding.val19.text = param19name.value

        binding.param20.text = param20.value
        binding.val20.text = param20name.value
        binding.param21.text = param21.value
        binding.val21.text = param21name.value
        binding.param22.text = param22.value
        binding.val22.text = param22name.value
        binding.param23.text = param23.value
        binding.val23.text = param23name.value
        binding.param24.text = param24.value
        binding.val24.text = param24name.value
        binding.param25.text = param25.value
        binding.val25.text = param25name.value
        binding.param26.text = param26.value
        binding.val26.text = param26name.value
        binding.param27.text = param27.value
        binding.val27.text = param27name.value
        binding.param28.text = param28.value
        binding.val28.text = param28name.value
        binding.param29.text = param29.value
        binding.val29.text = param29name.value

        binding.param30.text = param30.value
        binding.val30.text = param30name.value
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