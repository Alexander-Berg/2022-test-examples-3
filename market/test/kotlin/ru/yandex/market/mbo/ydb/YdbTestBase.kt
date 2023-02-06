package ru.yandex.market.mbo.ydb

import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.market.mbo.ydb.config.TestYdbConfig

@ContextConfiguration(
    classes = [TestYdbConfig::class],
    initializers = [YdbContainerContextInitializer::class]
)
@RunWith(SpringJUnit4ClassRunner::class)
abstract class YdbTestBase {

}
