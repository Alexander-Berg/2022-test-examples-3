package ru.yandex.market.logistics.calendaring.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.market.logistics.test.integration.db.TestDatabaseInitializer
import ru.yandex.market.logistics.test.integration.db.cleaner.DatabaseCleaner
import ru.yandex.market.logistics.test.integration.db.cleaner.GenericDatabaseCleaner
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider
import ru.yandex.market.logistics.test.integration.db.cleaner.strategy.PostgresDatabaseCleanerStrategy
import ru.yandex.misc.db.embedded.EmbeddedPostgres
import ru.yandex.misc.db.embedded.ImportEmbeddedPg
import ru.yandex.misc.db.embedded.PreparedDbProvider
import javax.sql.DataSource

@Configuration
@ImportEmbeddedPg
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
open class TestDataSourceConfiguration {
    @Bean
    open fun dataSource(postgres: EmbeddedPostgres,
                   @Autowired(required = false) dbIntializer: TestDatabaseInitializer?): DataSource {
        return createDataSource(postgres, dbIntializer)
    }

    @Bean
    open fun databaseCleaner(@Autowired dataSource: DataSource?,
                        @Autowired config: DatabaseCleanerConfig?): DatabaseCleaner {
        return GenericDatabaseCleaner(dataSource, config, PostgresDatabaseCleanerStrategy())
    }

    @Bean
    open fun customSchemaCleanerConfigProvider(): SchemaCleanerConfigProvider {
        return SchemaCleanerConfigProvider
                .builder()
                .schema("public").resetSequences().truncateAllExcept(
                        "databasechangelog",
                        "databasechangeloglock"
                )
                .schema("dbqueue").truncateAll()
                .schema("logbroker").truncateAll()
                .build()
    }

    companion object {
        private fun createDataSource(postgres: EmbeddedPostgres, dbIntializer: TestDatabaseInitializer?): DataSource {
            val provider = PreparedDbProvider.forPreparer("test", postgres)
            val dataSource = provider.createDataSource()
            dbIntializer?.initializeDatabase(dataSource)
            return dataSource
        }
    }
}
