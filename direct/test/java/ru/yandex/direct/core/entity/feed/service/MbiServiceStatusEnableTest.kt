package ru.yandex.direct.core.entity.feed.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestContextManager
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.model.StatusMBIEnabled
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.market.client.MarketClient

@CoreTest
@RunWith(JUnitParamsRunner::class)
class MbiServiceStatusEnableTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var mbiService: MbiService

    @Autowired
    private lateinit var feedRepository: FeedRepository

    @Autowired
    private lateinit var marketClient: MarketClient

    private lateinit var clientInfo: ClientInfo
    private lateinit var feedInfo: FeedInfo
    private var shard = 0
    private val testContextManager = TestContextManager(MbiServiceStatusEnableTest::class.java)

    @Before
    fun before() {
        testContextManager.prepareTestInstance(this)
        clientInfo = steps.clientSteps().createDefaultClient()
        shard = clientInfo.shard
        feedInfo = steps.feedSteps().createDefaultSyncedFeed(clientInfo)

        doReturn(MarketClient.SendFeedResult(feedInfo.marketFeedId, feedInfo.shopId, feedInfo.businessId))
            .whenever(marketClient).sendUrlFeedToMbi(any(), any())
    }

    fun parameters() =
        arrayOf<Array<Any>>(
            arrayOf("Включаем выключенный", StatusMBIEnabled.NO, true, StatusMBIEnabled.YES),
            arrayOf("Включаем включенный", StatusMBIEnabled.YES, true, StatusMBIEnabled.YES),
            arrayOf("Выключаем выключенный", StatusMBIEnabled.NO, false, StatusMBIEnabled.NO),
            arrayOf("Выключаем включенный", StatusMBIEnabled.YES, false, StatusMBIEnabled.NO),
        )

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{0}")
    fun test(
        @SuppressWarnings("unused") testCaseName: String,
        initialStatusMBIEnabled: StatusMBIEnabled,
        enable: Boolean,
        expectedStatusMBIEnabled: StatusMBIEnabled
    ) {
        steps.feedSteps().setFeedProperty(feedInfo, Feed.STATUS_MBI_ENABLED, initialStatusMBIEnabled)
        enableOrDisableShop(enable)
        val actual = feedRepository.getSimple(shard, listOf(feedInfo.feedId))[0].statusMbiEnabled
        Assertions.assertThat(actual).isEqualTo(expectedStatusMBIEnabled)
    }

    private fun enableOrDisableShop(enable: Boolean) {
        if (enable) {
            mbiService.sendFeed(feedInfo.clientId, feedInfo.uid, feedInfo.feed, forceEnableDirectPlacement = true)
        } else {
            mbiService.sendFeed(feedInfo.clientId, feedInfo.uid, feedInfo.feed, forceEnableDirectPlacement = false)
        }
    }
}
