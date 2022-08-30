package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.MainActivity.Companion.connectedDevice
import ua.cn.stu.navigation.MainActivity.Companion.connectedDeviceAddress
import ua.cn.stu.navigation.MainActivity.Companion.lastConnectDeviceAddress
import ua.cn.stu.navigation.MainActivity.Companion.reconnectThreadFlag
import ua.cn.stu.navigation.MainActivity.Companion.scanList
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.connection.ScanItem
import ua.cn.stu.navigation.contract.*
import ua.cn.stu.navigation.databinding.FragmentScanningBinding
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys
import ua.cn.stu.navigation.recyclers_adapters.ScanningAdapter
import ua.cn.stu.navigation.rx.RxUpdateMainEvent

class ScanningFragment : Fragment(), HasCustomTitle, HasReturnAction {

    private lateinit var binding: FragmentScanningBinding
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: ScanningAdapter? = null
    private var scanListLocal: ArrayList<ScanItem>? = null


    @SuppressLint("CheckResult")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentScanningBinding.inflate(inflater, container, false)
        navigator().showBottomNavigationMenu(false)

        println("Scanning fragment started")

        RxUpdateMainEvent.getInstance().scanListObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { scanItem ->
                try {
                    println("validate lastConnectDeviceAddress: $lastConnectDeviceAddress  scanItem.getAddr(): ${scanItem.getAddr()}")
                    if (scanItem.getAddr() == lastConnectDeviceAddress) {
                        connectedDevice = scanItem.getTitle()
                        connectedDeviceAddress = lastConnectDeviceAddress
                        navigator().saveString(PreferenceKeys.CONNECTES_DEVICE, connectedDevice)
                        navigator().saveString(PreferenceKeys.CONNECTES_DEVICE_ADDRESS, connectedDeviceAddress)
                        navigator().scanLeDevice(false)
                        goToHome()
                        reconnectThreadFlag = true
                        navigator().reconnectThread()
                    }
                    addScanListItem()
                } catch (ignored: Exception) {
                    println("ОШИБКА СКАНИРОВАНИЯ!!!")
                }
            }

        initAdapter(binding.scanningRv)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addScanListItem() {
        activity?.runOnUiThread {
            adapter?.notifyItemChanged(adapter?.itemCount?.minus(1) ?: 0)
        }
    }


    private fun initAdapter(profile_rv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        profile_rv.layoutManager = linearLayoutManager
        adapter = ScanningAdapter(object : OnScanClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onClicked(name: String, address: String) {
                println("tup scan name = $name    address = $address")
                connectedDevice = name
                connectedDeviceAddress = address
                lastConnectDeviceAddress = address
                navigator().saveString(PreferenceKeys.CONNECTES_DEVICE, connectedDevice)
                navigator().saveString(PreferenceKeys.CONNECTES_DEVICE_ADDRESS, connectedDeviceAddress)
                navigator().saveString(PreferenceKeys.LAST_CONNECTES_DEVICE_ADDRESS, lastConnectDeviceAddress)
                navigator().scanLeDevice(false)
                goToHome()
                reconnectThreadFlag = true
                navigator().reconnectThread()
            }
        })
        profile_rv.adapter = adapter
    }


    override fun getTitleRes(): String = getString(R.string.scanning_ble_device)
    override fun getReturnAction(): ReturnAction {
        return ReturnAction(
            onReturnAction = {
                goToHome()
            }
        )
    }

    private fun goToHome() {
        navigator().firstOpenHome()
    }
}