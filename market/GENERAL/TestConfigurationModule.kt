package ru.yandex.market.tpl.courier.data.feature

import dagger.Module
import dagger.Provides

@Module
class TestConfigurationModule {
    @Provides
    fun provideTestConfiguration(testConfigurationProvider: TestConfigurationProvider): TestConfiguration =
        testConfigurationProvider.get()
}