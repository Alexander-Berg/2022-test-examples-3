package ru.yandex.market.markup3.config

import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper

@TestConfiguration
class TestDatabaseConfig : DatabaseConfig() {
    override fun namedJdbcTemplate(): NamedParameterJdbcTemplate = Mockito.spy(super.namedJdbcTemplate())

    override fun commonTransactionHelper(): TransactionHelper {
        return TransactionHelper.MOCK
    }
}
