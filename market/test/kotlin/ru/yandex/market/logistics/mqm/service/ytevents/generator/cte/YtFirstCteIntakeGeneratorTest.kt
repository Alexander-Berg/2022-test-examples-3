package ru.yandex.market.logistics.mqm.service.ytevents.generator.cte

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.entity.yteventpayload.FirstCteIntakeYtEventPayload
import ru.yandex.market.logistics.mqm.service.enums.YtEventType
import ru.yandex.market.logistics.mqm.service.ytevents.reader.cte.FirstCteIntakeReader
import ru.yandex.market.logistics.mqm.service.ytevents.row.FirstCteIntakeYtDto
import java.time.Instant

internal class YtFirstCteIntakeGeneratorTest {
    private val generator = YtFirstCteIntakeGenerator()
    @DisplayName("Успешная генерация события")
    @Test
    fun successGeneration() {
        val ytDto = FirstCteIntakeYtDto(
            id = 123,
            orderId = "456",
            finishTime = FINISH_TIME,
            reader = FirstCteIntakeReader::class
        )
        val event = generator.generateEvent(ytDto)!!

        val payload = event.payload as FirstCteIntakeYtEventPayload
        assertSoftly { event.type shouldBe YtEventType.FIRST_CTE_INTAKE }
        assertSoftly { event.eventTime shouldBe FINISH_TIME }
        assertSoftly { event.uniqueKey shouldBe "FIRST_CTE_INTAKE;456;" }
        assertSoftly { payload.recordId shouldBe 123L }
        assertSoftly { payload.orderId shouldBe "456" }
    }

    companion object {
        private val FINISH_TIME = Instant.parse("2021-12-08T18:00:00.00Z")
    }
}
