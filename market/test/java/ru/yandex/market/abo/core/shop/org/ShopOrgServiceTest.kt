package ru.yandex.market.abo.core.shop.org

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest

class ShopOrgServiceTest @Autowired constructor(
    val jdbcTemplate: JdbcTemplate,
    val shopOrgService: ShopOrgService
) : EmptyTest() {

    @Test
    fun `empty org info`() {
        assertNull(shopOrgService.loadBusinessId(1L))

        jdbcTemplate.update("insert into ext_organization_info (datasource_id, business_id) values (?, ?)", 1L, 2L);
        assertEquals(2L, shopOrgService.loadBusinessId(1L))
    }

}
