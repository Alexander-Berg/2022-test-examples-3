package ru.yandex.market.mapi

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.mapi.core.MockContext
import ru.yandex.market.mapi.core.NonEngineConfig
import ru.yandex.market.mapi.db.PumpkinRepository
import ru.yandex.market.mapi.tms.config.MapiDatabaseSchedulerFactoryConfig
import ru.yandex.market.tms.quartz2.spring.config.TmsDataSourceConfig
import javax.sql.DataSource

@TestConfiguration
@NonEngineConfig
open class MapiMockDbConfig {

    @Bean
    open fun pgDataSource(): DataSource? {
        return MockContext.registerMock()
    }

    @Bean
    open fun pgJdbcTemplate(): JdbcTemplate {
        return MockContext.registerMock()
    }

    @Bean
    open fun pumpkinRepository(): PumpkinRepository {
        return MockContext.registerMock()
    }

    @Bean
    open fun tmsDataSourceConfig(): TmsDataSourceConfig {
        return MockContext.registerMock()
    }

    @Bean
    open fun mapiDatabaseSchedulerFactoryConfig(): MapiDatabaseSchedulerFactoryConfig {
        return MockContext.registerMock()
    }
}
