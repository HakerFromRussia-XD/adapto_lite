package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
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
import ua.cn.stu.navigation.databinding.FragmentDualPatternBolusBinding
import ua.cn.stu.navigation.databinding.FragmentExtendedBolusBinding
import ua.cn.stu.navigation.databinding.FragmentStepBolusBinding

class DualPatternBolusFragment : Fragment(), HasCustomTitle, HasReturnAction {

    private lateinit var binding: FragmentDualPatternBolusBinding

    @SuppressLint("StringFormatMatches")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDualPatternBolusBinding.inflate(inflater, container, false)

        binding.unitDualPatternBolus.minValue = 0
        binding.unitDualPatternBolus.maxValue = 49
        binding.unitDualPatternBolus.value = 5
        binding.unitDualPatternBolus.setFormatter { i -> String.format("%02d", i) }
        binding.unitDualPatternBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("unitStepBolus oldVal=$oldVal  newVal=$newVal")
            if (binding.unitDualPatternBolus.value == 0) {
                binding.subunitDualPatternBolus.minValue = 1
            } else {
                binding.subunitDualPatternBolus.minValue = 0
            }
        }
        binding.subunitDualPatternBolus.minValue = 0
        binding.subunitDualPatternBolus.maxValue = 99
        binding.subunitDualPatternBolus.value = 0
        binding.subunitDualPatternBolus.setFormatter { i -> String.format("%02d", i) }
        binding.subunitDualPatternBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("subunitStepBolus oldVal=$oldVal  newVal=$newVal")
        }
        binding.dualPatternBolusAddBtn.setOnClickListener { binding.subunitDualPatternBolus.value += 1 }
        binding.dualPatternBolusDecBtn.setOnClickListener { binding.subunitDualPatternBolus.value -= 1 }

        binding.unitDualPatternSecondBolus.minValue = 0
        binding.unitDualPatternSecondBolus.maxValue = 49
        binding.unitDualPatternSecondBolus.value = 5
        binding.unitDualPatternSecondBolus.setFormatter { i -> String.format("%02d", i) }
        binding.unitDualPatternSecondBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("unitStepBolus oldVal=$oldVal  newVal=$newVal")
            if (binding.unitDualPatternSecondBolus.value == 0) {
                binding.subunitDualPatternSecondBolus.minValue = 1
            } else {
                binding.subunitDualPatternSecondBolus.minValue = 0
            }
        }
        binding.subunitDualPatternSecondBolus.minValue = 0
        binding.subunitDualPatternSecondBolus.maxValue = 99
        binding.subunitDualPatternSecondBolus.value = 0
        binding.subunitDualPatternSecondBolus.setFormatter { i -> String.format("%02d", i) }
        binding.subunitDualPatternSecondBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("subunitStepBolus oldVal=$oldVal  newVal=$newVal")
        }
        binding.dualPatternSecondBolusAddBtn.setOnClickListener { binding.subunitDualPatternSecondBolus.value += 1 }
        binding.dualPatternSecondBolusDecBtn.setOnClickListener { binding.subunitDualPatternSecondBolus.value -= 1 }

        binding.timeDualPatternBolus.minValue = 30
        binding.timeDualPatternBolus.maxValue = 360
        binding.timeDualPatternBolus.value = 30
        binding.timeDualPatternBolus.setFormatter { i -> String.format("%02d", i) }
        binding.timeDualPatternBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("subunitStepBolus oldVal=$oldVal  newVal=$newVal")
        }

        binding.dualPatternBolusTimeAddBtn.setOnClickListener { binding.timeDualPatternBolus.value += 10 }
        binding.dualPatternBolusTimeDecBtn.setOnClickListener { binding.timeDualPatternBolus.value -= 10 }

        binding.dualPatternBolusGoBtn.setOnClickListener {
            navigator().showGoBolusDialog(
                title = getString(R.string.enter_bolus),
                getString(R.string.do_you_want_enter_dual_pattern_bolus_whith_2fu_basal_volume,
                                (binding.subunitDualPatternBolus.value + binding.unitDualPatternBolus.value*100).toFloat()/100 ,
                                          (binding.subunitDualPatternSecondBolus.value + binding.unitDualPatternSecondBolus.value*100).toFloat()/100,
                                          binding.timeDualPatternBolus.value),
                    binding.subunitDualPatternBolus.value + binding.unitDualPatternBolus.value*100,
                binding.subunitDualPatternSecondBolus.value + binding.unitDualPatternSecondBolus.value*100,
                binding.timeDualPatternBolus.value
            )
        }

        return binding.root
    }

    override fun getTitleRes(): String = getString(R.string.dual_pattern)+" "+getString(R.string.bolus)
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