package ua.cn.stu.navigation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.MainActivity.Companion.activateDualPatternBolus
import ua.cn.stu.navigation.MainActivity.Companion.activateExtendedBolus
import ua.cn.stu.navigation.MainActivity.Companion.activateStepBolus
import ua.cn.stu.navigation.MainActivity.Companion.activateSuperBolus
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.*
import ua.cn.stu.navigation.databinding.FragmentBolusBinding

class BolusFragment : Fragment(), HasCustomTitle, HasReturnAction {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View{
        val binding = FragmentBolusBinding.inflate(inflater, container, false)

        binding.stepBolusBtn.setOnClickListener {
            MainActivity.bolusType = 0
            onStepBolusPressed() }
        binding.dualPatternBolusBtn.setOnClickListener {
            MainActivity.bolusType = 3
            onDualPatternBolusPressed() }
        binding.extendedBolusBtn.setOnClickListener {
            MainActivity.bolusType = 2
            onExtendedBolusPressed() }
        binding.superBolusBtn.setOnClickListener {
            MainActivity.bolusType = 1
            onSuperBolusPressed() }

        return binding.root
    }

    override fun getTitleRes(): String = getString(R.string.bolus)
    override fun getReturnAction(): ReturnAction {
        return ReturnAction(
            onReturnAction = {
                onOkPressed()
            }
        )
    }

    private fun onStepBolusPressed() {
        if (activateStepBolus) {
            navigator().showStepBolusScreen()
        } else {
            Toast.makeText(context, getString(R.string.step_bolus_varning), Toast.LENGTH_SHORT).show()
        }
    }
    private fun onExtendedBolusPressed() {
        if (activateExtendedBolus) {
            navigator().showExtendedBolusScreen()
        } else {
            Toast.makeText(context, getString(R.string.extended_bolus_varning), Toast.LENGTH_SHORT).show()
        }
    }
    private fun onDualPatternBolusPressed() {
        if (activateDualPatternBolus) {
            navigator().showDualPatternBolusScreen()
        } else {
            Toast.makeText(context, getString(R.string.dual_pattern_bolus_varning), Toast.LENGTH_SHORT).show()
        }
    }
    private fun onSuperBolusPressed() {
        if (activateSuperBolus) {
            navigator().showSuperBolusScreen()
        } else {
            Toast.makeText(context, getString(R.string.super_bolus_varning), Toast.LENGTH_SHORT).show()
        }
    }
    private fun onOkPressed() {
        navigator().goBack()
    }

}