package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
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
    private var timer: CountDownTimer? = null
    private var actualAngle = 0f
    private var finishAngle = 0f
    private var animationAllowed = true

    @SuppressLint("InflateParams", "SetTextI18n", "ClickableViewAccessibility", "CheckResult",
        "NotifyDataSetChanged"
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        myDialogInfo = Dialog(requireContext())
        navigator().showBottomNavigationMenu(true)
        println("Home fragment started")

        binding.testSb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                finishAngle = progress.toFloat()
                rotateArrow(progress.toFloat())
            }
        })

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


    private fun rotateArrow(finishlAngleFunc: Float) {
        if (animationAllowed) {
            animationAllowed = false


            val rotate = RotateAnimation(
                actualAngle,
                finishlAngleFunc,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            rotate.duration = 1000
            rotate.fillAfter = true
            rotate.interpolator = LinearInterpolator()
            binding.speedArrowIv.startAnimation(rotate)


            timer = object : CountDownTimer(1000, 1) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    animationAllowed = true
                    actualAngle = finishAngle
                    if (finishAngle != finishlAngleFunc) {
                        println("finishlAngleFunc = $finishlAngleFunc     finishAngle = $finishAngle ")
                        actualAngle = finishlAngleFunc
                        rotateArrow(finishAngle)
                    }
                }
            }.start()
        }
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