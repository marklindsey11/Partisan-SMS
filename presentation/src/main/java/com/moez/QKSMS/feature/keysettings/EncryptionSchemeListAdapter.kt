package com.moez.QKSMS.feature.keysettings

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.moez.QKSMS.R
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.common.util.extensions.resolveThemeColorStateList
import com.moez.QKSMS.util.Preferences
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

@SuppressLint("NotifyDataSetChanged")
class EncryptionSchemeListAdapter(
    val context: Context,
    val isConversation: Boolean,
    val colors: Colors,
    val prefs: Preferences
    ) : RecyclerView.Adapter<EncryptionSchemeListAdapter.ViewHolder>() {

    private val items: Array<String> = context.resources.getStringArray(
        if (isConversation) R.array.encoding_scheme_labels_conversation
        else R.array.encoding_scheme_labels
    )

    private var selectedSchemeId: Int? = null
    val schemeChanged: Subject<Int> = PublishSubject.create()
    private var enabled: Boolean = true

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radio : RadioButton = itemView.findViewById(R.id.listRadio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.settings_keys_radio_button_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val radioButton = holder.radio
        radioButton.text = items[position]
        radioButton.isChecked = selectedSchemeId == position
        radioButton.textSize = when (prefs.textSize.get()) {
            Preferences.TEXT_SIZE_SMALL -> 14f
            Preferences.TEXT_SIZE_NORMAL -> 16f
            Preferences.TEXT_SIZE_LARGE -> 18f
            Preferences.TEXT_SIZE_LARGER -> 20f
            else -> 16f
        }
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ), intArrayOf(
                radioButton.currentHintTextColor,
                colors.theme().theme
            )
        )
        radioButton.setTextColor(context.resolveThemeColorStateList(android.R.attr.textColorSecondary))
        radioButton.buttonTintList = colorStateList
        if (enabled) {
            radioButton.setOnClickListener {
                if (enabled) {
                    setSelected(holder.adapterPosition)
                    schemeChanged.onNext(holder.adapterPosition)
                }
            }
        }
        radioButton.isClickable = enabled
    }

    fun setSelected(scheme: Int) {
        val oldScheme = selectedSchemeId
        if (oldScheme != scheme) {
            selectedSchemeId = scheme
            if (oldScheme != null) {
                notifyItemChanged(oldScheme)
            }
            notifyItemChanged(scheme)
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (this.enabled != enabled) {
            this.enabled = enabled
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = items.size

}