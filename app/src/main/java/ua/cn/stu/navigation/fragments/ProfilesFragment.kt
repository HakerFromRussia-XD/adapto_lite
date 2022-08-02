package ua.cn.stu.navigation.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import ua.cn.stu.navigation.MainActivity.Companion.changeProfile
import ua.cn.stu.navigation.MainActivity.Companion.dataAllCharts
import ua.cn.stu.navigation.MainActivity.Companion.inProfileSettingsFragmentFlag
import ua.cn.stu.navigation.MainActivity.Companion.inputSpeedAllPeriodsMain
import ua.cn.stu.navigation.MainActivity.Companion.periodNamesMain
import ua.cn.stu.navigation.MainActivity.Companion.profileNames
import ua.cn.stu.navigation.MainActivity.Companion.selectedProfile
import ua.cn.stu.navigation.MainActivity.Companion.startTimeAllPeriodsMain
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.*
import ua.cn.stu.navigation.contract.ConstantManager.Companion.MAX_COUNT_PROFILES
import ua.cn.stu.navigation.databinding.FragmentProfilesBinding
import ua.cn.stu.navigation.persistence.preference.PreferenceKeys
import ua.cn.stu.navigation.recyclers_adapters.ProfileAdapter
import ua.cn.stu.navigation.rx.RxUpdateMainEvent

class ProfilesFragment : Fragment(), HasCustomTitle, HasReturnAction {

    private lateinit var binding: FragmentProfilesBinding

    private var oldSelectedProfile: Int? = null
    private var image: Drawable? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var adapter: ProfileAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("profileNames = $profileNames")
    }

    @SuppressLint("CheckResult")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfilesBinding.inflate(inflater, container, false)
        oldSelectedProfile = selectedProfile
        image = ResourcesCompat.getDrawable(resources, R.drawable.ic_top_bar, null)

        RxUpdateMainEvent.getInstance().selectBasalProfileSubjectObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                adapter?.notifyItemChanged(oldSelectedProfile!!)
                oldSelectedProfile = selectedProfile
                adapter?.notifyItemChanged(selectedProfile)
            }

        initAdapter(binding.profileRv)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addProfile() {
        profileNames.removeLast()
        adapter?.notifyItemChanged(adapter!!.itemCount-1)//adapter!!.itemCount-1)
        profileNames.add("profile new")
        dataAllCharts.add(createFakeDataChart())
        adapter?.notifyItemChanged(adapter!!.itemCount-1)
        if (profileNames.size < MAX_COUNT_PROFILES) profileNames.add("add")
        adapter?.notifyItemChanged(adapter!!.itemCount-1)
        saveChange()

        // создание настроечных данных нового профиля с дефолтными данными
        periodNamesMain.add(arrayOf("period profile", "add"))
        startTimeAllPeriodsMain.add(intArrayOf(0))
        inputSpeedAllPeriodsMain.add(intArrayOf(100))

        if (profileNames.size < MAX_COUNT_PROFILES) {
//            navigator().runAddBasalProfiles(
//                (adapter!!.itemCount - 1),
//                "profile new"
//            ) // счёт профилей у нас с 0 а на помпе с 1 поэтому номер предпоследней ячейки у нас будет
        } else {
//            navigator().runAddBasalProfiles(
//            (adapter!!.itemCount),
//            "profile new")
        }
//        navigator().runReadNumBasalProfiles()
//        navigator().runReadBasalProfiles()
        onCancelPressed()
    }

    private fun saveChange() {
        navigator().saveArrayList(PreferenceKeys.PROFILE_NAMES, profileNames)
        navigator().saveArrayList(PreferenceKeys.DATA_ALL_CHARTS, dataAllCharts)

        navigator().saveArrayStringList(PreferenceKeys.PERIOD_NAMES_MAIN, periodNamesMain)
        navigator().saveIntArrayList(PreferenceKeys.START_TIME_ALL_PERIODS_MAIN, startTimeAllPeriodsMain)
        navigator().saveIntArrayList(PreferenceKeys.INPUT_SPEED_ALL_PERIODS_MAIN, inputSpeedAllPeriodsMain)
    }

    private fun initAdapter(profile_rv: RecyclerView) {
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        profile_rv.layoutManager = linearLayoutManager
//        ProfileAdapter
//        println("2 dataChart = $dataAllCharts")
        adapter = ProfileAdapter(profileNames, dataAllCharts, image!!, object : OnProfileClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onClicked(
                name: String,
                selectProfile: Int,
                deleteProfile: Boolean,
                addProfile: Boolean
            ) {
                if (name != "add") {
                    if (deleteProfile) {
                        println("код удаления профиля")
                        showDeleteDialog(selectProfile)
                    } else {
                        println("код выбора профиля")
                        if (selectProfile == oldSelectedProfile) {
                            changeProfile = selectProfile
                            onOpenProfilePressed()
                        }
                        else { showSelectDialog(selectProfile) }
                    }
                } else {
                    println("код добавления нового профиля")
                    addProfile()
                }
            }

        })
        profile_rv.adapter = adapter
    }
    private fun createFakeDataChart() :ArrayList<Int> {
        val dataChart = ArrayList<Int>()
        dataChart.add(0)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        dataChart.add(100)
        return dataChart
    }

    @SuppressLint("InflateParams")
    private fun showSelectDialog(selectProfile: Int) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_select_profile_basal, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()
        val title = dialogBinding.findViewById<View>(R.id.dialog_select_profile_title_tv) as TextView
        title.text = profileNames[selectProfile]

        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialog_select_profile_cancel)
        cancelBtn.setOnClickListener {
            onOpenProfilePressed()
            changeProfile = selectProfile
            myDialog.dismiss()
        }

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_select_profile_confirm)
        yesBtn.setOnClickListener {
            selectedProfile = selectProfile
            navigator().saveInt(PreferenceKeys.SELECTED_PROFILE, selectedProfile)
            adapter?.notifyItemChanged(oldSelectedProfile!!)
            oldSelectedProfile = selectProfile
            adapter?.notifyItemChanged(selectProfile)
            saveChange()
//            navigator().runActivateBasalProfiles(selectProfile)
            myDialog.dismiss()
        }
    }
    @SuppressLint("InflateParams", "NotifyDataSetChanged", "SetTextI18n")
    private fun showDeleteDialog(selectProfile: Int) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_delete_profile_basal, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()
        val title = dialogBinding.findViewById<View>(R.id.dialog_delete_profile_title_tv) as TextView
        title.text = "Удаление \""+profileNames[selectProfile]+"\""

        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialog_delete_profile_cancel)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

        val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_delete_profile_confirm)
        yesBtn.setOnClickListener {
            if (selectedProfile > selectProfile)  {
                --selectedProfile
                navigator().saveInt(PreferenceKeys.SELECTED_PROFILE, selectedProfile)
                oldSelectedProfile = selectedProfile
            }
            profileNames.removeAt(selectProfile)
            dataAllCharts.removeAt(selectProfile)
            if (profileNames.count() == (MAX_COUNT_PROFILES - 1)) {
                if (profileNames.last() != "add") {
                    profileNames.add("add")
                    for (item in selectProfile..profileNames.count()) {
                        adapter?.notifyItemChanged(item)
                    }
                } else {
                    adapter?.notifyDataSetChanged()
                }
            } else { adapter?.notifyDataSetChanged() }

            // удаляем настроечных данных профиля
            periodNamesMain.removeAt(selectProfile)
            startTimeAllPeriodsMain.removeAt(selectProfile)
            inputSpeedAllPeriodsMain.removeAt(selectProfile)
            saveChange()
//            navigator().runDeleteBasalProfiles(selectProfile)
            myDialog.dismiss()
        }
    }
    private fun onOpenProfilePressed() {
        navigator().showBasalProfileSettingsScreen()
        inProfileSettingsFragmentFlag = true
    }

    override fun getTitleRes(): String = getString(R.string.basal_profiles)
    override fun getReturnAction(): ReturnAction {
        return ReturnAction(
            onReturnAction = {
                onCancelPressed()
            }
        )
    }


    private fun onCancelPressed() {
        navigator().goBack()
    }
}