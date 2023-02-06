package ru.yandex.direct.grid.processing.service.creative

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.service.FeedService
import ru.yandex.direct.core.entity.feed.service.MbiService
import ru.yandex.direct.core.entity.uac.model.EcomDomain
import ru.yandex.direct.core.entity.uac.service.EcomDomainsService
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.misc.io.ClassPathResourceInputStreamSource
import kotlin.math.min

class OffersPreviewServiceTest {

    companion object {
        val OFFER_EXAMPLES_1 = ClassPathResourceInputStreamSource("creative/feed_offer_examples_1.json")
            .readLines().joinToString("\n")
        const val OFFER_EXAMPLES_1_SIZE = 4

        val OFFER_EXAMPLES_2 = ClassPathResourceInputStreamSource("creative/feed_offer_examples_2.json")
            .readLines().joinToString("\n")
        const val OFFER_EXAMPLES_2_SIZE = 14

        val OFFER_EXAMPLES_3 = ClassPathResourceInputStreamSource("creative/feed_offer_examples_3.json")
            .readLines().joinToString("\n")
        const val OFFER_EXAMPLES_3_SIZE = 10

        val OFFER_EXAMPLES_4 = ClassPathResourceInputStreamSource("creative/domain_offer_examples_1.json")
            .readLines().joinToString("\n")
        const val OFFER_EXAMPLES_4_SIZE = 4

        val OFFER_EXAMPLES_5 = ClassPathResourceInputStreamSource("creative/domain_offer_examples_2.json")
            .readLines().joinToString("\n")
        const val OFFER_EXAMPLES_5_SIZE = 10

        val OFFER_EXAMPLES_6 = ClassPathResourceInputStreamSource("creative/domain_offer_examples_3.json")
            .readLines().joinToString("\n")
        const val OFFER_EXAMPLES_6_SIZE = 12

        val CLIENT_ID = ClientId.fromLong(1L)
    }

    @Mock
    lateinit var feedService: FeedService

    @Mock
    lateinit var ecomDomainsService: EcomDomainsService

    lateinit var offersPreviewService: OffersPreviewService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        offersPreviewService = OffersPreviewService(feedService, ecomDomainsService)
    }

    @Test
    fun getOffersPreviewByFeedIds_success() {
        val feed1Id = 1L
        val feed2Id = 3L
        val feed3Id = 5L
        val feedsIds = listOf(feed1Id, feed2Id, feed3Id)
        whenever(feedService.getFeeds(eq(CLIENT_ID), eq(feedsIds)))
            .doReturn(
                listOf(
                    Feed().withId(feed1Id).withOfferExamples(OFFER_EXAMPLES_1),
                    Feed().withId(feed2Id).withOfferExamples(OFFER_EXAMPLES_2),
                    Feed().withId(feed3Id).withOfferExamples(OFFER_EXAMPLES_3)
                )
            )

        val result = offersPreviewService.getOffersPreviewByFeedIds(CLIENT_ID, feedsIds)

        assertThat(result, hasSize(feedsIds.size))

        assertThat(result[0].previews, hasSize(min(OFFER_EXAMPLES_1_SIZE, MbiService.OFFERS_COUNT_FOR_PREVIEW)))
        assertThat(result[1].previews, hasSize(min(OFFER_EXAMPLES_2_SIZE, MbiService.OFFERS_COUNT_FOR_PREVIEW)))
        assertThat(result[2].previews, hasSize(min(OFFER_EXAMPLES_3_SIZE, MbiService.OFFERS_COUNT_FOR_PREVIEW)))
    }

    @Test
    fun getOffersPreviewByFeedIds_emptyFeedIds_success() {
        val feedsIds = emptyList<Long>()
        whenever(feedService.getFeeds(eq(CLIENT_ID), eq(feedsIds)))
            .doReturn(emptyList())

        val result = offersPreviewService.getOffersPreviewByFeedIds(CLIENT_ID, feedsIds)

        assertThat(result, hasSize(0))
    }

    @Test
    fun getOffersPreviewByFeedIds_feedsNotFound_success() {
        val feed1Id = 1L
        val feed2Id = 3L
        val feed3Id = 5L
        val feedsIds = listOf(feed1Id, feed2Id, feed3Id)
        whenever(feedService.getFeeds(eq(CLIENT_ID), eq(feedsIds)))
            .doReturn(emptyList())

        val result = offersPreviewService.getOffersPreviewByFeedIds(CLIENT_ID, feedsIds)

        assertThat(result, hasSize(0))
    }

    @Test
    fun getOffersPreviewByFeedIds_offersExamplesIsEmpty_success() {
        val emptyOfferExamples1 = null
        val emptyOfferExamples2 = ""

        val feed1Id = 1L
        val feed2Id = 3L
        val feedsIds = listOf(feed1Id, feed2Id)
        whenever(feedService.getFeeds(eq(CLIENT_ID), eq(feedsIds)))
            .doReturn(
                listOf(
                    Feed().withId(feed1Id).withOfferExamples(emptyOfferExamples1),
                    Feed().withId(feed2Id).withOfferExamples(emptyOfferExamples2),
                )
            )

        val result = offersPreviewService.getOffersPreviewByFeedIds(CLIENT_ID, feedsIds)

        assertThat(result, hasSize(0))
    }

    @Test
    fun getOffersPreviewByFeedIds_offersExamplesWithoutElements_success() {
        val emptyOfferExamples1 = "{}"
        val emptyOfferExamples2 = "{}"

        val feed1Id = 1L
        val feed2Id = 3L
        val feedsIds = listOf(feed1Id, feed2Id)
        whenever(feedService.getFeeds(eq(CLIENT_ID), eq(feedsIds)))
            .doReturn(
                listOf(
                    Feed().withId(feed1Id).withOfferExamples(emptyOfferExamples1),
                    Feed().withId(feed2Id).withOfferExamples(emptyOfferExamples2),
                )
            )

        val result = offersPreviewService.getOffersPreviewByFeedIds(CLIENT_ID, feedsIds)

        assertThat(result, hasSize(2))

        assertThat(result[0].previews, hasSize(0))
        assertThat(result[1].previews, hasSize(0))
    }

    @Test
    fun getOffersPreviewByUrls_success() {
        val url1 = "https://domain.ru"
        val url2 = "http://other-domain.ru"
        val url3 = "https://domain.com"
        val urls = listOf(url1, url2, url3)

        whenever(ecomDomainsService.getEcomDomainsByUrls(eq(urls)))
            .doReturn(
                mapOf(
                    url1 to EcomDomain().withDomain("domain.ru").withOffersCount(OFFER_EXAMPLES_4_SIZE.toLong())
                        .withPreviewOffers(OFFER_EXAMPLES_4),
                    url2 to EcomDomain().withDomain("other-domain.ru").withOffersCount(OFFER_EXAMPLES_5_SIZE.toLong())
                        .withPreviewOffers(OFFER_EXAMPLES_5),
                    url3 to EcomDomain().withDomain("domain.com").withOffersCount(OFFER_EXAMPLES_6_SIZE.toLong())
                        .withPreviewOffers(OFFER_EXAMPLES_6)
                )
            )
        whenever(ecomDomainsService.getMinOffersForEcomAllowed())
            .doReturn(3L)
        whenever(ecomDomainsService.getMaxOffersForEcomAllowed())
            .doReturn(100_000L)

        val result = offersPreviewService.getOffersPreviewByUrls(urls)

        assertThat(result, hasSize(urls.size))

        assertThat(result[0].previews, hasSize(min(OFFER_EXAMPLES_4_SIZE, MbiService.OFFERS_COUNT_FOR_PREVIEW)))
        assertThat(result[0].strongOffersCount, equalTo(OFFER_EXAMPLES_4_SIZE.toLong()))
        assertThat(result[1].previews, hasSize(min(OFFER_EXAMPLES_5_SIZE, MbiService.OFFERS_COUNT_FOR_PREVIEW)))
        assertThat(result[1].strongOffersCount, equalTo(OFFER_EXAMPLES_5_SIZE.toLong()))
        assertThat(result[2].previews, hasSize(min(OFFER_EXAMPLES_6_SIZE, MbiService.OFFERS_COUNT_FOR_PREVIEW)))
        assertThat(result[2].strongOffersCount, equalTo(OFFER_EXAMPLES_6_SIZE.toLong()))
    }

    @Test
    fun getOffersPreview_invalidUrls_success() {
        val url1 = "not_valid_url"
        val url2 = "other-domain.ru"
        val url3 = "(https://domain.com)"
        val urls = listOf(url1, url2, url3)

        val result = offersPreviewService.getOffersPreviewByUrls(urls)

        assertThat(result, hasSize(0))
    }

    @Test
    fun getOffersPreview_emptyUrls_success() {
        val urls = emptyList<String>()

        val result = offersPreviewService.getOffersPreviewByUrls(urls)

        assertThat(result, hasSize(0))
    }

    @Test
    fun getOffersPreviewByUrls_domainsNotFound_success() {
        val url1 = "https://domain.ru"
        val url2 = "http://other-domain.ru"
        val url3 = "https://domain.com"
        val urls = listOf(url1, url2, url3)
        whenever(ecomDomainsService.getEcomDomainsByUrls(eq(urls)))
            .doReturn(emptyMap())

        val result = offersPreviewService.getOffersPreviewByUrls(urls)

        assertThat(result, hasSize(0))
    }

    @Test
    fun getOffersPreviewByUrls_emptyPreview_success() {
        val url1 = "https://domain.ru"
        val url2 = "http://other-domain.ru"
        val urls = listOf(url1, url2)

        val previews1 = null
        val previews2 = ""

        whenever(ecomDomainsService.getEcomDomainsByUrls(eq(urls)))
            .doReturn(
                mapOf(
                    url1 to EcomDomain().withDomain("domain.ru").withOffersCount(OFFER_EXAMPLES_4_SIZE.toLong())
                        .withPreviewOffers(previews1),
                    url2 to EcomDomain().withDomain("other-domain.ru").withOffersCount(OFFER_EXAMPLES_5_SIZE.toLong())
                        .withPreviewOffers(previews2)
                )
            )

        val result = offersPreviewService.getOffersPreviewByUrls(urls)

        assertThat(result, hasSize(0))
    }

    @Test
    fun getOffersPreviewByUrls_previewWithoutOffers_success() {
        val url1 = "https://domain.ru"
        val url2 = "http://other-domain.ru"
        val urls = listOf(url1, url2)

        val previews1 = "{}"
        val previews2 = "{\"data_params\": {}}"

        whenever(ecomDomainsService.getEcomDomainsByUrls(eq(urls)))
            .doReturn(
                mapOf(
                    url1 to EcomDomain().withDomain("domain.ru").withOffersCount(OFFER_EXAMPLES_4_SIZE.toLong())
                        .withPreviewOffers(previews1),
                    url2 to EcomDomain().withDomain("other-domain.ru").withOffersCount(OFFER_EXAMPLES_5_SIZE.toLong())
                        .withPreviewOffers(previews2)
                )
            )
        whenever(ecomDomainsService.getMinOffersForEcomAllowed())
            .doReturn(2L)
        whenever(ecomDomainsService.getMaxOffersForEcomAllowed())
            .doReturn(100_000L)

        val result = offersPreviewService.getOffersPreviewByUrls(urls)

        assertThat(result, hasSize(urls.size))

        assertThat(result[0].previews, hasSize(0))
        assertThat(result[0].strongOffersCount, equalTo(OFFER_EXAMPLES_4_SIZE.toLong()))
        assertThat(result[1].previews, hasSize(0))
        assertThat(result[1].strongOffersCount, equalTo(OFFER_EXAMPLES_5_SIZE.toLong()))
    }

    @Test
    fun getOffersPreviewByUrls_offersCountOutOfBound() {
        val url1 = "https://domain.ru"
        val url2 = "http://other-domain.ru"
        val urls = listOf(url1, url2)

        val previews1 = "{}"
        val previews2 = "{\"data_params\": {}}"


        whenever(ecomDomainsService.getEcomDomainsByUrls(eq(urls)))
            .doReturn(
                mapOf(
                    url1 to EcomDomain().withDomain("domain.ru").withOffersCount(OFFER_EXAMPLES_1_SIZE.toLong())
                        .withPreviewOffers(previews1),
                    url2 to EcomDomain().withDomain("other-domain.ru").withOffersCount(OFFER_EXAMPLES_2_SIZE.toLong())
                        .withPreviewOffers(previews2)
                )
            )

        whenever(ecomDomainsService.getMinOffersForEcomAllowed())
            .doReturn((OFFER_EXAMPLES_1_SIZE + 1).toLong())
        whenever(ecomDomainsService.getMaxOffersForEcomAllowed())
            .doReturn(((OFFER_EXAMPLES_2_SIZE - 1).toLong()))

        val result = offersPreviewService.getOffersPreviewByUrls(urls)

        assertThat(result, hasSize(0))
    }

    @Test
    fun getOffersPreviewByUrls_offersCountInBound() {
        val url1 = "https://domain.ru"
        val url2 = "http://other-domain.ru"
        val urls = listOf(url1, url2)

        val previews1 = "{}"
        val previews2 = "{\"data_params\": {}}"


        whenever(ecomDomainsService.getEcomDomainsByUrls(eq(urls)))
            .doReturn(
                mapOf(
                    url1 to EcomDomain().withDomain("domain.ru").withOffersCount(OFFER_EXAMPLES_1_SIZE.toLong())
                        .withPreviewOffers(previews1),
                    url2 to EcomDomain().withDomain("other-domain.ru").withOffersCount(OFFER_EXAMPLES_2_SIZE.toLong())
                        .withPreviewOffers(previews2)
                )
            )

        whenever(ecomDomainsService.getMinOffersForEcomAllowed())
            .doReturn((OFFER_EXAMPLES_1_SIZE - 1).toLong())
        whenever(ecomDomainsService.getMaxOffersForEcomAllowed())
            .doReturn(((OFFER_EXAMPLES_2_SIZE + 1).toLong()))

        val result = offersPreviewService.getOffersPreviewByUrls(urls)

        assertThat(result, hasSize(2))
    }

}
