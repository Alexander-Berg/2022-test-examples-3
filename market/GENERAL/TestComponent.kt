package ru.yandex.market.tpl.courier

import android.content.Context
import com.facebook.react.ReactApplication
import dagger.BindsInstance
import dagger.Component
import ru.yandex.market.tpl.courier.arch.rule.InstrumentedTestRuleManager
import ru.yandex.market.tpl.courier.data.feature.TestConfiguration
import ru.yandex.market.tpl.courier.data.feature.TestConfigurationModule
import ru.yandex.market.tpl.courier.data.feature.TestDataRepository
import ru.yandex.market.tpl.courier.data.remote.InternalRemoteApiModule
import ru.yandex.market.tpl.courier.data.remote.tus.TusRemoteApiModule
import ru.yandex.market.tpl.courier.presentation.feature.app.FakeMainProcessModule
import javax.inject.Singleton

@Component(
    modules = [
        CoreApplicationModule::class,
        FeatureConfigurationModule::class,
        FakeReactModule::class,
        FakeMainProcessModule::class,
        InternalRemoteApiModule::class,
        TestConfigurationModule::class,
        TusRemoteApiModule::class,
    ]
)
@Singleton
interface TestComponent : ApplicationComponent {

    val testDataRepository: TestDataRepository

    val testConfiguration: TestConfiguration

    val instrumentedTestRuleManager: InstrumentedTestRuleManager

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance reactApplication: ReactApplication,
            @BindsInstance context: Context,
        ): TestComponent
    }
}


