package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.TerminalViewModel
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentTerminalBinding
import ua.cn.stu.navigation.persistence.TerminalConstants
import ua.cn.stu.navigation.ui.theme.NavigationTheme
import java.util.*
import kotlin.random.Random.Default.nextBytes


@OptIn(DelicateCoroutinesApi::class)
class TerminalFragment : Fragment() {
    private var viewModel: TerminalViewModel = TerminalViewModel()

    private lateinit var binding: FragmentTerminalBinding
    private var size_pixel = 3.09f
    private val targetDisplayScale = 2.625f
    private var scaleCoefficient = Vector<Float>()
    private var scale = 0f
    private var dpi = 0

    private val bytes = ByteArray(1056)
    private var myInflater: LayoutInflater? = null
    private var myContainer: ViewGroup? = null
    private var pixelsPool = 0


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTerminalBinding.inflate(inflater, container, false)
        navigator().showBottomNavigationMenu(true)
        System.err.println("TERMINAL fragment started")


        myInflater = inflater
        myContainer = container

        //create random bytes array
        scale = resources.displayMetrics.density
        setScaleCoefficients()
        startTimer()

        System.err.println("metrics 1 height = ${getScreenHight()}  width = ${getScreenWeight()}  dpi = $dpi" )

        return drow(inflater, container) //binding.root
    }

    private fun startTimer() {
        object : CountDownTimer(100000000, 30) {
            override fun onTick(millisUntilFinished: Long) {
                viewModel.addNumber(millisUntilFinished.toInt())
                System.err.println("TerminalViewModel plan")
            }

            override fun onFinish() {}
        }.start()
    }


    @SuppressLint("UnrememberedMutableState")
    private fun drow(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.fragment_terminal , container, false).apply {
            findViewById<ComposeView>(R.id.terminal_composable)?.setContent {
                NavigationTheme {
                    val count by viewModel.number.observeAsState(0)
//                    var favourites: MutableList<String> by mutableStateOf(mutableListOf())
//                    var count by remember { mutableStateOf(0) }
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Yellow.copy(alpha = 0f)
                    ) {
                        run {
                            val vector =
                                ImageVector.vectorResource(id = R.drawable.ic_drawing)//drawable vector
                            val painter = rememberVectorPainter(image = vector)//convert to painter
                            nextBytes(bytes)
                            val translateCoeff = size_pixel * scale
                            val scaleXCoeff = scaleCoefficient[0]
                            val scaleYCoeff = scaleCoefficient[1]
                            count
//                            System.err.println("TerminalViewModel fakt count $count")
                            Canvas(
                                modifier = Modifier
                            ) {
                                for (i in 1 until TerminalConstants.TERMINAL_WEIGHT) {
                                    for (j in 1 until TerminalConstants.TERMINAL_HIGHT) {
//                                        composableScope.launch {
                                            pixelsPool = bytes[((i - 1) * 8) + (j - 1).div(8)].toInt()
//                                        }

                                        if (pixelsPool shr (j % 8) and 0b00000001 == 1) {
//                                            System.err.println("BYTES bitsets $i  $j-1")
                                                withTransform(
                                                    {
                                                        transform(
                                                            Matrix().apply {
                                                                scale(scaleXCoeff, scaleYCoeff)
                                                                translate(
                                                                    i * translateCoeff,
                                                                    j * translateCoeff
                                                                )
                                                            }
                                                        )
                                                    }
                                                ) {
                                                    with(painter) {
                                                        draw(
                                                            painter.intrinsicSize
                                                        )
                                                    }
                                                }
                                        } else {
//                                            System.err.println("BYTES bitsets $i  $j-0")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getScreenWeight(): Int {
        val metrics = DisplayMetrics()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            activity?.display?.getRealMetrics(metrics)
        } else {
            @Suppress("DEPRECATION")
            activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        }
        return metrics.widthPixels
    }
    private fun getScreenHight(): Int {
        val metrics = DisplayMetrics()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            activity?.display?.getRealMetrics(metrics)
        } else {
            @Suppress("DEPRECATION")
            activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        }
        return metrics.heightPixels
    }
    private fun setScaleCoefficients() {
        size_pixel = size_pixel / scale * targetDisplayScale
        dpi = resources.displayMetrics.densityDpi
        scale = resources.displayMetrics.density
        when (dpi) {
            320 -> {
                when(getScreenWeight()) {
                    in 0..720 -> { scaleCoefficient.add(1.33f/scale) }
                }
                when(getScreenHight()) {
                    in 0..1280 -> { scaleCoefficient.add(1.63f/scale) }
                    in 1281..1440 -> { scaleCoefficient.add(2.4f/scale) }
                }
            }
            400 -> {
                when(getScreenWeight()) {
                    in 0..1080 -> { scaleCoefficient.add(2.5f/scale) }
                }
                when(getScreenHight()) {
                    in 0..2160 -> { scaleCoefficient.add(3.7f/scale) }
                }
            }
            420 -> {
                when(getScreenWeight()) {
                    in 0..1080 -> { scaleCoefficient.add(2.63f/scale) }
                    in 1081..2200 -> { scaleCoefficient.add(5.35f/scale) }
                }
                when(getScreenHight()) {
                    in 0..1920 -> { scaleCoefficient.add(3.31f/scale) }
                    in 1921..2428 -> { scaleCoefficient.add(4.4f/scale) }
                    in 2429..2480 -> { scaleCoefficient.add(4.53f/scale) }
                }
            }
            440 -> {
                when(getScreenWeight()) {
                    in 0..1080 -> { scaleCoefficient.add(2.74f/scale) }
                }
                when(getScreenHight()) {
                    in 0..2340 -> { scaleCoefficient.add(4.4f/scale) }
                }
            }
            480 -> {
                when(getScreenWeight()) {
                    in 0..1080 -> { scaleCoefficient.add(3f/scale) }
                }
                when(getScreenHight()) {
                    in 0..1920 -> { scaleCoefficient.add(3.65f/scale) }
                    in 1921..2400 -> { scaleCoefficient.add(4.8f/scale) }
                    in 2401..2636 -> { scaleCoefficient.add(5.4f/scale) }
                }
            }
            560 -> {
                when(getScreenWeight()) {
                    in 0..1440 -> { scaleCoefficient.add(4.65f/scale) }
                }
                when(getScreenHight()) {
                    in 0..2560 -> { scaleCoefficient.add(5.9f/scale) }
                }
            }
        }
    }
}