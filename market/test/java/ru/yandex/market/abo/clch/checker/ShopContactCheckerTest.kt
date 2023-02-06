package ru.yandex.market.abo.clch.checker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.abo.clch.ClchTest

/**
 * @author zilzilok
 */
class ShopContactCheckerTest @Autowired constructor(
    private val shopContactChecker: ShopContactChecker,
    private val jdbcTemplate: JdbcTemplate
) : ClchTest() {

    @BeforeEach
    fun init() {
        shopContactChecker.configure(CheckerDescriptor(0, "testChecker"))
    }

    @Test
    fun `checking contacts with same name`() {
        createShopContact(SHOP_1, "123@123.ru", "Иван", "Иванов")
        createShopContact(SHOP_2, "321@321.ru", "Иван", "Иванов")
        flushAndClear()

        val result = shopContactChecker.checkShops(SHOP_1, SHOP_2)
        assertEquals(1.0, result.result)
    }

    @Test
    fun `checking contacts with same email`() {
        createShopContact(SHOP_1, "123@123.ru", "Иван", "Иванов")
        createShopContact(SHOP_2, "123@123.ru", "Олег", "Олегов")
        flushAndClear()

        val result = shopContactChecker.checkShops(SHOP_1, SHOP_2)
        assertEquals(1.0, result.result)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "amazingShop@yandex.ru,nice,123@123.ru,www.amazingShop.ru",
            "123@123.ru,www.amazingShop.ru,amazingShop@yandex.ru,nice",
        ]
    )
    fun `checking contacts with domain like email`(email1: String, domain1: String, email2: String, domain2: String) {
        createShopContact(SHOP_1, email1, "Иван", "Иванов", domain1)
        createShopContact(SHOP_2, email2, "Олег", "Олегов", domain2)
        flushAndClear()

        val result = shopContactChecker.checkShops(SHOP_1, SHOP_2)
        assertEquals(ShopContactChecker.DOMAIN_DUPLE, result.result)
    }

    private fun createShopContact(
        shopId: Long, email: String, firstName: String, lastName: String, domain: String? = null
    ) {
        jdbcTemplate.update("""
            INSERT INTO ext_datasource_contacts(datasource_id, first_name, last_name, email)
            VALUES ($shopId, '$firstName', '$lastName', '$email')
        """.trimIndent()
        )
        jdbcTemplate.update("""
            INSERT INTO supplier(id, name)
            VALUES ($shopId, '$domain')
        """.trimIndent()
        )
    }

    companion object {
        private const val SHOP_1 = 1L
        private const val SHOP_2 = 2L
    }
}
