package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.contract.*
import ua.cn.stu.navigation.databinding.FragmentStatisticBinding
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys
import ua.cn.stu.navigation.recyclers_adapters.ScanningAdapter
import ua.cn.stu.navigation.recyclers_adapters.StatisticAdapter

class StatisticFragment: Fragment(), HasDisconnectionAction {
    private lateinit var binding: FragmentStatisticBinding
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: StatisticAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStatisticBinding.inflate(inflater, container, false)
        navigator().showBottomNavigationMenu(true)
        println("BMS fragment started")

        initAdapter(binding.staticRv)
        return binding.root
    }

    private fun initAdapter(profile_rv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        profile_rv.layoutManager = linearLayoutManager
        adapter = StatisticAdapter(object : OnStatClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onClicked(name: String) {
                println("tup stat name = $name ")
            }
        })
        profile_rv.adapter = adapter
    }

    override fun getDisconnectionAction(): DisconnectionAction {
        return DisconnectionAction(
            onDisconnectionAction = {
                showDisconnectionDialog()
            }
        )
    }

    private fun showDisconnectionDialog() {
        navigator().showDisconnectDialog()
    }
}