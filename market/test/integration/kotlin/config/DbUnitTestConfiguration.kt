package ru.yandex.market.logistics.calendaring.config

import com.github.springtestdbunit.bean.DatabaseConfigBean
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean
import org.dbunit.database.DatabaseDataSourceConnection
import org.dbunit.ext.mysql.MySqlMetadataHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import ru.yandex.market.logistics.test.integration.db.LiquibaseTestConfiguration
import ru.yandex.market.logistics.test.integration.db.cleaner.DataSourceUtils
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DefaultSchemaCleanerConfigProvider
import ru.yandex.market.logistics.test.integration.db.cleaner.config.base.CompoundDatabaseCleanerConfig
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider
import javax.sql.DataSource


@Configuration
@Import(
    LiquibaseTestConfiguration::class
)
open class DbUnitTestConfiguration {
    @Bean
    @Primary
    @Throws(Exception::class)
    open fun dbUnitDatabaseConnection(dataSource: DataSource?): DatabaseDataSourceConnection? {
        val bean = DatabaseDataSourceConnectionFactoryBean(dataSource)
        bean.setDatabaseConfig(dbUnitDatabaseConfig())
        return bean.getObject()
    }

    @Bean
    open fun defaultSchemaCleanerProvider(dataSource: DataSource?): SchemaCleanerConfigProvider {
        return DefaultSchemaCleanerConfigProvider(DataSourceUtils.getDefaultSchemaFromConnection(dataSource))
    }

    @Bean
    @Primary
    open fun cleanerConfig(@Autowired configList: List<SchemaCleanerConfigProvider?>?): DatabaseCleanerConfig {
        return CompoundDatabaseCleanerConfig(configList)
    }

    @Bean
    open fun dbUnitDatabaseConfig(): DatabaseConfigBean {
        val config = DatabaseConfigBean()
        config.datatypeFactory = CustomPostgresqlDataTypeFactory()
        config.tableType = TABLE_TYPE
        config.metadataHandler = MySqlMetadataHandler()
        return config
    }

    companion object {
        private val TABLE_TYPE = arrayOf("TABLE", "VIEW")
    }
}
