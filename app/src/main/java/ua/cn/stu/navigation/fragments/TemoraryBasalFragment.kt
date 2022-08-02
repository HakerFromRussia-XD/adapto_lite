package ua.cn.stu.navigation.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.HasCustomTitle
import ua.cn.stu.navigation.contract.HasReturnAction
import ua.cn.stu.navigation.contract.ReturnAction
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentTemporaryBasalBinding

class TemoraryBasalFragment : Fragment(), HasCustomTitle, HasReturnAction {

    private lateinit var binding: FragmentTemporaryBasalBinding

    @Suppress("DEPRECATION")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTemporaryBasalBinding.inflate(inflater, container, false)
        MainActivity.stayOnTemporaryBasalScreen = true

        binding.unitTemporaryBasal.minValue = 0
        binding.unitTemporaryBasal.maxValue = 49
        binding.unitTemporaryBasal.value = 5
        binding.unitTemporaryBasal.setFormatter { i -> String.format("%02d", i) }
        binding.unitTemporaryBasal.setOnValueChangedListener { _, oldVal, newVal ->
            println("unitStepBolus oldVal=$oldVal  newVal=$newVal")
            if (binding.unitTemporaryBasal.value == 0) {
                binding.subunitTemporaryBasal.minValue = 1
            } else {
                binding.subunitTemporaryBasal.minValue = 0
            }
        }
        binding.subunitTemporaryBasal.minValue = 0
        binding.subunitTemporaryBasal.maxValue = 99
        binding.subunitTemporaryBasal.value = 0
        binding.subunitTemporaryBasal.setFormatter { i -> String.format("%02d", i) }
        binding.subunitTemporaryBasal.setOnValueChangedListener { _, oldVal, newVal ->
            println("subunitStepBolus oldVal=$oldVal  newVal=$newVal")
        }
        binding.temporaryBasalAddBtn.setOnClickListener { binding.subunitTemporaryBasal.value += 1 }
        binding.temporaryBasalDecBtn.setOnClickListener { binding.subunitTemporaryBasal.value -= 1 }

        binding.timeTemporaryBasal.minValue = 1
        binding.timeTemporaryBasal.maxValue = 240
        binding.timeTemporaryBasal.value = 1
        binding.timeTemporaryBasal.setFormatter { i -> String.format("%02d", i) }
        binding.timeTemporaryBasal.setOnValueChangedListener { _, oldVal, newVal ->
            println("subunitStepBolus oldVal=$oldVal  newVal=$newVal")
        }

        binding.temporaryBasalTimeAddBtn.setOnClickListener { binding.timeTemporaryBasal.value += 10 }
        binding.temporaryBasalTimeDecBtn.setOnClickListener { binding.timeTemporaryBasal.value -= 10 }

        binding.temporaryBasalGoBtn.setOnClickListener {
//            navigator().runTemporaryBasal((binding.subunitTemporaryBasal.value+binding.unitTemporaryBasal.value*100), binding.timeTemporaryBasal.value)
            Handler().postDelayed({
                MainActivity.stayOnTemporaryBasalScreen = false
            }, 1000)
            onOkPressed()
        }

        return binding.root
    }

    override fun getTitleRes(): String = getString(R.string.temporary_basal)
    override fun getReturnAction(): ReturnAction {
        return ReturnAction(
            onReturnAction = {
                onOkPressed()
            }
        )
    }

    private fun onOkPressed() {
        navigator().goBack()
    }
}