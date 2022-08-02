package ua.cn.stu.navigation.contract

/**
 * Implement this interface in your fragment if you want to override default toolbar title
 */
interface HasCustomTitle {
    /**
     * @return the string resource which should be displayed instead of default title
     */
    fun getTitleRes(): String
}