package com.moez.QKSMS.feature.keyssettings

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.moez.QKSMS.R

@SuppressLint("NotifyDataSetChanged")
class EncryptionSchemeListAdapter(
    private val items: Array<String>,
    encodingSchemeId: Int,
    private val callback: (Int) -> Unit
) : RecyclerView.Adapter<EncryptionSchemeListAdapter.ViewHolder>() {

    private var selectedSchemeId: Int = encodingSchemeId

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radio : RadioButton = itemView.findViewById(R.id.listRadio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.settings_keys_radio_button_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.radio.text = items[position]
        holder.radio.isChecked = selectedSchemeId == position
        holder.radio.setOnClickListener {
            selectedSchemeId = holder.adapterPosition
            notifyDataSetChanged()
            callback(selectedSchemeId)
        }
    }

    fun setSelected(item: Int) {
        selectedSchemeId = item
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

}