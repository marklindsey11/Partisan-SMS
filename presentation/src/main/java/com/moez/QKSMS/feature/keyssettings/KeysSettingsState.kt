package com.moez.QKSMS.feature.keyssettings

data class KeysSettingsState(
    val key: String  = "",
    val keyEnabled: Boolean  = false,
    val keySettingsIsShown: Boolean = false,
    val resetCheckIsShown: Boolean = false,
    val encodingScheme: Int = 0,
    val isConversation: Boolean = false
)