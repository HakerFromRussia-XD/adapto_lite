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
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import io.reactivex.android.schedulers.AndroidSchedulers
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.MainActivity.Companion.param2
import ua.cn.stu.navigation.MainActivity.Companion.param2name
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentHomeBinding
import ua.cn.stu.navigation.rx.RxUpdateMainEvent


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
    private val ANIMATION_DURATION = 300L
    private var stateMode = "ECO"
    private var statePower = "kW"
    private var stateBattery = "%"
    private var stateTemperatureSource = "CONTROLLER"
    private var trackingAngle = 0f

    @SuppressLint("InflateParams", "SetTextI18n", "ClickableViewAccessibility", "CheckResult",
        "NotifyDataSetChanged", "UseCompatLoadingForDrawables"
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
                finishAngle = progress.toFloat() - 135f
                finishPercentAlpha = progress.toFloat()/172*100
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
                binding.percentDistanceTv.text = (progress.toFloat()/172*100).toInt().toString()


                if (progress < 10) { binding.dotsTv.text = ".  .  .  . " }
                if (progress >= 10) { binding.dotsTv.text = ".  .  . " }
                if (progress >= 100) { binding.dotsTv.text = ".  ." }

                binding.mainTemperatureTv.text = "$progress°"
            }
        })

        binding.modeBtn.setOnClickListener {
            System.err.println("Click mode button")
            when(stateMode) {
                "ECO" -> {
                    binding.modeTv.text = "NORMAL"
                    binding.highlightsMainIv.setImageDrawable(context?.resources?.getDrawable(R.drawable.side_highlights_blue))
                    stateMode = "NORMAL"
                }
                "NORMAL" -> {
                    binding.modeTv.text = "BOOST"
                    binding.highlightsMainIv.setImageDrawable(context?.resources?.getDrawable(R.drawable.side_highlights_red))
                    stateMode = "BOOST"
                }
                "BOOST" -> {
                    binding.modeTv.text = "ECO"
                    binding.highlightsMainIv.setImageDrawable(context?.resources?.getDrawable(R.drawable.side_highlights_green))
                    stateMode = "ECO"
                }
            }
        }

        binding.powerBtn.setOnClickListener {
            when(statePower) {
                "kW" -> {
                    binding.powerUnitsTv.text = "A"
                    statePower = "A"
                }
                "A" -> {
                    binding.powerUnitsTv.text = "kW"
                    statePower = "kW"
                }
            }
        }

        binding.batteryPercentBtn.setOnClickListener {
            when(stateBattery) {
                "%" -> {
                    binding.percentBatteryUnitsTv.text = "A"
                    stateBattery = "A"
                }
                "A" -> {
                    binding.percentBatteryUnitsTv.text = "%"
                    stateBattery = "%"
                }
            }
        }

        binding.temperatureSourceBtn.setOnClickListener {
            when(stateTemperatureSource) {
                "CONTROLLER" -> {
                    binding.mainTemperatureUnitTv.text = "MOTOR"
                    stateTemperatureSource = "MOTOR"
                }
                "MOTOR" -> {
                    binding.mainTemperatureUnitTv.text = "BATTERY"
                    stateTemperatureSource = "BATTERY"
                }
                "BATTERY" -> {
                    binding.mainTemperatureUnitTv.text = "CONTROLLER"
                    stateTemperatureSource = "CONTROLLER"
                }
            }
        }

        RxUpdateMainEvent.getInstance().homeFragmentObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                System.err.println("param2name  ${param2name.value?.toFloat()}")
                binding.testSb.progress = param2name.value!!.toInt()
            }

        return binding.root
    }


    @SuppressLint("Recycle")
    private fun rotateArrow(finishlAngleFunc: Float, finishlAlphaFunc: Float) {
        if (animationAllowed) {
            animationAllowed = false

            val rotatinA: ObjectAnimator =
                ObjectAnimator.ofFloat(binding.speedArrowIv, View.ROTATION, actualAngle, finishlAngleFunc)
            rotatinA.duration = ANIMATION_DURATION
            rotatinA.interpolator = LinearInterpolator()

            val animA = AnimatorSet()
            animA.play(rotatinA)
            animA.start()

            val rotatinB: ObjectAnimator =
                ObjectAnimator.ofFloat(binding.speedArrowLvl3Iv, View.ROTATION, actualAngle, finishlAngleFunc)
            rotatinB.duration = ANIMATION_DURATION
            rotatinB.interpolator = LinearInterpolator()

            val alphaB: ObjectAnimator = ObjectAnimator.ofFloat(binding.speedArrowLvl3Iv, View.ALPHA,  actualPercentAlpha/75 - 0.5f, finishlAlphaFunc/75 - 0.5f)
            alphaB.duration = ANIMATION_DURATION
            alphaB.interpolator = LinearInterpolator()

            val animB = AnimatorSet()
            animB.play(rotatinB).with(alphaB)
            animB.start()

            val rotate: ObjectAnimator =
                ObjectAnimator.ofFloat(binding.speedArrowLvlMinus1Iv, View.ROTATION, actualAngle, finishlAngleFunc)
            rotate.duration = ANIMATION_DURATION
            rotate.interpolator = LinearInterpolator()

            val anim = AnimatorSet()
            anim.play(rotate)
            anim.start()


            val rotate2 = RotateAnimation(
                25f - actualAngle,
                25f - finishlAngleFunc,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            rotate2.duration = ANIMATION_DURATION
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
            rotate3.duration = ANIMATION_DURATION
            rotate3.fillAfter = true
            rotate3.interpolator = LinearInterpolator()
            binding.percentBatteryIv.startAnimation(rotate3)


            val rotatin4A: ObjectAnimator =
                ObjectAnimator.ofFloat(binding.temperatureIv, View.ROTATION, 15f - actualAngle/4 - 8f, 15f - finishlAngleFunc/4 - 8f)
            rotatin4A.duration = ANIMATION_DURATION
            rotatin4A.interpolator = LinearInterpolator()

            val alpha4A: ObjectAnimator = ObjectAnimator.ofFloat(binding.temperatureIv, View.ALPHA, 1f - actualPercentAlpha /100, 1f - finishlAlphaFunc/100)
            alpha4A.duration = ANIMATION_DURATION
            alpha4A.interpolator = LinearInterpolator()

            val anim4A = AnimatorSet()
            anim4A.play(rotatin4A).with(alpha4A)
            anim4A.start()

            val rotatin4B: ObjectAnimator =
                ObjectAnimator.ofFloat(binding.temperatureAlertCircleIv, View.ROTATION, 15f - actualAngle/4 - 8f, 15f - finishlAngleFunc/4 - 8f)
            rotatin4B.duration = ANIMATION_DURATION
            rotatin4B.interpolator = LinearInterpolator()

            val alpha4B: ObjectAnimator = ObjectAnimator.ofFloat(binding.temperatureAlertCircleIv, View.ALPHA,  actualPercentAlpha /100, finishlAlphaFunc/100)
            alpha4B.duration = ANIMATION_DURATION
            alpha4B.interpolator = LinearInterpolator()

            val anim4B = AnimatorSet()
            anim4B.play(rotatin4B).with(alpha4B)
            anim4B.start()



            timer = object : CountDownTimer(ANIMATION_DURATION, 1) {
                override fun onTick(millisUntilFinished: Long) {
//                    System.err.println("finishlAngleFunc = $finishlAngleFunc     finishAngle = $finishAngle    actualAngle = $actualAngle")
                    var delta = kotlin.math.abs(finishlAngleFunc - actualAngle)
                    if (actualAngle > finishlAngleFunc) { delta *= -1 }
                    val unitAngle = delta/ANIMATION_DURATION
                    trackingAngle = actualAngle + (ANIMATION_DURATION - millisUntilFinished)*unitAngle
//                    System.err.println("finishlAngleFunc > delta:$delta   unitAngle:$unitAngle    millisUntilFinished:$millisUntilFinished  trackingAngle:$trackingAngle")


                    if (trackingAngle < -112) {
                        binding.speedArrowLvlMinus1Iv.visibility = View.VISIBLE
                        binding.closingSpeedArrowLvlMinus1rIv.visibility = View.VISIBLE

                        binding.speedArrowIv.visibility = View.GONE
                    } else {
                        binding.speedArrowLvlMinus1Iv.visibility = View.GONE
                        binding.closingSpeedArrowLvlMinus1rIv.visibility = View.GONE

                        binding.speedArrowIv.visibility = View.VISIBLE
                    }
                }

                override fun onFinish() {
                    animationAllowed = true
                    actualAngle = finishAngle
                    actualPercentAlpha = finishPercentAlpha
                    if (finishAngle != finishlAngleFunc) {
//                        System.err.println("finishlAngleFunc = $finishlAngleFunc     finishAngle = $finishAngle    actualAngle = $actualAngle")
                        actualAngle = finishlAngleFunc
                        actualPercentAlpha = finishlAlphaFunc
                        rotateArrow(finishAngle, finishPercentAlpha)
                    }
                }
            }.start()
        }
    }
}