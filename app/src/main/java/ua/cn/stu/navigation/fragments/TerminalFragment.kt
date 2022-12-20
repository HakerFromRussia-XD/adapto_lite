package ua.cn.stu.navigation.fragments

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentTerminalBinding
import ua.cn.stu.navigation.persistence.TerminalConstants
import ua.cn.stu.navigation.ui.theme.NavigationTheme
import java.sql.DriverManager.println
import java.util.*
import kotlin.random.Random
import kotlin.random.Random.Default.nextBytes


class TerminalFragment : Fragment() {

    private lateinit var binding: FragmentTerminalBinding
    private var size_pixel = 3.09f
    private val targetDisplayScale = 2.625f
    private var scaleCoefficient = Vector<Float>()
    private var scale = 0f
    private var dpi = 0
    private val bytes = ByteArray(1056)


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTerminalBinding.inflate(inflater, container, false)
        navigator().showBottomNavigationMenu(true)
        System.err.println("TERMINAL fragment started")


        size_pixel = size_pixel / scale * targetDisplayScale
        setScaleCoefficient()

        System.err.println("metrics 1 height = ${getScreenHight()}  width = ${getScreenWeight()}  dpi = $dpi" )

//        while (true) {
//            for (i in bytes) {
//                val bitsets = ArrayList<String>()
//                bitsets.add(i.toString(2))
//                System.err.println("BYTES bitsets ${i.toString(2)}")
//                val intValue = i.toInt()
//
//                for (j in 0 until 8) {
//                    if (intValue shr j and 0b00000001 == 1) {
//                                                            System.err.println("BYTES bitsets $i  $j-1")
//                    } else {
//                                                            System.err.println("BYTES bitsets $i  $j-0")
//                    }
//                }
//            }
//            System.err.println("BYTES calculated frame")
//        }

        return drow(inflater, container)
    }


    private fun setScaleCoefficient() {
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


    private fun drow(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.fragment_terminal , container, false).apply {
            findViewById<ComposeView>(R.id.terminal_composable)?.setContent {
                NavigationTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize().background(Color.Transparent),
                        color = Color.Yellow.copy(alpha = 0f)
                    ) {
                        run {
                            val vector =
                                ImageVector.vectorResource(id = R.drawable.ic_drawing)//drawable vector
                            val painter = rememberVectorPainter(image = vector)//convert to painter
                            Canvas(
                                modifier = Modifier
                            ) {
                                for (i in 1 until TerminalConstants.TERMINAL_WEIGHT) {
                                    for (j in 1 until TerminalConstants.TERMINAL_HIGHT) {
                                        withTransform(
                                            {
                                                transform(
                                                    Matrix().apply {
                                                        scale(scaleCoefficient[0], scaleCoefficient[1])
                                                        translate(
                                                            i * size_pixel * scale * 1f,
                                                            j * size_pixel * scale * 1f
                                                        )
                                                    }
                                                )
                                            }
                                        ) {
//                                            while (true) {
//                                                for (i in bytes) {
//                                                    bitsets.add(i.toString(2))
////                                                    System.err.println("BYTES bitsets ${i.toString(2)}")
//                                                    val intValue = i.toInt()
//
//                                                    for (j in 0 until 8) {
//                                                        if (intValue shr j and 0b00000001 == 1) {
////                                                            System.err.println("BYTES bitsets $i  $j-1")
//                                                        } else {
////                                                            System.err.println("BYTES bitsets $i  $j-0")
//                                                        }
//                                                    }
//                                                }
//                                                System.err.println("BYTES calculated frame")
//                                            }
                                            with(painter) {
                                                draw(
                                                    painter.intrinsicSize
                                                )
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
}