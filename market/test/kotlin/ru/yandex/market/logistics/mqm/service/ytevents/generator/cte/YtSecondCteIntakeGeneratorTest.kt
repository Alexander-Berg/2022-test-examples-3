package ru.yandex.market.logistics.mqm.service.ytevents.generator.cte

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.entity.yteventpayload.SecondCteIntakeYtEventPayload
import ru.yandex.market.logistics.mqm.service.enums.YtEventType
import ru.yandex.market.logistics.mqm.service.ytevents.reader.cte.SecondCteIntakeReader
import ru.yandex.market.logistics.mqm.service.ytevents.row.SecondCteIntakeYtDto
import java.time.Instant

internal class YtSecondCteIntakeGeneratorTest {
    private val generator = YtSecondCteIntakeGenerator()

    @DisplayName("Успешная генерация события")
    @Test
    fun successGeneration() {
        val ytDto = SecondCteIntakeYtDto(
            id = 123,
            orderId = "456",
            intakeTime = INTAKE_TIME,
            warehouseId = 172L,
            reader = SecondCteIntakeReader::class
        )
        val event = generator.generateEvent(ytDto)!!

        val payload = event.payload as SecondCteIntakeYtEventPayload
        assertSoftly { event.type shouldBe YtEventType.SECOND_CTE_INTAKE }
        assertSoftly { event.eventTime shouldBe INTAKE_TIME }
        assertSoftly { event.uniqueKey shouldBe "SECOND_CTE_INTAKE;456;" }
        assertSoftly { payload.recordId shouldBe 123L }
        assertSoftly { payload.orderId shouldBe "456" }
        assertSoftly { payload.warehouseId shouldBe 172L }
    }

    companion object {
        private val INTAKE_TIME = Instant.parse("2021-12-08T18:00:00.00Z")
    }
}
