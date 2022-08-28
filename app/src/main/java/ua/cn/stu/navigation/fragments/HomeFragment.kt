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
    private var tripCount = 0
    private var odoCount = 0
    private var coveredDistance = 0
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
                finishAngle = progress.toFloat() - 109f
                rotateArrow(progress.toFloat() - 109f)
                binding.mainSpeedTv.text = progress.toString()
                binding.powerTv.text = "+" + (14 - (progress.toFloat()/10).toInt()) + "." + progress%10
                if (progress%24 == 0) {
                    tripCount += 1
                    if (tripCount < 1000) binding.tripTv.text = "TRIP: 00$tripCount"
                    if (tripCount < 100) binding.tripTv.text = "TRIP: 000$tripCount"
                    if (tripCount < 10) binding.tripTv.text = "TRIP: 0000$tripCount"
                }
                if (progress%13 == 0) {
                    odoCount += 1
                    if (tripCount < 1000) binding.odoTv.text = "ODO: 00$odoCount"
                    if (tripCount < 100) binding.odoTv.text = "ODO: 000$odoCount"
                    if (tripCount < 10) binding.odoTv.text = "ODO: 0000$odoCount"
                }
                binding.percentBatteryTv.text = (progress.toFloat()/10).toInt().toString()
                if (progress%100 == 0) {
                    coveredDistance += 1
                    binding.coveredDistanceTv.text = coveredDistance.toString()
                }
                binding.percentDistanceTv.text = progress.toString()

                if (progress < 10) { binding.dotsTv.text = ".  .  .  . " }
                if (progress >= 10) { binding.dotsTv.text = ".  .  . " }
                if (progress >= 100) { binding.dotsTv.text = ".  ." }

                binding.mainTemperatureTv.text = "$progress°"
            }
        })

        RxUpdateMainEvent.getInstance().selectBasalProfileSubjectObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
//                if (selectedProfile < profileNames.size) {
//                    binding.profilesButtonMassageTv.text = profileNames[selectedProfile]
//                }
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

            val rotate2 = RotateAnimation(
                25f - actualAngle,
                25f - finishlAngleFunc,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            rotate2.duration = 1000
            rotate2.fillAfter = true
            rotate2.interpolator = LinearInterpolator()
            binding.powerArrowIv.startAnimation(rotate2)

            val rotate3 = RotateAnimation(
                actualAngle/4 - 8f,
                finishlAngleFunc/4 - 8f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            rotate3.duration = 1000
            rotate3.fillAfter = true
            rotate3.interpolator = LinearInterpolator()
            binding.percentBatteryIv.startAnimation(rotate3)

            val rotate4 = RotateAnimation(
                15f - actualAngle/4 - 8f,
                15f - finishlAngleFunc/4 - 8f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            rotate4.duration = 1000
            rotate4.fillAfter = true
            rotate4.interpolator = LinearInterpolator()
            binding.temperatureIv.startAnimation(rotate4)

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