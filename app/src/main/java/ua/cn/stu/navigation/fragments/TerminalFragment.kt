package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.view.*
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_terminal.*
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.services.TerminalViewModel
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentTerminalBinding
import ua.cn.stu.navigation.persistence.TerminalConstants.TERMINAL_HIGHT
import ua.cn.stu.navigation.persistence.TerminalConstants.TERMINAL_WEIGHT
import ua.cn.stu.navigation.ui.theme.NavigationTheme
import java.util.*
import kotlin.random.Random.Default.nextBytes


class TerminalFragment : Fragment() {
    private var viewModel: TerminalViewModel = TerminalViewModel()

    private lateinit var binding: FragmentTerminalBinding
    private var size_pixel = 3.09f
    private val targetDisplayScale = 2.625f
    private var scaleCoefficient = Vector<Float>()
    private var scale = 0f
    private var dpi = 0

    private var bytes = ByteArray(1056)
    private var myInflater: LayoutInflater? = null
    private var myContainer: ViewGroup? = null
    private var pixelsPool = 0
    private var buttonScale = 70.dp


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTerminalBinding.inflate(inflater, container, false)
        navigator().showBottomNavigationMenu(true)
        System.err.println("TERMINAL fragment started")

        myInflater = inflater
        myContainer = container
        if (MainActivity.bytesArrayFrame.value != null) {
            bytes = MainActivity.bytesArrayFrame.value!!
        } else {
            nextBytes(bytes)
        }

        scale = resources.displayMetrics.density
        setScaleCoefficients()
        setButtonsScale()
        startTimer()

        System.err.println("metrics 1 height = ${getScreenHight()}  width = ${getScreenWeight()}  dpi = $dpi" )

        return drow(inflater, container)
    }


    private fun startTimer() {
        object : CountDownTimer(100000000, 30) {
            override fun onTick(millisUntilFinished: Long) {
                viewModel.addNumber(millisUntilFinished.toInt())
            }

            override fun onFinish() {}
        }.start()
    }



    @Suppress("UNUSED_EXPRESSION")
    @SuppressLint("UnrememberedMutableState")
    private fun drow(inflater: LayoutInflater, container: ViewGroup?): View {
        val myCompouseView = inflater.inflate(R.layout.fragment_terminal , container, false).apply {
            findViewById<ComposeView>(R.id.terminal_composable)?.setContent {
                NavigationTheme {
                    val count by viewModel.number.observeAsState(0)
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
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            if ((calculateXPixel((it.x).toInt()) in 1..TERMINAL_WEIGHT) && (calculateYPixel(
                                                    (it.y).toInt()
                                                ) in 1..TERMINAL_HIGHT
                                                        )
                                            ) {
                                                System.err.println(
                                                    "motionEvent onTap X: ${it.x}  pix:${
                                                        calculateXPixel(
                                                            (it.x).toInt()
                                                        )
                                                    }"
                                                )
                                                System.err.println(
                                                    "motionEvent onTap Y: ${it.y}  pix:${
                                                        calculateYPixel(
                                                            (it.y).toInt()
                                                        )
                                                    }"
                                                )
                                            }
                                        }
                                    )
                                }
                            ) {
                                for (i in 1 until TERMINAL_WEIGHT) {
                                    for (j in 1 until TERMINAL_HIGHT) {
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

                    Column( horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom))
                    {
                        Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ButtonDemo(R.drawable.arrow_cancel, "cancel")
                            ButtonDemo(R.drawable.arrow_up, "up")
                            Box(modifier = Modifier
                                .background(Color.Transparent)
                                .height(buttonScale)
                                .width(buttonScale))
                        }
                        Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ButtonDemo(R.drawable.arrow_left, "left")
                            ButtonDemo(R.drawable.center, "center")
                            ButtonDemo(R.drawable.arrow_right, "right")
                        }
                        Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier
                                .background(Color.Transparent)
                                .height(buttonScale)
                                .width(buttonScale))
                            ButtonDemo(R.drawable.arrow_down, "down")
                            Box(modifier = Modifier
                                .background(Color.Transparent)
                                .height(buttonScale)
                                .width(buttonScale))
                        }
                    }

                }
            }
        }

        return myCompouseView
    }

    @Composable
    fun ButtonDemo (@DrawableRes id: Int, idClick: String)  {
        val context = LocalContext.current
        Button(onClick = {
            when(idClick) {
                "cancel" -> { Toast.makeText(context, "Clicked on cancel", Toast.LENGTH_SHORT).show() }
                "up" -> { Toast.makeText(context, "Clicked on up", Toast.LENGTH_SHORT).show() }
                "down" -> { Toast.makeText(context, "Clicked on down", Toast.LENGTH_SHORT).show() }
                "left" -> { Toast.makeText(context, "Clicked on left", Toast.LENGTH_SHORT).show() }
                "right" -> { Toast.makeText(context, "Clicked on right", Toast.LENGTH_SHORT).show() }
                "center" -> { Toast.makeText(context, "Clicked on center", Toast.LENGTH_SHORT).show() }
            }},
            modifier = Modifier
                .height(buttonScale)
                .width(buttonScale),
            shape = RoundedCornerShape(20),
            border = BorderStroke(2.dp, Color.White),
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = Color.DarkGray,
                contentColor = Color.White
            )
        )
        {
            Image(
                painter = painterResource(id = id),
                contentDescription = "Image",
                modifier = Modifier
                    .height(buttonScale - 20.dp)
                    .width(buttonScale - 20.dp)

            )
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
                    in 1281..1440 -> { scaleCoefficient.add(1.9f/scale) }
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
    private fun setButtonsScale() {
        when (dpi) {
            320 -> {
                when(getScreenHight()) {
                    in 0..1280 -> { buttonScale = 58.dp }
                    in 1281..1440 -> { buttonScale = 70.dp }
                }
            }
            400 -> { buttonScale = 85.dp }
            420 -> {
                when(getScreenHight()) {
                    in 0..1920 -> { buttonScale = 70.dp }
                    in 1921..2428 -> { buttonScale = 95.dp }
                    in 2429..2480 -> { buttonScale = 97.dp }
                }
            }
            440 -> { buttonScale = 85.dp }
            480 -> {
                when(getScreenHight()) {
                    in 0..1920 -> { buttonScale = 57.dp }
                    in 1921..2400 -> {
//                        System.err.println("metrics getScreenHight 1921..2400")
                        buttonScale = 80.dp
                    }
                    in 2401..2636 -> { buttonScale = 90.dp }
                }
            }
            560 -> { buttonScale = 70.dp }
        }
    }
    private fun calculateXPixel(x: Int): Int {
        val minX = intArrayOf(46, 17, 14, 9, 11, 6, 13, 5, 7, 10)
        val maxX = intArrayOf(1044, 2182, 1075, 1070, 1070, 1074, 1069, 711, 1070, 1425)
        var konfig = 0
        when (dpi) {
            320 -> {
                when(getScreenHight()) {
                    in 0..1280 -> { konfig = 7 }
                    in 1281..1440 -> { scaleCoefficient.add(1.9f/scale) }
                }
            }
            400 -> {
                when(getScreenHight()) {
                    in 0..2160 -> { konfig = 8 }
                }
            }
            420 -> {
                when(getScreenHight()) {
                    in 0..1920 -> { konfig = 2 }
                    in 1921..2428 -> { konfig = 5 }
                    in 2429..2480 -> { konfig = 1 }
                }
            }
            440 -> {
                when(getScreenHight()) {
                    in 0..2340 -> { konfig = 3 }
                }
            }
            480 -> {
                when(getScreenHight()) {
                    in 0..1920 -> { konfig = 6 }
                    in 1921..2400 -> { konfig = 0 }
                    in 2401..2636 -> { konfig = 4 }
                }
            }
            560 -> {
                when(getScreenHight()) {
                    in 0..2560 -> { konfig = 9 }
                }
            }
        }
        return ((x - minX[konfig]) / ((maxX[konfig] - minX[konfig]).toFloat() / (TERMINAL_WEIGHT))).toInt() + 1
    }
    private fun calculateYPixel(y: Int): Int {
        val minY = intArrayOf(9, 17, 13, 19, 30, 28, 23, 14, 20, 20)
        val maxY = intArrayOf(810, 899, 654, 822, 920, 859, 623, 420, 757, 866)
        var konfig = 0
        when (dpi) {
            320 -> {
                when(getScreenHight()) {
                    in 0..1280 -> { konfig = 7 }
                    in 1281..1440 -> { scaleCoefficient.add(1.9f/scale) }
                }
            }
            400 -> {
                when(getScreenHight()) {
                    in 0..2160 -> { konfig = 8 }
                }
            }
            420 -> {
                when(getScreenHight()) {
                    in 0..1920 -> { konfig = 2 }
                    in 1921..2428 -> { konfig = 5 }
                    in 2429..2480 -> { konfig = 1 }
                }
            }
            440 -> {
                when(getScreenHight()) {
                    in 0..2340 -> { konfig = 3 }
                }
            }
            480 -> {
                when(getScreenHight()) {
                    in 0..1920 -> { konfig = 6 }
                    in 1921..2400 -> { konfig = 0 }
                    in 2401..2636 -> { konfig = 4 }
                }
            }
            560 -> {
                when(getScreenHight()) {
                    in 0..2560 -> { konfig = 9 }
                }
            }
        }
        return ((y - minY[konfig]) / ( (maxY[konfig] - minY[konfig]).toFloat() / (TERMINAL_HIGHT - 1) )).toInt() + 1
    }
}
