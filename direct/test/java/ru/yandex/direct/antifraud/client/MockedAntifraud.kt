package ru.yandex.direct.antifraud.client

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.asynchttpclient.DefaultAsyncHttpClient
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.LoggerFactory
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory
import ru.yandex.direct.tvm.TvmIntegration

class MockedAntifraud : BeforeAllCallback, AfterAllCallback {
    private lateinit var server: MockWebServer

    private val responseByRequest: MutableMap<String, MockResponse> = HashMap()

    fun add(request: String, response: MockResponse) {
        responseByRequest[request] = response
    }

    override fun beforeAll(context: ExtensionContext?) {
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

    override fun afterAll(context: ExtensionContext?) {
        server.shutdown()
    }

    fun createClient(tvmIntegration: TvmIntegration): AntifraudClient {
        val fetcherFactory = ParallelFetcherFactory(DefaultAsyncHttpClient(), FetcherSettings())
        return AntifraudClient(fetcherFactory, tvmIntegration, server.url("/").toString(), false)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MockedAntifraud::class.java)
    }
}
