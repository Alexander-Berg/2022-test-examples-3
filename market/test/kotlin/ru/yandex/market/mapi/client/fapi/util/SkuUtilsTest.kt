package ru.yandex.market.mapi.client.fapi.util

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.model.FapiSkuInfo
import kotlin.test.assertEquals

class SkuUtilsTest {
    companion object {
        const val SHORT_TEXT = "Электросамокат Xiaomi MiJia Electric Scooter Pro."
        const val NUMBERS_TEXT = "1234567890123445567"
        const val SENTENCE_TEXT = "Электросамокат Xiaomi. Электросамокат Xiaomi. Электросамокат Xiaomi."
    }

    @Test
    fun testShortText() {
        val result = SkuUtils.truncateDescription(SHORT_TEXT)
        assertEquals("Электросамокат Xiaomi MiJia Electric Scooter Pro.", result)
    }

    @Test
    fun testTruncateByLength() {
        val result = SkuUtils.truncateDescription(NUMBERS_TEXT, 4, 4)
        assertEquals("1234…", result)
    }

    @Test
    fun testTruncateBySentence() {
        val result = SkuUtils.truncateDescription(SENTENCE_TEXT, 25, 10)
        assertEquals("Электросамокат Xiaomi.", result)
    }

    @Test
    fun testTruncateByWord() {
        val result = SkuUtils.truncateDescription(SHORT_TEXT, 25, 10)
        assertEquals("Электросамокат Xiaomi", result)
    }

    @Test
    fun testNullFullDescription() {
        val sku = FapiSkuInfo()
        assertEquals(null, sku.fullDescription())
    }

    @Test
    fun testFullHtmlFullDescription() {
        val sku = FapiSkuInfo()
        val description = FapiSkuInfo.FormattedDescription()
        description.fullHtml = "Full HTML"
        description.fullPlain = "Full Plain"
        sku.formattedDescription = description
        sku.description = "Description"
        assertEquals("Full HTML", sku.fullDescription())
    }

    @Test
    fun testFullPlainFullDescription() {
        val sku = FapiSkuInfo()
        val description = FapiSkuInfo.FormattedDescription()
        description.fullPlain = "Full Plain"
        sku.formattedDescription = description
        sku.description = "Description"
        assertEquals("Full Plain", sku.fullDescription())
    }

    @Test
    fun testDescriptionFullDescription() {
        val sku = FapiSkuInfo()
        sku.description = "Description"
        assertEquals("Description", sku.fullDescription())
    }

    @Test
    fun testNullShortDescription() {
        val sku = FapiSkuInfo()
        assertEquals(null, sku.shortDescription())
    }

    @Test
    fun testShortHtmlShortDescription() {
        val sku = FapiSkuInfo()
        val description = FapiSkuInfo.FormattedDescription()
        description.shortHtml = "Short HTML"
        description.shortPlain = "Short Plain"
        sku.formattedDescription = description
        sku.description = "Description"
        assertEquals("Short HTML", sku.shortDescription())
    }

    @Test
    fun testShortPlainShortDescription() {
        val sku = FapiSkuInfo()
        val description = FapiSkuInfo.FormattedDescription()
        description.shortPlain = "Short Plain"
        sku.formattedDescription = description
        sku.description = "Description"
        assertEquals("Short Plain", sku.shortDescription())
    }

    @Test
    fun testDescriptionShortDescription() {
        val sku = FapiSkuInfo()
        sku.description = "Description"
        assertEquals("Description", sku.shortDescription())
    }
}
