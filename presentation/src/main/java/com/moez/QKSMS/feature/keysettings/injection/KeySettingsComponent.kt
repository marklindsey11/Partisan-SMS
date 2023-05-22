package com.moez.QKSMS.feature.keysettings.injection

import com.moez.QKSMS.feature.keysettings.KeySettingsController
import com.moez.QKSMS.injection.scope.ControllerScope
import dagger.Subcomponent

@ControllerScope
@Subcomponent(modules = [KeySettingsModule::class])
interface KeySettingsComponent {

    fun inject(controller: KeySettingsController)

    @Subcomponent.Builder
    interface Builder {
        fun keySettingsModule(module: KeySettingsModule): Builder
        fun build(): KeySettingsComponent
    }

}