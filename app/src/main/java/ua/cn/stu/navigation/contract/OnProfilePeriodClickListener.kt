package ua.cn.stu.navigation.contract

interface OnProfilePeriodClickListener {
    fun onClicked(name : String, selectProfile: Int, deleteProfile: Boolean, addProfile: Boolean, selectTime: Boolean, selectSpeed: Boolean)
}