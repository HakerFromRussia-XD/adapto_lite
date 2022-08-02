package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import kotlinx.android.synthetic.main.dialog_battery.view.*
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.MainActivity.Companion.basalSpeed
import ua.cn.stu.navigation.MainActivity.Companion.battryPercent
import ua.cn.stu.navigation.MainActivity.Companion.cannuleTime
import ua.cn.stu.navigation.MainActivity.Companion.liIonPercent
import ua.cn.stu.navigation.MainActivity.Companion.reservoirVolume
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.*
import ua.cn.stu.navigation.databinding.FragmentRefillingBinding

class RefillingFragment : Fragment(), HasCustomTitle, HasBatteryAction {

    private lateinit var binding: FragmentRefillingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentRefillingBinding.inflate(inflater, container, false)

        MainActivity.onRefillingScreen = true
        showLoader()

        return binding.root
    }

    override fun getTitleRes(): String = getString(R.string.refilling)
    override fun getBatteryAction(): BatteryAction {
        return BatteryAction(
            onBatteryAction = {
                showBatteryDialog()
            }
        )
    }

    @SuppressLint("InflateParams", "SetTextI18n")
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
        reservoirProgress.setProgressWithAnimation(((reservoirVolume).toFloat()/ ConstantManager.RESERVOIR_VOLUME *100),1000)
        if (((reservoirVolume).toFloat()/ ConstantManager.RESERVOIR_VOLUME *100) >= 0) { reservoirProgress.progressBarColor = Color.rgb(255, 49, 49) }
        if (((reservoirVolume).toFloat()/ ConstantManager.RESERVOIR_VOLUME *100) > 29) { reservoirProgress.progressBarColor = Color.rgb(255, 212, 34) }
        if (((reservoirVolume).toFloat()/ ConstantManager.RESERVOIR_VOLUME *100) > 50) { reservoirProgress.progressBarColor = Color.rgb(17, 184, 38) }
        val cannuleProgress = dialogBinding.findViewById<CircularProgressBar>(R.id.alert_dialog_circle_progress_cannule_pb)
        if (((cannuleTime).toFloat()/ ConstantManager.CANNULE_RESOURCE *100) >= 0) { cannuleProgress.progressBarColor = Color.rgb(17, 184, 38) }
        if (((cannuleTime).toFloat()/ ConstantManager.CANNULE_RESOURCE *100) > 50) { cannuleProgress.progressBarColor = Color.rgb(255, 212, 34) }
        if (((cannuleTime).toFloat()/ ConstantManager.CANNULE_RESOURCE *100) > 71) { cannuleProgress.progressBarColor = Color.rgb(255, 49, 49) }
        cannuleProgress.setProgressWithAnimation(((cannuleTime).toFloat()/ ConstantManager.CANNULE_RESOURCE *100), 1000)

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
            Toast.makeText(context, getString(R.string.refilling_varning), Toast.LENGTH_SHORT).show()
        }

        val yesBtn = dialogBinding.findViewById<View>(R.id.v_andex_alert_dialog_layout_confirm)
        yesBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }
    private fun showLoader() {
        binding.refillingLoaderView.setAnimation(R.raw.loader)
        binding.refillingLoaderView.playAnimation()
    }
}