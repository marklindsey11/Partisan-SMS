package com.moez.QKSMS.feature.keyssettings

import android.widget.ImageButton
import com.moez.QKSMS.common.base.QkViewContract
import com.moez.QKSMS.common.widget.PreferenceView
import io.reactivex.Observable

interface KeysSettingsView : QkViewContract<KeysSettingsState> {

    fun generateKey()
    fun selectEncodingScheme(schemeId: Int)
    fun preferenceClicks(): Observable<PreferenceView>
    fun buttonClicks(): Observable<ImageButton>
    fun copyKey()
    fun scanQrCode()
    fun setKey()
    fun resetKey()

}