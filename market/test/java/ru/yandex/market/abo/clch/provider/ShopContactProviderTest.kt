package ru.yandex.market.abo.clch.provider

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.mm.db.DbMailService

/**
 * @author zilzilok
 */
class ShopContactProviderTest @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    private val dbMailService: DbMailService = mock {
        on { loadEmailsForShops(eq(setOf(SHOP_ID)), any(), any()) } doReturn mapOf(
            SHOP_ID to listOf("789@789.ru")
        )
    }
    private val shopContactProvider = ShopContactProvider(dbMailService, jdbcTemplate)

    @Test
    fun `test fill shop contacts`() {
        jdbcTemplate.update("""
            INSERT INTO ext_datasource_contacts(datasource_id, first_name, last_name, email)
            VALUES ($SUPPLIER_ID, 'Иван', 'Иванов', '123@123.ru'),
                ($SUPPLIER_ID, 'Олег', 'Олегов', '321@321.ru')
        """.trimIndent()
        )
        jdbcTemplate.update("""
            INSERT INTO ext_supplier_contacts(datasource_id, email)
            VALUES ($SUPPLIER_ID, '456@456.ru')
        """.trimIndent()
        )
        jdbcTemplate.update("""
            INSERT INTO supplier(id, name)
            VALUES ($SUPPLIER_ID, 'Пупок')
        """.trimIndent()
        )

        jdbcTemplate.update("""
            INSERT INTO shop(id, domain)
            VALUES ($SHOP_ID, 'Пучек')
        """.trimIndent()
        )
        flushAndClear()

        assertEquals(1, shopContactProvider.getValue(SHOP_ID).size)
        assertEquals(3, shopContactProvider.getValue(SUPPLIER_ID).size)
    }

    @Test
    fun `test load data`() {
        jdbcTemplate.update(
            """
            INSERT INTO ext_datasource_contacts(datasource_id, first_name, last_name, email)
            VALUES ($SHOP_ID, 'Иван', 'Иванов', '123@123.ru'),
                ($SHOP_ID, 'Олег', 'Олегов', '321@321.ru')
        """.trimIndent()
        )
        flushAndClear()

        shopContactProvider.loadData()
        assertTrue(shopContactProvider.loadedShops.contains(SHOP_ID))
        assertEquals(1, shopContactProvider.loadedShops.size)
        assertEquals(2, shopContactProvider.getValue(SHOP_ID).size)
    }

    companion object {
        private const val SHOP_ID = 1L
        private const val SUPPLIER_ID = 2L
    }
}
