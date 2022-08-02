package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.dialog_enter_app_pin.*
import kotlinx.android.synthetic.main.dialog_enter_pump_pin.*
import kotlinx.android.synthetic.main.dialog_enter_settings_pin.*
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.MainActivity.Companion.activateDualPatternBolus
import ua.cn.stu.navigation.MainActivity.Companion.activateExtendedBolus
import ua.cn.stu.navigation.MainActivity.Companion.activatePinCodeApp
import ua.cn.stu.navigation.MainActivity.Companion.activatePinCodeSettings
import ua.cn.stu.navigation.MainActivity.Companion.activateStepBolus
import ua.cn.stu.navigation.MainActivity.Companion.activateSuperBolus
import ua.cn.stu.navigation.MainActivity.Companion.connectedDevice
import ua.cn.stu.navigation.MainActivity.Companion.pinCodeApp
import ua.cn.stu.navigation.MainActivity.Companion.pinCodeSettings
import ua.cn.stu.navigation.MainActivity.Companion.tupOnList
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.*
import ua.cn.stu.navigation.contract.ConstantManager.Companion.CANNULE_RESOURCE
import ua.cn.stu.navigation.databinding.FragmentSettingsBinding
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys

class SettingsFragment : Fragment(), HasCustomTitle, HasReturnAction {
    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        initIU()

        binding.settingsResetCannuleTimerBtn.setOnClickListener {
//            navigator().runCannuleTimeRegister(2, true)
//            navigator().runCannuleTimeRegister(2, false)
        }
        binding.settingsRefillReservoirBtn.setOnClickListener { showRefillDialog() }
        binding.settingsConnectedDeviceBtn.setOnClickListener {
            navigator().showScanScreen()
            MainActivity.inScanFragmentFlag = true
            MainActivity.showInfoDialogsFlag = true
            navigator().disconnect()
            navigator().initBLEStructure()
            navigator().scanLeDevice(true)
        }
        binding.pinCodeSw.setOnClickListener {
            if (binding.pinCodeSw.isChecked) {
                showPinCodeAppDialog()
            }
            activatePinCodeApp = binding.pinCodeSw.isChecked
            navigator().saveString(PreferenceKeys.ACTIVATE_PIN_CODE_APP, activatePinCodeApp.toString())
        }
        binding.pinCodeSettigsSw.setOnClickListener {
            if ( binding.pinCodeSettigsSw.isChecked ) {
                showPinCodeSettigsDialog()
            }
            activatePinCodeSettings = binding.pinCodeSettigsSw.isChecked
            navigator().saveString(PreferenceKeys.ACTIVATE_PIN_CODE_SETTINGS, activatePinCodeSettings.toString())
        }
        binding.settingsCannuleTimerBtn.setOnClickListener {  }
        binding.stepBolusSw.setOnClickListener {
            activateStepBolus = binding.stepBolusSw.isChecked
            navigator().saveString(PreferenceKeys.ACTIVATE_STEP_BOLUS, activateStepBolus.toString())
        }
        binding.extendedBolusSw.setOnClickListener {
            activateExtendedBolus = binding.extendedBolusSw.isChecked
            navigator().saveString(PreferenceKeys.ACTIVATE_EXTENDED_BOLUS, activateExtendedBolus.toString())
        }
        binding.dualPatternBolusSw.setOnClickListener {
            activateDualPatternBolus = binding.dualPatternBolusSw.isChecked
            navigator().saveString(PreferenceKeys.ACTIVATE_DUAL_PATTERN_BOLUS, activateDualPatternBolus.toString())
        }
        binding.superBolusSw.setOnClickListener {
            activateSuperBolus =  binding.superBolusSw.isChecked
            navigator().saveString(PreferenceKeys.ACTIVATE_SUPER_BOLUS, activateSuperBolus.toString())
        }
//        binding.settingsDisconnectFromDeviceBtn.setOnClickListener {  }
        if (tupOnList) {
            showPinCodePumpDialog()
            tupOnList = false
        }

        return binding.root
    }

    private fun initIU() {
        binding.settingsNameConnectedDeviceTv.text = connectedDevice
        binding.pinCodeSw.isChecked = activatePinCodeApp
        binding.pinCodeSettigsSw.isChecked = activatePinCodeSettings
        binding.settingsCannuleTimerTv.text = getString(R.string._h, CANNULE_RESOURCE/60)
        binding.stepBolusSw.isChecked = activateStepBolus
        binding.extendedBolusSw.isChecked = activateExtendedBolus
        binding.dualPatternBolusSw.isChecked = activateDualPatternBolus
        binding.superBolusSw.isChecked = activateSuperBolus
    }

    override fun getTitleRes(): String = getString(R.string.settings)
    override fun getReturnAction(): ReturnAction {
        return ReturnAction(
            onReturnAction = {
                onCancelPressed()
            }
        )
    }

    @SuppressLint("InflateParams")
    private fun showRefillDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_refill, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialog_refill_cancel)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_refill_confirm)
        yesBtn.setOnClickListener {
            onCancelPressed()
            showInstractionRefillingDialog()
//            navigator().runInitRefillingRegister(2)
            myDialog.dismiss()
        }
    }
    @SuppressLint("InflateParams")
    private fun showInstractionRefillingDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_instruction_refilling, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_instraction_refilling_confirm)
        yesBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }
    @SuppressLint("InflateParams")
    @Suppress("DEPRECATION")
    private fun showPinCodeAppDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_enter_app_pin, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        Handler().postDelayed({
            myDialog.enter_app_pin_dialog_et.showKeyboard()
            myDialog.enter_app_pin_dialog_et.requestFocus()
            myDialog.enter_app_pin_dialog_et.isFocusableInTouchMode = true
        }, 200)

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_enter_app_pin_confirm)
        yesBtn.setOnClickListener {
            if (myDialog.enter_app_pin_dialog_et.text.length < 4) {
                Toast.makeText(context, getString(R.string.dialog_pin_app_varning), Toast.LENGTH_SHORT).show()
            } else {
                pinCodeApp = myDialog.enter_app_pin_dialog_et.text.toString()
                navigator().saveString(PreferenceKeys.PIN_CODE_APP, pinCodeApp)
                Toast.makeText(context, "pinCodeApp: $pinCodeApp", Toast.LENGTH_SHORT).show()
                hideKeyboard(myDialog.enter_app_pin_dialog_et)
                myDialog.dismiss()
            }
        }
    }
    @SuppressLint("InflateParams")
    @Suppress("DEPRECATION")
    private fun showPinCodeSettigsDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_enter_settings_pin, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        Handler().postDelayed({
            myDialog.enter_settings_pin_dialog_et.showKeyboard()
            myDialog.enter_settings_pin_dialog_et.requestFocus()
            myDialog.enter_settings_pin_dialog_et.isFocusableInTouchMode = true
        }, 200)

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_enter_settings_pin_confirm)
        yesBtn.setOnClickListener {
            if (myDialog.enter_settings_pin_dialog_et.text.length < 4) {
                Toast.makeText(context, getString(R.string.dialog_pin_app_varning), Toast.LENGTH_SHORT).show()
            } else {
                pinCodeSettings = myDialog.enter_settings_pin_dialog_et.text.toString()
                navigator().saveString(PreferenceKeys.PIN_CODE_SETTINGS, pinCodeSettings)
                Toast.makeText(context, "pinCodeSettings: $pinCodeSettings", Toast.LENGTH_SHORT).show()
                hideKeyboard(myDialog.enter_settings_pin_dialog_et)
                myDialog.dismiss()
            }
        }
    }
    @SuppressLint("InflateParams")
    @Suppress("DEPRECATION")
    private fun showPinCodePumpDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_enter_pump_pin, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        myDialog.pincode_pump_view.requestToShowKeyboard()
        Handler().postDelayed({
            myDialog.pincode_pump_view.showKeyboard()
        }, 200)

        myDialog.pincode_pump_view.setPasscodeEntryListener { passcode ->
            MainActivity.inScanFragmentFlag = false
            navigator().reconnect()
            MainActivity.connectionPassword = passcode
            hideKeyboard(myDialog.pincode_pump_view)
            myDialog.dismiss()
        }
    }

    private fun View.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    private fun onCancelPressed() {
        navigator().goBack()
    }
}