package com.moez.QKSMS.feature.keyssettings

import com.moez.QKSMS.common.base.QkViewContract
import com.moez.QKSMS.common.widget.PreferenceView
import io.reactivex.Observable

interface KeysSettingsView : QkViewContract<KeysSettingsState> {

    fun generateKey()
    fun selectEncodingScheme(schemeId: Int)
    fun preferenceClicks(): Observable<PreferenceView>
    fun copyKey()
    fun scanQrCode()
    fun resetKey()
    fun legacyEncryptionEnabled(enabled: Boolean)

    fun setDeleteEncryptedAfter(delay: Int)
    fun setDeleteReceivedAfter(delay: Int)
    fun setDeleteSentAfter(delay: Int)
    fun showDeleteDialog()

}