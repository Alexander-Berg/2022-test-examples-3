package ru.yandex.market.mbi.feed.processor.samovar.proxy

import NKwYT.Queries
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.common.util.IOUtils
import ru.yandex.market.common.test.util.ProtoTestUtil
import ru.yandex.market.core.campaign.model.CampaignType
import ru.yandex.market.logbroker.LogbrokerEventPublisher
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.samovar.feedInfoBuilder
import ru.yandex.market.mbi.feed.processor.samovar.itemBuilder
import ru.yandex.market.mbi.feed.processor.samovar.result.SamovarMessageFactory
import ru.yandex.market.mbi.feed.processor.test.capture
import ru.yandex.market.yt.samovar.SamovarContextOuterClass

/**
 * Тесты для [SamovarResultDataProxy].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class SamovarResultDataProxyTest : FunctionalTest() {

    @Autowired
    private lateinit var samovarResultDataProxy: SamovarResultDataProxy

    @Autowired
    private lateinit var samovarResultDataProxyLogbrokerService: LogbrokerEventPublisher<SamovarResultDataProxyEvent>

    @Autowired
    private lateinit var samovarMessageFactory: SamovarMessageFactory

    @AfterEach
    fun checkMocks() {
        verifyNoMoreInteractions(samovarResultDataProxyLogbrokerService)
    }

    @Test
    fun `feeds for FP only`() {
        val feedInfo1 = feedInfoBuilder(
            feedId = 1001,
            shopId = 1,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val feedInfo2 = feedInfoBuilder(
            feedId = 1002,
            shopId = 999,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo1, feedInfo2))

        val samovarFeedInfos = samovarMessageFactory.getSamovarFeedInfos(original)

        samovarResultDataProxy.sendToMbi(original, samovarFeedInfos, emptyList())

        val actualEvent = samovarResultDataProxyLogbrokerService.capture()

        ProtoTestUtil.assertThat(Queries.TMarketFeedsItem.parseFrom(IOUtils.unzip(actualEvent.first().bytes)))
            .ignoringFieldsMatchingRegexes(".*context.*")
            .isEqualTo(original)

        val actualFeeds = SamovarContextOuterClass.SamovarContext.parseFrom(actualEvent.first().payload.context).feedsList
        assertThat(actualFeeds.all { it.processedInFeedProcessor }).isTrue
        ProtoTestUtil.assertThat(actualFeeds[0])
            .ignoringFieldsMatchingRegexes(".*processedInFeedProcessor.*", ".*memoizedSize.*")
            .isEqualTo(feedInfo1)
        ProtoTestUtil.assertThat(actualFeeds[1])
            .ignoringFieldsMatchingRegexes(".*processedInFeedProcessor.*", ".*memoizedSize.*")
            .isEqualTo(feedInfo2)
    }

    @Test
    fun `1 for FP and 1 for MBI`() {
        val feedInfo1 = feedInfoBuilder(
            feedId = 1001,
            shopId = 1,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val feedInfo2 = feedInfoBuilder(
            feedId = 1002,
            shopId = 999,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo1, feedInfo2))

        val samovarFeedInfos = samovarMessageFactory.getSamovarFeedInfos(original)

        samovarResultDataProxy.sendToMbi(original, listOf(samovarFeedInfos[0]), listOf(samovarFeedInfos[1]))

        val actualEvent = samovarResultDataProxyLogbrokerService.capture()

        ProtoTestUtil.assertThat(actualEvent.first().payload)
            .ignoringFieldsMatchingRegexes(".*context.*")
            .isEqualTo(original)

        val actualFeeds = SamovarContextOuterClass.SamovarContext.parseFrom(actualEvent.first().payload.context).feedsList
        assertThat(actualFeeds[0].processedInFeedProcessor).isTrue
        assertThat(actualFeeds[1].processedInFeedProcessor).isFalse()
        ProtoTestUtil.assertThat(actualFeeds[0])
            .ignoringFieldsMatchingRegexes(".*processedInFeedProcessor.*", ".*memoizedSize.*")
            .isEqualTo(feedInfo1)
        ProtoTestUtil.assertThat(actualFeeds[1])
            .ignoringFieldsMatchingRegexes(".*processedInFeedProcessor.*", ".*memoizedSize.*")
            .isEqualTo(feedInfo2)
    }

    @Test
    fun `all for MBI`() {
        val feedInfo1 = feedInfoBuilder(
            feedId = 1001,
            shopId = 1,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val feedInfo2 = feedInfoBuilder(
            feedId = 1002,
            shopId = 999,
            businessId = 134,
            campaignType = CampaignType.SHOP
        )
        val original = itemBuilder(feedInfos = listOf(feedInfo1, feedInfo2))

        val samovarFeedInfos = samovarMessageFactory.getSamovarFeedInfos(original)

        samovarResultDataProxy.sendToMbi(original, emptyList(), samovarFeedInfos)

        val actualEvent = samovarResultDataProxyLogbrokerService.capture()

        ProtoTestUtil.assertThat(actualEvent.first().payload)
            .ignoringFieldsMatchingRegexes(".*context.*")
            .isEqualTo(original)

        val actualFeeds = SamovarContextOuterClass.SamovarContext.parseFrom(actualEvent.first().payload.context).feedsList
        assertThat(actualFeeds.all { it.processedInFeedProcessor }).isFalse
        ProtoTestUtil.assertThat(actualFeeds[0])
            .ignoringFieldsMatchingRegexes(".*processedInFeedProcessor.*", ".*memoizedSize.*")
            .isEqualTo(feedInfo1)
        ProtoTestUtil.assertThat(actualFeeds[1])
            .ignoringFieldsMatchingRegexes(".*processedInFeedProcessor.*", ".*memoizedSize.*")
            .isEqualTo(feedInfo2)
    }
}
