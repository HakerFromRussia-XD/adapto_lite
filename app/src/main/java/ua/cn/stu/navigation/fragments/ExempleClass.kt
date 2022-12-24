package ua.cn.stu.navigation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.ui.theme.NavigationTheme
import kotlin.random.Random

class ExempleClass: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return drow(inflater, container)
    }

    private fun drow(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.fragment_terminal , container, false).apply {
            findViewById<ComposeView>(R.id.terminal_composable)?.setContent {
                var count by remember { mutableStateOf(0) }

                NavigationTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        run {
                            Canvas(
                                modifier = Modifier
                                .clickable { count ++ }
                            ) {
                                //некая функция отрисовки на холсте
                            }
                        }
                    }
                }
            }
        }
    }
}