package ru.yandex.market.tpl.courier

import dagger.Component
import dagger.MembersInjector
import ru.yandex.market.tpl.courier.data.JsonMapper
import ru.yandex.market.tpl.courier.presentation.feature.app.MainProcessModule
import javax.inject.Singleton

@Component(
    modules = [
        CoreApplicationModule::class,
        FeatureConfigurationModule::class,
        ReactModule::class,
        MainProcessModule::class,
    ]
)
@Singleton
interface RobolectricTestComponent : MembersInjector<RobolectricTestApplication> {

    val jsonMapper: JsonMapper
}