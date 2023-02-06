package ru.yandex.market.logistics.mqm.service.ytevents.generator.courier

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.yteventpayload.CourierShiftClosedEventPayload
import ru.yandex.market.logistics.mqm.service.enums.YtEventType
import ru.yandex.market.logistics.mqm.service.ytevents.reader.ORDER_EXTERNAL_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.ORDER_RECORD_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.RECORD_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.USER_EMAIL_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.USER_NAME_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.USER_PHONE_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.USER_SHIFT_RECORD_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.courier.YtCourierClientDeliveryReader
import ru.yandex.market.logistics.mqm.service.ytevents.row.DynamicYtDto
import ru.yandex.market.logistics.mqm.utils.createYtRow

internal class YtCourierShiftClosedGeneratorTest: AbstractTest() {
    private val generator = YtCourierShiftClosedGenerator()

    @DisplayName("Успешная генерация события")
    @Test
    fun successGeneration() {
        val rowValues = mapOf<String, Any?>(
            RECORD_ID_COLUMN to 123L,
            USER_SHIFT_RECORD_ID_COLUMN to 456L,
            ORDER_RECORD_ID_COLUMN to 789L,
            ORDER_EXTERNAL_ID_COLUMN to "123456",
            USER_NAME_COLUMN to "Курьер Доставочников",
            USER_EMAIL_COLUMN to "LeKo230@email.ru",
            USER_PHONE_COLUMN to "+79543212345",
        )
        val ytRow = createYtRow(rowValues)
        val ytDto = DynamicYtDto(ytRow, YtCourierClientDeliveryReader::class)
        val event = generator.generateEvent(ytDto)!!

        val payload = event.payload as CourierShiftClosedEventPayload
        assertSoftly { event.type shouldBe YtEventType.COURIER_SHIFT_CLOSED }
        assertSoftly { event.eventTime shouldBe null }
        assertSoftly { event.uniqueKey shouldBe "COURIER_SHIFT_CLOSED;456;" }
        assertSoftly { payload.userShiftId shouldBe 456L }
        assertSoftly { payload.shiftStartTime shouldBe null }
        assertSoftly { payload.courierName shouldBe "Курьер Доставочников" }
        assertSoftly { payload.courierEmail shouldBe "LeKo230@email.ru" }
        assertSoftly { payload.courierPhone shouldBe "+79543212345" }
    }
}
