/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.QKSMS.feature.conversationinfo

import com.moez.QKSMS.common.base.QkViewContract
import com.moez.QKSMS.model.Conversation
import io.reactivex.Observable

interface ConversationInfoView : QkViewContract<ConversationInfoState> {

    fun recipientClicks(): Observable<Long>
    fun recipientLongClicks(): Observable<Long>
    fun themeClicks(): Observable<Long>
    fun nameClicks(): Observable<*>
    fun nameChanges(): Observable<String>
    fun notificationClicks(): Observable<*>
    fun archiveClicks(): Observable<*>
    fun blockClicks(): Observable<*>
    fun deleteClicks(): Observable<*>
    fun confirmDelete(): Observable<*>
    fun mediaClicks(): Observable<Long>
    fun encryptionKeyClicks(): Observable<*>
//    fun deleteEncryptedAfterClicks(): Observable<*>
//    fun deleteReceivedAfterClicks(): Observable<*>
//    fun deleteSentAfterClicks(): Observable<*>
//    fun deleteEncryptedAfterSelected(): Observable<Int>
//    fun deleteReceivedAfterSelected(): Observable<Int>
//    fun deleteSentAfterSelected(): Observable<Int>
//    fun encodingSchemeSelected(): Observable<Int>
//    fun encodingSchemeClicks(): Observable<*>
//    fun showEncodingSchemeDialog(conversation: Conversation)

    fun showNameDialog(name: String)
    fun showThemePicker(recipientId: Long)
    fun showBlockingDialog(conversations: List<Long>, block: Boolean)
    fun requestDefaultSms()
    fun showDeleteDialog()
    fun showEncryptionKeyDialog(conversation: Conversation)
//    fun showDeleteEncryptedAfterDialog(conversation: Conversation)
//    fun showDeleteReceivedAfterDialog(conversation: Conversation)
//    fun showDeleteSentAfterDialog(conversation: Conversation)

}
