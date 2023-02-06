package ru.yandex.market.wms.yt.config

import org.dbunit.database.DatabaseDataSourceConnection
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles
import ru.yandex.market.wms.yt.dao.SingleTransactionSnapshotDao
import javax.sql.DataSource

@Configuration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
@SpringBootApplication(scanBasePackages = ["ru.yandex.market.wms"], exclude = [JdbcTemplateAutoConfiguration::class])
open class DbConfig {
    @Bean
    @Primary
    open fun testDbConfigService(nSqlConfigDao: ru.yandex.market.wms.shared.libs.configproperties.dao.NSqlConfigDao) = DbConfigService(nSqlConfigDao)

    @Bean
    @Primary
    open fun mockSnapshotDao() = Mockito.mock(SingleTransactionSnapshotDao::class.java)

    @Bean
    @Primary
    fun dataSource(): DataSource {
        val dataSource = dataSourceProperties().initializeDataSourceBuilder().build();
        val initScriptContent = FileContentUtils.getFileContent("yt-schema.sql");
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
    fun placementConnection(): DatabaseDataSourceConnection {
        return IntegrationTestConfig.dbUnitDatabaseConnection("PLACEMENT", dataSource())
    }

    @Bean
    fun archiveConnection(): DatabaseDataSourceConnection {
        return IntegrationTestConfig.dbUnitDatabaseConnection("ARCHIVE", dataSource())
    }

    @Bean
    @Primary
    fun jdbcTemplate(@Qualifier("dataSource") dataSource: DataSource): JdbcTemplate = JdbcTemplate(dataSource)
}
