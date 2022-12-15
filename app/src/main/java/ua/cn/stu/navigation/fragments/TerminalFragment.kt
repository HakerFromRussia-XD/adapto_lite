package ua.cn.stu.navigation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentTerminalBinding

class TerminalFragment : Fragment() {

    private lateinit var binding: FragmentTerminalBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTerminalBinding.inflate(inflater, container, false)
        navigator().showBottomNavigationMenu(true)
        println("TERMINAL fragment started")

        return inflater.inflate(R.layout.fragment_terminal , container, false).apply {
            findViewById<ComposeView>(R.id.terminal_composable).setContent {
                Text(text = "Hello world.",
                     color = Color.White)
            }
        }
//        return binding.root
    }


}