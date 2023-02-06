package ru.yandex.market.wms.placement.config

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
import ru.yandex.market.wms.placement.dao.NsqlCacheDao
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles
import javax.sql.DataSource

@Configuration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
@SpringBootApplication(scanBasePackages = ["ru.yandex.market.wms.placement"])
class DbConfig {
    @Bean
    @Primary
    fun dataSource(): DataSource {
        val dataSource = dataSourceProperties().initializeDataSourceBuilder().build();
        val initScriptContent = FileContentUtils.getFileContent("placement-schema.sql");
        dataSource.connection.prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource-placement")
    fun dataSourceProperties() = DataSourceProperties()

    @Bean
    @Primary
    fun namedParameterJdbcTemplate() = NamedParameterJdbcTemplate(dataSource())

    @Bean
    fun placementConnection(): DatabaseDataSourceConnection {
        return IntegrationTestConfig.dbUnitDatabaseConnection("PLACEMENT", dataSource())
    }

    @Bean
    fun archiveConnection(): DatabaseDataSourceConnection {
        return IntegrationTestConfig.dbUnitDatabaseConnection("ARCHIVE", dataSource())
    }

    @Bean
    @Primary
    fun dbConfigService(nsqlCacheDao: NsqlCacheDao) = DbConfigService(nsqlCacheDao)
}
