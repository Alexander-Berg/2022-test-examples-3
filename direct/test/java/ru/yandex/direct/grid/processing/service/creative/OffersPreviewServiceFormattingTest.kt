package ru.yandex.direct.grid.processing.service.creative

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.service.FeedService
import ru.yandex.direct.core.entity.uac.service.EcomDomainsService
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.misc.io.ClassPathResourceInputStreamSource

class OffersPreviewServiceFormattingTest {

    companion object {
        val OFFER_EXAMPLES = ClassPathResourceInputStreamSource("creative/feed_offer_examples_4.json")
                .readLines().joinToString("\n")
        const val OFFER_EXAMPLES_SIZE = 3

        val CLIENT_ID: ClientId = ClientId.fromLong(1L)
        const val FEED_ID = 1L
        val FEED_IDS = listOf(FEED_ID)
    }

    @Mock
    lateinit var feedService: FeedService

    @Mock
    lateinit var ecomDomainsService: EcomDomainsService

    private lateinit var offersPreviewService: OffersPreviewService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        offersPreviewService = OffersPreviewService(feedService, ecomDomainsService)
    }

    @Test
    fun getOffersPreviewByFeedIds_noTrailingZeros() {
        whenever(feedService.getFeeds(eq(CLIENT_ID), eq(FEED_IDS)))
                .doReturn(
                        listOf(
                                Feed().withId(FEED_ID).withOfferExamples(OFFER_EXAMPLES)
                        )
                )

        val result = offersPreviewService.getOffersPreviewByFeedIds(CLIENT_ID, FEED_IDS)

        Assert.assertThat(result, hasSize(FEED_IDS.size))
        Assert.assertThat(result[0].previews, hasSize(OFFER_EXAMPLES_SIZE))

        val price = result[0].previews[0].price

        Assert.assertThat(price.current, equalTo("3080"))
        Assert.assertThat(price.old, equalTo("4400"))
    }

    @Test
    fun getOffersPreviewByFeedIds_tooSmallPenny() {
        whenever(feedService.getFeeds(eq(CLIENT_ID), eq(FEED_IDS)))
                .doReturn(
                        listOf(
                                Feed().withId(FEED_ID).withOfferExamples(OFFER_EXAMPLES)
                        )
                )

        val result = offersPreviewService.getOffersPreviewByFeedIds(CLIENT_ID, FEED_IDS)

        Assert.assertThat(result, hasSize(FEED_IDS.size))
        Assert.assertThat(result[0].previews, hasSize(OFFER_EXAMPLES_SIZE))

        val price = result[0].previews[1].price

        Assert.assertThat(price.current, equalTo("10"))
        Assert.assertThat(price.old, equalTo("10"))
    }

    @Test
    fun getOffersPreviewByFeedIds_bigMoney() {
        whenever(feedService.getFeeds(eq(CLIENT_ID), eq(FEED_IDS)))
                .doReturn(
                        listOf(
                                Feed().withId(FEED_ID).withOfferExamples(OFFER_EXAMPLES)
                        )
                )

        val result = offersPreviewService.getOffersPreviewByFeedIds(CLIENT_ID, FEED_IDS)

        Assert.assertThat(result, hasSize(FEED_IDS.size))
        Assert.assertThat(result[0].previews, hasSize(OFFER_EXAMPLES_SIZE))

        val price = result[0].previews[2].price

        Assert.assertThat(price.current, equalTo("999999999999999"))
        Assert.assertThat(price.old, equalTo("999999999999999"))
    }
}