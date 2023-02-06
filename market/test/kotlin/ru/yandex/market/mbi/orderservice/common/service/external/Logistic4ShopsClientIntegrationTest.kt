package ru.yandex.market.mbi.orderservice.common.service.external

import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.retry.support.RetryTemplate
import org.springframework.web.client.RestTemplate
import ru.yandex.market.logistics.logistics4shops.ApiClient
import ru.yandex.market.logistics.logistics4shops.api.OrderItemsRemovalApi
import ru.yandex.market.mbi.orderservice.common.config.external.Logistics4ShopsConfig
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor
import ru.yandex.market.request.trace.Module
import ru.yandex.passport.tvmauth.NativeTvmClient
import ru.yandex.passport.tvmauth.TvmApiSettings

@Disabled
class Logistic4ShopsClientIntegrationTest {

    @Autowired
    lateinit var environment: Environment
    private lateinit var logistics4ShopsApiService: Logistics4ShopsApiService

    @BeforeEach
    fun init() {
        val tvmClient = NativeTvmClient.create(
            TvmApiSettings.create()
                .setSelfTvmId(2029927)
                .enableServiceTicketsFetchOptions(
                    "order-service-tvm",
                    listOf(2031819).toIntArray()
                )
        )
        val tvmInterceptor = ClientHttpRequestInterceptor { request, body, execution ->
            request.headers.add(
                Logistics4ShopsConfig.SERVICE_TICKET_HEADER,
                tvmClient.getServiceTicketFor(2031819)
            )
            execution.execute(request, body)
        }
        val httpClient = HttpClientBuilder.create()
            .useSystemProperties()
            .addInterceptorFirst(TraceHttpRequestInterceptor(Module.MARKET_LOGISTICS4SHOPS))
            .build()

        val factory = HttpComponentsClientHttpRequestFactory()
        factory.httpClient = httpClient

        val restTemplate = RestTemplate()
        restTemplate.requestFactory = factory
        restTemplate.interceptors = listOf(tvmInterceptor)

        val apiClient = ApiClient(restTemplate)
        apiClient.basePath = "http://logistics4shops.tst.vs.market.yandex.net"

        logistics4ShopsApiService = Logistics4ShopsApiService(
            OrderItemsRemovalApi(apiClient), RetryTemplate()
        )
    }

    @Test
    fun validatePartnerId() {
        println(logistics4ShopsApiService.validateOrderItemsRemoval(172))
    }
}
