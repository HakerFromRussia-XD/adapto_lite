package ua.cn.stu.navigation.contract

/**
 * Implement this interface in your fragment if you want to show additional action in the toolbar
 * while that fragment is displayed to the user.
 */
interface HasReturnAction {

    /**
     * @return a custom action specification, see [ReturnAction] class for details
     */
    fun getReturnAction(): ReturnAction

}

class ReturnAction(
    val onReturnAction: Runnable
)