package ru.yandex.market.mdm.service.task

import org.junit.runner.RunWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.market.mdm.lib.testutils.PgTestInitializer
import ru.yandex.market.mdm.service.shared.TestConfig

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@ContextConfiguration(
    initializers = [PgTestInitializer::class],
    classes = [
        TestConfig::class,
    ]
)
abstract class TaskBaseTestClass
