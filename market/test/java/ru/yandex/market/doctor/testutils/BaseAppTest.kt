package ru.yandex.market.doctor.testutils

import org.junit.runner.RunWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.doctor.config.TestConfig

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@ContextConfiguration(
    initializers = [
        PgTestInitializer::class, MockServerInitializer::class,
    ],
    classes = [TestConfig::class]
)
@Transactional
abstract class BaseAppTest {
}
