package ua.cn.stu.navigation

import android.os.Bundle
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.util.TypedValue
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
import ua.cn.stu.navigation.MainActivity.Companion.param2name
import ua.cn.stu.navigation.ui.theme.NavigationTheme


class ComposeActivity : ComponentActivity() {

    private val size_pixel = 3.09f
    private var scale = 0f
    private var dpi = 0
    private var timer: CountDownTimer? = null
    private var count: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scale = resources.displayMetrics.density
        dpi = resources.displayMetrics.densityDpi
        val coeff = scale*dpi
        val displaymetrics = DisplayMetrics()
        val dp =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, displaymetrics).toInt()
        println("scale = $scale  dpi = $dpi   coeff = $coeff  dp = $dp" )

        test2()
    }

    private fun test2() {
        timer = object : CountDownTimer(1000000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                drow(count)
                count += 1
                if (count == 132) { count = 0 }
                println("count = $count")
            }
            override fun onFinish() {}
        }.start()
    }

    private fun drow(value: Int) {
        setContent {
            NavigationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    run {
                        val vector = ImageVector.vectorResource(id = R.drawable.ic_drawing)//drawable vector
//                        val  vector = ImageBitmap.imageResource(id = R.drawable.ic_drawing)
                        val painter = rememberVectorPainter(image = vector)//convert to painter
//                        /////////////////////
//                        val image = ImageBitmap.imageResource(id = R.drawable.ic_drawing)//raster image
                        Canvas(
                            modifier = Modifier
                        ){
                            //draw the vector
                            for (i in 1 until value) {
                                for (j in 2 until value) {
                                    withTransform(
                                        {
                                            transform(
                                                Matrix().apply {
                                                    scale(1f, 1f)
                                                    translate(
                                                        i * size_pixel * scale,
                                                        j * size_pixel * scale
                                                    )
                                                }
                                            )
                                        }
                                    ) {
                                        if (i == 1 ) {
                                            println("paint param2name : ${param2name.value}")
                                            println("paint i = $i    j = $j")
                                            println("paint dot : " + (((i - 1) * 132) + j))
                                        }

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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {}