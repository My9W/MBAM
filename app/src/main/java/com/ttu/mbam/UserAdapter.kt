package com.ttu.mbam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ttu.mbam.model.User

class UserAdapter(
    private var users: List<User>,
    private val onUserClicked: (User) -> Unit,
    private val onUserEdited: (User) -> Unit,
    private val onUserDeleted: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvNik: TextView = itemView.findViewById(R.id.tvNik)
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        val tvUsage: TextView = itemView.findViewById(R.id.tvUsage)
        val tvDetails: TextView = itemView.findViewById(R.id.tvDetails)
        val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
        val ivEdit: ImageView = itemView.findViewById(R.id.ivEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.tvName.text = user.name
        holder.tvNik.text = "NIK: ${user.nik}"
        holder.tvAddress.text = "Alamat : ${user.address}"
        holder.tvUsage.text = "Penggunaan Bulan Ini: ${user.usageThisMonth}"
        holder.tvDetails.text = """
            Meter Saat Ini: ${user.meterReading}
            Terakhir Pembayaran: ${user.lastPaymentDate}
        """.trimIndent()

        holder.itemView.setOnClickListener { onUserClicked(user) }
        holder.ivDelete.setOnClickListener { onUserDeleted(user) }
        holder.ivEdit.setOnClickListener { onUserEdited(user) }
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        this.users = newUsers
        notifyDataSetChanged()
    }
}
