package ua.cn.stu.navigation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ua.cn.stu.navigation.contract.navigator
import ua.cn.stu.navigation.databinding.FragmentBmsBinding

class BMSFragment : Fragment() {
    private lateinit var binding: FragmentBmsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentBmsBinding.inflate(inflater, container, false)
        navigator().showBottomNavigationMenu(true)
        println("BMS fragment started")


        return binding.root
    }
}