package ru.yandex.market.abo.core.verticalshare

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest

class VerticalShareShopRepositoryTest @Autowired constructor(
    private val verticalShareShopRepository: VerticalShareShopRepository
) : EmptyTest() {

    @Test
    fun searchByQuery() {
        fillDb()

        val query = "test"
        val expectedIds = shops
            .filter {
                it.feedUrl.contains(query, true) ||
                (it.domain?.contains(query, true) ?: false)
            }
            .map { it.partnerId }
            .toLongArray()
        val actualIds = verticalShareShopRepository
            .findAllByQuery(query)
            .map { it.partnerId }
            .toLongArray()

        Assertions.assertArrayEquals(expectedIds, actualIds)
    }

    private fun fillDb() {
        verticalShareShopRepository.saveAll(shops)
        flushAndClear()
    }

    companion object {
        private val shops = listOf(
            VerticalShareShop(
                2, 1, 21, "shop test 2",
                "custom-query.com", "custom-query.com/feed.xml", VerticalSharePartnerType.WEBMASTER, "213", "SUCCESS"
            ),
            VerticalShareShop(
                4, 3, 41, "shop 4",
                "custom-query-test.com", "custom-query.com/feed.xml", VerticalSharePartnerType.WEBMASTER, "166;172;213", "SUCCESS"
            ),
            VerticalShareShop(
                6, 5, 61, "shop 6",
                "custom-query.com", "custom-query.com/feed.xml", VerticalSharePartnerType.WEBMASTER, "", "SUCCESS"
            ),
            VerticalShareShop(
                8, 7, 81, "shop 8",
                "custom-query.com", "custom-query.com/feed.xml", VerticalSharePartnerType.WEBMASTER, "213", "SUCCESS"
            ),
            VerticalShareShop(
                10, 9, 101, "shop 10",
                "Test-custom-query.com", "custom-query.com/feed.xml", VerticalSharePartnerType.WEBMASTER, "213", "SUCCESS"
            )
        )
    }
}
