package ru.yandex.market.markup3.testutils

import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.markup3.config.BeansInitializer
import ru.yandex.market.markup3.config.TestConfig


@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@ContextConfiguration(
    initializers = [PgTestInitializer::class, MockServerInitializer::class, BeansInitializer::class],
    classes = [TestConfig::class]
)
@Transactional
abstract class BaseAppTest {
    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate
}
