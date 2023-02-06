package ru.yandex.market.mbi.orderservice.common.service.external

import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.retry.support.RetryTemplate
import ru.yandex.common.geocoder.client.GeoClientBuilder
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryOptionAddress
import ru.yandex.market.mbi.orderservice.common.util.defaultObjectMapper
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor
import ru.yandex.market.request.trace.Module
import ru.yandex.passport.tvmauth.NativeTvmClient
import ru.yandex.passport.tvmauth.TvmApiSettings
import java.util.Optional

/**
 * Интеграционный тест для проверки Ваших запросов к геокодеру.
 */
@Disabled
class GeocoderClientIntegrationTest {

    @Autowired
    lateinit var environment: Environment
    private lateinit var geocoderApiService: GeocoderApiService

    @Autowired
    lateinit var retryTemplate: RetryTemplate

    @BeforeEach
    fun init() {
        val tvmClient = NativeTvmClient.create(
            TvmApiSettings.create()
                .setSelfTvmId(2029927)
                .enableServiceTicketsFetchOptions(
                    "order-service-tvm",
                    listOf(2008261).toIntArray()
                )
        )

        geocoderApiService = GeocoderApiService(
            GeoClientBuilder.newBuilder()
                .withHttpClient(
                    HttpClientBuilder.create()
                        .setDefaultRequestConfig(
                            RequestConfig.custom()
                                // Millis
                                .setConnectTimeout(1000)
                                .setSocketTimeout(1000)
                                .build()
                        )
                        .setRetryHandler(DefaultHttpRequestRetryHandler(3, false))
                        .addInterceptorFirst(TraceHttpRequestInterceptor(Module.GEOSEARCH))
                        .addInterceptorFirst(TraceHttpResponseInterceptor())
                        .build()
                )
                .withTvmTicketProvider {
                    Optional.ofNullable(tvmClient.getServiceTicketFor(2008261))
                }
                .withApiBaseUrl("http://addrs-testing.search.yandex.net/search/stable/yandsearch")
                .withIsCached(false)
                .build(),
            "mbi-order-service.tst.vs.market.yandex.net"
        )
    }

    @Test
    fun getGeoObject() {
        val result = geocoderApiService.getDeliveryAddresses(
            DeliveryOptionAddress(
                city = "Москва",
                street = "Измайловский проспект",
                house = "73/2"
            )
        )
        println(defaultObjectMapper.writeValueAsString(result))
    }
}
