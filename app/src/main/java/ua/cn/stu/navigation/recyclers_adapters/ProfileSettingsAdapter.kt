package ua.cn.stu.navigation.recyclers_adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.OnProfilePeriodClickListener

class ProfileSettingsAdapter(private val profileName: ArrayList<String>,//profileName
                             private val timeStartPeriod: ArrayList<Int>,
                             private val inputSpeed: ArrayList<Int>,
                             private val onProfilePeriodClickListener: OnProfilePeriodClickListener
) : RecyclerView.Adapter<ProfileSettingsAdapter.BasalProfileViewHolder>() {

    inner class BasalProfileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val selectProfilePeriodTimeBtn: View = view.findViewById(R.id.select_time_btn)
        val selectProfileInputSpeedBtn: View = view.findViewById(R.id.select_input_speed_btn)
        val deleteProfilePeriodBtn: View = view.findViewById(R.id.delete_period_profile_btn)
        val deleteProfilePeriodIv: ImageView = view.findViewById(R.id.delete_profile_period_iv) as ImageView
        val addProfileBtn: View = view.findViewById(R.id.add_profile_btn)
        val cell: CardView = view.findViewById(R.id.cv) as CardView
        val profileCell: ConstraintLayout = view.findViewById(R.id.profile_cell) as ConstraintLayout
        val addCell: ConstraintLayout = view.findViewById(R.id.add_cell) as ConstraintLayout
        val startTimePeriodProfile: TextView = view.findViewById(R.id.start_time_period_profile_tv) as TextView
        val inputSpeedPeriodProfile: TextView = view.findViewById(R.id.input_speed_period_profile_tv) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasalProfileViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_period_profile, parent, false)
        return BasalProfileViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BasalProfileViewHolder, position: Int) {
        println("onBindViewHolder selectedProfile = " + profileName.size)
        val countCell = profileName.size
        val profileName = profileName[position]


        if (profileName != "add") {
            val startTimePeriodProfile = timeStartPeriod[position]
            val inputSpeedPeriodProfile = inputSpeed[position]
            holder.profileCell.visibility = View.VISIBLE
            holder.addCell.visibility = View.GONE
            holder.deleteProfilePeriodIv.visibility = View.VISIBLE
            holder.deleteProfilePeriodBtn.visibility = View.VISIBLE
            holder.cell.setCardBackgroundColor(Color.WHITE)
            holder.cell.cardElevation = 5F
            if (startTimePeriodProfile < 10) {
                holder.startTimePeriodProfile.text = "0$startTimePeriodProfile : 00"
            } else {
                holder.startTimePeriodProfile.text = "$startTimePeriodProfile : 00"
            }
            if (inputSpeedPeriodProfile%100 < 10) {
                holder.inputSpeedPeriodProfile.text = "${inputSpeedPeriodProfile/100}.0${inputSpeedPeriodProfile%100} U"
            } else {
                holder.inputSpeedPeriodProfile.text = "${inputSpeedPeriodProfile/100}.${inputSpeedPeriodProfile%100} U"
            }


            holder.deleteProfilePeriodBtn.setOnClickListener {
                onProfilePeriodClickListener.onClicked(profileName, position, deleteProfile = true, addProfile = false, selectTime = false, selectSpeed = false)
            }
            if (countCell <= 2) {
                holder.deleteProfilePeriodIv.visibility = View.GONE
                holder.deleteProfilePeriodBtn.visibility = View.GONE
            }

            holder.selectProfilePeriodTimeBtn.setOnClickListener {
                onProfilePeriodClickListener.onClicked(profileName, position, deleteProfile = false, addProfile = false, selectTime = true, selectSpeed = false)
            }
            holder.selectProfileInputSpeedBtn.setOnClickListener {
                onProfilePeriodClickListener.onClicked(profileName, position, deleteProfile = false, addProfile = false, selectTime = false, selectSpeed = true)
            }
        } else {
            holder.profileCell.visibility = View.GONE
            holder.addCell.visibility = View.VISIBLE
            holder.cell.setCardBackgroundColor(Color.TRANSPARENT)
            holder.cell.cardElevation = 0F
            holder.addProfileBtn.setOnClickListener {
                onProfilePeriodClickListener.onClicked(profileName, position, deleteProfile = false, addProfile = true, selectTime = false, selectSpeed = false)
            }
        }
    }

    override fun getItemCount(): Int {
        return profileName.size
    }
}