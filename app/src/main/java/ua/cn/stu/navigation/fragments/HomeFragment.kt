package ua.cn.stu.navigation.fragments

import android.R
import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import io.reactivex.android.schedulers.AndroidSchedulers
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.MainActivity.Companion.activatePinCodeSettings
import ua.cn.stu.navigation.MainActivity.Companion.profileNames
import ua.cn.stu.navigation.MainActivity.Companion.selectedProfile
import ua.cn.stu.navigation.contract.*
import ua.cn.stu.navigation.databinding.FragmentHomeBinding
import ua.cn.stu.navigation.rx.RxUpdateMainEvent


class HomeFragment : Fragment(), HasBatteryAction {

    private lateinit var binding: FragmentHomeBinding
    private var myDialogInfo: Dialog? = null
//    private val myRotation: Animation =
//        AnimationUtils.loadAnimation(requireContext(), R.anim.my_rotator)

    @SuppressLint("InflateParams", "SetTextI18n", "ClickableViewAccessibility", "CheckResult",
        "NotifyDataSetChanged"
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        myDialogInfo = Dialog(requireContext())
        navigator().showBottomNavigationMenu(true)
        println("Home fragment started")

//        val myRotation: Animation =
//            AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out)

//        binding.mainIv.startAnimation(myRotation)

        binding.profilesButton.setOnClickListener {
            println("I love Yanochka")
            if (MainActivity.refreshBasalProfile) {
//                showInfoBasalProfileDialog()
            }
            else { onProfilePressed() }
        }
        binding.settingsButton.setOnClickListener { onSettingsPressed() }
        binding.bolusButton.setOnClickListener { onBolusPressed() }
        binding.basalButton.setOnClickListener {
            if (MainActivity.temporaryBasalActivated) {
//                navigator().runTemporaryBasalStatus()
            }
            else { onTemporaryBasalPressed() }
        }
        if (profileNames.size > 1) {
            binding.profilesButtonMassageTv.text = profileNames[selectedProfile]
        }

        RxUpdateMainEvent.getInstance().selectBasalProfileSubjectObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (selectedProfile < profileNames.size) {
                    binding.profilesButtonMassageTv.text = profileNames[selectedProfile]
                }
            }

        RxUpdateMainEvent.getInstance().updateChatSubjectObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
//                adapter?.notifyDataSetChanged()
//                scrollToEndChat(binding.chatRv)
            }
        return binding.root
    }


    override fun getBatteryAction(): BatteryAction {
        return BatteryAction(
            onBatteryAction = {
//                showBatteryDialog()
            }
        )
    }

    private fun onProfilePressed() {
        navigator().showProfileScreen()
    }
    private fun onSettingsPressed() {
        if (activatePinCodeSettings) {
//            showPinCodeSettigsDialog()
        } else {
            navigator().showSettingsScreen()
        }
    }
    private fun onBolusPressed() {
        navigator().showBolusScreen()
    }
    private fun onTemporaryBasalPressed() {
        navigator().showTemporaryBasalScreen()
    }
}