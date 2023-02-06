package ru.yandex.direct.ess.router.rules.feeds.usagetypes

import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.dbschema.ppc.Tables.FEEDS
import ru.yandex.direct.ess.logicobjects.feeds.usagetypes.FeedUsageTypesObject
import ru.yandex.direct.ess.router.configuration.TestConfiguration
import ru.yandex.direct.ess.router.testutils.FeedsChange
import java.math.BigInteger

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
class FeedUsageTypesRuleTest {

    @Autowired
    private lateinit var rule: FeedUsageTypesRule

    @Test
    fun testUpdateWithStatusChanges() {
        val feedId = BigInteger.valueOf(FEED_ID)
        val feedsChange = FeedsChange().withFeedId(feedId).withClientId(CLIENT_ID)
        feedsChange.addChangedColumn(
            FEEDS.USAGE_TYPE,
            "goods_ads",
            ""
        )
        val binlogEvent = FeedsChange.createFeedsEvent(listOf(feedsChange), Operation.UPDATE)
        val actual = rule.mapBinlogEvent(binlogEvent)
        val expected = FeedUsageTypesObject(FEED_ID, null,null,null,Operation.UPDATE)
        val soft = SoftAssertions()
        soft.assertThat(actual).hasSize(1)
        soft.assertThat(actual[0]).isEqualTo(expected).usingRecursiveComparison()
        soft.assertAll()
    }

    @Test
    fun testUpdateWithoutStatusChanges() {
        val feedId = BigInteger.valueOf(FEED_ID)
        val feedsChange = FeedsChange().withFeedId(feedId).withClientId(CLIENT_ID)
        feedsChange.addChangedColumn(
            FEEDS.USAGE_TYPE,
            "goods_ads",
            "goods_ads"
        )
        val binlogEvent = FeedsChange.createFeedsEvent(listOf(feedsChange), Operation.UPDATE)
        val actual = rule.mapBinlogEvent(binlogEvent)
        Assertions.assertThat(actual).hasSize(0)
    }

    companion object {
        private const val CLIENT_ID = 1L
        private const val FEED_ID = 2L
    }
}
