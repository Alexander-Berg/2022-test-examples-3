package ru.yandex.direct.core.entity.feedoffer.converter

import Market.DataCamp.DataCampContractTypes.Currency
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.core.entity.feedoffer.model.FeedOffer
import ru.yandex.direct.core.entity.feedoffer.model.RetailFeedOfferParams
import ru.yandex.misc.io.ClassPathResourceInputStreamSource
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

internal class FeedOffersToYmlConverterTest {

    @Test
    fun convertRetailOffersToYml() {
        val clientId = 1111L
        val prices = listOf(10.0, 50.0, 15.05)
        val feedOffers = prices.mapIndexed { idx, price ->
            createOffer(id = idx + 1L, clientId = clientId, price = BigDecimal.valueOf(price), category = "category-1")
        }

        val result = toYmlResult(feedOffers)
        checkResult(result, "feed/feed_offers_1.xml")
    }

    @Test
    fun convertRetailOffersToYml_withDifferentCategories() {
        val clientId = 1111L
        val categories = listOf("category 1", "category 2", "category 3")
        val feedOffers = categories.plus(categories).mapIndexed { idx, category ->
            createOffer(id = idx + 1L, clientId = clientId, imagesCount = 1,
                category = category, fillOptionalFields = false)
        }

        val result = toYmlResult(feedOffers)
        checkResult(result, "feed/feed_offers_2.xml")
    }

    @Test
    fun convertRetailOffersToYml_withDifferentCurrencies() {
        val clientId = 1111L
        val currencies = listOf(Currency.EUR, Currency.USD, Currency.BYN, Currency.EUR)
        val feedOffers = currencies.mapIndexed { idx, currencyCode ->
            createOffer(id = idx + 1L, clientId = clientId, imagesCount = 1, currencyCode = currencyCode,
                category = "category-1", fillOptionalFields = false)
        }

        val result = toYmlResult(feedOffers)
        checkResult(result, "feed/feed_offers_3.xml")
    }

    private fun createOffer(id: Long,
        clientId: Long,
        price: BigDecimal = BigDecimal.valueOf(123.55),
        imagesCount: Int = 3,
        currencyCode: Currency = Currency.RUR,
        isAvailable: Boolean = true,
        category: String? = null,
        fillOptionalFields: Boolean = true): FeedOffer {
        val images = (0 until imagesCount).map { "img-$id-$it" }
        return FeedOffer().apply {
            this.id = id
            this.clientId = clientId
            this.label = id.toString()
            this.description = "description $id"
            this.href = "http://$id.ru"
            this.images = images
            this.currency = currencyCode
            this.currentPrice = price
            this.oldPrice = if (fillOptionalFields) price.multiply(BigDecimal.TEN) else null
            this.isAvailable = isAvailable
            this.updateTime = null
            this.retailFeedOfferParams = RetailFeedOfferParams().apply {
                this.category = category
                this.vendor = if (fillOptionalFields) "vendor $id" else null
                this.model = if (fillOptionalFields) "model $id" else null
            }
        }
    }

    private fun toYmlResult(feedOffers: Collection<FeedOffer>): String {
        val yml = FeedOffersToYmlConverter.convertRetailOffersToYml(feedOffers)
        yml.date = "2022-02-07T10:00:00+03:00"
        val result = yml.toByteArray(true)
        return String(result, StandardCharsets.UTF_8)
    }

    private fun checkResult(result: String, expectedResultPath: String) {
        val expected = ClassPathResourceInputStreamSource(expectedResultPath)
            .readLines().joinToString("\n")

        assertThat(result).isEqualToIgnoringNewLines(expected)
    }
}
