package ru.yandex.market.partner.status

import org.junit.jupiter.api.Test

class DatabaseTest : AbstractFunctionalTest() {
    @Test
    fun simpleTest() {
        jdbcTemplate.execute("SELECT 1")
    }
}
