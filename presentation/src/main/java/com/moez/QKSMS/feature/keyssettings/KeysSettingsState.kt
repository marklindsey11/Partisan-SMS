package com.moez.QKSMS.feature.keyssettings

data class KeysSettingsState(
    val key: String  = "",
    val keyEnabled: Boolean  = false,
    val keySettingsIsShown: Boolean = false,
    val encodingScheme: Int = 0,
    val isConversation: Boolean = false,
    val deleteEncryptedAfter: Int = 0,
    val deleteReceivedAfter: Int = 0,
    val deleteSentAfter: Int = 0,
)