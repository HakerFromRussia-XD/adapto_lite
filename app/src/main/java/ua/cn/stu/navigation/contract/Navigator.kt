package ua.cn.stu.navigation.contract

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

typealias ResultListener<T> = (T) -> Unit

fun Fragment.navigator(): Navigator {
    return requireActivity() as Navigator
}

interface Navigator {

    fun showScanScreen()
    fun showStatisticScreen()
    fun showHomeScreen()
    fun showBMSScreen()
    fun showDebugScreen()
    fun showBottomNavigationMenu(show: Boolean)
    fun showDisconnectDialog()
    fun <T> saveArrayList(key: String, list: ArrayList<T>)
    fun saveIntArrayList(key: String, list: ArrayList<IntArray>)
    fun saveArrayStringList(key: String, list: ArrayList<Array<String>>)
    fun saveInt(key: String, value: Int)
    fun saveString(key: String, text: String)
    fun initBLEStructure()
    fun scanLeDevice(enable: Boolean)
    fun disconnect ()
    fun reconnect ()
//    fun bleCommand(byteArray: ByteArray?, command: String, typeCommand: String)
    fun reconnectThread()
//    fun showGoBolusDialog(title: String, massage: String, numberOfHundredthsInsulin: Int, numberOfHundredStrechedInsulin: Int, timeBolus: Int)

    fun sendCoordFromTerminal(xPix: Int, yPix: Int)
    fun sendButtonFromTerminal(buttonId: Int)
    fun firstOpenHome()
    fun goBack()
    fun goToMenu()

    fun <T : Parcelable> publishResult(result: T)
    fun <T : Parcelable> listenResult(clazz: Class<T>, owner: LifecycleOwner, listener: ResultListener<T>)
}