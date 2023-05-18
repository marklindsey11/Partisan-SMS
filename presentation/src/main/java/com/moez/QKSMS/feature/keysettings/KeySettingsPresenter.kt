package com.moez.QKSMS.feature.keysettings

import android.util.Base64
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkPresenter
import com.moez.QKSMS.extensions.asObservable
import com.moez.QKSMS.interactor.SetDeleteMessagesAfter
import com.moez.QKSMS.interactor.SetEncodingScheme
import com.moez.QKSMS.interactor.SetEncryptionKey
import com.moez.QKSMS.interactor.SetLegacyEncryptionEnabled
import com.moez.QKSMS.model.Conversation
import com.moez.QKSMS.repository.ConversationRepository
import com.moez.QKSMS.util.Preferences
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.crypto.KeyGenerator
import javax.inject.Inject

class KeySettingsPresenter @Inject constructor() : QkPresenter<KeySettingsView, KeySettingsState>(KeySettingsState()) {
    @Inject lateinit var setDeleteMessagesAfter: SetDeleteMessagesAfter
    @Inject lateinit var setLegacyEncryptionEnabled: SetLegacyEncryptionEnabled
    @Inject lateinit var setEncryptionKey: SetEncryptionKey
    @Inject lateinit var setEncodingScheme: SetEncodingScheme
    @Inject lateinit var prefs: Preferences
    @Inject lateinit var conversationRepo: ConversationRepository

    var initialState: KeySettingsState? = null
    private val conversation: Subject<Conversation> = BehaviorSubject.create()

    fun initConversationState(threadId: Long) {
        disposables += conversationRepo.getConversationAsync(threadId)
            .asObservable()
            .filter { conversation -> conversation.isLoaded }
            .filter { conversation -> conversation.isValid }
            .filter { conversation -> conversation.id != 0L }
            .subscribe { conv ->
                conversation.onNext(conv)

                initialState = KeySettingsState (
                    key = conv.encryptionKey,
                    keySettingsIsShown = conv.encryptionKey.isNotEmpty(),
                    keyValid = validateKey(conv.encryptionKey),
                    encodingScheme = conv.encodingSchemeId
                        .takeIf { it != Conversation.SCHEME_NOT_DEF }
                        ?: GLOBAL_SCHEME_INDEX,
                    legacyEncryptionEnabled = conv.legacyEncryptionEnabled,
                    deleteEncryptedAfter = conv.deleteEncryptedAfter,
                    deleteReceivedAfter = conv.deleteReceivedAfter,
                    deleteSentAfter = conv.deleteSentAfter,
                    threadId = threadId,
                )

                newState { initialState!! }
            }
    }

    fun initGlobalState() {
        initialState = KeySettingsState (
            key = prefs.globalEncryptionKey.get(),
            keySettingsIsShown = prefs.globalEncryptionKey.get().isNotEmpty(),
            keyValid = validateKey(prefs.globalEncryptionKey.get()),
            encodingScheme = prefs.encodingScheme.get(),
            legacyEncryptionEnabled = prefs.legacyEncryptionEnabled.get(),
        )
        newState { initialState!! }
    }

    override fun bindIntents(view: KeySettingsView) {
        super.bindIntents(view)

        view.preferenceClicks()
            .autoDisposable(view.scope())
            .subscribe {
                when (it.id) {
                    R.id.enableKey -> {
                        newState {
                            if (key.isNotBlank()) {
                                view.showDeleteKeyDialog()
                                copy()
                            } else {
                                copy(key = generateKey(), keySettingsIsShown = true, keyValid = true)
                            }

                        }
                    }
                    R.id.scanQr -> {
                        view.scanQrCode()
                    }
                    R.id.generateKey -> {
                        newState {
                            copy(key = generateKey(), keyValid = true)
                        }
                    }
                    R.id.legacyEncryption -> {
                        newState {
                            val wasEnabled = legacyEncryptionEnabled ?: false
                            copy(legacyEncryptionEnabled = !wasEnabled)
                        }
                    }
                    R.id.legacyEncryptionConversation -> {
                        view.showCompatibilityModeDialog()
                    }
                }
            }

        view.keyDeletionConfirmed
            .autoDisposable(view.scope())
            .subscribe { newState { copy(keySettingsIsShown = false, key = "", keyValid = true) }}

        view.compatibilityModeSelected()
            .autoDisposable(view.scope())
            .subscribe { modeIndex ->
                newState {
                    val mode = when(modeIndex) {
                        0 -> null
                        1 -> false
                        2 -> true
                        else -> null
                    }
                    copy(legacyEncryptionEnabled = mode)
                }
            }

        view.optionsItemIntent
            .withLatestFrom(state) { itemId, lastState ->
                when(itemId) {
                    R.id.confirm -> {
                        if (lastState.keyValid) {
                            if (lastState != initialState) {
                                saveChanges(lastState)
                            }
                            view.goBack()
                        }
                    }
                }

            }
            .autoDisposable(view.scope())
            .subscribe()

        view.backClicked
            .withLatestFrom(state) { _, latestState ->
                if (latestState != initialState) {
                    view.showSaveDialog(latestState.keyValid)
                } else {
                    view.goBack()
                }
            }
            .autoDisposable(view.scope())
            .subscribe()

        view.exitWithSavingIntent
            .withLatestFrom(state) { withSaving, lastState ->
                if (withSaving) {
                    saveChanges(lastState)
                }
                view.goBack()
            }
            .autoDisposable(view.scope())
            .subscribe()

        view.qrScanned
            .autoDisposable(view.scope())
            .subscribe {key ->
                if (validateKey(key)) {
                    newState { copy(key = key, keyValid = true) }
                    view.keySet()
                } else {
                    view.keyNotSet()
                }
            }

        view.deleteEncryptedAfterChanged()
            .autoDisposable(view.scope())
            .subscribe { delay ->
                newState { copy(deleteEncryptedAfter = delay) }
            }

        view.deleteReceivedAfterChanged()
            .autoDisposable(view.scope())
            .subscribe { delay ->
                newState { copy(deleteReceivedAfter = delay) }
            }

        view.deleteSentAfterChanged()
            .autoDisposable(view.scope())
            .subscribe { delay ->
                newState { copy(deleteSentAfter = delay) }
            }

        view.keyChanged()
            .withLatestFrom(state) { key, lastState ->
                if (key != lastState.key) {
                    if (validateKey(key)) {
                        newState { copy(key = key, keyValid = true) }
                    } else {
                        newState { copy(key = key, keyValid = false) }
                    }
                }
            }
            .autoDisposable(view.scope())
            .subscribe()

        view.schemeChanged()
            .autoDisposable(view.scope())
            .subscribe { scheme ->
                newState { copy(encodingScheme = scheme) }
            }
    }

    private fun generateKey(): String {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return Base64.encodeToString(keyGen.generateKey().encoded, Base64.NO_WRAP)
    }

    private fun saveChanges(lastState: KeySettingsState) {
        if (!lastState.keyValid) {
            return
        }
        if (lastState.isConversation) {
            val threadId = lastState.threadId
            setDeleteMessagesAfter.execute(SetDeleteMessagesAfter.Params(threadId, SetDeleteMessagesAfter.MessageType.ENCRYPTED, lastState.deleteEncryptedAfter))
            setDeleteMessagesAfter.execute(SetDeleteMessagesAfter.Params(threadId, SetDeleteMessagesAfter.MessageType.RECEIVED, lastState.deleteReceivedAfter))
            setDeleteMessagesAfter.execute(SetDeleteMessagesAfter.Params(threadId, SetDeleteMessagesAfter.MessageType.SENT, lastState.deleteSentAfter))
            setLegacyEncryptionEnabled.execute(SetLegacyEncryptionEnabled.Params(threadId, lastState.legacyEncryptionEnabled))
            setEncryptionKey.execute(SetEncryptionKey.Params(threadId, lastState.key))
            val schemeId = lastState.encodingScheme
                .takeIf { it != GLOBAL_SCHEME_INDEX }
                ?: Conversation.SCHEME_NOT_DEF
            setEncodingScheme.execute(SetEncodingScheme.Params(threadId, schemeId))
        } else {
            prefs.globalEncryptionKey.set(lastState.key)
            prefs.encodingScheme.set(lastState.encodingScheme)
            prefs.legacyEncryptionEnabled.set(lastState.legacyEncryptionEnabled ?: false)
        }
        initialState = lastState
    }

    private fun validateKey(text: String): Boolean {
        try {
            if (text.isEmpty()) {
                return false
            }
            val data = Base64.decode(text, Base64.DEFAULT)
            return data.size == 16 || data.size == 24 || data.size == 32
        } catch (ignored: IllegalArgumentException) {
            return false
        }
    }

    companion object {
        /*index of item "Use Scheme from Settings" at R.array.encoding_scheme_labels_conversation*/
        private const val GLOBAL_SCHEME_INDEX = 3
    }
}