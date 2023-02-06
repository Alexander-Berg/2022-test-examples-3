package ru.yandex.direct.market.client.http

import okhttp3.mockwebserver.MockResponse
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.market.client.MarketClient.FileFeedInfo
import ru.yandex.direct.market.client.MarketClient.Schema
import ru.yandex.direct.market.client.MarketClient.SendFeedResult
import ru.yandex.direct.market.client.MarketClient.SiteFeedInfo
import ru.yandex.direct.market.client.MarketClient.UrlFeedInfo
import ru.yandex.direct.tvm.TvmIntegration

class MarketHttpClientTest {

    companion object {
        private const val TVM_TICKET_BODY = "ticketBody"
    }

    @get:Rule
    var mockedMarket = MockedMarket()

    @get:Rule
    var exception: ExpectedException = ExpectedException.none()

    private lateinit var client: MarketHttpClient

    @Before
    fun setup() {
        val tvmIntegration = Mockito.mock(TvmIntegration::class.java)
        Mockito.`when`(tvmIntegration.isEnabled).thenReturn(true)
        Mockito.`when`(tvmIntegration.getTicket(ArgumentMatchers.any())).thenReturn(TVM_TICKET_BODY)
        client = mockedMarket.createClient(tvmIntegration)
    }

    @Test
    fun sendUrlFeedToMbi_success() {
        val clientId = 4L
        val chiefUid = 2L
        val feedId = 3L
        val feedUrl = "https://yandex.ru/"
        val marketFeedId = 777L
        val shopId = 888L
        val businessId = 999L
        val login = "login"
        val password = "password"

        val requestStr = "POST:/direct/feed/refresh:<add-direct-feed-request>" +
                "<uid>${chiefUid}</uid>" +
                "<direct-feed-id>${feedId}</direct-feed-id>" +
                "<client-id>${clientId}</client-id>" +
                "<feed-url>${feedUrl}</feed-url>" +
                "<feed-http-login>${login}</feed-http-login>" +
                "<feed-http-password>${password}</feed-http-password>" +
                "</add-direct-feed-request>"
        val responseBody = "<add-direct-feed-response>" +
                "<market-feed-id>${marketFeedId}</market-feed-id>" +
                "<partner-id>${shopId}</partner-id>" +
                "<business-id>${businessId}</business-id>" +
                "</add-direct-feed-response>"
        val response = MockResponse().addHeader("Content-Type", "application/json")
                .setBody(responseBody)
        mockedMarket.add(requestStr, response)

        val feedInfo = UrlFeedInfo(ClientId.fromLong(clientId), chiefUid, feedId, feedUrl, login, password)
        val result = client.sendUrlFeedToMbi(feedInfo)

        val expected = SendFeedResult(marketFeedId, shopId, businessId)
        assertThat(result, beanDiffer(expected))
    }

    @Test
    fun sendUrlFeedToMbi_withoutLogin_success() {
        val clientId = 4L
        val chiefUid = 2L
        val feedId = 3L
        val feedUrl = "https://yandex.ru/"
        val marketFeedId = 777L
        val shopId = 888L
        val businessId = 999L

        val requestStr = "POST:/direct/feed/refresh:<add-direct-feed-request>" +
                "<uid>${chiefUid}</uid>" +
                "<direct-feed-id>${feedId}</direct-feed-id>" +
                "<client-id>${clientId}</client-id>" +
                "<feed-url>${feedUrl}</feed-url>" +
                "</add-direct-feed-request>"
        val responseBody = "<add-direct-feed-response>" +
                "<market-feed-id>${marketFeedId}</market-feed-id>" +
                "<partner-id>${shopId}</partner-id>" +
                "<business-id>${businessId}</business-id>" +
                "</add-direct-feed-response>"
        val response = MockResponse().addHeader("Content-Type", "application/json")
                .setBody(responseBody)
        mockedMarket.add(requestStr, response)

        val feedInfo = UrlFeedInfo(ClientId.fromLong(clientId), chiefUid, feedId, feedUrl, null, null)
        val result = client.sendUrlFeedToMbi(feedInfo)

        val expected = SendFeedResult(marketFeedId, shopId, businessId)
        assertThat(result, beanDiffer(expected))
    }

    @Test
    fun sendSiteFeedToMbi_success() {
        val clientId = 4L
        val chiefUid = 2L
        val feedId = 3L
        val schema = Schema.HTTPS
        val domain = "yandex.ru"
        val path = "/"
        val marketFeedId = 777L
        val shopId = 888L
        val businessId = 999L

        val requestStr = "POST:/direct/site/refresh:<add-direct-site-request>" +
                "<uid>${chiefUid}</uid>" +
                "<direct-feed-id>${feedId}</direct-feed-id>" +
                "<client-id>${clientId}</client-id>" +
                "<schema>${schema.name.toLowerCase()}</schema>" +
                "<domain>${domain}</domain>" +
                "<path>${path}</path>" +
                "</add-direct-site-request>"
        val responseBody = "<add-direct-feed-response>" +
                "<market-feed-id>${marketFeedId}</market-feed-id>" +
                "<partner-id>${shopId}</partner-id>" +
                "<business-id>${businessId}</business-id>" +
                "</add-direct-feed-response>"
        val response = MockResponse().addHeader("Content-Type", "application/json")
                .setBody(responseBody)
        mockedMarket.add(requestStr, response)

        val feedInfo = SiteFeedInfo(ClientId.fromLong(clientId), chiefUid, feedId, schema, domain)
        val result = client.sendSiteFeedToMbi(feedInfo)

        val expected = SendFeedResult(marketFeedId, shopId, businessId)
        assertThat(result, beanDiffer(expected))
    }

    @Test
    fun sendFileFeedToMbi_success() {
        val clientId = 4L
        val chiefUid = 2L
        val feedId = 3L
        val feedMdsUrl = "https://mdst.yandex.net/fake_url/123"
        val marketFeedId = 777L
        val shopId = 888L
        val businessId = 999L
        val refresh = false

        val requestStr = "POST:/direct/file/refresh:<add-direct-file-request>" +
                "<uid>${chiefUid}</uid>" +
                "<direct-feed-id>${feedId}</direct-feed-id>" +
                "<client-id>${clientId}</client-id>" +
                "<feed-url>${feedMdsUrl}</feed-url>" +
                "<refresh>${refresh}</refresh>" +
                "</add-direct-file-request>"
        val responseBody = "<add-direct-feed-response>" +
                "<market-feed-id>${marketFeedId}</market-feed-id>" +
                "<partner-id>${shopId}</partner-id>" +
                "<business-id>${businessId}</business-id>" +
                "</add-direct-feed-response>"
        val response = MockResponse().addHeader("Content-Type", "application/json")
                .setBody(responseBody)
        mockedMarket.add(requestStr, response)

        val feedInfo = FileFeedInfo(ClientId.fromLong(clientId), chiefUid, feedId, feedMdsUrl, refresh)
        val result = client.sendFileFeedToMbi(feedInfo)

        val expected = SendFeedResult(marketFeedId, shopId, businessId)
        assertThat(result, beanDiffer(expected))
    }

}
