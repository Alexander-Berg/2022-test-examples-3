package ru.yandex.market.abo.shoppinger.generator.db

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest

/**
 * @author artemmz
 * @date 23/01/19.
 */
class DBMarketTaskGeneratorTest @Autowired constructor(
    private val dbMarketTaskGenerator: DBMarketTaskGenerator,
    var jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    val URL = "someURL"
    val SHOP_ID = 1

    @Test
    fun addTasks() {
        jdbcTemplate.update("INSERT INTO shop(id, is_enabled, ping_enabled, in_prd_base, is_offline) " +
                "VALUES (?, TRUE, TRUE, TRUE, FALSE)", SHOP_ID)
        jdbcTemplate.update("insert into mp_url(generation_id, shop_id, url, ware_md5) values (?, ?, ?, ?)",
                1, SHOP_ID, URL, "ware")

        assertFalse(hasTasks())
        dbMarketTaskGenerator.addNewTasks()
        assertTrue(hasTasks())
    }

    fun hasTasks() = jdbcTemplate.queryForObject("select count(*) > 0 from pinger_content_task where url = ?", Boolean::class.java, URL)
}
