package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.dialog_battery.view.*
import kotlinx.android.synthetic.main.dialog_enter_pin.*
import kotlinx.android.synthetic.main.dialog_info.*
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.MainActivity.Companion.activatePinCodeSettings
import ua.cn.stu.navigation.MainActivity.Companion.basalSpeed
import ua.cn.stu.navigation.MainActivity.Companion.battryPercent
import ua.cn.stu.navigation.MainActivity.Companion.cannuleTime
import ua.cn.stu.navigation.MainActivity.Companion.liIonPercent
import ua.cn.stu.navigation.MainActivity.Companion.massagesListMain
import ua.cn.stu.navigation.MainActivity.Companion.pinCodeSettings
import ua.cn.stu.navigation.MainActivity.Companion.profileNames
import ua.cn.stu.navigation.MainActivity.Companion.reservoirVolume
import ua.cn.stu.navigation.MainActivity.Companion.selectedProfile
import ua.cn.stu.navigation.MainActivity.Companion.timestampsListMain
import ua.cn.stu.navigation.MainActivity.Companion.typeCellsListMain
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.*
import ua.cn.stu.navigation.contract.ConstantManager.Companion.CANNULE_RESOURCE
import ua.cn.stu.navigation.contract.ConstantManager.Companion.RESERVOIR_VOLUME
import ua.cn.stu.navigation.databinding.FragmentMenuBinding
import ua.cn.stu.navigation.recyclers_adapters.ChatAdapter
import ua.cn.stu.navigation.rx.RxUpdateMainEvent
import java.text.SimpleDateFormat
import java.util.*


class MenuFragment : Fragment(), HasBatteryAction {

    private lateinit var binding: FragmentMenuBinding
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: ChatAdapter? = null
    private var ifShowDialog: Boolean = false
    private var myDialogInfo: Dialog? = null

    @SuppressLint("InflateParams", "SetTextI18n", "ClickableViewAccessibility", "CheckResult",
        "NotifyDataSetChanged"
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMenuBinding.inflate(inflater, container, false)
        myDialogInfo = Dialog(requireContext())
        navigator().showBottomNavigationMenu(true)


        binding.profilesButton.setOnClickListener {
            if (MainActivity.refreshBasalProfile) { showInfoBasalProfileDialog() }
            else { onProfilePressed() }
        }
        binding.settingsButton.setOnClickListener { onSettingsPressed() }
        binding.bolusButton.setOnClickListener { onBolusPressed() }
        binding.basalButton.setOnClickListener {
            if (MainActivity.temporaryBasalActivated) {
//                navigator().runTemporaryBasalStatus()
            }
            else { onTemporaryBasalPressed() }
        }
        if (profileNames.size > 1) {
            binding.profilesButtonMassageTv.text = profileNames[selectedProfile]
        }

        RxUpdateMainEvent.getInstance().selectBasalProfileSubjectObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (selectedProfile < profileNames.size) {
                    binding.profilesButtonMassageTv.text = profileNames[selectedProfile]
                }
            }


        initAdapter(binding.chatRv)
        fastScrollToEndChat(binding.chatRv)
        binding.sendBtn.setOnClickListener {
            if (binding.writeMassageEt.text.toString() != "") {
                addMassage()
                binding.chatRv.smoothScrollToPosition(binding.chatRv.adapter!!.itemCount - 2)
//                navigator().runDateAndTime()
//                navigator().runLogCommand(true, binding.writeMassageEt.text.toString())
//                navigator().runLogCommand(false, binding.writeMassageEt.text.toString())
                binding.writeMassageEt.text.clear()
            }
            println("send")
        }

        RxUpdateMainEvent.getInstance().updateChatSubjectObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                adapter?.notifyDataSetChanged()
                scrollToEndChat(binding.chatRv)
            }
        return binding.root
    }



    @SuppressLint("NotifyDataSetChanged", "NewApi")
    private fun addMassage() {
        val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ROOT)
        fun getDateString(time: Long) : String = simpleDateFormat.format(time * 1000L)

        val timestamp = System.currentTimeMillis()/1000L

        val dt2 = getDateString(timestamp)

        typeCellsListMain.removeLast()
        adapter?.notifyItemChanged(adapter!!.itemCount-1)
        typeCellsListMain.add("type_2")
        massagesListMain.add(binding.writeMassageEt.text.toString())
        timestampsListMain.add(timestamp.toString())
        adapter?.notifyItemChanged(adapter!!.itemCount-1)
        typeCellsListMain.add("invalidate")
        adapter?.notifyItemChanged(adapter!!.itemCount-1)
        println("displayDatePump=$timestamp  dt2=$dt2 logCommand")
    }
    private fun initAdapter(chat_rv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        chat_rv.layoutManager = linearLayoutManager
        adapter = ChatAdapter(typeCellsListMain, massagesListMain, timestampsListMain, object : OnChatClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onClicked(name: String, selectCell: Int) {
                if (name != "invalidate") {
                    println("код работы с информационной ячейкой чата №$selectCell")
                } else {
//                    navigator().runDateAndTime()
                }
            }
        })
        chat_rv.adapter = adapter
    }

    override fun getBatteryAction(): BatteryAction {
        return BatteryAction(
            onBatteryAction = {
                showBatteryDialog()
            }
        )
    }

    @SuppressLint("InflateParams", "StringFormatInvalid", "SetTextI18n")
    private fun showBatteryDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_battery, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        //заполнение текстовых данных диалога
        dialogBinding.alert_dialog_percent_battery_tv.text = "$battryPercent%"
        dialogBinding.alert_dialog_percent_li_ion_tv.text = "$liIonPercent%"
        dialogBinding.alert_dialog_volume_reservoir_tv.text = getString(R.string._u, reservoirVolume)
        if (cannuleTime > 119) {dialogBinding.alert_dialog_volume_cannule_tv.text = getString(R.string.cannule_timer_h, cannuleTime/60)}
        else {dialogBinding.alert_dialog_volume_cannule_tv.text = getString(R.string.cannule_timer_m, cannuleTime)}
        dialogBinding.alert_dialog_reservoir_message_tv.text = getString(R.string.up_to_46_hours, (reservoirVolume/basalSpeed).toInt())

        val reservoirProgress = dialogBinding.findViewById<CircularProgressBar>(R.id.alert_dialog_circle_progress_reservoir_pb)
        reservoirProgress.setProgressWithAnimation(((reservoirVolume).toFloat()/RESERVOIR_VOLUME*100),1000)
        if (((reservoirVolume).toFloat()/RESERVOIR_VOLUME*100) >= 0) { reservoirProgress.progressBarColor = Color.rgb(255, 49, 49) }
        if (((reservoirVolume).toFloat()/RESERVOIR_VOLUME*100) > 29) { reservoirProgress.progressBarColor = Color.rgb(255, 212, 34) }
        if (((reservoirVolume).toFloat()/RESERVOIR_VOLUME*100) > 50) { reservoirProgress.progressBarColor = Color.rgb(17, 184, 38) }
        val cannuleProgress = dialogBinding.findViewById<CircularProgressBar>(R.id.alert_dialog_circle_progress_cannule_pb)
        if (((cannuleTime).toFloat()/CANNULE_RESOURCE*100) >= 0) { cannuleProgress.progressBarColor = Color.rgb(17, 184, 38) }
        if (((cannuleTime).toFloat()/CANNULE_RESOURCE*100) > 50) { cannuleProgress.progressBarColor = Color.rgb(255, 212, 34) }
        if (((cannuleTime).toFloat()/CANNULE_RESOURCE*100) > 71) { cannuleProgress.progressBarColor = Color.rgb(255, 49, 49) }
        cannuleProgress.setProgressWithAnimation(((cannuleTime).toFloat()/CANNULE_RESOURCE*100), 1000)

        val batteryImage = dialogBinding.findViewById<ImageView>(R.id.alert_dialog_battery_iv)
        if (battryPercent >= 0) batteryImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_10))
        if (battryPercent > 10) batteryImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_30))
        if (battryPercent > 30) batteryImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_50))
        if (battryPercent > 50) batteryImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_70))
        if (battryPercent > 70) batteryImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_90))
        if (battryPercent > 90) batteryImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_100))

        val akbImage = dialogBinding.findViewById<ImageView>(R.id.alert_dialog_li_ion_iv)
        if (liIonPercent >= 0) akbImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_10))
        if (liIonPercent > 10) akbImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_30))
        if (liIonPercent > 30) akbImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_50))
        if (liIonPercent > 50) akbImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_70))
        if (liIonPercent > 70) akbImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_90))
        if (liIonPercent > 90) akbImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.battery_for_dialog_100))

        val cancelBtn = dialogBinding.findViewById<View>(R.id.v_andex_alert_dialog_layout_cancel)
        cancelBtn.setOnClickListener {
            showRefillDialog()
            myDialog.dismiss()
        }

        val yesBtn = dialogBinding.findViewById<View>(R.id.v_andex_alert_dialog_layout_confirm)
        yesBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }
    @SuppressLint("InflateParams")
    private fun showRefillDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_refill, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()


        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialog_refill_cancel)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_refill_confirm)
        yesBtn.setOnClickListener {
            showInstractionRefillingDialog()
//            navigator().runInitRefillingRegister(2)
            myDialog.dismiss()
        }
    }
    @SuppressLint("InflateParams")
    @Suppress("DEPRECATION")
    private fun showPinCodeSettigsDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_enter_pin, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        myDialog.pincode_settings_view.requestToShowKeyboard()
        Handler().postDelayed({
            myDialog.pincode_settings_view.showKeyboard()
        }, 200)

        myDialog.pincode_settings_view.setPasscodeEntryListener { passcode ->
            if (passcode == pinCodeSettings) { navigator().showSettingsScreen() }
            else { Toast.makeText(context, getString(R.string.pin_entering_varning), Toast.LENGTH_SHORT).show() }
            hideKeyboard(myDialog.pincode_settings_view)
            myDialog.dismiss()
        }

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_settings_pin_confirm)
        yesBtn.setOnClickListener {
            hideKeyboard(myDialog.pincode_settings_view)
            myDialog.dismiss()
        }
    }
    @SuppressLint("InflateParams")
    private fun showInstractionRefillingDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_instruction_refilling, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_instraction_refilling_confirm)
        yesBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }
    @SuppressLint("InflateParams", "SetTextI18n", "CheckResult")
    @Suppress("DEPRECATION")
    private fun showInfoBasalProfileDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_info, null)
        myDialogInfo?.setContentView(dialogBinding)
        myDialogInfo?.setCancelable(false)
        myDialogInfo?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialogInfo?.show()
        ifShowDialog = true

        myDialogInfo?.dialog_info_title_tv?.text = getString(R.string.update_basal_profiles_info)
        myDialogInfo?.info_dialog_massage_tv?.text = getString(R.string.update_basal_profiles_info_massage, 0) + "%"
        RxUpdateMainEvent.getInstance().percentSinhronizationProfileSubjectObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                myDialogInfo?.info_dialog_massage_tv?.text = getString(R.string.update_basal_profiles_info_massage,
                MainActivity.percentSinhronizeBasalProfiles
                ) + "%"
                if (MainActivity.percentSinhronizeBasalProfiles == 100) {
                    if (ifShowDialog) {
                        Handler().postDelayed({
                          //TODO раскомментировать, когда ваня сделает одинарную отправку флага изменения
                          // профилей в нотификацию, после добавления нового профиля с мобилки

//                            onProfilePressed()
                        }, 300)
                        ifShowDialog = false
                        myDialogInfo?.dismiss()
                    }

                }
            }

        myDialogInfo?.info_animation_view?.setAnimation(R.raw.updating)
        myDialogInfo?.info_animation_view?.loop(true)

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_info_confirm)
        yesBtn.setOnClickListener {
            ifShowDialog = false
            myDialogInfo?.dismiss()
        }
    }

    @Suppress("DEPRECATION")
    private fun scrollToEndChat(chatRv: RecyclerView) {

        if (chatRv.adapter?.itemCount!! >= 2) {
            chatRv.smoothScrollToPosition(chatRv.adapter!!.itemCount - 2)
        }
        binding.writeMassageEt.setOnFocusChangeListener { _, _ ->
            Handler().postDelayed({
                chatRv.smoothScrollToPosition(chatRv.adapter!!.itemCount - 2)
            }, 200)
        }
        binding.writeMassageEt.setOnClickListener {
            Handler().postDelayed({
                chatRv.smoothScrollToPosition(chatRv.adapter!!.itemCount - 2)
            }, 300)
        }
    }
    @Suppress("DEPRECATION")
    private fun fastScrollToEndChat(chatRv: RecyclerView) {
//        chatRv.smoothScrollToPosition(chatRv.adapter!!.itemCount - 2)
        chatRv.scrollToPosition(chatRv.adapter!!.itemCount - 2)
        binding.writeMassageEt.setOnFocusChangeListener { _, _ ->
            Handler().postDelayed({
                chatRv.scrollToPosition(chatRv.adapter!!.itemCount - 2)
            }, 200)
        }
    }
    private fun View.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    private fun onProfilePressed() {
        navigator().showProfileScreen()
    }
    private fun onSettingsPressed() {
        if (activatePinCodeSettings) {
            showPinCodeSettigsDialog()
        } else {
            navigator().showSettingsScreen()
        }
    }
    private fun onBolusPressed() {
        navigator().showBolusScreen()
    }
    private fun onTemporaryBasalPressed() {
        navigator().showTemporaryBasalScreen()
    }
}