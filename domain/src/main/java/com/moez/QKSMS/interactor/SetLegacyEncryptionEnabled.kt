package com.moez.QKSMS.interactor

import com.moez.QKSMS.repository.ConversationRepository
import io.reactivex.Flowable
import javax.inject.Inject

class SetLegacyEncryptionEnabled @Inject constructor(
    private val conversationRepo: ConversationRepository
) : Interactor<SetLegacyEncryptionEnabled.Params>() {

    data class Params(val threadId: Long, val legacyEncryptionEnabled: Boolean?)

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params)
            .doOnNext { (threadId, legacyEncryptionEnabled) ->
                conversationRepo.setLegacyEncryptionEnabled(threadId, legacyEncryptionEnabled)
            }
    }

}