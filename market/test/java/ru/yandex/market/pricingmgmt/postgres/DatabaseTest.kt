package ru.yandex.market.pricingmgmt.postgres

import org.junit.jupiter.api.Test

class DatabaseTest : AbstractDatabaseTest() {

    @Test
    fun simpleTest() {
        jdbcTemplate.execute("SELECT 1")
    }
}
