package ua.cn.stu.navigation

import android.os.CountDownTimer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TerminalViewModel: ViewModel() {

    private var _number: MutableLiveData<Int> = MutableLiveData(0)
    var number: LiveData<Int> = _number
//    var number : Int by mutableStateOf(0)
//        private set

    fun addNumber(count: Int){
        _number.value = count
    }

    init {
//        startTimer()
        System.err.println("TerminalViewModel init")
    }
}