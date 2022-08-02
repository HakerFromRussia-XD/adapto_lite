package ua.cn.stu.navigation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.HasCustomTitle
import ua.cn.stu.navigation.contract.HasReturnAction
import ua.cn.stu.navigation.contract.ReturnAction
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentStepBolusBinding

class StepBolusFragment : Fragment(), HasCustomTitle, HasReturnAction {

    private lateinit var binding: FragmentStepBolusBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStepBolusBinding.inflate(inflater, container, false)

        binding.unitStepBolus.minValue = 0
        binding.unitStepBolus.maxValue = 49
        binding.unitStepBolus.value = 5
        binding.unitStepBolus.setFormatter { i -> String.format("%02d", i) }
        binding.unitStepBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("unitStepBolus oldVal=$oldVal  newVal=$newVal")
            if (binding.unitStepBolus.value == 0) {
                binding.subunitStepBolus.minValue = 1
            } else {
                binding.subunitStepBolus.minValue = 0
                if (binding.unitStepBolus.value == 50) {
                    binding.subunitStepBolus.maxValue = 0
                    binding.subunitStepBolus.value = 0
                } else {
                    binding.subunitStepBolus.maxValue = 99
                }
            }
        }

        binding.subunitStepBolus.minValue = 0
        binding.subunitStepBolus.maxValue = 99
        binding.subunitStepBolus.value = 0
        binding.subunitStepBolus.setFormatter { i -> String.format("%02d", i) }
        binding.subunitStepBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("subunitStepBolus oldVal=$oldVal  newVal=$newVal")
        }

        binding.simpleBolusAddBtn.setOnClickListener {
            binding.subunitStepBolus.value += 1
        }
        binding.simpleBolusDecBtn.setOnClickListener {
            binding.subunitStepBolus.value -= 1
        }
        binding.simpleBolusGoBtn.setOnClickListener {
            navigator().showGoBolusDialog(
                getString(R.string.enter_bolus),
                getString(R.string.do_you_want_enter_bolus_whith_2fu_basal_volume, (binding.subunitStepBolus.value + binding.unitStepBolus.value*100).toFloat()/100),
                binding.subunitStepBolus.value + binding.unitStepBolus.value*100,
                0,
                0)
        }

        return binding.root
    }

    override fun getTitleRes(): String = getString(R.string.step)+" "+getString(R.string.bolus)
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