package ru.yandex.direct.market.client.http

import com.google.common.base.Preconditions
import com.google.common.primitives.Ints.saturatedCast
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.netty.util.HashedWheelTimer
import org.assertj.core.api.Assertions
import org.asynchttpclient.DefaultAsyncHttpClient
import org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory
import ru.yandex.direct.config.DirectConfigFactory
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.env.EnvironmentType.TESTING
import ru.yandex.direct.market.client.MarketClient.Schema.HTTP
import ru.yandex.direct.market.client.MarketClient.SiteFeedInfo
import ru.yandex.direct.market.client.MarketClient.UrlFeedInfo
import ru.yandex.direct.market.client.http.MarketHttpClient.MarketConfiguration
import ru.yandex.direct.tvm.TvmIntegration
import ru.yandex.direct.tvm.TvmIntegrationImpl
import ru.yandex.direct.tvm.TvmService
import ru.yandex.direct.tvm.TvmService.MARKET_MBI_API_TEST
import java.time.Duration
import java.time.Duration.ofSeconds
import java.util.HashMap

@Ignore("Ходит во внешние сервисы, не должно выполняться на CI.")
class MarketHttpClientManualTest {

    lateinit var client: MarketHttpClient

    @Before
    fun setUp() {

        //TVM
        val tvmService = MARKET_MBI_API_TEST
        val conf: MutableMap<String, Any> = HashMap()
        conf["tvm.enabled"] = true

        //     conf["tvm.app_id"] = TvmService.DIRECT_DEVELOPER.id

        conf["tvm.app_id"] = TvmService.DIRECT_WEB_TEST.id
        conf["tvm.api.url"] = "https://tvm-api.yandex.net"
        conf["tvm.api.error_delay"] = "5s"
        conf["tvm.secret"] = "file://~/.direct-tokens/tvm2_direct-web-test"
        val directConfig = DirectConfigFactory.getConfig(TESTING, conf)
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.initialize()
        val tvmIntegration: TvmIntegration = TvmIntegrationImpl.create(directConfig, scheduler)
        val ticket = tvmIntegration.getTicket(tvmService)
        Preconditions.checkNotNull(ticket, "tvm-ticket is wrong")

        //MarketConfiguration
        val marketConfiguration = MarketConfiguration(tvmService, "http://mbi-back.tst.vs.market.yandex.net:34820")

        //AsyncHttpClient
        val builder = Builder()
        builder.setRequestTimeout(saturatedCast(ofSeconds(30).toMillis()))
        builder.setReadTimeout(saturatedCast(ofSeconds(30).toMillis()))
        builder.setConnectTimeout(saturatedCast(ofSeconds(10).toMillis()))
        builder.setConnectionTtl(saturatedCast(Duration.ofMinutes(1).toMillis()))
        builder.setPooledConnectionIdleTimeout(saturatedCast(ofSeconds(20).toMillis()))
        builder.setIoThreadsCount(2)
        val threadFactory = ThreadFactoryBuilder().setNameFormat("ahc-timer-%02d").setDaemon(true).build()
        builder.setNettyTimer(HashedWheelTimer(threadFactory))
        val asyncHttpClient = DefaultAsyncHttpClient(builder.build())

        //ParallelFetcherFactory
        val fetcherSettings = FetcherSettings()
        val parallelFetcherFactory = ParallelFetcherFactory(asyncHttpClient, fetcherSettings)
        client = MarketHttpClient(marketConfiguration, parallelFetcherFactory, tvmIntegration)
    }

    @Test
    fun sendUrlFeedToMbi() {
        val urlFeedInfo = UrlFeedInfo(ClientId.fromLong(30L), 10L, 20L, "http://partnet.ru/path/to/feed", null, null)
        val (marketFeedId, shopId, businessId) = client.sendUrlFeedToMbi(urlFeedInfo)
        Assertions.assertThat(marketFeedId).`as`("MarketFeedId").isPositive
        Assertions.assertThat(businessId).`as`("BusinessId").isPositive
        Assertions.assertThat(shopId).`as`("ShopId").isPositive
    }

    @Test
    fun sendSiteFeedToMbi() {
        val siteFeedInfo = SiteFeedInfo(ClientId.fromLong(11L), 13L, 111L, HTTP, "yandex.ru")
        val (marketFeedId, shopId, businessId) = client.sendSiteFeedToMbi(siteFeedInfo)
        Assertions.assertThat(marketFeedId).`as`("MarketFeedId").isPositive
        Assertions.assertThat(businessId).`as`("BusinessId").isPositive
        Assertions.assertThat(shopId).`as`("ShopId").isPositive
    }

}
