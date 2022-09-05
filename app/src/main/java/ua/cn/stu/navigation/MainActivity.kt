package ua.cn.stu.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import ua.cn.stu.navigation.ble.BluetoothLeService
import ua.cn.stu.navigation.ble.SampleGattAttributes.*
import ua.cn.stu.navigation.connection.ScanItem
import ua.cn.stu.navigation.contract.*
import ua.cn.stu.navigation.contract.ConstantManager.Companion.ACTIVATE_BASAL_PROFILE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.ACTIVATE_BASAL_PROFILE_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.AKB_PERCENT
import ua.cn.stu.navigation.contract.ConstantManager.Companion.AKB_PERCENT_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BALANCE_DRUG
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BALANCE_DRUG_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_LOCK_CONTROL
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_LOCK_CONTROL_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_SPEED
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_SPEED_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_TEMPORARY_PERFORMANCE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_TEMPORARY_PERFORMANCE_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_TEMPORARY_TIME
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_TEMPORARY_TIME_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_TEMPORARY_TYPE_ADJUSTMENT
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_TEMPORARY_TYPE_ADJUSTMENT_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_TEMPORARY_VALUE_ADJUSTMENT
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BASAL_TEMPORARY_VALUE_ADJUSTMENT_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BATTERY_PERCENT
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BATTERY_PERCENT_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BOLUS_ACTIVATE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BOLUS_ACTIVATE_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BOLUS_AMOUNT
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BOLUS_AMOUNT_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BOLUS_DELETE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BOLUS_DELETE_CONFIRM
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BOLUS_DELETE_CONFIRM_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BOLUS_DELETE_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BOLUS_TYPE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.BOLUS_TYPE_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.DATE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.DATE_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.DELETE_BASAL_PROFILE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.DELETE_BASAL_PROFILE_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.EXTENEDED_AND_DUAL_PATTERN_BOLUS_RESTRICTION_FLAG
import ua.cn.stu.navigation.contract.ConstantManager.Companion.EXTENEDED_AND_DUAL_PATTERN_BOLUS_RESTRICTION_FLAG_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.FAKE_DATA_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.INIT_REFUELLING
import ua.cn.stu.navigation.contract.ConstantManager.Companion.INIT_REFUELLING_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.IOB
import ua.cn.stu.navigation.contract.ConstantManager.Companion.IOB_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.LOG_UPDATE_DEPTH
import ua.cn.stu.navigation.contract.ConstantManager.Companion.MY_PERMISSIONS_REQUEST_LOCATION
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NAME_BASAL_PROFILE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NAME_BASAL_PROFILE_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NUM_ACTIVE_BASAL_PROFILE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NUM_ACTIVE_BASAL_PROFILES_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NUM_BASAL_PROFILES
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NUM_BASAL_PROFILES_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NUM_MODIFIED_BASAL_PROFILES
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NUM_MODIFIED_BASAL_PROFILES_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NUM_MODIFIED_PERIOD_MODIFIED_BASAL_PROFILE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NUM_MODIFIED_PERIOD_MODIFIED_BASAL_PROFILE_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NUM_PERIODS_MODIFIED_BASAL_PROFILE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.NUM_PERIODS_MODIFIED_BASAL_PROFILE_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.PERIOD_BASAL_PROFILE_DATA
import ua.cn.stu.navigation.contract.ConstantManager.Companion.PERIOD_BASAL_PROFILE_DATA_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.READ_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.RECONNECT_BLE_PERIOD
import ua.cn.stu.navigation.contract.ConstantManager.Companion.REQUEST_ENABLE_BT
import ua.cn.stu.navigation.contract.ConstantManager.Companion.SUPER_BOLUS_BASL_VOLIUM
import ua.cn.stu.navigation.contract.ConstantManager.Companion.SUPER_BOLUS_BASL_VOLIUM_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.SUPER_BOLUS_RESTRICTION_FLAG
import ua.cn.stu.navigation.contract.ConstantManager.Companion.SUPER_BOLUS_RESTRICTION_FLAG_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.SUPER_BOLUS_TIME
import ua.cn.stu.navigation.contract.ConstantManager.Companion.SUPER_BOLUS_TIME_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.SUPPLIES_RSOURCE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.SUPPLIES_RSOURCE_REGISTER
import ua.cn.stu.navigation.contract.ConstantManager.Companion.TIME_WORK_PUMP
import ua.cn.stu.navigation.contract.ConstantManager.Companion.TIME_WORK_PUMP_REGISTER
import ua.cn.stu.navigation.databinding.ActivityMainBinding
import ua.cn.stu.navigation.fragments.*
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.ACTIVATE_DUAL_PATTERN_BOLUS
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.ACTIVATE_EXTENDED_BOLUS
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.ACTIVATE_PIN_CODE_APP
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.ACTIVATE_PIN_CODE_SETTINGS
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.ACTIVATE_STEP_BOLUS
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.ACTIVATE_SUPER_BOLUS
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.ATTEMPTS_TO_UN_LOCK
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.CONNECTES_DEVICE
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.CONNECTES_DEVICE_ADDRESS
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.CONNECTION_PASSWORD
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.DATA_ALL_CHARTS
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.INPUT_SPEED_ALL_PERIODS_MAIN
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.LAST_CONNECTES_DEVICE_ADDRESS
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.MASSAGES_LIST_MAIN
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.PERIOD_NAMES_MAIN
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.PIN_CODE_APP
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.PIN_CODE_SETTINGS
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.PROFILE_NAMES
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.SELECTED_PROFILE
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.START_TIME_ALL_PERIODS_MAIN
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.TIMESTAMPS_LIST_MAIN
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.TIMESTAMP_LAST_LOG_MASSAGE
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.TIMESTAMP_LAST_READ_LOG
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys.TYPE_CELLS_LIST_MAIN
import ua.cn.stu.navigation.rx.RxUpdateMainEvent
import ua.cn.stu.navigation.services.BasalPeriod
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
    private var mCharacteristic: BluetoothGattCharacteristic? = null
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
    private var flagScanWithoutConnect = false

    private val currentFragment: Fragment
        get() = supportFragmentManager.findFragmentById(R.id.fragmentContainer)!!

    private val fragmentListener = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            super.onFragmentViewCreated(fm, f, v, savedInstanceState)
            updateUi()
        }
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        mSettings = this.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        mGattServicesList = findViewById(R.id.gatt_services_list)
        setSupportActionBar(binding.toolbar)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.menu_bottom_layout_bg)
        initAllVariables()


        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, ScanningFragment())
                .commit()
//                supportFragmentManager
//                    .beginTransaction()
//                    .add(R.id.fragmentContainer, MenuFragment())
//                    .commit()
        }


        createStatList()
        scanList = reinitScanList()
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentListener, false)

        binding.leftFragmentBtn.setOnClickListener { showStatisticScreen() }
        binding.centerFragmentBtn.setOnClickListener { showMenuScreen() }
        binding.rightFragmentBtn.setOnClickListener { showBMSScreen() }


        // инициализация блютуз
        checkLocationPermission()
        initBLEStructure()
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
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (mBluetoothLeService != null) {
            connectedDevice =  getString(CONNECTES_DEVICE)
            connectedDeviceAddress =  getString(CONNECTES_DEVICE_ADDRESS)
        }
//        if (!mConnected) {
//            reconnectThreadFlag = true
//            reconnectThread()
//        }
    }
    override fun onPause() {
        super.onPause()
        endFlag = true
    }
    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentListener)
        pumpStatusNotifyDataThreadFlag = false
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

    override fun showScanScreen() { launchFragment(ScanningFragment()) }
    override fun showStatisticScreen() { launchFragmentWihtoutStack(StatisticFragment()) }
    override fun showMenuScreen() { launchFragmentWihtoutStack(HomeFragment()) }
    override fun showBMSScreen() { launchFragmentWihtoutStack(BMSFragment()) }
    override fun showBottomNavigationMenu (show: Boolean) {
        if (show) bottom_menu_cl.visibility = View.VISIBLE
        else bottom_menu_cl.visibility = View.GONE
    }

    override fun firstOpenHome() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, BMSFragment())//HomeFragment
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

    override fun setNewTitle(newTitle: String) {
//        binding.titleFragmentTv.text = newTitle
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

        if (supportFragmentManager.backStackEntryCount > 0) {
            return_rl.visibility = View.VISIBLE
            battery_rl.visibility = View.GONE
        } else {
            battery_rl.visibility = View.VISIBLE
            return_rl.visibility = View.GONE
        }

        if (fragment is HasReturnAction) { returned(fragment.getReturnAction()) }
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

    private fun returned(action: ReturnAction) {
        return_btn.setOnClickListener {
            action.onReturnAction.run()
            return@setOnClickListener
        }
    }

    //TODO тут инициализация переменных
    private fun initAllVariables() {
        //init
        lastConnectDeviceAddress = ""
        reconnectThreadFlag = false


        //settings
        pumpStatusNotifyDataThreadFlag = true
        showInfoDialogsFlag = false
        inScanFragmentFlag = false
//
        if (getString(LAST_CONNECTES_DEVICE_ADDRESS) == "NOT SET!") {
            lastConnectDeviceAddress = ""
            saveString(LAST_CONNECTES_DEVICE_ADDRESS, lastConnectDeviceAddress)
        } else { lastConnectDeviceAddress =  getString(LAST_CONNECTES_DEVICE_ADDRESS)}

        if (getString(CONNECTES_DEVICE) == "NOT SET!") {
            connectedDevice = "BT-Pump 12"
            saveString(CONNECTES_DEVICE, connectedDevice)
        } else { connectedDevice =  getString(CONNECTES_DEVICE)}
        if (getString(CONNECTES_DEVICE_ADDRESS) == "NOT SET!") {
            connectedDeviceAddress = "D7:77:A9:47:F9:EC"//"12:34:56:78:90:12"
            saveString(CONNECTES_DEVICE_ADDRESS, connectedDeviceAddress)
        } else { connectedDeviceAddress =  getString(CONNECTES_DEVICE_ADDRESS)}
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
        listA.add("statistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cellstatistic 8 cell")
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
                    if (mBluetoothLeService != null) {
                        displayGattServices(mBluetoothLeService!!.supportedGattServices)
                        println("Notify подписка mGattUpdateReceiver")
                        bleCommand(null, NOTIFICATION_PUMP_STATUS, NOTIFY)
                    }
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE == action -> {
                    if(intent.getByteArrayExtra(BluetoothLeService.PASS_DATA) != null) {
                        intent.getStringExtra(BluetoothLeService.ACTION_STATE)?.let { setActionState(it) }
                    }
                    if(intent.getByteArrayExtra(BluetoothLeService.REGISTER_DATA) != null) displayDataRegister(intent.getByteArrayExtra(BluetoothLeService.REGISTER_DATA))
                    if(intent.getByteArrayExtra(BluetoothLeService.LOG_POINTER) != null) displayLogPointer()

                    setSensorsDataThreadFlag(intent.getBooleanExtra(BluetoothLeService.SENSORS_DATA_THREAD_FLAG, true))
                }
            }
        }
    }
    fun setActionState(value: String) {
        actionState = value
    }
    fun setSensorsDataThreadFlag(value: Boolean){ pumpStatusNotifyDataThreadFlag = value }

    private fun displayDataRegister(data: ByteArray?) {
        if (data != null) {
//            if (dataSortSemaphore == IOB) displayDataIOB(data)
//            if (dataSortSemaphore == SUPPLIES_RSOURCE) displayDataCannuleTime(data)
//            if (dataSortSemaphore == BATTERY_PERCENT) displayDataBatteryPercent(data)
//            if (dataSortSemaphore == AKB_PERCENT) displayDataAkbPercent(data)
//            if (dataSortSemaphore == BALANCE_DRUG) displayDataBalanceDrag(data)
//            if (dataSortSemaphore == BASAL_SPEED) displayDataBasalSpeed(data)
//            if (dataSortSemaphore == INIT_REFUELLING) {}
//            if (dataSortSemaphore == BASAL_TEMPORARY_VALUE_ADJUSTMENT) displayDataBasalTemporaryValueAdjustment(data)
//            if (dataSortSemaphore == BASAL_TEMPORARY_TIME) displayDataBasalTemporaryTime(data)
//            if (dataSortSemaphore == BASAL_TEMPORARY_PERFORMANCE) {}
//            if (dataSortSemaphore == BASAL_TEMPORARY_TYPE_ADJUSTMENT) {}
//            if (dataSortSemaphore == NUM_BASAL_PROFILES) displayDataNumBasalProfiles(data)
//            if (dataSortSemaphore == NUM_MODIFIED_BASAL_PROFILES) displayDataNumModifiedBasalProfiles(data)
//            if (dataSortSemaphore == NAME_BASAL_PROFILE) displayDataNameBasalProfile(data)
//            if (dataSortSemaphore == NUM_PERIODS_MODIFIED_BASAL_PROFILE) displayDataNumPeriodsModifiedBasalProfile(data)
//            if (dataSortSemaphore == NUM_MODIFIED_PERIOD_MODIFIED_BASAL_PROFILE) {}
//            if (dataSortSemaphore == PERIOD_BASAL_PROFILE_DATA) displayDataPeriodBasalProfile(data)
//            if (dataSortSemaphore == BASAL_LOCK_CONTROL) {}
//            if (dataSortSemaphore == ACTIVATE_BASAL_PROFILE) {}
//            if (dataSortSemaphore == DELETE_BASAL_PROFILE) {}
//            if (dataSortSemaphore == NUM_ACTIVE_BASAL_PROFILE) displayDataNumActiveBasalProfile(data)
//            if (dataSortSemaphore == DATE) displayDatePump(data)
//            if (dataSortSemaphore == TIME_WORK_PUMP) displayTimeWorkPump(data)

//            if (dataSortSemaphore == BOLUS_DELETE_CONFIRM) {}
//            if (dataSortSemaphore == BOLUS_DELETE) {}
//            if (dataSortSemaphore == EXTENEDED_AND_DUAL_PATTERN_BOLUS_RESTRICTION_FLAG) { displayDataExtendedBolusRestrictionLimit(data) }
//            if (dataSortSemaphore == SUPER_BOLUS_RESTRICTION_FLAG) displayDataSuperBolusRestrictionLimit(data)
//            if (dataSortSemaphore == BOLUS_TYPE) displayDataBolusType()
//            if (dataSortSemaphore == BOLUS_AMOUNT) displayDataBolusAmount(data)
//            if (dataSortSemaphore == SUPER_BOLUS_TIME) displayDataSuperBolusTime(data)
//            if (dataSortSemaphore == SUPER_BOLUS_BASL_VOLIUM) displayDataSuperBolusBasalVolume(data)
        }
        globalSemaphore = true
    }
    private fun displayLogPointer() {
        globalSemaphore = true
        println( "displayLogPointer logCommand globalSemaphore=$globalSemaphore")
    }
    private fun moveDataSortSemaphore() {
        if (contentEquals(readRegisterPointer!!, IOB_REGISTER)) {
            dataSortSemaphore = IOB
        }
        if (contentEquals(readRegisterPointer!!, SUPPLIES_RSOURCE_REGISTER)) {
            dataSortSemaphore = SUPPLIES_RSOURCE
        }
        if (contentEquals(readRegisterPointer!!, BATTERY_PERCENT_REGISTER)) {
            dataSortSemaphore = BATTERY_PERCENT
        }
        if (contentEquals(readRegisterPointer!!, AKB_PERCENT_REGISTER)) {
            dataSortSemaphore = AKB_PERCENT
        }
        if (contentEquals(readRegisterPointer!!, BALANCE_DRUG_REGISTER)) {
            dataSortSemaphore = BALANCE_DRUG
        }
        if (contentEquals(readRegisterPointer!!, BASAL_SPEED_REGISTER)) {
            dataSortSemaphore = BASAL_SPEED
        }
        if (contentEquals(readRegisterPointer!!, INIT_REFUELLING_REGISTER)) {
            dataSortSemaphore = INIT_REFUELLING
        }
        if (contentEquals(readRegisterPointer!!, BASAL_TEMPORARY_VALUE_ADJUSTMENT_REGISTER)) {
            dataSortSemaphore = BASAL_TEMPORARY_VALUE_ADJUSTMENT
        }
        if (contentEquals(readRegisterPointer!!, BASAL_TEMPORARY_TIME_REGISTER)) {
            dataSortSemaphore = BASAL_TEMPORARY_TIME
        }
        if (contentEquals(readRegisterPointer!!, BASAL_TEMPORARY_PERFORMANCE_REGISTER)) {
            dataSortSemaphore = BASAL_TEMPORARY_PERFORMANCE
        }
        if (contentEquals(readRegisterPointer!!, BASAL_TEMPORARY_TYPE_ADJUSTMENT_REGISTER)) {
            dataSortSemaphore = BASAL_TEMPORARY_TYPE_ADJUSTMENT
        }
        if (contentEquals(readRegisterPointer!!, NUM_BASAL_PROFILES_REGISTER)) {
            dataSortSemaphore = NUM_BASAL_PROFILES
        }
        if (contentEquals(readRegisterPointer!!, NUM_MODIFIED_BASAL_PROFILES_REGISTER)) {
            dataSortSemaphore = NUM_MODIFIED_BASAL_PROFILES
        }
        if (contentEquals(readRegisterPointer!!, NAME_BASAL_PROFILE_REGISTER)) {
            dataSortSemaphore = NAME_BASAL_PROFILE
        }
        if (contentEquals(readRegisterPointer!!, NUM_PERIODS_MODIFIED_BASAL_PROFILE_REGISTER)) {
            dataSortSemaphore = NUM_PERIODS_MODIFIED_BASAL_PROFILE
        }
        if (contentEquals(readRegisterPointer!!, NUM_MODIFIED_PERIOD_MODIFIED_BASAL_PROFILE_REGISTER)) {
            dataSortSemaphore = NUM_MODIFIED_PERIOD_MODIFIED_BASAL_PROFILE
        }
        if (contentEquals(readRegisterPointer!!, PERIOD_BASAL_PROFILE_DATA_REGISTER)) {
            dataSortSemaphore = PERIOD_BASAL_PROFILE_DATA
        }
        if (contentEquals(readRegisterPointer!!, BASAL_LOCK_CONTROL_REGISTER)) {
            dataSortSemaphore = BASAL_LOCK_CONTROL
        }
        if (contentEquals(readRegisterPointer!!, ACTIVATE_BASAL_PROFILE_REGISTER)) {
            dataSortSemaphore = ACTIVATE_BASAL_PROFILE
        }
        if (contentEquals(readRegisterPointer!!, DELETE_BASAL_PROFILE_REGISTER)) {
            dataSortSemaphore = DELETE_BASAL_PROFILE
        }
        if (contentEquals(readRegisterPointer!!, NUM_ACTIVE_BASAL_PROFILES_REGISTER)) {
            dataSortSemaphore = NUM_ACTIVE_BASAL_PROFILE
        }
        if (contentEquals(readRegisterPointer!!, DATE_REGISTER)) {
            dataSortSemaphore = DATE
        }
        if (contentEquals(readRegisterPointer!!, TIME_WORK_PUMP_REGISTER)) {
            dataSortSemaphore = TIME_WORK_PUMP
        }



        if (contentEquals(readRegisterPointer!!, BOLUS_DELETE_CONFIRM_REGISTER)) {
            dataSortSemaphore = BOLUS_DELETE_CONFIRM
        }
        if (contentEquals(readRegisterPointer!!, BOLUS_DELETE_REGISTER)) {
            dataSortSemaphore = BOLUS_DELETE
        }
        if (contentEquals(readRegisterPointer!!, EXTENEDED_AND_DUAL_PATTERN_BOLUS_RESTRICTION_FLAG_REGISTER)) {
            dataSortSemaphore = EXTENEDED_AND_DUAL_PATTERN_BOLUS_RESTRICTION_FLAG
        }
        if (contentEquals(readRegisterPointer!!, SUPER_BOLUS_RESTRICTION_FLAG_REGISTER)) {
            dataSortSemaphore = SUPER_BOLUS_RESTRICTION_FLAG
        }
        if (contentEquals(readRegisterPointer!!, BOLUS_TYPE_REGISTER)) {
            dataSortSemaphore = BOLUS_TYPE
        }
        if (contentEquals(readRegisterPointer!!, BOLUS_AMOUNT_REGISTER)) {
            dataSortSemaphore = BOLUS_AMOUNT
        }
        if (contentEquals(readRegisterPointer!!, BOLUS_ACTIVATE_REGISTER)) {
            dataSortSemaphore = BOLUS_ACTIVATE
        }
        if (contentEquals(readRegisterPointer!!, SUPER_BOLUS_TIME_REGISTER)) {
            dataSortSemaphore = SUPER_BOLUS_TIME
        }
        if (contentEquals(readRegisterPointer!!, SUPER_BOLUS_BASL_VOLIUM_REGISTER)) {
            dataSortSemaphore = SUPER_BOLUS_BASL_VOLIUM
        }
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
            mBluetoothLeService!!.connect(getString(CONNECTES_DEVICE_ADDRESS))
        } else {
            println("--> вызываем функцию коннекта к устройству $connectedDevice = null")
        }
    }
    override fun disconnect () {
        pumpStatusNotifyDataThreadFlag = false
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
    override fun scanLeDevice(enable: Boolean) {
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
    }
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
    override fun bleCommand(byteArray: ByteArray?, command: String, typeCommand: String){
//        println("пароль command=$command")
        for (i in mGattCharacteristics.indices) {
            for (j in mGattCharacteristics[i].indices) {
//                println("пароль mGattCharacteristics[i][j]=${mGattCharacteristics[i][j].uuid}")
                if (mGattCharacteristics[i][j].uuid.toString() == command) {
                    mCharacteristic = mGattCharacteristics[i][j]
//                    println("пароль command=$command")
                    if (typeCommand == WRITE){
//                        println("пароль WRITE")
                        if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
                            mCharacteristic?.value = byteArray
//                            println("пароль WRITE!!!!")
                            mBluetoothLeService?.writeCharacteristic(mCharacteristic)
                        }
                    }

                    if (typeCommand == READ){
                        if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                            mBluetoothLeService?.readCharacteristic(mCharacteristic)
                        }
                    }

                    if (typeCommand == NOTIFY){
                        if (mCharacteristic?.properties!! and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                            mNotifyCharacteristic = mCharacteristic
                            mBluetoothLeService!!.setCharacteristicNotification(
                                mCharacteristic, true)
                        }
                    }

                }
            }
        }
    }

    private fun readRegister(state: Int, stateMachineState: Int, registerPointer: ByteArray):Int {
        var mStateMachineState = stateMachineState
        when (state) {
            0 -> { bleCommand(registerPointer, REGISTER_POINTER, WRITE) }
            1 -> { bleCommand(READ_REGISTER, REGISTER_POINTER, READ) }
            2 -> {
                moveDataSortSemaphore()
                return if (contentEquals(readRegisterPointer!!, registerPointer)) {
                    mStateMachineState++
                    mStateMachineState
                } else {
                    mStateMachineState -= 2
                    mStateMachineState
                }
            }
            3 -> { bleCommand(FAKE_DATA_REGISTER, REGISTER_DATA, READ) }
        }
        return 555
    }
    private fun writeRegister(state: Int, stateMachineState: Int, registerPointer: ByteArray, dataForRecord: ByteArray):Int {
        var mStateMachineState = stateMachineState
        when (state) {
            0 -> { bleCommand(registerPointer, REGISTER_POINTER, WRITE) }
            1 -> { bleCommand(READ_REGISTER, REGISTER_POINTER, READ) }
            2 -> {
                moveDataSortSemaphore()

                return if (contentEquals(readRegisterPointer!!, registerPointer)) {
                    mStateMachineState++
                    mStateMachineState
                } else {
                    mStateMachineState -= 2
                    mStateMachineState
                }
            }
            3 -> { bleCommand(dataForRecord, REGISTER_DATA, WRITE) }
        }
        return 555
    }
    private fun writeLogCommand(state: Int, dataForRecord: ByteArray):Int {
        when (state) {
            0 -> { bleCommand(null, NOTIFICATION_PUMP_LOG, NOTIFY) }
            1 -> { bleCommand(dataForRecord, LOG_POINTER, WRITE) }
        }
        return 555
    }


    /**
     * Создаёт массив двух байт из входного числа. Переполнение игнорирует.
     * @param convertibleInt - конвертируемое число из диапазона 0-65535
     */
    private fun castIntToByteArray (convertibleInt: Int): ByteArray {
        var seniorByte: Byte = 0x00
        val result = byteArrayOf(0x00, 0x00)

        if (convertibleInt % 255 > 0) {
            seniorByte = (convertibleInt / 256).toByte()
        }
        val youngerByte: Byte = convertibleInt.toByte()
        result[0] = seniorByte
        result[1] = youngerByte
        return result
    }
    /**
     * Реализация перевода байта в обычное беззнаковое число диапазона 0-255
     * @param Ubyte - диапазон -127 : 128
     */
    private fun castUnsignedCharToInt(Ubyte: Byte): Int {
        var cast = Ubyte.toInt()
        if (cast < 0) {
            cast += 256
        }
        return cast
    }
    //  Реализация сравнения двух массивов байт побайтно
    private fun contentEquals(array1: ByteArray, array2: ByteArray): Boolean {
        var equalsByteArray = true
        var j = 0
        if ((array1.size - array2.size) == 0) { //защита от сравнения массивов разной длинны
            while (j < array1.size){
                if (array1[j] != array2[j]) equalsByteArray = false
                j++
            }
        } else equalsByteArray = false
        return equalsByteArray
    }
    //  Реализация toast доступного из BlockingQueue
    private fun showToast(massage: String) {
        runOnUiThread {
            Toast.makeText(baseContext, massage, Toast.LENGTH_LONG).show()
        }
    }
    private fun convertListStringToArray(convertedList: ArrayList<String>): Array<String> {
        val result = Array(convertedList.size) { "" }
        for (i in 0 until convertedList.size) {
            result[i] = convertedList[i]
        }
        return result
    }
    private fun convertListIntToArray(convertedList: ArrayList<Int>) :IntArray {
        val result = IntArray(convertedList.size)
        for (i in 0 until convertedList.size) {
            result[i] = convertedList[i]
        }
        return result
    }


    companion object {
        @JvmStatic private val KEY_RESULT = "RESULT"

        var lastConnectDeviceAddress by Delegates.notNull<String>()
        var reconnectThreadFlag by Delegates.notNull<Boolean>()

        //переменные чата
        var typeCellsListMain by Delegates.notNull<ArrayList<String>>()
        var massagesListMain by Delegates.notNull<ArrayList<String>>()
        var timestampsListMain by Delegates.notNull<ArrayList<String>>()


        //настройки
        var pumpStatusNotifyDataThreadFlag by Delegates.notNull<Boolean>()

        var showInfoDialogsFlag by Delegates.notNull<Boolean>()
        var inScanFragmentFlag by Delegates.notNull<Boolean>()
        var scanList by Delegates.notNull<ArrayList<ScanItem>>()
        var statList by Delegates.notNull<ArrayList<String>>()
        var connectedDevice by Delegates.notNull<String>()
        var connectedDeviceAddress by Delegates.notNull<String>()
    }
}
