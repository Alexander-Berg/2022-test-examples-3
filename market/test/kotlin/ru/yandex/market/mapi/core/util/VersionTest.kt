package ru.yandex.market.mapi.core.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * @author Ilya Kislitsyn / ilyakis@ / 27.06.2022
 */
class VersionTest {
    @Test
    fun testVersionCheck() {
        assertEquals(1, cmp("1.23", "1.22"))
        assertEquals(0, cmp("1.23", "1.23"))
        assertEquals(-1, cmp("1.23", "1.24"))

        assertEquals(1, cmp("2.23", "1.24"))

        assertEquals(1, cmp("1.23.3", "1.23"))
        assertEquals(0, cmp("1.23.0", "1.23"))
        assertEquals(-1, cmp("1.23", "1.23.4"))
        assertEquals(-1, cmp("1.23.2", "1.23.4"))
        assertEquals(1, cmp("1.23.5", "1.23.4"))

        assertEquals(0, cmp("1.23.a5", "1.23"))
        assertEquals(-1, cmp("1.23b.5", "1.23"))
    }

    private fun cmp(left: String, right: String): Int {
        return Version(left).compareTo(Version(right))
    }
}