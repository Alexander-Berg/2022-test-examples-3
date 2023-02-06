package ru.yandex.market.logistics.mqm.service.ytevents.generator.courier

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.yteventpayload.CourierDeliveryEventPayload
import ru.yandex.market.logistics.mqm.service.enums.YtEventType
import ru.yandex.market.logistics.mqm.service.ytevents.reader.CLIENT_RETURN_BARCODE_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.ORDER_EXTERNAL_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.ORDER_RECORD_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.RECORD_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.TABLE_TYPE_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.USER_SHIFT_RECORD_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.courier.YtCourierClientDeliveryReader
import ru.yandex.market.logistics.mqm.service.ytevents.reader.courier.YtCourierPickupPointDeliveryReader
import ru.yandex.market.logistics.mqm.service.ytevents.row.DynamicYtDto
import ru.yandex.market.logistics.mqm.utils.createYtRow

internal class YtCourierDeliveryEventGeneratorTest: AbstractTest() {
    private val generator = YtCourierDeliveryEventGenerator()

    @DisplayName("Успешная генерация события")
    @Test
    fun successGeneration() {
        val rowValues = mapOf<String, Any?>(
            RECORD_ID_COLUMN to 123L,
            USER_SHIFT_RECORD_ID_COLUMN to 456L,
            ORDER_RECORD_ID_COLUMN to 789L,
            ORDER_EXTERNAL_ID_COLUMN to "123456N",
            TABLE_TYPE_COLUMN to "task_order_delivery",
        )
        val ytRow = createYtRow(rowValues)
        val ytDto = DynamicYtDto(ytRow, YtCourierClientDeliveryReader::class)
        val event = generator.generateEvent(ytDto)!!

        val payload = event.payload as CourierDeliveryEventPayload
        assertSoftly { event.type shouldBe YtEventType.COURIER_DELIVERY }
        assertSoftly { event.eventTime shouldBe null }
        assertSoftly { event.uniqueKey shouldBe "COURIER_DELIVERY;123456N;456;" }
        assertSoftly { payload.externalId shouldBe "123456N" }
        assertSoftly { payload.recordId shouldBe 123L }
        assertSoftly { payload.userShiftId shouldBe 456L }
        assertSoftly { payload.orderId shouldBe 789L }
        assertSoftly { payload.sourceTable shouldBe "task_order_delivery" }
        assertSoftly { payload.clientReturnBarcode shouldBe null }
    }

    @DisplayName("Успешная генерация клиентского возврата")
    @Test
    fun successClientReturnGeneration() {
        val rowValues = mapOf<String, Any?>(
            RECORD_ID_COLUMN to 123L,
            USER_SHIFT_RECORD_ID_COLUMN to 456L,
            TABLE_TYPE_COLUMN to "subtask_locker_delivery",
            CLIENT_RETURN_BARCODE_COLUMN to "VOZVRAT_1",
        )
        val ytRow = createYtRow(rowValues)
        val ytDto = DynamicYtDto(ytRow, YtCourierPickupPointDeliveryReader::class)
        val event = generator.generateEvent(ytDto)!!

        val payload = event.payload as CourierDeliveryEventPayload
        assertSoftly { event.type shouldBe YtEventType.COURIER_DELIVERY }
        assertSoftly { event.eventTime shouldBe null }
        assertSoftly { event.uniqueKey shouldBe "COURIER_DELIVERY;VOZVRAT_1;456;" }
        assertSoftly { payload.externalId shouldBe null }
        assertSoftly { payload.recordId shouldBe 123L }
        assertSoftly { payload.userShiftId shouldBe 456L }
        assertSoftly { payload.orderId shouldBe null }
        assertSoftly { payload.sourceTable shouldBe "subtask_locker_delivery" }
        assertSoftly { payload.clientReturnBarcode shouldBe "VOZVRAT_1" }
    }
}
