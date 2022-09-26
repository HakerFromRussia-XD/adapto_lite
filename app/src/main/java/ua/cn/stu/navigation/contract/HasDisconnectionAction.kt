package ua.cn.stu.navigation.contract

/**
 * Implement this interface in your fragment if you want to show additional action in the toolbar
 * while that fragment is displayed to the user.
 */
interface HasDisconnectionAction {

    /**
     * @return a custom action specification, see [DisconnectionAction] class for details
     */
    fun getDisconnectionAction(): DisconnectionAction

}

class DisconnectionAction(
    val onDisconnectionAction: Runnable
)