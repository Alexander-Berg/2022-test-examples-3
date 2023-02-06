package ru.yandex.market.mbo.cms.core.models

import org.junit.Assert
import org.junit.Test
import ru.yandex.market.mbo.cms.core.models.Key.buildKey

/**
 * Тест проверяет корректност работы Key.buildKey() при начилии символа "#".
 *
 * @author werytert
 */
class KeyTest {

    @Test
    fun testBuildKeyWithSharp() {
        val template = KeyTemplate(
                listOf("#key1#", "#k#e#y#2#", "####", "key4", "ds"),
                true,
                false,
                "group",
        )

        val keyValues = listOf("#value1#", "#v#a#l#u#e#2#", "####", "value4", null)

        val actual = buildKey(template, keyValues)

        val expected = Key(
                "#key1#=%23value1%23##k#e#y#2#=%23v%23a%23l%23u%23e%232%23#####=%23%23%23%23#key4=value4"
        )

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testBuildKeyWithNull() {
        val template = KeyTemplate(
                listOf("key"),
                true,
                false,
                "group",
        )

        val keyValues = listOf(null)

        val actual = buildKey(template, keyValues)

        val expected = null

        Assert.assertEquals(expected, actual)
    }

}
