package com.moez.QKSMS.feature.keyssettings

import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkPresenter
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import javax.inject.Inject

class KeysSettingsPresenter @Inject constructor() : QkPresenter<KeysSettingsView, KeysSettingsState>(KeysSettingsState()) {

    fun setConversationParameters(
        key: String,
        encodingScheme: Int,
        legacyEncryptionEnabled: Boolean?,
        deleteEncryptedAfter: Int,
        deleteReceivedAfter: Int,
        deleteSentAfter: Int,
    ) {
       newState { copy(
           key = key,
           encodingScheme = encodingScheme,
           legacyEncryptionEnabled = legacyEncryptionEnabled,
           isConversation = true,
           deleteEncryptedAfter = deleteEncryptedAfter,
           deleteReceivedAfter = deleteReceivedAfter,
           deleteSentAfter = deleteSentAfter
       ) }
    }

    fun setGlobalParameters(
        key: String,
        encodingScheme: Int,
        legacyEncryptionEnabled: Boolean,
        ) {
        newState { copy(
            key = key,
            encodingScheme = encodingScheme,
            legacyEncryptionEnabled = legacyEncryptionEnabled,
            isConversation = false,
        ) }
    }

    fun setKey(key: String) {
        newState { copy(key = key) }
    }

    fun setDeleteEncryptedAfter(delay: Int) {
        newState { copy(deleteEncryptedAfter = delay) }
    }

    fun setDeleteReceivedAfter(delay: Int) {
        newState { copy(deleteReceivedAfter = delay) }
    }

    fun setDeleteSentAfter(delay: Int) {
        newState { copy(deleteSentAfter = delay) }
    }

    fun disableKey() {
        newState { copy(keySettingsIsShown = false, key = "") }
    }

    override fun bindIntents(view: KeysSettingsView) {
        super.bindIntents(view)

        view.preferenceClicks()
            .autoDisposable(view.scope())
            .subscribe {
                when (it.id) {
                    R.id.enableKey -> {
                        newState {
                            if (key.isNotBlank()) {
                                view.showDeleteDialog()
                                copy()
                            } else {
                                view.generateKey()
                                copy(
                                    keySettingsIsShown = true
                                )
                            }

                        }
                    }
                    R.id.scanQr -> {
                        view.scanQrCode()
                    }
                    R.id.generateKey -> {
                        newState {
                            if (!keySettingsIsShown) {
                                view.generateKey()
                            }
                            copy(keySettingsIsShown = !keySettingsIsShown)
                        }
                    }
                    R.id.legacyEncryption -> {
                        newState {
                            val wasEnabled = legacyEncryptionEnabled ?: false
                            view.legacyEncryptionEnabled(!wasEnabled)
                            copy(legacyEncryptionEnabled = !wasEnabled)
                        }
                    }
                    R.id.legacyEncryptionConversation -> {
                        view.showCompatibilityModeDialog()
                    }
                }
            }

        view.compatibilityModeSelected()
            .doOnNext { modeIndex ->
                newState {
                    val mode = when(modeIndex) {
                        0 -> null
                        1 -> false
                        2 -> true
                        else -> null
                    }
                    view.legacyEncryptionEnabled(mode)
                    copy(legacyEncryptionEnabled = mode)
                }
            }
            .autoDisposable(view.scope())
            .subscribe()
    }
}