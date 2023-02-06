package ru.yandex.market.logistics.calendaring.dbqueue.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.ff.client.dto.quota.TakeQuotaResponseDto
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.payload.UpdateQuotaBookingIdPayload
import java.time.LocalDate

class UpdateQuotaBookingIdProcessingServiceTest(
    @Autowired private val service: UpdateQuotaBookingIdProcessingService
) : AbstractContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/dbqueue/service/update-booking-id/before.xml")
    @ExpectedDatabase(
        "classpath:fixtures/dbqueue/service/update-booking-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun executeTest() {
        val quotaDate = LocalDate.of(2021, 5, 18)
        Mockito.`when`(ffwfClientApi!!.updateBookingId(eq(100), eq(101))).thenReturn(
            TakeQuotaResponseDto(quotaDate)
        )
        service.processPayload(
            UpdateQuotaBookingIdPayload(
                oldBookingId = 100,
                newBookingId = 101
            )
        )
    }

}
