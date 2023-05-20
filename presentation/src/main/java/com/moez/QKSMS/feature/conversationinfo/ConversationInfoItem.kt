package com.moez.QKSMS.feature.conversationinfo

import com.moez.QKSMS.model.MmsPart
import com.moez.QKSMS.model.Recipient
import io.realm.RealmList

sealed class ConversationInfoItem {

    data class ConversationInfoRecipient(val value: Recipient) : ConversationInfoItem()

    data class ConversationInfoSettings(
        val name: String,
        val recipients: RealmList<Recipient>,
        val archived: Boolean,
        val blocked: Boolean,
        // hidden
        val encryptionKeyExist: Boolean,
        val deleteEncryptedAfter: Int,
        var deleteReceivedAfter: Int,
        var deleteSentAfter: Int
    ) : ConversationInfoItem()

    data class ConversationInfoMedia(val value: MmsPart) : ConversationInfoItem()

}
