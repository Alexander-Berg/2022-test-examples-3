package ru.yandex.market.abo.test.db

import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

class RecipeTestPostgres : TestPostgres {
    private val jdbcUrl =
        "jdbc:postgresql://localhost:${System.getenv("PG_LOCAL_PORT")}/${System.getenv("PG_LOCAL_DATABASE")}"

    override fun getDataSource(): DataSource = HikariDataSource(HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = this@RecipeTestPostgres.jdbcUrl
        username = System.getenv("PG_LOCAL_USER")
        password = System.getenv("PG_LOCAL_PASSWORD")
        maximumPoolSize = 6
        minimumIdle = 1
        idleTimeout = 60000
        isRegisterMbeans = false
        connectionTimeout = TimeUnit.SECONDS.toMillis(60)
        connectionInitSql = "set statement_timeout to '600s';"
    })

    override fun getJdbcUrl(): String = jdbcUrl

    override fun close() = Unit
}
