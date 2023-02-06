package ru.yandex.market.rom.postgres

import org.junit.jupiter.api.Test
import ru.yandex.market.javaframework.postgres.test.AbstractJdbcRecipeTest

class DatabaseTest : AbstractJdbcRecipeTest() {
    @Test
    fun simpleTest() {
        jdbcTemplate.execute("SELECT 1")
    }
}
