package ru.yandex.market.abo.test.db

import javax.sql.DataSource

interface TestPostgres : AutoCloseable {
    fun getDataSource(): DataSource

    fun getJdbcUrl(): String
}
