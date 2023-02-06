package ru.yandex.market.markup3.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import ru.yandex.market.markup3.core.services.TestEvent
import ru.yandex.market.markup3.core.services.TestEventObject
import ru.yandex.market.markup3.core.services.TypeSerializerService
import ru.yandex.market.markup3.testutils.handlers.TestHandler

@Import(
    AppConfig::class,
    RemoteServicesMockConfig::class,
    TestDatabaseConfig::class,
    TestHandler::class,
    TestMarkupSolomonConfig::class,
    TolokaClientServicesMockConfig::class,
    TestTrackerConfig::class,
)
@Configuration
@PropertySource("classpath:test.properties")
open class TestConfig {
    @Bean(autowireCandidate = false)
    open fun registerTestTypes(typeSerializer: TypeSerializerService): String {
        typeSerializer.register(TestEvent::class.java)
        typeSerializer.register(TestEventObject)
        return "Done"
    }
}
