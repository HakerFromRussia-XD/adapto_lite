package ua.cn.stu.navigation.recyclers_adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import ua.cn.stu.navigation.MainActivity
import ua.cn.stu.navigation.R
import ua.cn.stu.navigation.contract.OnChatClickListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ChatAdapter(private val typeCellsList: ArrayList<String>,
                  private val massagesList: ArrayList<String>,
                  private val timestampsList: ArrayList<String>,
                  private val onChatClickListener: OnChatClickListener
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val invalidateChatBtn: View = view.findViewById(R.id.invalidate_chat_btn)
        val typeOneCell: ConstraintLayout = view.findViewById(R.id.massage_type_1_cell) as ConstraintLayout
        val typeTwoCell: ConstraintLayout = view.findViewById(R.id.massage_type_2_cell) as ConstraintLayout
        val typeTreeCell: ConstraintLayout = view.findViewById(R.id.massage_type_3_cell) as ConstraintLayout
        val invalidateCell: ConstraintLayout = view.findViewById(R.id.invalidate_cell) as ConstraintLayout
        val massageType1Tv: TextView = view.findViewById(R.id.massage_type_1_tv) as TextView
        val massageType2Tv: TextView = view.findViewById(R.id.massage_type_2_tv) as TextView
        val massageType3Tv: TextView = view.findViewById(R.id.massage_type_3_tv) as TextView
        val timestampType1Tv: TextView = view.findViewById(R.id.timestamp_type_1_tv) as TextView
        val timestampType2Tv: TextView = view.findViewById(R.id.timestamp_type_2_tv) as TextView
        val timestampType3Tv: TextView = view.findViewById(R.id.timestamp_type_3_tv) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        println("onBindViewHolder selectedProfile = " + MainActivity.selectedProfile)
        val simpleDateFormat = SimpleDateFormat("dd MMMM  HH:mm", Locale.ROOT)
        fun getDateString(time: Long) : String = simpleDateFormat.format(time * 1000L)
        val typeCell = typeCellsList[position]



        if (typeCell != "invalidate") {
            if (position < massagesList.size && position < timestampsList.size) {
                if (typeCell == "type_1") {
                    holder.typeTreeCell.visibility = View.GONE
                    holder.typeTwoCell.visibility = View.GONE
                    holder.typeOneCell.visibility = View.VISIBLE
                    holder.invalidateCell.visibility = View.GONE
                    holder.massageType1Tv.text = massagesList[position]
                    holder.timestampType1Tv.text = getDateString(timestampsList[position].toLong())
                }
                if (typeCell == "type_2") {

                    holder.typeTreeCell.visibility = View.GONE
                    holder.typeTwoCell.visibility = View.VISIBLE
                    holder.typeOneCell.visibility = View.GONE
                    holder.invalidateCell.visibility = View.GONE
                    holder.massageType2Tv.text = massagesList[position]
                    holder.timestampType2Tv.text = getDateString(timestampsList[position].toLong())
                }
                if (typeCell == "type_3") {
                    holder.typeTreeCell.visibility = View.VISIBLE
                    holder.typeTwoCell.visibility = View.GONE
                    holder.typeOneCell.visibility = View.GONE
                    holder.invalidateCell.visibility = View.GONE
                    holder.massageType3Tv.text = massagesList[position]
                    holder.timestampType3Tv.text =
                        getDateString(timestampsList[position].toLong())
                }
            }
        } else {
            holder.typeTreeCell.visibility = View.GONE
            holder.typeTwoCell.visibility = View.GONE
            holder.typeOneCell.visibility = View.GONE
            holder.invalidateCell.visibility = View.VISIBLE
            holder.invalidateChatBtn.setOnClickListener {
                onChatClickListener.onClicked(typeCell, position)
            }
        }
    }
    override fun getItemCount(): Int {
        return typeCellsList.size
    }

}