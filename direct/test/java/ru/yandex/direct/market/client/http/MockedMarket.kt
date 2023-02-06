package ru.yandex.direct.market.client.http

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.asynchttpclient.DefaultAsyncHttpClient
import org.junit.rules.ExternalResource
import org.slf4j.LoggerFactory
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory
import ru.yandex.direct.market.client.http.MarketHttpClient.MarketConfiguration
import ru.yandex.direct.tvm.TvmIntegration
import ru.yandex.direct.tvm.TvmService

class MockedMarket : ExternalResource() {

    companion object {
        private val logger = LoggerFactory.getLogger(MockedMarket::class.java)
    }

    private lateinit var server: MockWebServer

    private val responseByRequest: MutableMap<String, MockResponse> = HashMap()

    fun add(request: String, response: MockResponse) {
        responseByRequest[request] = response
    }

    override fun before() {
        server = MockWebServer()
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                var req = "${request.method}:${request.path}"
                val body = request.body.readUtf8()
                if (body.isNotEmpty()) {
                    req = "$req:$body"
                }
                val response = responseByRequest[req]
                if (response != null) {
                    return response
                }
                logger.error("UNEXPECTED REQUEST: {}", req)
                return MockResponse().setResponseCode(404).setBody("Request not supported")
            }
        })
        server.start()
    }

    override fun after() {
        server.shutdown()
    }

    fun createClient(tvmIntegration: TvmIntegration?): MarketHttpClient {
        val tvmService = TvmService.MARKET_MBI_API_TEST
        val marketApiUrl = server.url("/").toString()
        val configuration = MarketConfiguration(tvmService, marketApiUrl)
        val parallelFetcherFactory = ParallelFetcherFactory(DefaultAsyncHttpClient(), FetcherSettings())
        return MarketHttpClient(configuration, parallelFetcherFactory, tvmIntegration!!)
    }

}
