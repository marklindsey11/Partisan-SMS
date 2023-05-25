package com.moez.QKSMS.feature.keysettings

import android.util.Base64
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkPresenter
import com.moez.QKSMS.extensions.Optional
import com.moez.QKSMS.extensions.asObservable
import com.moez.QKSMS.interactor.SetDeleteMessagesAfter
import com.moez.QKSMS.interactor.SetEncodingScheme
import com.moez.QKSMS.interactor.SetEncryptionEnabled
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
import javax.inject.Named

class KeySettingsPresenter @Inject constructor(
    @Named("keySettingsConversationThreadId") threadId: Long,
    private val setDeleteMessagesAfter: SetDeleteMessagesAfter,
    private val setLegacyEncryptionEnabled: SetLegacyEncryptionEnabled,
    private val setEncryptionKey: SetEncryptionKey,
    private val setEncodingScheme: SetEncodingScheme,
    private val setEncryptionEnabled: SetEncryptionEnabled,
    private val prefs: Preferences,
    conversationRepo: ConversationRepository
) : QkPresenter<KeySettingsView, KeySettingsState>(
    KeySettingsState(threadId = threadId)
) {

    var initialState: KeySettingsState? = null
    private var conversation: Subject<Optional<Conversation>> = BehaviorSubject.create()

    init {
        if (threadId == -1L) {
            conversation.onNext(Optional(null))
            initialState = KeySettingsState (
                key = prefs.globalEncryptionKey.get(),
                keyEnabled = prefs.globalEncryptionKey.get().isNotEmpty(),
                keySettingsIsShown = false,
                resetKeyIsShown = prefs.globalEncryptionKey.get().isNotEmpty(),
                keyValid = validateKey(prefs.globalEncryptionKey.get()),
                encodingScheme = prefs.encodingScheme.get(),
                legacyEncryptionEnabled = prefs.legacyEncryptionEnabled.get(),
                initialized = true,
            )
            newState { initialState!! }
        } else {
            disposables += conversationRepo.getConversationAsync(threadId)
                .asObservable()
                .filter { conversation -> conversation.isLoaded }
                .filter { conversation -> conversation.isValid }
                .filter { conversation -> conversation.id != 0L }
                .subscribe { conv ->
                    conversation.onNext(Optional(conv))

                    initialState = KeySettingsState (
                        key = conv.encryptionKey,
                        keyEnabled = conv.encryptionKey.isNotEmpty(),
                        keySettingsIsShown = false,
                        resetKeyIsShown = conv.encryptionKey.isNotEmpty(),
                        keyValid = validateKey(conv.encryptionKey),
                        encodingScheme = conv.encodingSchemeId
                            .takeIf { it != Conversation.SCHEME_NOT_DEF }
                            ?: GLOBAL_SCHEME_INDEX,
                        legacyEncryptionEnabled = conv.legacyEncryptionEnabled,
                        deleteEncryptedAfter = conv.deleteEncryptedAfter,
                        deleteReceivedAfter = conv.deleteReceivedAfter,
                        deleteSentAfter = conv.deleteSentAfter,
                        threadId = threadId,
                        initialized = true,
                    )

                    newState { initialState!! }
                }
        }
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
                                if (initialState?.key?.isNotBlank() == true && initialState?.key == key) {
                                    view.showResetKeyDialog(true)
                                    copy()
                                } else {
                                    copy(
                                        key = "",
                                        keyEnabled = false,
                                        keySettingsIsShown = false,
                                        keyValid = false
                                    )
                                }
                            } else {
                                copy(
                                    key = generateKey(),
                                    keyEnabled = true,
                                    keySettingsIsShown = true,
                                    keyValid = true
                                )
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
                    R.id.resetKey -> {
                        view.showResetKeyDialog(false)
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

        view.keyResetConfirmed
            .autoDisposable(view.scope())
            .subscribe {
                newState {
                    copy(
                        key = "",
                        keyEnabled = true,
                        keySettingsIsShown = true,
                        keyValid = false,
                        resetKeyIsShown = false
                    )
                }
            }

        view.keyDisableConfirmed
            .autoDisposable(view.scope())
            .subscribe {
                newState {
                    copy(
                        key = "",
                        keyEnabled = false,
                        keySettingsIsShown = false,
                        keyValid = false,
                        resetKeyIsShown = false
                    )
                }
            }

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
            .withLatestFrom(state, conversation) { itemId, lastState, conv ->
                when(itemId) {
                    R.id.confirm -> {
                        if (lastState.allowSave) {
                            if (lastState != initialState) {
                                saveChanges(lastState, conv.value, view)
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
                    view.showSaveDialog(latestState.allowSave)
                } else {
                    view.goBack()
                }
            }
            .autoDisposable(view.scope())
            .subscribe()

        view.exitWithSavingIntent
            .withLatestFrom(state, conversation) { withSaving, lastState, conv ->
                if (withSaving) {
                    saveChanges(lastState, conv.value, view)
                }
                view.goBack()
            }
            .autoDisposable(view.scope())
            .subscribe()

        view.qrScannedIntent
            .autoDisposable(view.scope())
            .subscribe {key ->
                if (validateKey(key)) {
                    newState {
                        copy(
                            key = key,
                            keyEnabled = true,
                            keySettingsIsShown = true,
                            keyValid = true
                        )
                    }
                    view.keySet()
                } else {
                    view.keyNotSet()
                }
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

        view.schemeChanged
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

    private fun saveChanges(lastState: KeySettingsState, conversation: Conversation?, view: KeySettingsView) {
        if (!lastState.allowSave) {
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
            if (conversation?.encryptionEnabled == true && lastState.key.isBlank() && prefs.globalEncryptionKey.get().isBlank()) {
                setEncryptionEnabled.execute(SetEncryptionEnabled.Params(threadId, null))
            } else if (conversation?.encryptionEnabled == null && lastState.key.isNotBlank()) {
                setEncryptionEnabled.execute(SetEncryptionEnabled.Params(threadId, true))
            }
        } else {
            prefs.globalEncryptionKey.set(lastState.key)
            prefs.encodingScheme.set(lastState.encodingScheme)
            prefs.legacyEncryptionEnabled.set(lastState.legacyEncryptionEnabled ?: false)
        }
        initialState = lastState
        view.onSaved(if (lastState.keyValid) lastState.key else null)
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