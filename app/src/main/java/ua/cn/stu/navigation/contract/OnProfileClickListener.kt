package ua.cn.stu.navigation.contract

interface OnProfileClickListener {
    fun onClicked(name : String, selectProfile: Int, deleteProfile: Boolean, addProfile: Boolean)
}