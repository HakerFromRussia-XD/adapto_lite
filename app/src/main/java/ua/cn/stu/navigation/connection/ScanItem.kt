package ua.cn.stu.navigation.connection

class ScanItem (private val title: String, private val addr: String) {
    fun getTitle(): String { return title }
    fun getAddr(): String { return addr }
}