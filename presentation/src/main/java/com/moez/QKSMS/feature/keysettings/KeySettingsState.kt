package com.moez.QKSMS.feature.keysettings

data class KeySettingsState(
    val threadId: Long = -1L,
    val key: String = "",
    val keySettingsIsShown: Boolean = false,
    val keyValid: Boolean = true,
    val encodingScheme: Int = 0,
    val legacyEncryptionEnabled: Boolean? = null,
    val deleteEncryptedAfter: Int = 0,
    val deleteReceivedAfter: Int = 0,
    val deleteSentAfter: Int = 0,
)

val KeySettingsState.isConversation: Boolean
    get() = threadId != -1L
