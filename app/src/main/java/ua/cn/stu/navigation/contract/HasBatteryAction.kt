package ua.cn.stu.navigation.contract

/**
 * Implement this interface in your fragment if you want to show additional action in the toolbar
 * while that fragment is displayed to the user.
 */
interface HasBatteryAction {

    /**
     * @return a custom action specification, see [BatteryAction] class for details
     */
    fun getBatteryAction(): BatteryAction

}

class BatteryAction(
    val onBatteryAction: Runnable
)