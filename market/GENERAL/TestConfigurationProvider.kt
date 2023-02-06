package ru.yandex.market.tpl.courier.data.feature

import dagger.Reusable
import ru.yandex.market.tpl.courier.arch.validation.validateAll
import ru.yandex.market.tpl.courier.domain.account.TusToken
import javax.inject.Inject
import javax.inject.Provider

@Reusable
class TestConfigurationProvider @Inject constructor() : Provider<TestConfiguration> {
    private val configuration: TestConfiguration

    init {
        val tusToken = System.getProperty(TUS_TOKEN_PROPERTY_NAME)
        validateAll {
            notEmpty(tusToken, TUS_TOKEN_PROPERTY_NAME)
        }

        configuration = TestConfiguration(
            tusToken = TusToken(tusToken!!),
        )
    }

    override fun get(): TestConfiguration = configuration

    companion object {
        private const val TUS_TOKEN_PROPERTY_NAME = "tus_token"
    }
}