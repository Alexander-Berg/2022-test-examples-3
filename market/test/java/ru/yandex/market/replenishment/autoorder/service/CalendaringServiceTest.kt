package ru.yandex.market.replenishment.autoorder.service

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import ru.yandex.market.logistics.calendaring.client.dto.DailyQuotaInfoResponse
import ru.yandex.market.logistics.calendaring.client.dto.IntervalQuotaInfoResponse
import ru.yandex.market.logistics.calendaring.client.dto.QuotaInfoResponse
import ru.yandex.market.logistics.calendaring.client.dto.enums.CalendaringType
import ru.yandex.market.logistics.calendaring.client.dto.enums.QuotaType
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.config.security.tvm.TvmService
import ru.yandex.market.replenishment.autoorder.exception.BadRequestException
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@WithMockLogin("boris")
open class CalendaringServiceTest: FunctionalTest() {

    @Value("\${calendaring-service.tvm-service-id}")
    var tvmServiceId: Int = 0

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var tvmService: TvmService

    lateinit var service: CalendaringService
    lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        service = CalendaringService(
            tvmServiceId,
            mockWebServer.url("/").toString(),
            objectMapper,
            tvmService
        )
    }

    @Test
    open fun testGetDailyQuotaSuccess() {
        mockWebServer.enqueue(MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .throttleBody(64, 5, TimeUnit.MILLISECONDS)
            .setBody(objectMapper.writeValueAsString(IntervalQuotaInfoResponse(
                listOf(
                    DailyQuotaInfoResponse(
                        type = QuotaType.SUPPLY,
                        date = LocalDate.now(),
                        firstPartyQuotas = QuotaInfoResponse(1L, 1L, 1L, 1L),
                        thirdPartyQuotas = QuotaInfoResponse(1L, 1L, 1L, 1L),
                        movementQuotas = null,
                        xDockQuotas = null
                    )
                )
            )))
            .setResponseCode(200)
        )
        assertNotNull(service.getDailyQuota(CalendaringType.INBOUND, 147L, LocalDate.now(), LocalDate.now()))
    }

    @Test
    open fun testGetDailyQuotaFail() {
        mockWebServer.enqueue(MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .throttleBody(64, 5, TimeUnit.MILLISECONDS)
            .setResponseCode(404)
            .setBody("error")
        )
        assertThrows(
            BadRequestException::class.java
        ) { service.getDailyQuota(CalendaringType.INBOUND, 147L, LocalDate.now(), LocalDate.now()) }
    }
}
