package com.moez.QKSMS.feature.keysettings

import com.moez.QKSMS.common.base.QkViewContract
import com.moez.QKSMS.common.widget.PreferenceView
import io.reactivex.Observable
import io.reactivex.subjects.Subject

interface KeySettingsView : QkViewContract<KeySettingsState> {
    val keyDeletionConfirmed: Subject<Unit>
    val optionsItemIntent: Subject<Int>
    val backClicked: Subject<Unit>
    val exitWithSavingIntent: Subject<Boolean>
    val qrScanned: Subject<String>

    fun preferenceClicks(): Observable<PreferenceView>
    fun compatibilityModeSelected(): Observable<Int>
    fun copyKey()
    fun scanQrCode()
    fun keySet()
    fun keyNotSet()
    fun deleteEncryptedAfterChanged(): Observable<Int>
    fun deleteReceivedAfterChanged(): Observable<Int>
    fun deleteSentAfterChanged(): Observable<Int>
    fun schemeChanged(): Observable<Int>
    fun keyChanged(): Observable<String>
    fun showDeleteKeyDialog()
    fun showSaveDialog(valid: Boolean)
    fun showCompatibilityModeDialog()
    fun goBack()
    fun onSaved(key: String?)

}