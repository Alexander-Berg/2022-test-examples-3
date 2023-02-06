package ru.yandex.market.pricingmgmt.config

import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import ru.yandex.market.common.postgres.spring.configs.PGCommonConfig
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import javax.sql.DataSource

@Configuration
@Import(PGCommonConfig::class)
open class DbConfiguration {
    companion object {
        private const val schema = "pricing_management"
    }

    @Value("\${pricing-mgmt.axapta.db.username}")
    private val axaptaDbUser: String? = null

    @Value("\${pricing-mgmt.axapta.db.password}")
    private val axaptaDbPassword: String? = null

    @Value("\${pricing-mgmt.axapta.db.url}")
    private val axaptalDbUrl: String? = null

    @Value("\${pricing-mgmt.axapta.db.driverClassName}")
    private val axaptalDbDriverClassName: String? = null

    @Primary
    @Bean
    open fun dataSource(pgCommonConfig: PGCommonConfig): DataSource {
        val dataSource = BasicDataSource()
        dataSource.username = pgCommonConfig.userName
        dataSource.password = pgCommonConfig.password
        dataSource.driverClassName = pgCommonConfig.driverName
        dataSource.url = pgCommonConfig.url
        dataSource.defaultSchema = schema
        dataSource.connection.use { connection -> createSchema(connection, schema) }
        return dataSource
    }

    @Bean
    open fun axaptaDataSource(): DataSource {
        val dataSourceBuilder = DataSourceBuilder.create()
        dataSourceBuilder.driverClassName(axaptalDbDriverClassName)
        dataSourceBuilder.url(axaptalDbUrl)
        dataSourceBuilder.username(axaptaDbUser)
        dataSourceBuilder.password(axaptaDbPassword)
        return dataSourceBuilder.build()
    }

    @Throws(SQLException::class)
    @Suppress("SameParameterValue")
    private fun createSchema(connection: Connection, schemaName: String) {
        connection.prepareStatement("CREATE SCHEMA IF NOT EXISTS $schemaName")
            .use(PreparedStatement::executeUpdate)
    }
}
