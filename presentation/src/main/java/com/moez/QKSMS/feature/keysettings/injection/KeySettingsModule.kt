package com.moez.QKSMS.feature.keysettings.injection

import com.moez.QKSMS.feature.keysettings.KeySettingsController
import com.moez.QKSMS.injection.scope.ControllerScope
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class KeySettingsModule(private val controller: KeySettingsController)  {

    @Provides
    @ControllerScope
    @Named("keySettingsConversationThreadId")
    fun provideThreadId(): Long = controller.threadId

}