package com.moez.QKSMS.feature.keysettings

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.qrcode.QRCodeWriter
import com.jakewharton.rxbinding2.view.clicks
import com.moez.QKSMS.R
import com.moez.QKSMS.common.QkDialog
import com.moez.QKSMS.common.base.QkController
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.common.util.extensions.animateLayoutChanges
import com.moez.QKSMS.common.util.extensions.setBackgroundTint
import com.moez.QKSMS.common.util.extensions.setTint
import com.moez.QKSMS.common.widget.PreferenceView
import com.moez.QKSMS.injection.appComponent
import com.moez.QKSMS.util.Preferences
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.settings_keys_activity.copyKey
import kotlinx.android.synthetic.main.settings_keys_activity.enableKey
import kotlinx.android.synthetic.main.settings_keys_activity.encodingSchemesRecycler
import kotlinx.android.synthetic.main.settings_keys_activity.encryptionKeyCategory
import kotlinx.android.synthetic.main.settings_keys_activity.generateKey
import kotlinx.android.synthetic.main.settings_keys_activity.keyField
import kotlinx.android.synthetic.main.settings_keys_activity.keyInputGroup
import kotlinx.android.synthetic.main.settings_keys_activity.legacyEncryption
import kotlinx.android.synthetic.main.settings_keys_activity.legacyEncryptionConversation
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
import javax.inject.Inject


class KeySettingsController : QkController<KeySettingsView, KeySettingsState, KeySettingsPresenter>(), KeySettingsView {

    companion object {
        const val EncryptionKeyKey = "encryption_key"
        private const val ScanQrRequestCode = 201
    }

    @Inject lateinit var prefs: Preferences
    @Inject lateinit var colors: Colors
    @Inject lateinit var context: Context
    @Inject lateinit var qrCodeWriter: QRCodeWriter
    @Inject lateinit var compatibilityModeDialog: QkDialog
    @Inject override lateinit var presenter: KeySettingsPresenter

    override val keyDeletionConfirmed: Subject<Unit> = PublishSubject.create()
    override val optionsItemIntent: Subject<Int> = PublishSubject.create()
    override val backClicked: Subject<Unit> = PublishSubject.create()
    override val exitWithSavingIntent: Subject<Boolean> = PublishSubject.create()
    override val qrScannedIntent: Subject<String> = PublishSubject.create()

    private val keyTextWatcher = KeyTextWatcher()
    private val deleteEncryptedAfterListener = SeekBarListener()
    private val deleteReceivedAfterListener = SeekBarListener()
    private val deleteSentAfterListener = SeekBarListener()
    private lateinit var schemesListAdapter: EncryptionSchemeListAdapter
    private var scannedQr: String? = null

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

    override fun compatibilityModeSelected(): Observable<Int> = compatibilityModeDialog.adapter.menuItemClicks

    override fun render(state: KeySettingsState) {
        encryptionKeyCategory.text =
            if (!state.isConversation) context.getText(R.string.settings_global_encryption_key_title)
            else context.getText(R.string.settings_conversation_encryption_key_title)

        enableKey.checkbox.isChecked = state.keySettingsIsShown

        keyInputGroup.visibility = if(state.keySettingsIsShown) View.VISIBLE else View.GONE
        scanQr.alpha = if (state.keySettingsIsShown) 1f else 0.5f
        scanQr.isClickable = state.keySettingsIsShown
        generateKey.alpha = if (state.keySettingsIsShown) 1f else 0.5f
        generateKey.isClickable = state.keySettingsIsShown
        keyField.setBackgroundTint(colors.theme().theme)
        if (state.keySettingsIsShown) {
            if (state.keyValid) {
                if (keyField.text.toString() != state.key) {
                    keyField.setText(state.key)
                }
                keyField.error = null
                renderQr(state.key)
            } else {
                keyField.error = context.getText(R.string.settings_bad_key)
            }
        }

        val nonKeyEncryptionSettingsEnabled = state.keySettingsIsShown
                || state.isConversation && prefs.globalEncryptionKey.get().isNotBlank()
        if (state.isConversation) {
            val strings = context.resources.getStringArray(R.array.compatibility_mode_settings_conversation)
            val selectedItem = when(state.legacyEncryptionEnabled) {
                null -> 0
                false -> 1
                true -> 2
            }
            legacyEncryption.visibility = View.GONE
            legacyEncryptionConversation.visibility = View.VISIBLE
            legacyEncryptionConversation.summary = strings[selectedItem]
            legacyEncryptionConversation.alpha = if (nonKeyEncryptionSettingsEnabled) 1f else 0.5f
            legacyEncryptionConversation.isClickable = nonKeyEncryptionSettingsEnabled
            compatibilityModeDialog.adapter.selectedItem = selectedItem
        } else {
            legacyEncryption.visibility = View.VISIBLE
            legacyEncryption.checkbox.isChecked = state.legacyEncryptionEnabled ?: false
            legacyEncryption.alpha = if (nonKeyEncryptionSettingsEnabled) 1f else 0.5f
            legacyEncryption.isClickable = nonKeyEncryptionSettingsEnabled
            legacyEncryptionConversation.visibility = View.GONE
        }

        encodingSchemesRecycler.alpha = if (nonKeyEncryptionSettingsEnabled) 1f else 0.5f

        schemesListAdapter.setSelected(state.encodingScheme)
        schemesListAdapter.setEnabled(nonKeyEncryptionSettingsEnabled)

        val deleteAfterLabels = context.resources.getStringArray(R.array.delete_message_after_labels)
        settings_deletion.visibility = if (state.isConversation) View.VISIBLE else View.GONE
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

    private fun renderQr(key: String) {
        val matrix = qrCodeWriter.encode(key, BarcodeFormat.QR_CODE, 512, 512)
        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
        for (i in 0 until matrix.width)
            for (j in 0 until matrix.height) {
                bitmap.setPixel(i,j, if(matrix[i,j]) Color.BLACK else Color.WHITE)
            }
        qrCodeImage.setImageBitmap(bitmap)
    }

    override fun onViewCreated() {
        super.onViewCreated()

        val threadId = requireActivity().intent.getLongExtra("threadId", -1)
        schemesListAdapter = EncryptionSchemeListAdapter(context, threadId != -1L, colors, prefs)
        if (threadId == -1L) {
            presenter.initGlobalState()
        } else {
            presenter.initConversationState(threadId)
        }

        preferences.postDelayed( { preferences?.animateLayoutChanges = true }, 100)
        encodingSchemesRecycler.layoutManager = LinearLayoutManager(context)
        encodingSchemesRecycler.adapter = schemesListAdapter

        val deleteAfterLabels = context.resources.getStringArray(R.array.delete_message_after_labels)
        settings_delete_encrypted_after.max = deleteAfterLabels.lastIndex
        settings_delete_received_after.max = deleteAfterLabels.lastIndex
        settings_delete_sent_after.max = deleteAfterLabels.lastIndex

        settings_delete_encrypted_after.setOnSeekBarChangeListener(deleteEncryptedAfterListener)
        settings_delete_received_after.setOnSeekBarChangeListener(deleteReceivedAfterListener)
        settings_delete_sent_after.setOnSeekBarChangeListener(deleteSentAfterListener)
        keyField.addTextChangedListener(keyTextWatcher)
        copyKey.setOnClickListener { copyKey() }
        compatibilityModeDialog.adapter.setData(R.array.compatibility_mode_settings_conversation)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        checkScannedQr()
        setTitle(R.string.settings_encryption_key_title)
        showBackButton(true)
        setHasOptionsMenu(true)
    }

    private fun checkScannedQr() {
        val qr = scannedQr
        if (qr != null) {
            qrScannedIntent.onNext(qr)
            scannedQr = null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        optionsItemIntent.onNext(item.itemId)
        return true
    }

    override fun handleBack(): Boolean {
        backClicked.onNext(Unit)
        return true
    }

    override fun copyKey() {
        keyField.apply {
            if(copyToClipboard()) {
                selectAll()
                Toast.makeText(context, R.string.encryption_key_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun scanQrCode() {
        val intent = IntentIntegrator(this.themedActivity)
            .setBeepEnabled(false)
            .setOrientationLocked(true)
            .setBarcodeImageEnabled(true)
            .createScanIntent()
        startActivityForResult(intent, ScanQrRequestCode)
    }

    override fun keySet() {
        val text = context.getText(R.string.settings_key_has_been_set)
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    override fun keyNotSet() {
        val text = context.getText(R.string.settings_bad_key)
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    override fun deleteEncryptedAfterChanged(): Observable<Int> = deleteEncryptedAfterListener.posChanged
    override fun deleteReceivedAfterChanged(): Observable<Int> = deleteReceivedAfterListener.posChanged
    override fun deleteSentAfterChanged(): Observable<Int> = deleteSentAfterListener.posChanged
    override fun schemeChanged(): Observable<Int> = schemesListAdapter.schemeChanged
    override fun keyChanged(): Observable<String> = keyTextWatcher.keyChanged
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ScanQrRequestCode) {
            val qrResult = IntentIntegrator.parseActivityResult(resultCode, data)
            if (qrResult != null && qrResult.contents != null) {
                scannedQr = qrResult.contents
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun EditText.copyToClipboard(): Boolean {
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        return if (text.isNotBlank() && clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.conversation_encryption_key_title), text))
            true
        } else false
    }

    override fun showCompatibilityModeDialog() = compatibilityModeDialog.show(activity!!)

    override fun showDeleteKeyDialog() {
        AlertDialog.Builder(this.activity)
            .setMessage(R.string.settings_delete_ecnryption_key)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.button_delete) { _, _ -> keyDeletionConfirmed.onNext(Unit) }
            .create()
            .show()
    }

    override fun showSaveDialog(allowSave: Boolean) {
        val builder = AlertDialog.Builder(this.activity)
            .setMessage(R.string.settings_exit_with_no_changes)
            .setNeutralButton(R.string.button_cancel, null)
            .setNegativeButton(R.string.rate_dismiss) { _, _ -> exitWithSavingIntent.onNext(false) }
        if (allowSave) {
            builder.setPositiveButton(R.string.button_save) { _, _ -> exitWithSavingIntent.onNext(true) }
        }
        builder.create().show()
    }

    override fun goBack() {
        activity?.finish()
    }

    override fun onSaved(key: String?) {
        val intent = Intent().putExtra(EncryptionKeyKey, key)
        activity?.setResult(Activity.RESULT_OK, intent)
    }
}