package ua.cn.stu.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import ua.cn.stu.navigation.ble.BluetoothLeService
import ua.cn.stu.navigation.ble.SampleGattAttributes.*
import ua.cn.stu.navigation.connection.ScanItem
import ua.cn.stu.navigation.contract.*
import ua.cn.stu.navigation.contract.ConstantManager.Companion.MY_PERMISSIONS_REQUEST_LOCATION
import ua.cn.stu.navigation.contract.ConstantManager.Companion.RECONNECT_BLE_PERIOD
import ua.cn.stu.navigation.contract.ConstantManager.Companion.REQUEST_ENABLE_BT
import ua.cn.stu.navigation.databinding.ActivityMainBinding
import ua.cn.stu.navigation.fragments.*
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.CONNECTED_DEVICE
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.CONNECTED_DEVICE_ADDRESS
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.LAST_CONNECTED_DEVICE_ADDRESS
import ua.cn.stu.navigation.rx.RxUpdateMainEvent
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), Navigator {

    private lateinit var binding: ActivityMainBinding
    private var mSettings: SharedPreferences? = null
    private var countActivatedTitleFragment: Int = 0

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
    private var mGattServicesList: ExpandableListView? = null
//    private var mCharacteristic: BluetoothGattCharacteristic? = null
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var globalSemaphore = true // флаг, который преостанавливает отправку новой
    private val queue = ua.cn.stu.navigation.services.BlockingQueue()
    private var dataSortSemaphore = "" // строчка, показывающая с каким регистром мы сейчас работаем, чтобы однозначно понять кому пердназначаются принятые данные

    private var readRegisterPointer: ByteArray? = null
    private var mConnected = false
    private var endFlag = false
    var percentSynchronize = 0
    private var mScanning = false
    private val listName = "NAME"
    private val listUUID = "UUID"
    private var actionState = WRITE


    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            gattBle = mBluetoothLeService?.myBluetoothGatt
            if (!mBluetoothLeService?.initialize()!!) {
                finish()
            }
            if (!flagScanWithoutConnect) {
                mBluetoothLeService?.connect(connectedDeviceAddress)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    private val currentFragment: Fragment
        get() = supportFragmentManager.findFragmentById(R.id.fragmentContainer)!!

    private val fragmentListener = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            super.onFragmentViewCreated(fm, f, v, savedInstanceState)
            updateUi()
        }
    }

    private external fun new_dg_from_bt(dgram: ByteArray)// отправить пакет gatt в обработку
    external fun eth_ble_stack_control(status: Int)// оповестить обработчик пакетов что статус подключения поменялся
    external fun change_dbg_scr(scr: Int)// оповещаем какой дебаг экран показывать
    external fun tap_detected(area: Int)// оповещаем какая область экрана была нажата
    private external fun coord_from_terminal(xPix: Int, yPix: Int)// передаём координаты места касания вью терминала
    private external fun button_from_terminal(buttonId: Int)// передаём координаты места касания вью терминала


    @SuppressLint("SetTextI18n", "CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        mSettings = this.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        mGattServicesList = findViewById(R.id.gatt_services_list)
        setSupportActionBar(binding.toolbar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.menu_bottom_layout_bg)
        eth_ble_stack_control(0)
        initAllVariables()


        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, ScanningFragment())
                .commit()
//            supportFragmentManager
//                .beginTransaction()
//                .add(R.id.fragmentContainer, TerminalFragment())
//                .commit()
        }


        createStatList()
        scanList = reinitScanList()
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentListener, false)

        binding.leftFragmentBtn.setOnClickListener { showStatisticScreen() }
        binding.centerFragmentBtn.setOnClickListener { showHomeScreen() }
        binding.rightFragmentBtn.setOnClickListener { showBMSScreen() }
        binding.rightTestFragmentBtn.setOnClickListener { showDebugScreen() }


        // инициализация блютуз
        checkLocationPermission()
        askPermissions()
        initBLEStructure()
        //TODO включить сканирование в продакшне
        scanLeDevice(true)


        //запуск очереди блютуз команд
        val worker = Thread {
            while (true) {
                val task: Runnable = queue.get()
                task.run()
            }
        }
        worker.start()
    }

    override fun onResume() {
        super.onResume()
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                System.err.println("BLUETOOTH_CONNECT PERMISSION NOT GRANTED")
                return
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (mBluetoothLeService != null) {
            connectedDevice =  getString(CONNECTED_DEVICE)
            connectedDeviceAddress =  getString(CONNECTED_DEVICE_ADDRESS)
        }
        if (!mConnected) {
            reconnectThreadFlag = true
            reconnectThread()
        }
    }
    override fun onPause() {
        super.onPause()
        endFlag = true
    }
    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentListener)
        if (mBluetoothLeService != null) {
            unbindService(mServiceConnection)
            mBluetoothLeService = null
        }
        endFlag = true
        if (mScanning) { mBluetoothAdapter!!.stopLeScan(mLeScanCallback) }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        updateUi()
        return true
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    override fun onBackPressed() {
        super.onBackPressed()
        val fragment = currentFragment
        if (fragment is HasCustomTitle) {
            if ( fragment.getTitleRes() == getString(R.string.settings)) {
                scanLeDevice(false)
                flagScanWithoutConnect = false
            }
        }
        RxUpdateMainEvent.getInstance().updateBackPresedIvent()
    }

    override fun showScanScreen() { launchFragmentWihtoutStack(ScanningFragment()) }
    override fun showStatisticScreen() { launchFragmentWihtoutStack(StatisticFragment()) }
    override fun showHomeScreen() { launchFragmentWihtoutStack(HomeFragment()) }
    override fun showBMSScreen() { launchFragmentWihtoutStack(BMSFragment()) }
    //TODO исправить терминал на дебаг
    override fun showDebugScreen() {
        launchFragmentWihtoutStack(TerminalFragment())
//        launchFragmentWihtoutStack(DebugFragment())
    }
    override fun showBottomNavigationMenu (show: Boolean) {
        if (show) bottom_menu_cl.visibility = View.VISIBLE
        else bottom_menu_cl.visibility = View.GONE
    }

    override fun sendCoordFromTerminal(xPix: Int, yPix: Int) {
        coord_from_terminal(xPix, yPix)
        System.err.println("motionEvent X pix:${xPix}")
        System.err.println("motionEvent Y pix:${yPix}")
    }
    override fun sendButtonFromTerminal(buttonId: Int) {
        button_from_terminal(buttonId)
    }
    override fun firstOpenHome() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())//HomeFragment
            .commit()
    }
    override fun goBack() {
        println("goBack")
        flagScanWithoutConnect = false
        onBackPressed()
    }
    override fun goToMenu() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    override fun <T : Parcelable> publishResult(result: T) {
        supportFragmentManager.setFragmentResult(result.javaClass.name, bundleOf(KEY_RESULT to result))
    }
    override fun <T : Parcelable> listenResult(clazz: Class<T>, owner: LifecycleOwner, listener: ResultListener<T>) {
        supportFragmentManager.setFragmentResultListener(clazz.name, owner) { _, bundle ->
            listener.invoke(bundle.getParcelable(KEY_RESULT)!!)
        }
    }

    private fun launchFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out
            )
            .addToBackStack(null)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    private fun launchFragmentWihtoutStack(fragment: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
//        transaction.setCustomAnimations(
//            R.anim.slide_in,
//            R.anim.fade_out,
//            R.anim.fade_in,
//            R.anim.slide_out
//        )
        transaction.replace(R.id.fragmentContainer, fragment)
        if (!supportFragmentManager.isDestroyed) transaction.commit()
    }

    private fun updateUi() {
        val fragment = currentFragment

        if (fragment is HasCustomTitle) {
            countActivatedTitleFragment += 1
        } else {
            if (countActivatedTitleFragment != 0) {
            } else {
                binding.toolbar.title = ""
                binding.titleToolbatTv.text = getString(R.string.app_name)
            }
            countActivatedTitleFragment = 0
        }

        if (fragment is HasDisconnectionAction) { connectionClicked(fragment.getDisconnectionAction()) }
        if (fragment is HasCustomAction) { createCustomToolbarAction(fragment.getCustomAction())
        } else { binding.toolbar.menu.clear() }
    }

    private fun createCustomToolbarAction(action: CustomAction) {
        binding.toolbar.menu.clear() // clearing old action if it exists before assigning a new one

        val iconDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(this, action.iconRes)!!)
        iconDrawable.setTint(Color.WHITE)

        val menuItem = binding.toolbar.menu.add(action.textRes)
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menuItem.icon = iconDrawable
        menuItem.setOnMenuItemClickListener {
            action.onCustomAction.run()
            return@setOnMenuItemClickListener true
        }
    }

    private fun connectionClicked(action: DisconnectionAction) {
        connection_btn.setOnClickListener {
            action.onDisconnectionAction.run()
            return@setOnClickListener
        }
    }

    private fun initAllVariables() {
        //init
        lastConnectDeviceAddress = ""
        reconnectThreadFlag = false
        flagScanWithoutConnect = true


        //settings
        showInfoDialogsFlag = false
        inScanFragmentFlag = false
//
        if (getString(LAST_CONNECTED_DEVICE_ADDRESS) == "NOT SET!") {
            lastConnectDeviceAddress = ""
            saveString(LAST_CONNECTED_DEVICE_ADDRESS, lastConnectDeviceAddress)
        } else { lastConnectDeviceAddress =  getString(LAST_CONNECTED_DEVICE_ADDRESS)}

        if (getString(CONNECTED_DEVICE) == "NOT SET!") {
            connectedDevice = "BT-Pump 12"
            saveString(CONNECTED_DEVICE, connectedDevice)
        } else { connectedDevice =  getString(CONNECTED_DEVICE)}
        if (getString(CONNECTED_DEVICE_ADDRESS) == "NOT SET!") {
            connectedDeviceAddress = "D7:77:A9:47:F9:EC"//"12:34:56:78:90:12"
            saveString(CONNECTED_DEVICE_ADDRESS, connectedDeviceAddress)
        } else { connectedDeviceAddress =  getString(CONNECTED_DEVICE_ADDRESS)}
    }
    private fun createStatList(){
        val listA = ArrayList<String>()
        listA.add("statistic 1 cell")
        listA.add("statistic 2 cell")
        listA.add("statistic 3 cell")
        listA.add("statistic 4 cell")
        listA.add("statistic 5 cell")
        listA.add("statistic 6 cell")
        listA.add("statistic 7 cell")
        listA.add("statistic 8 cell")
        listA.add("statistic 9 cell")
        listA.add("statistic 10 cell")
        listA.add("statistic 11 cell")
        listA.add("statistic 12 cell")
        listA.add("statistic 13 cell")
        listA.add("statistic 14 cell")
        statList = listA
    }
    private fun reinitScanList():ArrayList<ScanItem> {
        val result = ArrayList<ScanItem>()
        result.add(ScanItem("NOT SET!", "null"))
        return result
    }


    @SuppressLint("InflateParams")
    override fun showDisconnectDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_disconnection, null)
        val myDialog = Dialog(this)
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_disconnection_confirm)
        yesBtn.setOnClickListener {
            disconnect()
            initBLEStructure()
            showScanScreen()
            inScanFragmentFlag = true
            showInfoDialogsFlag = true
            scanLeDevice(true)
            lastConnectDeviceAddress = ""
            saveString(LAST_CONNECTED_DEVICE_ADDRESS, lastConnectDeviceAddress)
            //TODO сделать сброс мак-адреса последнего подключённого устройства к дефолтному

            myDialog.dismiss()
        }
        val noBtn = dialogBinding.findViewById<View>(R.id.dialog_disconnection_cancel)
        noBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }


    // сохранение и загрузка данных
    override fun saveIntArrayList(key: String, list: ArrayList<IntArray>) {
        saveInt(key+"_list_size", list.size)
        for (item in 0 until list.size) { saveIntArray(key+item, list[item]) }
    }
    private fun saveIntArray(key: String, array: IntArray) {
        saveInt(key + "_size", array.size)
        for (i in array.indices) saveInt(key + "_" + i, array[i])
    }
    private fun loadIntArray(key: String): IntArray {
        val size = getInt(key + "_size")
        val array = IntArray(size)//Array(size) { "" }
        for (i in 0 until size) array[i] = getInt(key + "_" + i)
        return array
    }
    private fun loadIntArrayList(key: String) :ArrayList<IntArray>{
        val size = getInt(key + "_list_size")
        val result = ArrayList<IntArray>()
        for (i in 0 until size) result.add(loadIntArray(key+i))
        return result
    }
    override fun saveArrayStringList(key: String, list: ArrayList<Array<String>>) {
        saveInt(key+"_list_size", list.size)
        for (item in 0 until list.size) { saveStringArray(key+item, list[item]) }
    }
    private fun loadArrayStringList(key: String) :ArrayList<Array<String>>{
        val size = getInt(key + "_list_size")
        val result = ArrayList<Array<String>>()
        for (i in 0 until size) result.add(loadStringArray(key+i))
        return result
    }
    private fun saveStringArray(key: String, array: Array<String>) {
        saveInt(key + "_size", array.size)
        for (i in array.indices) saveString(key + "_" + i, array[i])
    }
    private fun loadStringArray(key: String): Array<String> {
        val size = getInt(key + "_size")
        val array = Array(size) { "" }
        for (i in 0 until size) array[i] = getString(key + "_" + i)
        return array
    }
    override fun <T> saveArrayList(key: String, list: ArrayList<T>){
        val arrayString = Gson().toJson(list)
        saveString(key, arrayString)
    }
    private inline fun <reified T> loadArrayList(key: String) :ArrayList<T>{
        val listJson = getString(key)
        val result = ArrayList<T>()
        if (listJson != "NOT SET!") {
            val type = object : TypeToken<List<T>>() {}.type
            return Gson().fromJson(listJson, type)
        }
        return result
    }
    override fun saveInt(key: String, value: Int) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putInt(key, value)
        editor.apply()
    }
    private fun getInt(key: String) :Int {
        return mSettings!!.getInt(key, 65000)
    }
    override fun saveString(key: String, text: String) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putString(key, text)
        editor.apply()
    }
    private fun getString(key: String) :String {
        return mSettings!!.getString(key, "NOT SET!").toString()
    }

    // работа с блютузом
    override fun initBLEStructure() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ошибка 1", Toast.LENGTH_SHORT).show()
            finish()
        }
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "ошибка 2", Toast.LENGTH_SHORT).show()
            finish()
        } else {
//            Toast.makeText(this, "mBluetoothAdapter != null", Toast.LENGTH_SHORT).show()
        }
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
    }
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("ResourceAsColor")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when {
                BluetoothLeService.ACTION_GATT_CONNECTED == action -> {
                    Toast.makeText(context, "подключение установлено к $connectedDeviceAddress", Toast.LENGTH_SHORT).show()
                    reconnectThreadFlag = false
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED == action -> {
                    mConnected = false
                    endFlag = true
                    status_connection_tv.text = getString(R.string.offline)
                    status_connection_tv.setTextColor(Color.rgb(255, 49,49))
                    invalidateOptionsMenu()
                    mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
                    percentSynchronize = 0
                    eth_ble_stack_control(3)
                    if(!reconnectThreadFlag && !mScanning && !inScanFragmentFlag){
                        reconnectThreadFlag = true
                        reconnectThread()
                        println("--> ACTION_GATT_DISCONNECTED  reconnectThread()")
                    }
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action -> {
                    mConnected = true
                    status_connection_tv.text = getString(R.string.connected)
                    status_connection_tv.setTextColor(Color.rgb(10, 132,255))
                    setCharacteristicSend()
                    eth_ble_stack_control(2)
                    if (mBluetoothLeService != null) {
                        displayGattServices(mBluetoothLeService!!.supportedGattServices)
                        println("Notify подписка mGattUpdateReceiver")
                        subscriptionNotification()
                    }
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE == action -> {
                    if(intent.getByteArrayExtra(BluetoothLeService.TX_CHAR) != null) displayDataRegister(intent.getByteArrayExtra(BluetoothLeService.TX_CHAR))
                }
            }
        }
    }

    private fun displayDataRegister(data: ByteArray?) {
        if (data != null) {
            new_dg_from_bt(data)
        }
        globalSemaphore = true
    }

    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String?
        val unknownServiceString = ("unknown_service")
        val unknownCharaString =("unknown_characteristic")
        val gattServiceData = ArrayList<HashMap<String, String?>>()
        val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String?>>>()
        mGattCharacteristics = java.util.ArrayList()


        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String?>()
            uuid = gattService.uuid.toString()
            currentServiceData[listName] = lookup(uuid, unknownServiceString)
            currentServiceData[listUUID] = uuid
            gattServiceData.add(currentServiceData)
            val gattCharacteristicGroupData = ArrayList<HashMap<String, String?>>()
            val gattCharacteristics = gattService.characteristics
            val charas = ArrayList<BluetoothGattCharacteristic>()

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                charas.add(gattCharacteristic)
                val currentCharaData = HashMap<String, String?>()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData[listName] = lookup(uuid, unknownCharaString)
                currentCharaData[listUUID] = uuid
                gattCharacteristicGroupData.add(currentCharaData)
//                System.err.println("------->   ХАРАКТЕРИСТИКА: $uuid")
            }
            mGattCharacteristics.add(charas)
            gattCharacteristicData.add(gattCharacteristicGroupData)
        }
        val gattServiceAdapter = SimpleExpandableListAdapter(
            this,
            gattServiceData,
            android.R.layout.simple_expandable_list_item_2, arrayOf(listName, listUUID), intArrayOf(android.R.id.text1, android.R.id.text2),
            gattCharacteristicData,
            android.R.layout.simple_expandable_list_item_2, arrayOf(listName, listUUID), intArrayOf(android.R.id.text1, android.R.id.text2))
        mGattServicesList!!.setAdapter(gattServiceAdapter)
        if (mScanning) { scanLeDevice(false) }
    }
    override fun reconnectThread() {
        System.err.println("--> reconnectThread started")
        var j = 1
        val reconnectThread = Thread {
            while (reconnectThreadFlag) {
                runOnUiThread {
                    if(j % 5 == 0) {
                        reconnectThreadFlag = false
                        scanLeDevice(true)
                        System.err.println("DeviceControlActivity------->   Переподключение со сканированием №$j")
                    } else {
                        reconnect()
                        System.err.println("DeviceControlActivity------->   Переподключение без сканирования №$j")
                    }
                    j++
                }
                try {
                    Thread.sleep(RECONNECT_BLE_PERIOD.toLong())
                } catch (ignored: Exception) { }
            }
        }
        reconnectThread.start()
    }
    override fun reconnect () {
        //полное завершение сеанса связи и создание нового в onResume
        if (mBluetoothLeService != null) {
            unbindService(mServiceConnection)
            mBluetoothLeService = null
        }

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)

        //BLE
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBluetoothLeService != null) {
            mBluetoothLeService!!.connect(getString(CONNECTED_DEVICE_ADDRESS))
        } else {
            println("--> вызываем функцию коннекта к устройству $connectedDevice = null")
        }
    }
    override fun disconnect () {
        if (mBluetoothLeService != null) {
            println("--> дисконнектим всё к хуям и анбайндим")
            mBluetoothLeService!!.disconnect()
            unbindService(mServiceConnection)
            mBluetoothLeService = null
        }
        mConnected = false
        endFlag = true
        runOnUiThread {
            status_connection_tv.text = getString(R.string.offline)
            status_connection_tv.setTextColor(Color.rgb(255, 49,49))
            mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
        }
        invalidateOptionsMenu()
        percentSynchronize = 0

        if(!reconnectThreadFlag && !mScanning && !inScanFragmentFlag){
            reconnectThreadFlag = true
            reconnectThread()
            println("--> disconnect  reconnectThread()")
        }
        flagScanWithoutConnect = true
    }
    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }
    private fun isPermissionsGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
    @SuppressLint("MissingPermission")
    override fun scanLeDevice(enable: Boolean) {
        if (isPermissionsGranted(context = baseContext)) {
            System.err.println("DeviceControlActivity------->   isPermissionsGranted = true")
            if (enable) {
                mScanning = true
                scanList = reinitScanList()
                mBluetoothAdapter!!.startLeScan(mLeScanCallback)
                System.err.println("DeviceControlActivity------->   startLeScan flagScanWithoutConnect=$flagScanWithoutConnect")
            } else {
                mScanning = false
                mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
                System.err.println("DeviceControlActivity------->   stopLeScan flagScanWithoutConnect=$flagScanWithoutConnect")
            }
        } else {
            System.err.println("DeviceControlActivity------->   isPermissionsGranted = false")
        }
    }
    @SuppressLint("MissingPermission")
    private val mLeScanCallback = BluetoothAdapter.LeScanCallback { device, _, _ ->
        runOnUiThread {
            if (device.name != null) {
//                System.err.println("------->   ===============найден девайс: " + device.address +"-"+ device.name +"==============")
                if (device.address == connectedDeviceAddress) {
//                    System.err.println("------->   ==========это нужный нам девайс $device  $flagScanWithoutConnect ==============")
                    System.err.println("DeviceControlActivity------->   mLeScanCallback flagScanWithoutConnect=$flagScanWithoutConnect")
                    if (!flagScanWithoutConnect) {
                        scanLeDevice(false)
                        reconnectThreadFlag = true
                        reconnectThread()
                        println("--> mLeScanCallback  reconnectThread()")
                    }
                }
                addLEDeviceToScanList(device.name, device)
            }
        }
    }
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.title_location_permission)
                    .setMessage(R.string.text_location_permission)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            MY_PERMISSIONS_REQUEST_LOCATION)
                        scanLeDevice(true)
                    }
                    .create()
                    .show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
            }
        }

        //проверка включена ли геолокация и если выключена, то показ предложения её включить
        val lm = this.getSystemService(LOCATION_SERVICE) as LocationManager
        var gpsEnabled = false
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ignored: java.lang.Exception) {
        }
        if (!gpsEnabled) {
            // notify user
//            showLocationPermissionDialog()
        }
    }
    private fun askPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Handler().postDelayed({
                requestMultiplePermissions.launch(arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT))
            }, 300)
        }
        else{
//            Dexter.withActivity(this).withPermissions(
//                Manifest.permission.BLUETOOTH,
//                Manifest.permission.BLUETOOTH_ADMIN,
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//            )
//                .withListener(object : MultiplePermissionsListener {
//                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
//                        if (report.areAllPermissionsGranted()) {
////                            System.err.println(" LOLOLOEFWEF --->  onPermissionsChecked true")
//                            val intent = Intent(this@SplashScreen, ScanActivity::class.java)
//                            startActivity(intent)
//                            finish()
//                        } else {
////                            System.err.println(" LOLOLOEFWEF --->  onPermissionsChecked false")
//                            askPermissions()
//                        }
//                    }
//
//                    override fun onPermissionRationaleShouldBeShown(
//                        permissions: List<PermissionRequest>,
//                        token: PermissionToken
//                    ) {
//                        token.continuePermissionRequest()
//                    }
//                }).check()
        }
    }
    @SuppressLint("LogNotTimber")
    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
                System.err.println("LOLOLOEFWEF --->  ${it.key} = ${it.value}")
        }
    }
    private fun addLEDeviceToScanList(item: String, device: BluetoothDevice?) {
        var canAdd = true
        for (i in scanList.indices) {
            if (device != null) {
                if (scanList[i].getAddr() == device.address) {
                    canAdd = false
                }
            }
        }
        if (canAdd) {
            if (device != null) {
                scanList.add(ScanItem(item, device.address.toString()))
                RxUpdateMainEvent.getInstance().updateScanList(ScanItem(item, device.address.toString()))
            }
        }
    }
    private fun setCharacteristicSend() {
        for (i in mGattCharacteristics.indices) {
            for (j in mGattCharacteristics[i].indices) {
                if (mGattCharacteristics[i][j].uuid.toString() == RX_CHAR) {
                    bleSendCharacteristic = mGattCharacteristics[i][j]
                }
            }
        }
    }
    private fun setCharacteristicReceive() {
        for (i in mGattCharacteristics.indices) {
            for (j in mGattCharacteristics[i].indices) {
                if (mGattCharacteristics[i][j].uuid.toString() == TX_CHAR) {
                    bleReceivedCharacteristic = mGattCharacteristics[i][j]
                }
            }
        }
    }
    private fun subscriptionNotification() {
        setCharacteristicReceive()
        mBluetoothLeService!!.setCharacteristicNotification(bleReceivedCharacteristic, true)
    }


    companion object {
        @JvmStatic private val KEY_RESULT = "RESULT"


        var gattBle : BluetoothGatt?=null
        var bleSendCharacteristic: BluetoothGattCharacteristic? = null
        var bleReceivedCharacteristic: BluetoothGattCharacteristic? = null
        var mBluetoothLeService: BluetoothLeService? = null

        @SuppressLint("MissingPermission")
        @JvmStatic
        fun send_to_ble(dg: ByteArray):Int {// вызов из NDK - отправить кодограмму в gatt.
            bleSendCharacteristic?.value = dg
            val ret = gattBle?.writeCharacteristic(bleSendCharacteristic)
            if (ret == true) return 1; else return 0
        }

        @JvmStatic
        fun frame(array: ByteArray) {
//            System.err.println("frame array = ${array.size}")
            bytesArrayFrame.postValue(array)
            RxUpdateMainEvent.getInstance().updateTerminalFragment()
        }

        @JvmStatic
        fun upd_status_param(par_no:Int, v: ByteArray, s: ByteArray) {// вызов из NDK - обновить значения переменных
            when(par_no) {
                1 ->  { param1.postValue(String(v)); param1name.postValue(String(s)) }
                2 ->  { param2.postValue(String(v));  param2name.postValue(String(s)) }
                3 ->  { param3.postValue(String(v));  param3name.postValue(String(s)) }
                4 ->  { param4.postValue(String(v));  param4name.postValue(String(s)) }
                5 ->  { param5.postValue(String(v));  param5name.postValue(String(s)) }
                6 ->  { param6.postValue(String(v));  param6name.postValue(String(s)) }
                7 ->  { param7.postValue(String(v));  param7name.postValue(String(s)) }
                8 ->  { param8.postValue(String(v));  param8name.postValue(String(s)) }
                9 ->  { param9.postValue(String(v));  param9name.postValue(String(s)) }
                10 -> { param10.postValue(String(v)); param10name.postValue(String(s)) }
                11 -> { param11.postValue(String(v)); param11name.postValue(String(s)) }
                12 -> { param12.postValue(String(v)); param12name.postValue(String(s)) }
                13 -> { param13.postValue(String(v)); param13name.postValue(String(s)) }
                14 -> { param14.postValue(String(v)); param14name.postValue(String(s)) }
                15 -> { param15.postValue(String(v)); param15name.postValue(String(s)) }
                16 -> { param16.postValue(String(v)); param16name.postValue(String(s)) }
                17 -> { param17.postValue(String(v)); param17name.postValue(String(s)) }
                18 -> { param18.postValue(String(v)); param18name.postValue(String(s)) }
                19 -> { param19.postValue(String(v)); param19name.postValue(String(s)) }
                20 -> { param20.postValue(String(v)); param20name.postValue(String(s)) }
                21 -> { param21.postValue(String(v)); param21name.postValue(String(s)) }
                22 -> { param22.postValue(String(v)); param22name.postValue(String(s)) }
                23 -> { param23.postValue(String(v)); param23name.postValue(String(s)) }
                24 -> { param24.postValue(String(v)); param24name.postValue(String(s)) }
                25 -> { param25.postValue(String(v)); param25name.postValue(String(s)) }
                26 -> { param26.postValue(String(v)); param26name.postValue(String(s)) }
                27 -> { param27.postValue(String(v)); param27name.postValue(String(s)) }
                28 -> { param28.postValue(String(v)); param28name.postValue(String(s)) }
                29 -> { param29.postValue(String(v)); param29name.postValue(String(s)) }
                30 -> { param30.postValue(String(v)); param30name.postValue(String(s)) }
            }
            RxUpdateMainEvent.getInstance().updateDebugFragment()
            RxUpdateMainEvent.getInstance().updateHomeFragment()
        }

        var bytesArrayFrame: MutableLiveData<ByteArray> = MutableLiveData<ByteArray>()

        var param1: MutableLiveData<String> = MutableLiveData<String>()
        var param2: MutableLiveData<String> = MutableLiveData<String>()
        var param3: MutableLiveData<String> = MutableLiveData<String>()
        var param4: MutableLiveData<String> = MutableLiveData<String>()
        var param5: MutableLiveData<String> = MutableLiveData<String>()
        var param6: MutableLiveData<String> = MutableLiveData<String>()
        var param7: MutableLiveData<String> = MutableLiveData<String>()
        var param8: MutableLiveData<String> = MutableLiveData<String>()
        var param9: MutableLiveData<String> = MutableLiveData<String>()
        var param10: MutableLiveData<String> = MutableLiveData<String>()
        var param11: MutableLiveData<String> = MutableLiveData<String>()
        var param12: MutableLiveData<String> = MutableLiveData<String>()
        var param13: MutableLiveData<String> = MutableLiveData<String>()
        var param14: MutableLiveData<String> = MutableLiveData<String>()
        var param15: MutableLiveData<String> = MutableLiveData<String>()
        var param16: MutableLiveData<String> = MutableLiveData<String>()
        var param17: MutableLiveData<String> = MutableLiveData<String>()
        var param18: MutableLiveData<String> = MutableLiveData<String>()
        var param19: MutableLiveData<String> = MutableLiveData<String>()
        var param20: MutableLiveData<String> = MutableLiveData<String>()
        var param21: MutableLiveData<String> = MutableLiveData<String>()
        var param22: MutableLiveData<String> = MutableLiveData<String>()
        var param23: MutableLiveData<String> = MutableLiveData<String>()
        var param24: MutableLiveData<String> = MutableLiveData<String>()
        var param25: MutableLiveData<String> = MutableLiveData<String>()
        var param26: MutableLiveData<String> = MutableLiveData<String>()
        var param27: MutableLiveData<String> = MutableLiveData<String>()
        var param28: MutableLiveData<String> = MutableLiveData<String>()
        var param29: MutableLiveData<String> = MutableLiveData<String>()
        var param30: MutableLiveData<String> = MutableLiveData<String>()

        var param1name: MutableLiveData<String> = MutableLiveData<String>()
        var param2name: MutableLiveData<String> = MutableLiveData<String>()
        var param3name: MutableLiveData<String> = MutableLiveData<String>()
        var param4name: MutableLiveData<String> = MutableLiveData<String>()
        var param5name: MutableLiveData<String> = MutableLiveData<String>()
        var param6name: MutableLiveData<String> = MutableLiveData<String>()
        var param7name: MutableLiveData<String> = MutableLiveData<String>()
        var param8name: MutableLiveData<String> = MutableLiveData<String>()
        var param9name: MutableLiveData<String> = MutableLiveData<String>()
        var param10name: MutableLiveData<String> = MutableLiveData<String>()
        var param11name: MutableLiveData<String> = MutableLiveData<String>()
        var param12name: MutableLiveData<String> = MutableLiveData<String>()
        var param13name: MutableLiveData<String> = MutableLiveData<String>()
        var param14name: MutableLiveData<String> = MutableLiveData<String>()
        var param15name: MutableLiveData<String> = MutableLiveData<String>()
        var param16name: MutableLiveData<String> = MutableLiveData<String>()
        var param17name: MutableLiveData<String> = MutableLiveData<String>()
        var param18name: MutableLiveData<String> = MutableLiveData<String>()
        var param19name: MutableLiveData<String> = MutableLiveData<String>()
        var param20name: MutableLiveData<String> = MutableLiveData<String>()
        var param21name: MutableLiveData<String> = MutableLiveData<String>()
        var param22name: MutableLiveData<String> = MutableLiveData<String>()
        var param23name: MutableLiveData<String> = MutableLiveData<String>()
        var param24name: MutableLiveData<String> = MutableLiveData<String>()
        var param25name: MutableLiveData<String> = MutableLiveData<String>()
        var param26name: MutableLiveData<String> = MutableLiveData<String>()
        var param27name: MutableLiveData<String> = MutableLiveData<String>()
        var param28name: MutableLiveData<String> = MutableLiveData<String>()
        var param29name: MutableLiveData<String> = MutableLiveData<String>()
        var param30name: MutableLiveData<String> = MutableLiveData<String>()

        var lastConnectDeviceAddress by Delegates.notNull<String>()
        var reconnectThreadFlag by Delegates.notNull<Boolean>()
        var flagScanWithoutConnect by Delegates.notNull<Boolean>()


        //настройки
        var showInfoDialogsFlag by Delegates.notNull<Boolean>()
        var inScanFragmentFlag by Delegates.notNull<Boolean>()
        var scanList by Delegates.notNull<ArrayList<ScanItem>>()
        var statList by Delegates.notNull<ArrayList<String>>()
        var connectedDevice by Delegates.notNull<String>()
        var connectedDeviceAddress by Delegates.notNull<String>()
//        var  by Delegates.notNull<Int>()
        init {
            System.loadLibrary("bt_drv")
        }
    }
}
