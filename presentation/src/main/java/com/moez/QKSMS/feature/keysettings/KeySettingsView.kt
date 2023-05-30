package com.moez.QKSMS.feature.keysettings

import com.moez.QKSMS.common.base.QkViewContract
import com.moez.QKSMS.common.widget.PreferenceView
import com.moez.QKSMS.extensions.Optional
import io.reactivex.Observable
import io.reactivex.subjects.Subject

interface KeySettingsView : QkViewContract<KeySettingsState> {
    val keyResetConfirmed: Subject<Unit>
    val keyDisableConfirmed: Subject<Unit>
    val optionsItemIntent: Subject<Int>
    val backClicked: Subject<Unit>
    val exitWithSavingIntent: Subject<Boolean>
    val qrScannedIntent: Subject<String>
    val schemeChanged: Subject<Int>
    val stateRestored: Subject<Optional<KeySettingsState>>

    fun preferenceClicks(): Observable<PreferenceView>
    fun compatibilityModeSelected(): Observable<Int>
    fun copyKey()
    fun scanQrCode()
    fun keySet()
    fun keyNotSet()
    fun keyChanged(): Observable<String>
    fun showResetKeyDialog(disableKey: Boolean)
    fun showSaveDialog(allowSave: Boolean)
    fun showCompatibilityModeDialog()
    fun goBack()
    fun onSaved(key: String?)

}