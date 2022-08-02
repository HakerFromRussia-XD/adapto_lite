package ua.cn.stu.navigation.contract

/**
 * Implement this interface in your fragment if you want to show additional action in the toolbar
 * while that fragment is displayed to the user.
 */
interface HasRenameProfileAction {
    fun getRenameProfileAction(): RenameProfileAction
//    fun getNewTitle
}
class RenameProfileAction(
    val onRenameProfileAction: Runnable
)