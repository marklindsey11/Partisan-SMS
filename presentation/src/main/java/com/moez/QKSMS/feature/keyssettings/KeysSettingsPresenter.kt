package com.moez.QKSMS.feature.keyssettings

import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkPresenter
import com.moez.QKSMS.util.Preferences
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

class KeysSettingsPresenter @Inject constructor(
    prefs: Preferences
) : QkPresenter<KeysSettingsView, KeysSettingsState>(
    KeysSettingsState(keyEnabled = prefs.globalEncryptionKey.get().isNotBlank())
) {

    init {
        disposables += prefs.globalEncryptionKey.asObservable()
            .subscribe { newState { copy(key = it) } }
        disposables += prefs.encodingScheme.asObservable()
            .subscribe { newState { copy(encodingScheme = it) } }
    }

    fun setKeyEnabled(state: Boolean) {
        newState { copy(keyEnabled = state) }
    }

    override fun bindIntents(view: KeysSettingsView) {
        super.bindIntents(view)

        view.preferenceClicks()
            .autoDisposable(view.scope())
            .subscribe {
                when(it.id) {
                    R.id.enableKey -> {
                        newState {
                            if(!keyEnabled) copy(keyEnabled = true)
                            else if(key.isBlank()) copy(
                                keyEnabled = false,
                                keySettingsIsShown = false
                            )
                            else copy(resetCheckIsShown = true)
                        }
                    }
                    R.id.scanQr -> view.scanQrCode()
                    R.id.generateKey -> {
                        newState {
                            if(!keySettingsIsShown) view.generateKey()
                            copy(keySettingsIsShown = !keySettingsIsShown)
                        }
                    }
                    R.id.resetKey -> {
                        newState { copy(resetCheckIsShown = !resetCheckIsShown) }
                    }
                    R.id.resetKeyCheck -> {
                        view.resetKey()
                        newState { copy(
                            keySettingsIsShown = false,
                            resetCheckIsShown = false,
                            keyEnabled = false
                        ) }
                    }
                }
            }

        view.buttonClicks()
            .autoDisposable(view.scope())
            .subscribe {
                when (it.id) {
                    R.id.setGlobalKey -> {
                        view.setKey()
                        newState { copy(keySettingsIsShown = false) }
                    }
                }
            }
    }

}