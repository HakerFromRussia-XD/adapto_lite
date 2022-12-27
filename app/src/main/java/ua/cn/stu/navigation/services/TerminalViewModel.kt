package ua.cn.stu.navigation.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TerminalViewModel: ViewModel() {

    private var _number: MutableLiveData<Int> = MutableLiveData(0)
    var number: LiveData<Int> = _number

    fun addNumber(count: Int){
        _number.value = count
    }
}