package ru.yandex.market.mbi.orderservice.api.controller.summary

import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.helpers.defaultTestMapper
import ru.yandex.market.mbi.helpers.loadResourceAsString
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.api.assertBody
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderSummaryExtended
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderSummaryExtendedRepository
import ru.yandex.market.mbi.orderservice.model.CommonApiErrorResponse
import ru.yandex.market.mbi.orderservice.model.SummaryPeriod
import java.time.Clock
import java.time.Instant

/**
 * Тесты для [BusinessSummaryController]
 */
@DbUnitDataSet(before = ["BusinessSummaryControllerTest.csv"])
class BusinessSummaryControllerTest : FunctionalTest() {
    companion object {
        const val orderSummaryExtendedResourceName = "business_order_summary_extended.json"
    }

    @Autowired
    lateinit var clock: Clock

    @Autowired
    lateinit var orderSummaryExtendedRepository: OrderSummaryExtendedRepository

    @BeforeAll
    fun init() {
        val orderSummariesExtended =
            this::class.loadTestEntities<OrderSummaryExtended>(orderSummaryExtendedResourceName)
                .sortedBy { it.key.partnerId }

        orderSummaryExtendedRepository.insertRows(orderSummariesExtended, null)
    }

    @Test
    fun `verify that missing required parameter returns http code 400`() {
        val request = HttpGet(getUri("business/0/summary/orders/count", emptyMap()))
        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(400)
        assertThat(
            defaultTestMapper.readValue<CommonApiErrorResponse>(IOUtils.toString(response.entity.content)).errors
        ).first()
            .extracting("message")
            .isEqualTo("Required request parameter 'period' for method parameter type SummaryPeriod is not present")
    }

    @Test
    fun `verify that invalid businessId returns http code 400`() {
        val request = HttpGet(
            getUri(
                "business/0/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.WEEK.value,
                    "fromTime" to "2022-01-14T10:00:00Z"
                )
            )
        )
        val response = HttpClientBuilder.create().build().execute(request)

        assertThat(response.statusLine.statusCode).isEqualTo(400)
        assertThat(
            defaultTestMapper.readValue<CommonApiErrorResponse>(IOUtils.toString(response.entity.content)).errors
        ).first()
            .extracting("message")
            .isEqualTo("getDeliveredOrderCountForBusinessSummary.businessId: must be greater than or equal to 1")
    }

    @Test
    fun `verify that no error is thrown if no partners are found in business`() {
        val request = HttpGet(
            getUri(
                "business/1/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.WEEK.value,
                    "fromTime" to "2022-01-14T10:00:00Z"
                )
            )
        )
        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(200)
        assertBody(
            response,
            // language=json
            """
                {
                 "total": 0,
                 "change": 0
                }
            """.trimIndent()
        )
    }

    @DisplayName("Тест на получение количества заказов по бизнесу с таймзоной")
    @Test
    fun `test get orders count by business zoned`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-15T10:00:00Z"))
        val request1 = HttpGet(
            getUri(
                "business/223/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.WEEK.value, "fromTime" to "2022-02-09T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request1, 42, "1300")

        val request2 = HttpGet(
            getUri(
                "business/223/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.WEEK.value, "fromTime" to "2022-02-09T00:00:00+12:00"
                )
            )
        )
        callAndCheck(request2, 43, "2050")
    }

    @DisplayName("Тест на получение количества заказов по партнеру по которым нет данных за предыдущий период с таймзоной")
    @Test
    fun `test get orders count by business without history zoned`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-15T10:00:00Z"))
        val request1 = HttpGet(
            getUri(
                "business/223/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.MONTH.value, "fromTime" to "2022-02-01T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request1, 45, "100")
    }

    @DisplayName("Тест на получение количества заказов по таймзоне другой день")
    @Test
    fun `test get orders count by business zoned another day`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-15T10:00:00Z"))
        val request1 = HttpGet(
            getUri(
                "business/223/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.WEEK.value, "fromTime" to "2022-02-09T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request1, 42, "1300")

        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-14T23:00:00Z"))
        val request2 = HttpGet(
            getUri(
                "business/223/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.WEEK.value, "fromTime" to "2022-02-09T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request2, 42, "1300")
    }

    @DisplayName("Тест на получение количества заказов по таймзоне другой месяц")
    @Test
    fun `test get orders count by business zoned another month`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-03-01T10:00:00Z"))
        val request1 = HttpGet(
            getUri(
                "business/223/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.PREV_MONTH.value, "fromTime" to "2022-02-01T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request1, 45, "4400")

        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-28T22:00:00Z"))
        val request2 = HttpGet(
            getUri(
                "business/223/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.PREV_MONTH.value, "fromTime" to "2022-02-01T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request2, 45, "4400")
    }

    private fun callAndCheck(request: HttpGet, total: Int, change: String) {
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        JSONAssert.assertEquals(
            "{\"result\":{\"total\":$total,\"change\":$change}}",
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    @DisplayName("Тест на получение статистики по заказам по бизнесу с таймзоной")
    @Test
    fun `test get orders summary by business zoned`() {
        val request = HttpGet(
            getUri(
                "business/223/summary/orders/by-date", mapOf(
                    "fromTime" to "2022-02-01T00:00:00+03:00", "toTime" to "2022-02-16T03:00:00+03:00"
                )
            )
        )
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("business_summary_by_day_zoned_expected.json"),
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    @DisplayName("Тест на получение статистики по заказам по бизнесу с получасовойтаймзоной")
    @Test
    fun `test get orders summary by business zoned half hour`() {
        val request = HttpGet(
            getUri(
                "business/223/summary/orders/by-date", mapOf(
                    "fromTime" to "2022-02-01T00:00:00+03:30", "toTime" to "2022-02-16T03:00:00+03:30"
                )
            )
        )
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("business_summary_by_day_zoned_expected.json"),
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    @DisplayName("Тест на получение статистики по проблемным заказам по бизнесу с таймзоной")
    @Test
    fun `test get problems orders summary by business zoned`() {
        val request = HttpGet(
            getUri(
                "business/223/summary/orders/problems/by-date", mapOf(
                    "fromTime" to "2022-02-01T00:00:00+03:00", "toTime" to "2022-02-16T03:00:00+03:00"
                )
            )
        )
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        val toString = IOUtils.toString(response.entity.content)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("business_problem_summary_by_day_zoned_expected.json"),
            toString,
            JSONCompareMode.STRICT_ORDER
        )
    }

    @DisplayName("Тест на получение статистики по проблемным заказам по бизнесу с получасовойт аймзоной")
    @Test
    fun `test get problems orders summary by business half hour zoned`() {
        val request = HttpGet(
            getUri(
                "business/223/summary/orders/problems/by-date", mapOf(
                    "fromTime" to "2022-02-01T00:00:00+03:30", "toTime" to "2022-02-16T03:00:00+03:00"
                )
            )
        )
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        val toString = IOUtils.toString(response.entity.content)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("business_problem_summary_by_day_zoned_expected.json"),
            toString,
            JSONCompareMode.STRICT_ORDER
        )
    }
}
