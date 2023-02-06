package ru.yandex.market.contentmapping.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.contentmapping.repository.ParamMappingRepository
import ru.yandex.market.contentmapping.repository.ParamMappingRuleRepository
import ru.yandex.market.contentmapping.repository.ParametersMigrationRepository
import ru.yandex.market.contentmapping.services.rules.RulesLoadService
import ru.yandex.market.contentmapping.services.rules.RulesLoadServiceImpl

@TestConfiguration
class TestServicesConfig {
    @Bean
    fun parametersMigrationRepository(
        jdbcTemplate: NamedParameterJdbcTemplate,
        transactionTemplate: TransactionTemplate
    ): ParametersMigrationRepository {
        return ParametersMigrationRepository(jdbcTemplate, transactionTemplate)
    }
}
