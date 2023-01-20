package com.moez.QKSMS.feature.keyssettings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.qrcode.QRCodeWriter
import com.jakewharton.rxbinding2.view.clicks
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkController
import com.moez.QKSMS.common.util.extensions.animateLayoutChanges
import com.moez.QKSMS.common.widget.PreferenceView
import com.moez.QKSMS.injection.appComponent
import com.moez.QKSMS.interactor.SetEncryptionEnabled
import com.moez.QKSMS.interactor.SetEncryptionKey
import com.moez.QKSMS.repository.ConversationRepository
import com.moez.QKSMS.util.Preferences
import io.reactivex.Observable
import kotlinx.android.synthetic.main.settings_keys_activity.*
import kotlinx.android.synthetic.main.settings_keys_activity.preferences
import kotlinx.android.synthetic.main.settings_keys_activity.view.*
import kotlinx.android.synthetic.main.settings_switch_widget.view.*
import kotlinx.android.synthetic.main.text_input_dialog.*
import javax.crypto.KeyGenerator
import javax.inject.Inject

class KeysSettingsController : QkController<KeysSettingsView, KeysSettingsState, KeysSettingsPresenter>(), KeysSettingsView {

    @Inject lateinit var setEncryptionKey: SetEncryptionKey
    @Inject lateinit var setEncryptionEnabled: SetEncryptionEnabled
    @Inject lateinit var context: Context
    @Inject lateinit var prefs: Preferences
    @Inject lateinit var conversationsRepo: ConversationRepository
    @Inject lateinit var qrCodeWriter: QRCodeWriter

    @Inject override lateinit var presenter: KeysSettingsPresenter

    private lateinit var generatedKey: String
    private var threadId = -1L

    init {
        appComponent.inject(this)
        retainViewMode = RetainViewMode.RETAIN_DETACH
        layoutRes = R.layout.settings_keys_activity
    }

    override fun onActivityStarted(activity: Activity) {
        super.onActivityStarted(activity)
        threadId = activity.intent.getLongExtra("threadId", -1)
        Log.w("KSC_", threadId.toString())

        if(threadId == -1L) {
            presenter.setKeyEnabled(prefs.globalEncryptionKey.get().isNotBlank())
            presenter.setKey(prefs.globalEncryptionKey.get())
            presenter.setEncodingScheme(prefs.encodingScheme.get())
        } else {
            presenter.setConversation()
            presenter.setKeyEnabled(conversationsRepo.getConversation(threadId)?.encryptionEnabled ?: false)
            presenter.setKey(conversationsRepo.getConversation(threadId)?.encryptionKey ?: "")
            presenter.setEncodingScheme(conversationsRepo.getConversation(threadId)?.encodingSchemeId ?: prefs.encodingScheme.get())
        }
    }

    override fun preferenceClicks(): Observable<PreferenceView> = (0 until preferences.childCount)
        .map { index -> preferences.getChildAt(index) }
        .mapNotNull { view -> view as? PreferenceView }
        .map { preference -> preference.clicks().map { preference } }
        .let { preferences -> Observable.merge(preferences) }

    override fun buttonClicks(): Observable<ImageButton> = (0 until keyInputGroup.buttons.childCount)
        .map { index -> keyInputGroup.buttons.getChildAt(index) }
        .mapNotNull { view -> view as? ImageButton }
        .map { buttons -> buttons.clicks().map { buttons } }
        .let { buttons -> Observable.merge(buttons) }

    override fun render(state: KeysSettingsState) {
        encryptionKeyCategory.text =
            if(threadId == -1L) context.getText(R.string.settings_global_encryption_key_title)
            else context.getText(R.string.settings_encryption_key_title)
        keyInputGroup.visibility = if(state.keySettingsIsShown) View.VISIBLE else View.GONE
        resetKeyCheck.visibility = if(state.resetCheckIsShown) View.VISIBLE else View.GONE
        scanQr.alpha = if(state.keyEnabled) 1f else 0.5f
        scanQr.isClickable = state.keyEnabled
        generateKey.alpha = if(state.keyEnabled) 1f else 0.5f
        generateKey.isClickable = state.keyEnabled
        enableKey.checkbox.isChecked = if(!state.isConversation) state.key.isNotBlank() else state.keyEnabled
        resetKey.alpha = if(state.key.isNotBlank()) 1f else 0.5f
        resetKey.isClickable = state.keyEnabled
    }

    override fun onViewCreated() {
        super.onViewCreated()
        preferences.postDelayed( { preferences?.animateLayoutChanges = true }, 100)
        val encodingSchemes = context.resources.getStringArray(R.array.encoding_scheme_labels)
        val schemesListAdapter = KeysSettingsListAdapter(encodingSchemes, prefs.encodingScheme.get(), this::selectEncodingScheme)
        encodingSchemesRecycler.layoutManager = LinearLayoutManager(context)
        encodingSchemesRecycler.adapter = schemesListAdapter
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(context.getText(R.string.settings_category_hidden))
        showBackButton(true)
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
    }

    override fun selectEncodingScheme(schemeId: Int) {
        prefs.encodingScheme.set(schemeId)
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
                Toast.makeText(context,"${context.getText(R.string.settings_key_from_qr_set)}: ${qrResult.contents}",Toast.LENGTH_LONG).show()
                if(threadId == -1L) prefs.globalEncryptionKey.set(generatedKey)
                else {
                    setEncryptionKey.execute(SetEncryptionKey.Params(threadId,generatedKey))
                    setEncryptionEnabled.execute(SetEncryptionEnabled.Params(threadId, true))
                }
            }
            else Toast.makeText(context, context.getText(R.string.settings_bad_key), Toast.LENGTH_SHORT).show()
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    override fun setKey() {
        if(validate(field.text.toString())) {
            if(threadId == -1L) {
                prefs.globalEncryptionKey.set(field.text.toString())
            }
            else {
                setEncryptionKey.execute(SetEncryptionKey.Params(threadId,field.text.toString()))
                setEncryptionEnabled.execute(SetEncryptionEnabled.Params(threadId, true))
            }
            presenter.setKey(field.text.toString())
            copyKey()
        } else {
            Toast.makeText(context, context.getText(R.string.settings_bad_key), Toast.LENGTH_SHORT).show()
        }
    }

    override fun resetKey() {
        if(threadId == -1L) {
            prefs.globalEncryptionKey.set("")
        } else {
            setEncryptionKey.execute(SetEncryptionKey.Params(threadId,""))
            setEncryptionEnabled.execute(SetEncryptionEnabled.Params(threadId, false))
        }
        Toast.makeText(context, context.getText(R.string.settings_key_reset), Toast.LENGTH_SHORT).show()
    }

    override fun keyEnabled(enabled: Boolean) {
        setEncryptionEnabled.execute(SetEncryptionEnabled.Params(threadId, enabled))
    }

    private fun EditText.copyToClipboard(): Boolean {
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        return if (text.isNotBlank() && clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.conversation_encryption_key_title), text))
            true
        } else false
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

}