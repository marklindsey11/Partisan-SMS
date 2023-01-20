package com.moez.QKSMS.feature.keyssettings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.widget.checked
import com.moez.QKSMS.R

class KeysSettingsListAdapter(
    private val items: Array<String>,
    private val encodingSchemeId: Int,
    private val callback: (Int) -> Unit
) : RecyclerView.Adapter<KeysSettingsListAdapter.ViewHolder>() {

    private var selectedItem: Int = encodingSchemeId

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
        holder.radio.isChecked = selectedItem == position
        holder.radio.setOnClickListener {
            selectedItem = holder.adapterPosition
            notifyDataSetChanged()
            callback(selectedItem)
        }
    }

    override fun getItemCount() = items.size

}