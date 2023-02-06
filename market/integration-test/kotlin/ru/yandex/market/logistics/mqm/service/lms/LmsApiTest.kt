package ru.yandex.market.logistics.mqm.service.lms

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import java.time.LocalTime
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason
import ru.yandex.market.logistics.mqm.AbstractContextualTest

class LmsApiTest: AbstractContextualTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @DisplayName("Проверка корректности обработки ответа с PartnerResponse")
    @Test
    fun parsePartnerResponse() {
        val expectedResponse = PartnerResponse.newBuilder()
            .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
            .intakeSchedule(
                listOf(
                    ScheduleDayResponse(
                        1L,
                        2,
                        LocalTime.of(10, 15, 30),
                        LocalTime.of(10, 15, 31),
                        true,
                    ),
                )
            )
            .build()
        val actualResponse = objectMapper.readValue(
            ClassPathResource("/service/lms/partner_response.json").inputStream,
            PartnerResponse::class.java
        )

        actualResponse shouldBe expectedResponse
    }
}
