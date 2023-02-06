package ru.yandex.market.logistics.mqm.queue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ReturnSegmentStatus
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ShipmentDestinationType
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ShipmentRecipientType
import ru.yandex.market.logistics.mqm.entity.lrm.LrmShipmentFields
import ru.yandex.market.logistics.mqm.queue.dto.LrmSegmentStatusChangedDto
import java.time.LocalDateTime

@DisplayName("Тест обработки изменения статуса возвратного сегмента")
class ReturnSegmentStatusChangedConsumerTest: AbstractContextualTest() {

    @Autowired
    lateinit var consumer: ReturnSegmentStatusChangedConsumer


    private fun getShipmentFields(): LrmShipmentFields {
        return LrmShipmentFields(
            destination = LrmShipmentFields.Destination(
                type = ShipmentDestinationType.SORTING_CENTER,
                partnerId = 20,
                logisticPointId = 21,
                name = "22",
                returnSegmentId = 23
            ),
            recipient = LrmShipmentFields.Recipient(
                type = ShipmentRecipientType.DELIVERY_SERVICE,
                partnerId = 33,
                name = "ricnorr",
                courier = LrmShipmentFields.Courier(
                    id = 1,
                    uid = 2,
                    name = "okhttp",
                    carNumber = "car",
                    carDescription = "car?",
                    phoneNumber = "13"
                )
            )
        )
    }

    /**
     * Тест перехода из CREATED в IN, проверяем, что создался соответствующий план факт
     */
    @Test
    @DatabaseSetup("/queue/consumer/before/return_segment_status_changed/in_to_out_success.xml")
    @ExpectedDatabase(
        "/queue/consumer/after/return_segment_status_changed/in_to_out_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createdToInSuccess() {
        consumer.processPayload(
            LrmSegmentStatusChangedDto(
                15, ReturnSegmentStatus.IN,
                getShipmentFields(),
                LocalDateTime.of(2007, 12, 3, 10, 15, 30).atZone(
                    DateTimeUtils.MOSCOW_ZONE
                ).toInstant(),
            )
        )
    }

    /**
     * Тест перехода из CREATED в TRANSIT_PREPARED, проверяем, что создался соответствующий план факт
     */
    @Test
    @DatabaseSetup("/queue/consumer/before/return_segment_status_changed/sc_transit_prepared_to_out_success.xml")
    @ExpectedDatabase(
        "/queue/consumer/after/return_segment_status_changed/sc_transit_prepared_to_out_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createdToTransitPreparedSuccess() {
        consumer.processPayload(
            LrmSegmentStatusChangedDto(
                15, ReturnSegmentStatus.TRANSIT_PREPARED,
                getShipmentFields(), LocalDateTime.of(2007, 12, 3, 10, 15, 30).atZone(
                    DateTimeUtils.MOSCOW_ZONE
                ).toInstant()
            )
        )
    }

    /**
     * Тест, что статус не поменяется, т.к было применено более позднее изменение статуса изначально.
     */
    @Test
    @DatabaseSetup("/queue/consumer/before/return_segment_status_changed/change_ignored.xml")
    @ExpectedDatabase(
        "/queue/consumer/before/return_segment_status_changed/change_ignored.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun consumerChangeIgnored() {
        consumer.processPayload(
            LrmSegmentStatusChangedDto(
                15, ReturnSegmentStatus.IN,
                getShipmentFields(),
                LocalDateTime.of(2007, 12, 3, 10, 15, 30).atZone(
                    DateTimeUtils.MOSCOW_ZONE
                ).toInstant()
            )
        )
    }

    /**
     * Тест, что ничего не изменится, так как нет возвратного сегмента в БД.
     */
    @Test
    @DatabaseSetup("/queue/consumer/before/return_segment_status_changed/return_segment_not_exists.xml")
    @ExpectedDatabase(
        "/queue/consumer/before/return_segment_status_changed/return_segment_not_exists.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun returnSegmentNotExists() {
        consumer.processPayload(
            LrmSegmentStatusChangedDto(
                20, ReturnSegmentStatus.IN,
                getShipmentFields(), LocalDateTime.of(2007, 12, 3, 10, 15, 30).atZone(
                    DateTimeUtils.MOSCOW_ZONE
                ).toInstant()
            )
        )
    }
}
