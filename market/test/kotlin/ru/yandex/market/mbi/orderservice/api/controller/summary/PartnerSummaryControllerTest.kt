package ru.yandex.market.mbi.orderservice.api.controller.summary

import org.apache.commons.io.IOUtils
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
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.helpers.loadResourceAsString
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderSummaryExtended
import ru.yandex.market.mbi.orderservice.common.model.yt.summary.OrderByRegionsSummary
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderByRegionsSummaryRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderSummaryExtendedRepository
import ru.yandex.market.mbi.orderservice.model.SummaryPeriod
import java.time.Clock
import java.time.Instant

/**
 * Тесты для [PartnerSummaryController]
 */
@DbUnitDataSet(before = ["PartnerSummaryControllerTest.csv"])
class PartnerSummaryControllerTest : FunctionalTest() {
    companion object {
        const val orderSummaryExtendedResourceName = "partner_order_summary_extended.json"
        const val orderByRegionsSummaryResourceName = "partner_order_by_regions_summary.json"
    }

    @Autowired
    lateinit var clock: Clock

    @Autowired
    lateinit var orderSummaryExtendedRepository: OrderSummaryExtendedRepository

    @Autowired
    lateinit var orderByRegionsSummaryRepository: OrderByRegionsSummaryRepository

    @BeforeAll
    fun init() {
        val orderSummariesExtended =
            this::class.loadTestEntities<OrderSummaryExtended>(orderSummaryExtendedResourceName)
                .sortedBy { it.key.partnerId }

        orderSummaryExtendedRepository.insertRows(orderSummariesExtended, null)

        val orderByRegionsSummaries =
            this::class.loadTestEntities<OrderByRegionsSummary>(orderByRegionsSummaryResourceName)
                .sortedBy { it.key.partnerId }

        orderByRegionsSummaryRepository.insertRows(orderByRegionsSummaries, null)
    }

    @DisplayName("Тест на получение количества заказов по партнеру с таймзоной")
    @Test
    fun `test get orders count by partner zoned`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-15T10:00:00Z"))
        val request1 = HttpGet(
            getUri(
                "partners/1130852/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.WEEK.value,
                    "fromTime" to "2022-02-09T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request1, 32, "966.7")

        val request2 = HttpGet(
            getUri(
                "partners/1130852/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.WEEK.value,
                    "fromTime" to "2022-02-09T00:00:00+12:00"
                )
            )
        )
        callAndCheck(request2, 33, "1550")
    }

    @DisplayName("Тест на получение количества заказов по партнеру по которым нет данных")
    @Test
    fun `test get orders count by partner no data`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-01-14T10:00:00Z"))
        val request = HttpGet(
            getUri(
                "partners/46/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.MONTH.value,
                    "fromTime" to "2022-01-14T10:00:00Z"
                )
            )
        )
        callAndCheck(request, 0, "0")
    }

    @DisplayName("Тест на получение количества заказов по партнеру по которым нет данных с таймзоной")
    @Test
    fun `test get orders count by partner no data zoned`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-15T10:00:00Z"))
        val request1 = HttpGet(
            getUri(
                "partners/23/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.WEEK.value,
                    "fromTime" to "2022-02-09T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request1, 0, "0")
    }

    @DisplayName("Тест на получение количества заказов по партнеру по которым нет данных за предыдущий период с таймзоной")
    @Test
    fun `test get orders count by partner without history zoned`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-15T10:00:00Z"))
        val request1 = HttpGet(
            getUri(
                "partners/1130852/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.MONTH.value,
                    "fromTime" to "2022-02-01T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request1, 35, "100")
    }

    @DisplayName("Тест на получение количества заказов по таймзоне другой день")
    @Test
    fun `test get orders count by partner zoned another day`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-15T10:00:00Z"))
        val request1 = HttpGet(
            getUri(
                "partners/1130852/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.WEEK.value,
                    "fromTime" to "2022-02-09T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request1, 32, "966.7")

        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-14T23:00:00Z"))
        val request2 = HttpGet(
            getUri(
                "partners/1130852/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.WEEK.value,
                    "fromTime" to "2022-02-09T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request2, 32, "966.7")
    }

    @DisplayName("Тест на получение количества заказов по таймзоне другой месяц")
    @Test
    fun `test get orders count by partner zoned another month`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-03-01T10:00:00Z"))
        val request1 = HttpGet(
            getUri(
                "partners/1130852/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.PREV_MONTH.value,
                    "fromTime" to "2022-02-01T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request1, 35, "3400")

        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-28T22:00:00Z"))
        val request2 = HttpGet(
            getUri(
                "partners/1130852/summary/orders/count", mapOf(
                    "period" to SummaryPeriod.PREV_MONTH.value,
                    "fromTime" to "2022-02-01T00:00:00+03:00"
                )
            )
        )
        callAndCheck(request2, 35, "3400")
    }

    private fun callAndCheck(request: HttpGet, total: Int, change: String) {
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        JSONAssert.assertEquals(
            "{\"result\":{\"total\":$total,\"change\":$change}}",
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    @DisplayName("Тест на получение статистики по заказам по партнеру с таймзоной")
    @Test
    fun `test get orders summary by partner zoned`() {
        val request = HttpGet(
            getUri(
                "partners/1130852/summary/orders/by-date", mapOf(
                    "fromTime" to "2022-02-01T00:00:00+03:00",
                    "toTime" to "2022-02-16T03:00:00+03:00"
                )
            )
        )
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("partner_summary_by_day_zoned_expected.json"),
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    @DisplayName("Тест на получение статистики по заказам по партнеру с получасовойтаймзоной")
    @Test
    fun `test get orders summary by partner zoned half hour timezone`() {
        val request = HttpGet(
            getUri(
                "partners/1130852/summary/orders/by-date", mapOf(
                    "fromTime" to "2022-02-01T00:00:00+03:30",
                    "toTime" to "2022-02-16T03:00:00+03:30"
                )
            )
        )
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("partner_summary_by_day_zoned_expected.json"),
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    @DisplayName("Тест на получение статистики по проблемным заказам по партнеру с таймзоной")
    @Test
    fun `test get problem orders summary by partner zoned`() {
        val request = HttpGet(
            getUri(
                "partners/1130852/summary/orders/problems/by-date", mapOf(
                    "fromTime" to "2022-02-01T00:00:00+03:00",
                    "toTime" to "2022-02-16T03:00:00+03:00"
                )
            )
        )
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        val toString = IOUtils.toString(response.entity.content)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("partner_problem_summary_by_day_zoned_expected.json"),
            toString,
            JSONCompareMode.STRICT_ORDER
        )
    }

    @DisplayName("Тест на получение статистики по проблемным заказам по партнеру с получасовой таймзоной")
    @Test
    fun `test get problem orders summary by partner zoned half hout`() {
        val request = HttpGet(
            getUri(
                "partners/1130852/summary/orders/problems/by-date", mapOf(
                    "fromTime" to "2022-02-01T00:00:00+03:30",
                    "toTime" to "2022-02-16T03:00:00+03:00"
                )
            )
        )
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        val toString = IOUtils.toString(response.entity.content)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("partner_problem_summary_by_day_zoned_expected.json"),
            toString,
            JSONCompareMode.STRICT_ORDER
        )
    }

    @DisplayName("Тест на получение процента по заказам по партнеру по ФО если нет данных")
    @Test
    fun `test get orders by regions for partner summary empty`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2021-03-10T10:00:00Z"))
        val request = HttpGet(
            getUri(
                "partners/465221/summary/orders/regions", listOf()
            )
        )
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("partners_summary_orders_region_empty.json"),
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    @DisplayName("Тест на получение процента по заказам по партнеру по ФО")
    @Test
    fun `test get orders by regions for partner summary`() {
        whenever(clock.instant()).thenReturn(Instant.parse("2022-02-10T10:00:00Z"))
        val request = HttpGet(
            getUri(
                "partners/465221/summary/orders/regions", listOf()
            )
        )
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        val toString = IOUtils.toString(response.entity.content)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("partners_summary_orders_region.json"),
            toString,
            JSONCompareMode.STRICT_ORDER
        )
    }
}
