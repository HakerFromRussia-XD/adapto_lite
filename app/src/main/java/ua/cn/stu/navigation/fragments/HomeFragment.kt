package ua.cn.stu.navigation.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private var myDialogInfo: Dialog? = null
    private var timer: CountDownTimer? = null
    private var actualAngle = 0f
    private var finishAngle = 0f
    private var actualPercentAlpha = 0f
    private var finishPercentAlpha = 0f
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
                finishPercentAlpha = progress.toFloat()/146*100
                rotateArrow(finishAngle, finishPercentAlpha)
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
                binding.percentDistanceTv.text = (progress.toFloat()/146*100).toInt().toString()


                if (progress < 10) { binding.dotsTv.text = ".  .  .  . " }
                if (progress >= 10) { binding.dotsTv.text = ".  .  . " }
                if (progress >= 100) { binding.dotsTv.text = ".  ." }

                binding.mainTemperatureTv.text = "$progress°"
            }
        })

        return binding.root
    }


    @SuppressLint("Recycle")
    private fun rotateArrow(finishlAngleFunc: Float, finishlAlphaFunc: Float) {
        if (animationAllowed) {
            animationAllowed = false

            val rotatinA: ObjectAnimator =
                ObjectAnimator.ofFloat(binding.speedArrowIv, View.ROTATION, actualAngle, finishlAngleFunc)
            rotatinA.duration = 300
            rotatinA.interpolator = LinearInterpolator()

            val alphaA: ObjectAnimator = ObjectAnimator.ofFloat(binding.speedArrowIv, View.ALPHA, 1.5f - actualPercentAlpha/100, 1.5f - finishlAlphaFunc/100)
            alphaA.duration = 300
            alphaA.interpolator = LinearInterpolator()

            val anim = AnimatorSet()
            anim.play(rotatinA).with(alphaA)
            anim.start()

            val rotatinA2: ObjectAnimator =
                ObjectAnimator.ofFloat(binding.speedArrowLvl3Iv, View.ROTATION, actualAngle, finishlAngleFunc)
            rotatinA2.duration = 300
            rotatinA2.interpolator = LinearInterpolator()

            val alphaA2: ObjectAnimator = ObjectAnimator.ofFloat(binding.speedArrowLvl3Iv, View.ALPHA,  actualPercentAlpha/75 - 0.5f, finishlAlphaFunc/75 - 0.5f)
            alphaA2.duration = 300
            alphaA2.interpolator = LinearInterpolator()

            val anim2 = AnimatorSet()
            anim2.play(rotatinA2).with(alphaA2)
            anim2.start()


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

            timer = object : CountDownTimer(300, 1) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    animationAllowed = true
                    actualAngle = finishAngle
                    actualPercentAlpha = finishPercentAlpha
                    if (finishAngle != finishlAngleFunc) {
                        println("finishlAngleFunc = $finishlAngleFunc     finishAngle = $finishAngle ")
                        actualAngle = finishlAngleFunc
                        actualPercentAlpha = finishlAlphaFunc
                        rotateArrow(finishAngle, finishPercentAlpha)
                    }
                }
            }.start()
        }
    }
}