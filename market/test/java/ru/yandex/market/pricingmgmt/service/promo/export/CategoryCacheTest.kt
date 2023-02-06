package ru.yandex.market.pricingmgmt.service.promo.export

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import ru.yandex.market.pricingmgmt.model.postgres.Category
import ru.yandex.market.pricingmgmt.service.promo.export.CategoryCache.Leaf
import java.util.*

internal class CategoryCacheTest {

    @Test
    fun getLeafsForCategory() {
        val cache = CategoryCache(
            categories = listOf(
                Category(id = -1L, parentId = 0L),
                Category(id = 1L, parentId = -1L),
                Category(id = 2L, parentId = -1L),
                Category(id = 3L, parentId = -1L),
                Category(id = 4L, parentId = 2L),
                Category(id = 6L, parentId = 2L),
                Category(id = 5L, parentId = 4L),
                Category(id = 7L, parentId = 3L),
            )
        )

        assertEquals(Collections.emptyList<Long>(), cache.getLeafsForCategory(0L))
        assertEquals(Collections.emptyList<Long>(), cache.getLeafsForCategory(8L))
        assertEquals(listOf(Leaf(1L, 0)), cache.getLeafsForCategory(1L))
        assertEquals(listOf(Leaf(7L, 1)), cache.getLeafsForCategory(3L))
        assertEquals(
            listOf(
                Leaf(6L, 1),
                Leaf(5L, 2)
            ), cache.getLeafsForCategory(2L)
        )
        assertEquals(
            listOf(
                Leaf(1L, 1),
                Leaf(6L, 2),
                Leaf(7L, 2),
                Leaf(5L, 3)
            ), cache.getLeafsForCategory(-1L)
        )

    }
}
