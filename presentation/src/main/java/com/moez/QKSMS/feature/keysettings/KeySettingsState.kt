package com.moez.QKSMS.feature.keysettings

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class KeySettingsState(
    val hasError: Boolean = false,
    val bound: Boolean = false,
    val threadId: Long = -1L,
    val key: String = "",
    val keyEnabled: Boolean = false,
    val keySettingsIsShown: Boolean = false,
    val resetKeyIsShown: Boolean = false,
    val keyValid: Boolean = true,
    val encodingScheme: Int = -1,
    val legacyEncryptionEnabled: Boolean? = null,
    val deleteEncryptedAfter: Int = 0,
    val deleteReceivedAfter: Int = 0,
    val deleteSentAfter: Int = 0,
) : Parcelable

val KeySettingsState.isConversation: Boolean
    get() = threadId != -1L

val KeySettingsState.allowSave: Boolean
    get() = key.isBlank() || keyValid