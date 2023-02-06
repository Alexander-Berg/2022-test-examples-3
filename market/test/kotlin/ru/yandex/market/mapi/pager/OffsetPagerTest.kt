package ru.yandex.market.mapi.pager

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.util.OffsetPager
import kotlin.test.assertEquals

/**
 * @author Ilya Kislitsyn / ilyakis@ / 20.01.2022
 */
class OffsetPagerTest {
    @Test
    fun testPaging() {
        val data = (1..8).toList()
        val pageSize = 3

        val pager = OffsetPager.initial(pageSize)

        val page1 = pager.apply(data)
        assertEquals(Pair((1..3).toList(), OffsetPager(3, pageSize, data.size)), page1)
        assertEquals("3;8", page1.second!!.toToken())

        val page2 = page1.second!!.apply(data)
        assertEquals(Pair((4..6).toList(), OffsetPager(6, pageSize, data.size)), page2)
        assertEquals("6;8", page2.second!!.toToken())

        val page3 = page2.second!!.apply(data)
        assertEquals(Pair((7..8).toList(), null), page3)
    }

    @Test
    fun testTokenParsing() {
        val data = (1..8).toList()

        val pageByToken = OffsetPager.fromToken("4;100500", 10).apply(data)
        assertEquals((5..8).toList(), pageByToken.first)

        val pageByTokenSimple = OffsetPager.fromToken("3", 10).apply(data)
        assertEquals((4..8).toList(), pageByTokenSimple.first)

        val pageByNoToken = OffsetPager.fromToken(null, 5).apply(data)
        assertEquals((1..5).toList(), pageByNoToken.first)
    }
}