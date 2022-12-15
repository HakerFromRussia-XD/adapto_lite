package ua.cn.stu.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import ua.cn.stu.navigation.persistence.TerminalConstants.TERMINAL_HIGHT
import ua.cn.stu.navigation.persistence.TerminalConstants.TERMINAL_WEIGHT
import ua.cn.stu.navigation.ui.theme.NavigationTheme


class ComposeActivity : ComponentActivity() {

    private var size_pixel = 3.09f
    private val targetDisplayScale = 2.625f
//    private val targetDisplayDPI = 420
    private var scale = 0f
    private var dpi = 0
//    private var timer: CountDownTimer? = null
//    private var count: Int = 1
    private val bytearray = byteArrayOfInts(0x55, 0xA5)
    private val bitsets = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scale = resources.displayMetrics.density
        dpi = resources.displayMetrics.densityDpi
        size_pixel = size_pixel / scale * targetDisplayScale
        println("scale = $scale  dpi = $dpi" )


        for (i in bytearray) {
            bitsets.add(i.toString(2))
            println("bitsets ${i.toString(2)}")
            val intValue = i.toInt()

            for (j in 0 until 8) {
                if (intValue shr j and 0b00000001 == 1) { println("bitsets $i  $j-1") } else { println("bitsets $i  $j-0") }
            }

        }

//        test2()
        drow()
    }



    private fun drow() {
        setContent {
            NavigationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    run {
                        val vector = ImageVector.vectorResource(id = R.drawable.ic_drawing)//drawable vector
                        val painter = rememberVectorPainter(image = vector)//convert to painter
//                        /////////////////////
//                        val image = ImageBitmap.imageResource(id = R.drawable.ic_drawing)//raster image
                        Canvas(
                            modifier = Modifier
                        ){
                            for (i in 1 until TERMINAL_WEIGHT) {
                                for (j in 2 until TERMINAL_HIGHT) {
                                    withTransform(
                                        {
                                            transform(
                                                Matrix().apply {
                                                    scale(1f, 1f)
                                                    translate(
                                                        i * size_pixel * scale,// * getDPICoefficient(),
                                                        j * size_pixel * scale// * getDPICoefficient()
                                                    )
                                                }
                                            )
                                        }
                                    ) {
//                                        if (i == 1 ) {
//                                            println("paint param2name : ${param2name.value}")
//                                            println("paint i = $i    j = $j")
//                                            println("paint dot : " + (((i - 1) * 132) + j))
//                                        }

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


    private fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

//    private fun test2() {
//        timer = object : CountDownTimer(1000000, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                drow()
//                count += 1
//                if (count == 132) { count = 0 }
//            }
//            override fun onFinish() {}
//        }.start()
//    }

//    private fun getDPICoefficient() :Float {
//        println("DPICoefficient : ${(dpi/(targetDisplayDPI / 100f))/100}")
//        return (dpi/(targetDisplayDPI / 100f))/100
//    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {}