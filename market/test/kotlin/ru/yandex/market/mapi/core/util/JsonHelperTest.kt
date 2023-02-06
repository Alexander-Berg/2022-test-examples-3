package ru.yandex.market.mapi.core.util

import com.fasterxml.jackson.databind.node.NullNode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @author Ilya Kislitsyn / ilyakis@ / 21.01.2022
 */
class JsonHelperTest {
    @Test
    fun testParseMain() {
        val data = """
           {
                "testKey": "123",
                "value": "val"
           }
        """.trimIndent()

        val expected = TestData("123", "val")
        val tree = JsonHelper.parseTree(data)

        assertEquals(expected, JsonHelper.parse(data))
        assertEquals(expected, JsonHelper.parse(data, TestData::class))
        assertEquals(expected, JsonHelper.parse(tree, TestData::class))

        assertTrue { tree.isNotNothing() }
        assertFalse { tree.isNothing() }

        val copy = JsonHelper.copy(expected)

        // equals
        assertTrue { copy == expected }
        // not same object
        assertFalse { copy === expected }

        assertEquals("{\"testKey\":\"123\",\"value\":\"val\"}", JsonHelper.toString(expected))
    }

    @Test
    fun testTryParse() {
        val data = """
                {"id": "12312", "field": {"sub1": 123, "sub2": "strdata", "sub3": 3.1415926}},
        """.trimIndent()

        val json = JsonHelper.parseTree(data)

        assertEquals(null, JsonHelper.tryParse<Int>(json.at("/field/sub")))
        assertEquals(123, JsonHelper.tryParse<Int>(json.at("/field/sub1")))
        assertEquals(123, json.at("/field/sub1").tryParse<Int>())
        assertEquals("strdata", JsonHelper.tryParse<String>(json.at("/field/sub2")))
        assertEquals(BigDecimal.valueOf(3.1415926), JsonHelper.tryParse<BigDecimal>(json.at("/field/sub3")))
    }

    @Test
    fun testExtensions() {
        val data = """
                {"id": "12312", "field": {"sub1": 123, "sub2": "strdata", "sub3": 3.1415926}},
        """.trimIndent()
        val json = JsonHelper.parseTree(data)

        assertFalse { json.isNothing() }
        assertTrue { json["nothing"].isNothing() }
        assertTrue { json.at("/field/sub").isNothing() }
        assertFalse { json.at("/field/sub1").isNothing() }
    }

    @Test
    fun testJsonExtraction() {
        val data = """
           {
                "optInt": 123,
                "optStr": "val",
                "optBool": false,
                "optDouble": 123.56,
                "none": null
           }
        """.trimIndent()

        val tree = JsonHelper.parseTree(data)


        assertEquals(null, tree.get("null"))
        assertTrue { tree.get("none") is NullNode }
        assertTrue { tree.get("none").isNothing() }
        assertTrue { tree.get("optInt").isNotNothing() }

        assertEquals(123, tree.optInt("optInt"))
        assertEquals(123, tree.optLong("optInt"))
        assertEquals(123.0, tree.optDouble("optInt"))
        assertEquals(null, tree.optInt("optDouble"))
        assertEquals(null, tree.optLong("optDouble"))
        assertEquals(123.56, tree.optDouble("optDouble"))
        assertEquals("val", tree.optStr("optStr"))
        assertEquals(false, tree.optBool("optBool"))


        // null checks on casts
        assertNull(tree.optInt("optStr"))
        assertNull(tree.optLong("optStr"))
        assertNull(tree.optDouble("optStr"))
        assertNull(tree.optBool("optStr"))
        assertNull(tree.optBool("optInt"))
        assertNull(tree.optStr("optInt"))

        // null on empty node
        assertNull(tree.optInt("none"))
        assertNull(tree.optLong("none"))
        assertNull(tree.optDouble("none"))
        assertNull(tree.optStr("none"))
        assertNull(tree.optBool("none"))


    }

    data class TestData(
        val testKey: String,
        val value: String,
    )
}