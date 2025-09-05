package com.ttu.mbam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ttu.mbam.activity.HistoryActivity
import com.ttu.mbam.model.History

class HistoryAdapter(
    private var historyList: List<History>,
    private val activity: HistoryActivity
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvHistoryUserName)
        val tvNik: TextView = itemView.findViewById(R.id.tvHistoryNIK)
        val tvOldReading: TextView = itemView.findViewById(R.id.tvOldReading)
        val tvNewReading: TextView = itemView.findViewById(R.id.tvNewReading)
        val tvUsage: TextView = itemView.findViewById(R.id.tvUsage)
        val tvTotalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)
        val tvPaymentDate: TextView = itemView.findViewById(R.id.tvPaymentDate)
        val ivSave: ImageView = itemView.findViewById(R.id.ivSave)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]

        holder.tvName.text = history.name
        holder.tvNik.text = "NIK: ${history.nik}"
        holder.tvOldReading.text = "Lama: ${history.oldReading} m³"
        holder.tvNewReading.text = "Baru: ${history.newReading} m³"
        holder.tvUsage.text = "Penggunaan: ${history.usage} m³"
        holder.tvTotalPrice.text = "Harga: Rp ${"%,.2f".format(history.totalPrice)}"
        holder.tvPaymentDate.text = "Tanggal: ${history.date}"

        holder.ivSave.setOnClickListener {
            activity.savePaymentNoteAsImage(history)
        }
    }

    override fun getItemCount(): Int = historyList.size

    fun updateHistory(newHistory: List<History>) {
        this.historyList = newHistory
        notifyDataSetChanged()
    }
}
