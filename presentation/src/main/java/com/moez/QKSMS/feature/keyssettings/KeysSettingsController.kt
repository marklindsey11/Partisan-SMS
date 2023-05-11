package com.moez.QKSMS.feature.keyssettings

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.qrcode.QRCodeWriter
import com.jakewharton.rxbinding2.view.clicks
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkController
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.common.util.FontProvider
import com.moez.QKSMS.common.util.extensions.animateLayoutChanges
import com.moez.QKSMS.common.util.extensions.setBackgroundTint
import com.moez.QKSMS.common.util.extensions.setTint
import com.moez.QKSMS.common.widget.PreferenceView
import com.moez.QKSMS.injection.appComponent
import com.moez.QKSMS.interactor.SetDeleteMessagesAfter
import com.moez.QKSMS.interactor.SetEncodingScheme
import com.moez.QKSMS.interactor.SetEncryptionEnabled
import com.moez.QKSMS.interactor.SetEncryptionKey
import com.moez.QKSMS.interactor.SetLegacyEncryptionEnabled
import com.moez.QKSMS.repository.ConversationRepository
import com.moez.QKSMS.util.Preferences
import io.reactivex.Observable
import kotlinx.android.synthetic.main.settings_keys_activity.copyKey
import kotlinx.android.synthetic.main.settings_keys_activity.enableKey
import kotlinx.android.synthetic.main.settings_keys_activity.encodingSchemesRecycler
import kotlinx.android.synthetic.main.settings_keys_activity.encryptionKeyCategory
import kotlinx.android.synthetic.main.settings_keys_activity.field
import kotlinx.android.synthetic.main.settings_keys_activity.generateKey
import kotlinx.android.synthetic.main.settings_keys_activity.keyInputGroup
import kotlinx.android.synthetic.main.settings_keys_activity.legacyEncryption
import kotlinx.android.synthetic.main.settings_keys_activity.preferences
import kotlinx.android.synthetic.main.settings_keys_activity.qrCodeImage
import kotlinx.android.synthetic.main.settings_keys_activity.scanQr
import kotlinx.android.synthetic.main.settings_keys_activity.settings_delete_encrypted_after
import kotlinx.android.synthetic.main.settings_keys_activity.settings_delete_encrypted_after_pref
import kotlinx.android.synthetic.main.settings_keys_activity.settings_delete_received_after
import kotlinx.android.synthetic.main.settings_keys_activity.settings_delete_received_after_pref
import kotlinx.android.synthetic.main.settings_keys_activity.settings_delete_sent_after
import kotlinx.android.synthetic.main.settings_keys_activity.settings_delete_sent_after_pref
import kotlinx.android.synthetic.main.settings_keys_activity.settings_deletion
import kotlinx.android.synthetic.main.settings_switch_widget.view.checkbox
import javax.crypto.KeyGenerator
import javax.inject.Inject


class KeysSettingsController : QkController<KeysSettingsView, KeysSettingsState, KeysSettingsPresenter>(), KeysSettingsView {

    @Inject lateinit var setEncryptionKey: SetEncryptionKey
    @Inject lateinit var setEncodingScheme: SetEncodingScheme
    @Inject lateinit var setLegacyEncryptionEnabled: SetLegacyEncryptionEnabled
    @Inject lateinit var colors: Colors
    @Inject lateinit var context: Context
    @Inject lateinit var prefs: Preferences
    @Inject lateinit var conversationsRepo: ConversationRepository
    @Inject lateinit var qrCodeWriter: QRCodeWriter
    @Inject lateinit var setDeleteMessagesAfter: SetDeleteMessagesAfter
    @Inject lateinit var fontProvider: FontProvider

    @Inject override lateinit var presenter: KeysSettingsPresenter

    private lateinit var generatedKey: String
    private lateinit var deleteAfterLabels: Array<String>
    private lateinit var encodingSchemes: Array<String>
    private lateinit var schemesListAdapter: EncryptionSchemeListAdapter
    private var threadId = -1L
    private var initialState = KeysSettingsState()
    private var newState = KeysSettingsState()

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.settings_keys_activity
    }

    override fun preferenceClicks(): Observable<PreferenceView> = (0 until preferences.childCount)
        .map { index -> preferences.getChildAt(index) }
        .mapNotNull { view -> view as? PreferenceView }
        .map { preference -> preference.clicks().map { preference } }
        .let { preferences -> Observable.merge(preferences) }

    override fun render(state: KeysSettingsState) {
        encryptionKeyCategory.text =
            if(state.isConversation) context.getText(R.string.settings_global_encryption_key_title)
            else context.getText(R.string.settings_encryption_key_title)

        val isEncryptionEnabled = state.key.isNotBlank()
        enableKey.checkbox.isChecked = isEncryptionEnabled

        keyInputGroup.visibility = if(state.keySettingsIsShown) View.VISIBLE else View.GONE
        scanQr.alpha = if (isEncryptionEnabled) 1f else 0.5f
        scanQr.isClickable = isEncryptionEnabled
        generateKey.alpha = if (isEncryptionEnabled) 1f else 0.5f
        generateKey.isClickable = isEncryptionEnabled
        field.setBackgroundTint(colors.theme().theme)
        legacyEncryption.checkbox.isChecked = state.legacyEncryptionEnabled
        legacyEncryption.alpha = if (isEncryptionEnabled) 1f else 0.5f
        legacyEncryption.isClickable = isEncryptionEnabled

        encodingSchemesRecycler.alpha = if (isEncryptionEnabled) 1f else 0.5f
        encodingSchemesRecycler.children.forEach { radioButton ->
            (radioButton as AppCompatRadioButton?)?.let { renderRadioButton(it, state) }
        }

        settings_deletion.visibility = if(state.isConversation) View.VISIBLE else View.GONE
        settings_delete_encrypted_after.progress = state.deleteEncryptedAfter
        settings_delete_encrypted_after.setTint(colors.theme().theme)
        settings_delete_encrypted_after.thumb.setTint(colors.theme().theme)
        settings_delete_encrypted_after_pref.summary = deleteAfterLabels[state.deleteEncryptedAfter]
        settings_delete_received_after.progress = state.deleteReceivedAfter
        settings_delete_received_after.setTint(colors.theme().theme)
        settings_delete_received_after.thumb.setTint(colors.theme().theme)
        settings_delete_received_after_pref.summary = deleteAfterLabels[state.deleteReceivedAfter]
        settings_delete_sent_after.progress = state.deleteSentAfter
        settings_delete_sent_after.setTint(colors.theme().theme)
        settings_delete_sent_after.thumb.setTint(colors.theme().theme)
        settings_delete_sent_after_pref.summary = deleteAfterLabels[state.deleteSentAfter]
    }

    private fun renderRadioButton(radioButton: AppCompatRadioButton, state: KeysSettingsState) {
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ), intArrayOf(
                radioButton.currentHintTextColor,
                colors.theme().theme
            )
        )

        radioButton.textSize = when (prefs.textSize.get()) {
            Preferences.TEXT_SIZE_SMALL -> 14f
            Preferences.TEXT_SIZE_NORMAL -> 16f
            Preferences.TEXT_SIZE_LARGE -> 18f
            Preferences.TEXT_SIZE_LARGER -> 20f
            else -> 16f
        }

        radioButton.buttonTintList = colorStateList
        radioButton.isClickable = state.key.isNotBlank()
    }

    override fun onViewCreated() {
        super.onViewCreated()
        threadId = requireActivity().intent.getLongExtra("threadId", -1)
        val currentScheme = if(threadId == -1L) prefs.encodingScheme.get()
            else conversationsRepo.getConversation(threadId)?.encodingSchemeId ?: 0
        encodingSchemes = context.resources.getStringArray(R.array.encoding_scheme_labels)
        deleteAfterLabels = context.resources.getStringArray(R.array.delete_message_after_labels)
        schemesListAdapter = EncryptionSchemeListAdapter(encodingSchemes, currentScheme, this::selectEncodingScheme)

        if(threadId == -1L) {
            newState = newState.copy(
                key = prefs.globalEncryptionKey.get(),
                encodingScheme = prefs.encodingScheme.get(),
                legacyEncryptionEnabled = prefs.legacyEncryptionEnabled.get(),
                isConversation = false
            )
            initialState = newState
            presenter.setGlobalParameters(
                key = newState.key,
                encodingScheme = newState.encodingScheme,
                legacyEncryptionEnabled = newState.legacyEncryptionEnabled
            )
            schemesListAdapter.setSelected(prefs.encodingScheme.get())
        } else {
            val conversation = conversationsRepo.getConversation(threadId)
            newState = newState.copy(
                key = conversation?.encryptionKey ?: "",
                encodingScheme = conversation?.encodingSchemeId ?: prefs.encodingScheme.get(),
                legacyEncryptionEnabled = conversation?.legacyEncryptionEnabled ?: prefs.legacyEncryptionEnabled.get(),
                deleteEncryptedAfter = conversation?.deleteEncryptedAfter ?: 0,
                deleteReceivedAfter = conversation?.deleteReceivedAfter ?: 0,
                deleteSentAfter = conversation?.deleteSentAfter ?: 0,
                isConversation = true
            )
            initialState = newState
            presenter.setConversationParameters(
                key = newState.key,
                encodingScheme = newState.encodingScheme,
                legacyEncryptionEnabled = newState.legacyEncryptionEnabled,
                deleteEncryptedAfter = newState.deleteEncryptedAfter,
                deleteReceivedAfter = newState.deleteReceivedAfter,
                deleteSentAfter = newState.deleteSentAfter,
            )
            schemesListAdapter.setSelected(conversation?.encodingSchemeId ?: 0)
        }


        preferences.postDelayed( { preferences?.animateLayoutChanges = true }, 100)
        encodingSchemesRecycler.layoutManager = LinearLayoutManager(context)
        encodingSchemesRecycler.adapter = schemesListAdapter

        settings_delete_encrypted_after.max = deleteAfterLabels.lastIndex
        settings_delete_received_after.max = deleteAfterLabels.lastIndex
        settings_delete_sent_after.max = deleteAfterLabels.lastIndex

        settings_delete_encrypted_after.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    if(fromUser) {
                        presenter.setDeleteEncryptedAfter(value)
                        setDeleteEncryptedAfter(value)
                    }
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            }
        )
        settings_delete_received_after.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    if(fromUser) {
                        presenter.setDeleteReceivedAfter(value)
                        setDeleteReceivedAfter(value)
                    }
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            }
        )
        settings_delete_sent_after.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    if(fromUser) {
                        presenter.setDeleteSentAfter(value)
                        setDeleteSentAfter(value)
                    }
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            }
        )
        field.doOnTextChanged { text, _, _, _ ->
            if(validate(text.toString())) {
                newState = newState.copy(key = text.toString())
                Toast.makeText(context, R.string.settings_key_has_been_set, Toast.LENGTH_SHORT).show()
            } else field.error = context.getText(R.string.settings_bad_key)
        }
        copyKey.setOnClickListener {
            copyKey()
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.settings_category_hidden)
        showBackButton(true)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.keysettings, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.confirm -> {
                if(newState != initialState) {
                    saveChanges()
                }
                requireActivity().finish()
            }
        }
        return true
    }

    override fun handleBack(): Boolean {
        if(newState == initialState) {
            return super.handleBack()
        }
        showSaveDialog()
        return true
    }

    override fun generateKey() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        field.text.clear()
        generatedKey = Base64.encodeToString(keyGen.generateKey().encoded, Base64.NO_WRAP)
        field.text.insert(0,generatedKey)
        //generate QR
        val matrix = qrCodeWriter.encode(generatedKey,BarcodeFormat.QR_CODE, 512, 512)
        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
        for(i in 0 until matrix.width)
            for(j in 0 until matrix.height) {
                bitmap.setPixel(i,j, if(matrix[i,j]) Color.BLACK else Color.WHITE)
            }
        qrCodeImage.setImageBitmap(bitmap)
        newState = newState.copy(key = generatedKey)
        presenter.setKey(newState.key)
    }

    override fun selectEncodingScheme(schemeId: Int) {
        newState = newState.copy(encodingScheme = schemeId)
    }

    override fun copyKey() {
        field.apply {
            if(copyToClipboard()) {
                selectAll()
                Toast.makeText(context, R.string.encryption_key_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun scanQrCode() {
        val i = IntentIntegrator(this.themedActivity)
            .setBeepEnabled(false)
            .setOrientationLocked(true)
            .setBarcodeImageEnabled(true)
            .createScanIntent()
        startActivityForResult(i, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val qrResult = IntentIntegrator.parseActivityResult(resultCode, data)
        if(qrResult != null && qrResult.contents != null) {
            if(validate(qrResult.contents)) {
                generatedKey = qrResult.contents
                newState = newState.copy(key = qrResult.contents)
                Toast.makeText(context,"${context.getText(R.string.settings_key_has_been_set)}",Toast.LENGTH_LONG).show()
                if(newState.isConversation) {
                    presenter.setConversationParameters(
                        key = generatedKey,
                        encodingScheme = newState.encodingScheme,
                        legacyEncryptionEnabled = newState.legacyEncryptionEnabled,
                        deleteEncryptedAfter = newState.deleteEncryptedAfter,
                        deleteReceivedAfter = newState.deleteReceivedAfter,
                        deleteSentAfter = newState.deleteSentAfter
                    )
                } else {
                    presenter.setGlobalParameters(
                        key = generatedKey,
                        encodingScheme = newState.encodingScheme,
                        legacyEncryptionEnabled = newState.legacyEncryptionEnabled
                    )
                }
            }
            else Toast.makeText(context, context.getText(R.string.settings_bad_key), Toast.LENGTH_SHORT).show()
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    override fun resetKey() {
        newState = newState.copy(key = "")
        Toast.makeText(context, context.getText(R.string.settings_key_reset), Toast.LENGTH_SHORT).show()
    }

    override fun legacyEncryptionEnabled(enabled: Boolean) {
        newState = newState.copy(legacyEncryptionEnabled = enabled)
    }

    private fun EditText.copyToClipboard(): Boolean {
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        return if (text.isNotBlank() && clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.conversation_encryption_key_title), text))
            true
        } else false
    }

    override fun setDeleteEncryptedAfter(delay: Int) {
        newState = newState.copy(deleteEncryptedAfter = delay)
    }

    override fun setDeleteReceivedAfter(delay: Int) {
        newState = newState.copy(deleteReceivedAfter = delay)
    }

    override fun setDeleteSentAfter(delay: Int) {
        newState = newState.copy(deleteSentAfter = delay)
    }

    override fun showDeleteDialog() {
        AlertDialog.Builder(this.activity)
            .setMessage(R.string.settings_delete_ecnryption_key)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.button_delete) { _, _ ->
                presenter.disableKey()
                newState = newState.copy(
                    keySettingsIsShown = false,
                    key = "")
            }
            .create()
            .show()
    }

    private fun showSaveDialog() {
        AlertDialog.Builder(this.activity)
            .setMessage(R.string.settings_exit_with_no_changes)
            .setNegativeButton(R.string.rate_dismiss) { _, _ ->
                newState = initialState
                requireActivity().finish()
            }
            .setPositiveButton(R.string.button_save) { _, _ ->
                saveChanges()
                requireActivity().finish()
            }
            .create()
            .show()
    }

    private fun validate(text: String): Boolean {
        return try {
            if (text.isEmpty()) return true
            val data = Base64.decode(text, Base64.DEFAULT)
            data.size == 16 || data.size == 24 || data.size == 32
        } catch (ignored: IllegalArgumentException) {
            false
        }
    }

    private fun saveChanges() {
        if(newState.isConversation) {
            setDeleteMessagesAfter.execute(SetDeleteMessagesAfter.Params(threadId, SetDeleteMessagesAfter.MessageType.ENCRYPTED, newState.deleteEncryptedAfter))
            setDeleteMessagesAfter.execute(SetDeleteMessagesAfter.Params(threadId, SetDeleteMessagesAfter.MessageType.RECEIVED, newState.deleteReceivedAfter))
            setDeleteMessagesAfter.execute(SetDeleteMessagesAfter.Params(threadId, SetDeleteMessagesAfter.MessageType.SENT, newState.deleteSentAfter))
            setLegacyEncryptionEnabled.execute(SetLegacyEncryptionEnabled.Params(threadId, newState.legacyEncryptionEnabled))
            setEncryptionKey.execute(SetEncryptionKey.Params(threadId, newState.key))
            setEncodingScheme.execute(SetEncodingScheme.Params(threadId, newState.encodingScheme))
        } else {
            prefs.globalEncryptionKey.set(newState.key)
            prefs.encodingScheme.set(newState.encodingScheme)
            prefs.legacyEncryptionEnabled.set(newState.legacyEncryptionEnabled)
        }
        initialState = newState
    }

}