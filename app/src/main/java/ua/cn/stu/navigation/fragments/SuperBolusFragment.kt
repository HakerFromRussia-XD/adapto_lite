package ua.cn.stu.navigation.fragments

import android.os.Bundle
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
import ua.cn.stu.navigation.databinding.FragmentSuperBolusBinding

class SuperBolusFragment : Fragment(), HasCustomTitle, HasReturnAction {

    private lateinit var binding: FragmentSuperBolusBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSuperBolusBinding.inflate(inflater, container, false)

        binding.unitSuperBolus.minValue = 0
        binding.unitSuperBolus.maxValue = 49
        binding.unitSuperBolus.value = 5
        binding.unitSuperBolus.setFormatter { i -> String.format("%02d", i) }
        binding.unitSuperBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("unitStepBolus oldVal=$oldVal  newVal=$newVal")
            if (binding.unitSuperBolus.value == 0) {
                binding.subunitSuperBolus.minValue = 1
            } else {
                binding.subunitSuperBolus.minValue = 0
            }
        }
        binding.subunitSuperBolus.minValue = 0
        binding.subunitSuperBolus.maxValue = 99
        binding.subunitSuperBolus.value = 0
        binding.subunitSuperBolus.setFormatter { i -> String.format("%02d", i) }
        binding.subunitSuperBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("subunitStepBolus oldVal=$oldVal  newVal=$newVal")
        }
        binding.superBolusAddBtn.setOnClickListener { binding.subunitSuperBolus.value += 1 }
        binding.superBolusDecBtn.setOnClickListener { binding.subunitSuperBolus.value -= 1 }

        binding.timeSuperBolus.minValue = 30
        binding.timeSuperBolus.maxValue = 360
        binding.timeSuperBolus.value = 30
        binding.timeSuperBolus.setFormatter { i -> String.format("%02d", i) }
        binding.timeSuperBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("subunitStepBolus oldVal=$oldVal  newVal=$newVal")
        }

        binding.superBolusTimeAddBtn.setOnClickListener { binding.timeSuperBolus.value += 10 }
        binding.superBolusTimeDecBtn.setOnClickListener { binding.timeSuperBolus.value -= 10 }

        binding.superBolusGoBtn.setOnClickListener {
            MainActivity.superBolusVoliume = binding.subunitSuperBolus.value + binding.unitSuperBolus.value*100
            MainActivity.superBolusTime = binding.timeSuperBolus.value
//            navigator().runSuperBolus((binding.subunitSuperBolus.value + binding.unitSuperBolus.value*100), binding.timeSuperBolus.value)
        }

        return binding.root
    }

    override fun getTitleRes(): String = getString(R.string.superr)+" "+getString(R.string.bolus)
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