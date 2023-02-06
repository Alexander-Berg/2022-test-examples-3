package ru.yandex.market.logistics.mqm.service.ytevents.generator.sc

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.yteventpayload.ReturnScIntakeYtEventPayload
import ru.yandex.market.logistics.mqm.service.enums.YtEventType.RETURN_SC_INTAKE
import ru.yandex.market.logistics.mqm.service.ytevents.reader.EVENT_TIME_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.EXTERNAL_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.FAKE_ORDER_TYPE_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.PARTNER_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.PARTNER_NAME_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.RECORD_ID_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.STATUS_COLUMN
import ru.yandex.market.logistics.mqm.service.ytevents.reader.sc.YtScReader
import ru.yandex.market.logistics.mqm.service.ytevents.row.DynamicYtDto
import ru.yandex.market.logistics.mqm.utils.createYtRow

class YtScReturnIntakeEventGeneratorTest: AbstractTest() {
    private val generator = YtScReturnIntakeEventGenerator()

    @DisplayName("Успешная генерация события")
    @Test
    fun successGeneration() {
        val rowValues = mapOf<String, Any?>(
            EXTERNAL_ID_COLUMN to "764234",
            EVENT_TIME_COLUMN to "2021-07-24T18:57:29.742054+03:00",
            RECORD_ID_COLUMN to 1L,
            STATUS_COLUMN to "RETURNED_ORDER_AT_SO_WAREHOUSE",
            FAKE_ORDER_TYPE_COLUMN to null,
            PARTNER_ID_COLUMN to "2",
            PARTNER_NAME_COLUMN to "SC1"
        )
        val ytRow = createYtRow(rowValues)
        val ytDto = DynamicYtDto(ytRow, YtScReader::class)
        val event = generator.generateEvent(ytDto)!!

        val payload = event.payload as ReturnScIntakeYtEventPayload
        assertSoftly { event.type shouldBe RETURN_SC_INTAKE }
        assertSoftly { event.eventTime shouldBe "2021-07-24T18:57:29.742054".toInstant() }
        assertSoftly { event.uniqueKey shouldBe "RETURN_SC_INTAKE;764234;2021-07-24T15:57:29.742054Z;" }
        assertSoftly { payload.externalId shouldBe "764234" }
        assertSoftly { payload.status shouldBe "RETURNED_ORDER_AT_SO_WAREHOUSE" }
        assertSoftly { payload.sortingCenterId shouldBe 2L }
        assertSoftly { payload.isClientReturn shouldBe false }
    }


    @DisplayName("Успешная генерация события клиентских возвратов")
    @Test
    fun generateOnlyClientReturns() {
        val rowValues = mapOf<String, Any?>(
            EXTERNAL_ID_COLUMN to "764234",
            EVENT_TIME_COLUMN to "2021-07-24T18:57:29.742054+03:00",
            RECORD_ID_COLUMN to 1L,
            STATUS_COLUMN to "RETURNED_ORDER_AT_SO_WAREHOUSE",
            FAKE_ORDER_TYPE_COLUMN to "CLIENT_RETURN",
        )
        val ytRow = createYtRow(rowValues)
        val ytDto = DynamicYtDto(ytRow, YtScReader::class)
        val event = generator.generateEvent(ytDto)!!

        val payload = event.payload as ReturnScIntakeYtEventPayload
        assertSoftly { event.type shouldBe RETURN_SC_INTAKE }
        assertSoftly { payload.isClientReturn shouldBe true }
    }

    @DisplayName("Генерировать только правильного типа")
    @Test
    fun generateEventOnlyForRightType() {
        val rowValues = mapOf<String, Any?>(
            EXTERNAL_ID_COLUMN to "764234",
            EVENT_TIME_COLUMN to "2021-07-24T18:57:29.742054+03:00",
            RECORD_ID_COLUMN to 1L,
            STATUS_COLUMN to "RETURNED_ORDER_READY_TO_BE_SENT_TO_IM",
        )
        val ytRow = createYtRow(rowValues)
        val ytDto = DynamicYtDto(ytRow, YtScReader::class)
        val event = generator.generateEvent(ytDto)

        assertSoftly { event shouldBe null }
    }
}

