package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
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
import ua.cn.stu.navigation.databinding.FragmentExtendedBolusBinding
import ua.cn.stu.navigation.databinding.FragmentStepBolusBinding

class ExtendedBolusFragment : Fragment(), HasCustomTitle, HasReturnAction {

    private lateinit var binding: FragmentExtendedBolusBinding

    @SuppressLint("StringFormatMatches")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentExtendedBolusBinding.inflate(inflater, container, false)

        binding.unitExtendedBolus.minValue = 0
        binding.unitExtendedBolus.maxValue = 49
        binding.unitExtendedBolus.value = 5
        binding.unitExtendedBolus.setFormatter { i -> String.format("%02d", i) }
        binding.unitExtendedBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("unitStepBolus oldVal=$oldVal  newVal=$newVal")
            if (binding.unitExtendedBolus.value == 0) {
                binding.subunitExtendedBolus.minValue = 1
            } else {
                binding.subunitExtendedBolus.minValue = 0
            }
        }
        binding.subunitExtendedBolus.minValue = 0
        binding.subunitExtendedBolus.maxValue = 99
        binding.subunitExtendedBolus.value = 0
        binding.subunitExtendedBolus.setFormatter { i -> String.format("%02d", i) }
        binding.subunitExtendedBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("subunitStepBolus oldVal=$oldVal  newVal=$newVal")
        }
        binding.extendedBolusAddBtn.setOnClickListener { binding.subunitExtendedBolus.value += 1 }
        binding.extendedBolusDecBtn.setOnClickListener { binding.subunitExtendedBolus.value -= 1 }

        binding.timeExtendedBolus.minValue = 30
        binding.timeExtendedBolus.maxValue = 360
        binding.timeExtendedBolus.value = 30
        binding.timeExtendedBolus.setFormatter { i -> String.format("%02d", i) }
        binding.timeExtendedBolus.setOnValueChangedListener { _, oldVal, newVal ->
            println("subunitStepBolus oldVal=$oldVal  newVal=$newVal")
        }

        binding.extendedBolusTimeAddBtn.setOnClickListener { binding.timeExtendedBolus.value += 10 }
        binding.extendedBolusTimeDecBtn.setOnClickListener { binding.timeExtendedBolus.value -= 10 }

        binding.extendedBolusGoBtn.setOnClickListener {
            navigator().showGoBolusDialog(
                title = getString(R.string.enter_bolus),
                getString(R.string.do_you_want_enter_extended_bolus_whith_2fu_basal_volume, (binding.subunitExtendedBolus.value + binding.unitExtendedBolus.value*100).toFloat()/100 ,
                    binding.timeExtendedBolus.value),
                binding.subunitExtendedBolus.value + binding.unitExtendedBolus.value*100,
                0,
                binding.timeExtendedBolus.value
            )
        }

        return binding.root
    }

    override fun getTitleRes(): String = getString(R.string.extended)+" "+getString(R.string.bolus)
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