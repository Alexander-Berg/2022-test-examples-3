package ru.yandex.direct.core.testing.stub

import org.asynchttpclient.DefaultAsyncHttpClient
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.market.client.MarketClient
import ru.yandex.direct.market.client.http.MarketHttpClient
import ru.yandex.direct.tvm.TvmService

class MarketClientStub(
    private val shardHelper: ShardHelper,
    private val feedRepository: FeedRepository
) : MarketHttpClient(
    MarketConfiguration(TvmService.MARKET_MBI_API_TEST, "https://do-not-use-me.yandex.ru/"),
    ParallelFetcherFactory(DefaultAsyncHttpClient(), FetcherSettings()),
    TvmIntegrationTestStub(TvmService.MARKET_MBI_API_TEST)
) {
    override fun sendUrlFeedToMbi(urlFeedInfo: MarketClient.UrlFeedInfo, enableDirectPlacement: Boolean?): MarketClient.SendFeedResult {
        return getSendFeedResult(urlFeedInfo.clientId, urlFeedInfo.feedId)
    }

    override fun sendSiteFeedToMbi(siteFeedInfo: MarketClient.SiteFeedInfo, enableDirectPlacement: Boolean?): MarketClient.SendFeedResult {
        return getSendFeedResult(siteFeedInfo.clientId, siteFeedInfo.feedId)
    }

    override fun sendFileFeedToMbi(fileFeedInfo: MarketClient.FileFeedInfo, enableDirectPlacement: Boolean?): MarketClient.SendFeedResult {
        return getSendFeedResult(fileFeedInfo.clientId, fileFeedInfo.feedId)
    }

    override fun deleteSitePreviewsFromMBI(deleteSitePreviewInfo: List<MarketClient.SitePreviewIds>): List<MarketClient.SitePreviewIds> {
        return deleteSitePreviewInfo
    }

    override fun setFeedFeaturesToMbi(feedFeaturesInfo: MarketClient.FeedFeaturesInfo) {
        // do nothing
    }

    private fun getSendFeedResult(clientId: ClientId, feedId: Long): MarketClient.SendFeedResult {
        val shard = shardHelper.getShardByClientIdStrictly(clientId)
        val feed = feedRepository.getSimple(shard, listOf(feedId))
            .firstOrNull()

        return MarketClient.SendFeedResult(
            marketFeedId = feed?.marketFeedId ?: (feedId + 1000000),
            shopId = feed?.marketShopId ?: (feedId + 5000000),
            businessId = feed?.marketBusinessId ?: (clientId.asLong() + 1000000)
        )
    }
}
