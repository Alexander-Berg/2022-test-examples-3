package ru.yandex.market.mbi.orderservice.api.controller.summary

import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import ru.yandex.market.mbi.helpers.loadResourceAsString
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.api.assertBody
import ru.yandex.market.mbi.orderservice.common.model.yt.summary.DailyDbsExpirations
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.DefaultYtCrudRepository
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.TableBindingHolder
import ru.yandex.market.mbi.orderservice.common.util.DEFAULT_TIMEZONE_ZONE_ID
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource
import java.time.Clock
import java.time.Instant

class DailyDbsExpirationsControllerTest : FunctionalTest() {
    companion object {
        const val dailyDbsExpirationsResourceName = "daily_dbs_expirations.json"
    }

    @Autowired
    lateinit var readOnlyClient: YtClientProxySource

    @Autowired
    lateinit var readWriteClient: YtClientProxy

    @Autowired
    lateinit var tableBindingHolder: TableBindingHolder

    @Autowired
    lateinit var clock: Clock

    @BeforeAll
    fun init() {
        val repository = DefaultYtCrudRepository(
            tableBindingHolder,
            Long::class.java,
            DailyDbsExpirations::class.java,
            readWriteClient,
            readOnlyClient
        )

        val expirations = this::class.loadTestEntities<DailyDbsExpirations>(dailyDbsExpirationsResourceName)
            .sortedBy { it.partnerId }

        repository.insertRows(expirations, null)
    }

    @DisplayName("Проверка получения информации по истекающим срокам заказов")
    @Test
    fun `test get dbs expirations`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-04-05T10:00:00Z"))
        whenever(clock.zone).thenReturn(DEFAULT_TIMEZONE_ZONE_ID)

        val response = performGetRequest(0, 10)

        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        assertBody(response, this::class.loadResourceAsString("expected_daily_dbs_expirations.json"))
    }

    @DisplayName("Проверка получения информации по истекающим срокам заказов по пейджированию")
    @Test
    fun `test get paged dbs expirations`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-04-05T10:00:00Z"))
        whenever(clock.zone).thenReturn(DEFAULT_TIMEZONE_ZONE_ID)

        val page0Response = performGetRequest(0, 4)

        Assertions.assertThat(page0Response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        assertBody(
            page0Response,
            this::class.loadResourceAsString("expected_daily_dbs_expirations_page_0.json")
        )

        val page1Response = performGetRequest(1, 4)

        Assertions.assertThat(page1Response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        assertBody(
            page1Response,
            this::class.loadResourceAsString("expected_daily_dbs_expirations_page_1.json")
        )

        val page2Response = performGetRequest(2, 4)

        Assertions.assertThat(page2Response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        assertBody(
            page2Response,
            this::class.loadResourceAsString("expected_daily_dbs_expirations_empty_page.json")
        )
    }

    @DisplayName("Проверка получения количества партнеров, у которых истекают статусы заказов")
    @Test
    fun `test get partners total count`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-04-05T10:00:00Z"))
        whenever(clock.zone).thenReturn(DEFAULT_TIMEZONE_ZONE_ID)

        val response = performGetCountRequest()

        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("expected_partners_count_response.json"),
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    private fun performGetRequest(page: Int, pageSize: Int): HttpResponse {
        val request = HttpGet(
            getUri(
                "/partners/summary/dbs-expirations",
                mapOf(
                    "page" to page.toString(),
                    "size" to pageSize.toString()
                )
            )
        )

        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return HttpClientBuilder.create().build().execute(request)
    }

    private fun performGetCountRequest(): HttpResponse {
        val request = HttpGet(
            getUri(
                "/partners/summary/dbs-expirations/count",
                emptyMap()
            )
        )

        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return HttpClientBuilder.create().build().execute(request)
    }
}
