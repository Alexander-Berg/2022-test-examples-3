package ru.yandex.market.wms.constraints.config

import org.dbunit.database.DatabaseDataSourceConnection
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.constraints.dao.ConstraintsConfigPropertyDao
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider
import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles
import java.time.Clock
import javax.sql.DataSource

@Configuration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
@SpringBootApplication(scanBasePackages = ["ru.yandex.market.wms.constraints"])
class DbConfig {
    @Bean
    @Primary
    fun dataSource(): DataSource {
        val dataSource = dataSourceProperties().initializeDataSourceBuilder().build();
        val initScriptContent = FileContentUtils.getFileContent("constraints-schema.sql");
        dataSource.connection.prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    fun dataSourceProperties() = DataSourceProperties()

    @Bean
    @Primary
    fun namedParameterJdbcTemplate() = NamedParameterJdbcTemplate(dataSource())

    @Bean
    fun constraintsConnection(): DatabaseDataSourceConnection {
        return IntegrationTestConfig.dbUnitDatabaseConnection("CONSTRAINTS", dataSource())
    }

    @Bean
    fun globalConfigurationDao(
        securityDataProvider: SecurityDataProvider,
        jdbcTemplate: NamedParameterJdbcTemplate,
        clock: Clock
    ): GlobalConfigurationDao = ConstraintsConfigPropertyDao(securityDataProvider, jdbcTemplate, clock)

    @Bean
    fun dbConfigService(
        globalConfigurationDao: GlobalConfigurationDao
    ): DbConfigService = DbConfigService(globalConfigurationDao)
}
