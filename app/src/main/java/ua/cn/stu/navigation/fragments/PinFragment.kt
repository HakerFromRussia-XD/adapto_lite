package ua.cn.stu.navigation.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.MainActivity.Companion.attemptsToUnlock
import ua.cn.stu.navigation.MainActivity.Companion.pinCodeApp
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.HasCustomTitle
import ua.cn.stu.navigation.contract.RenameProfileAction
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentPinBinding
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys

@Suppress("DEPRECATION")
class PinFragment : Fragment(), HasCustomTitle {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPinBinding.inflate(inflater, container, false)

        binding.pincodeView.requestToShowKeyboard()
        Handler().postDelayed({
            binding.pincodeView.showKeyboard()
        }, 200)


        binding.pincodeView.setPasscodeEntryListener { passcode ->
            System.err.println("tup enter passcode: $passcode")
            if (passcode == pinCodeApp) {
                goToMenu()
                hideKeyboard(binding.pincodeView)
                attemptsToUnlock = 3
                navigator().saveInt(PreferenceKeys.ATTEMPTS_TO_UN_LOCK, attemptsToUnlock)
            } else {
                attemptsToUnlock -= 1
                navigator().saveInt(PreferenceKeys.ATTEMPTS_TO_UN_LOCK, attemptsToUnlock)
                if (attemptsToUnlock != 0) {
                Toast.makeText(
                    context,
                    getString(R.string.app_pin_entering_varning, attemptsToUnlock),
                    Toast.LENGTH_SHORT
                ).show()
                } else {
                    Toast.makeText(
                        context,
                        getString(R.string.app_main_varning),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        }

        return binding.root
    }

    private fun View.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun getTitleRes(): String = getString(R.string.enter_pincode)

    private fun goToMenu() {
        navigator().firstOpenMenu()
    }
}