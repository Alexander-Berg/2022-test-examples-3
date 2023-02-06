package ru.yandex.market.logistics.mqm.service.logbroker

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.lrm.event_model.ReturnEvent
import ru.yandex.market.logistics.lrm.event_model.ReturnEventType
import ru.yandex.market.logistics.lrm.event_model.payload.LogisticPointInfo
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnBox
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnCommittedPayload
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnEventPayload
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnItem
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnReasonType
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSegmentCreatedPayload
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSegmentStatusChangedPayload
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSource
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatus
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatusChangedPayload
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSubreason
import ru.yandex.market.logistics.lrm.event_model.payload.ShipmentFieldsInfo
import ru.yandex.market.logistics.lrm.event_model.payload.enums.ReturnSegmentStatus
import ru.yandex.market.logistics.lrm.event_model.payload.enums.ShipmentDestinationType
import ru.yandex.market.logistics.lrm.event_model.payload.enums.ShipmentRecipientType
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import java.time.LocalDateTime
import java.time.ZoneOffset


@DisplayName("Проверка чтения событий из LRM")
class LrmReturnEventConsumerTest : AbstractContextualTest() {

    @Autowired
    lateinit var lrmReturnEventConsumer: LrmReturnEventConsumer

    @Test
    @ExpectedDatabase(
        "/service/logbroker/returns/after/lrm_return_event_segment_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun segmentCreateEvent() {
        lrmReturnEventConsumer.accept(listOf(baseEntity(segmentCreatedPayload(), ReturnEventType.RETURN_SEGMENT_CREATED)))
    }

    @Test
    @ExpectedDatabase(
        "/service/logbroker/returns/after/lrm_return_event_segment_created_with_null_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun segmentCreateWithNullFields() {
        lrmReturnEventConsumer.accept(
            listOf(
                baseEntity(
                    createdPayloadWithNullFields(),
                    ReturnEventType.RETURN_SEGMENT_CREATED
                )
            )
        )
    }

    @Test
    @DatabaseSetup("/service/logbroker/returns/before/lrm_return_event_segment_status_changed.xml")
    @ExpectedDatabase(
        "/service/logbroker/returns/after/lrm_return_event_segment_status_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun segmentChangeStatus() {
        lrmReturnEventConsumer.accept(
            listOf(
                baseEntity(
                    segmentStatusChangedPayload(),
                    ReturnEventType.RETURN_SEGMENT_STATUS_CHANGED
                )
            )
        )
    }

    @Test
    @ExpectedDatabase(
        "/service/logbroker/returns/after/lrm_return_event_return_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun returnCreate() {
        lrmReturnEventConsumer.accept(
            listOf(
                baseEntity(
                    returnCommittedPayload(),
                    ReturnEventType.RETURN_COMMITTED
                )
            )
        )
    }

    @Test
    @ExpectedDatabase(
        "/service/logbroker/returns/after/lrm_return_event_return_status_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun returnStatusChange() {
        lrmReturnEventConsumer.accept(
            listOf(
                baseEntity(
                    returnStatusChangedPayload(),
                    ReturnEventType.RETURN_STATUS_CHANGED
                )
            )
        )
    }

    fun baseEntity(payload: ReturnEventPayload, type: ReturnEventType): ReturnEvent {
        return ReturnEvent.builder().id(1).returnId(1)
            .created(LocalDateTime.of(2007, 12, 3, 10, 15, 30).toInstant(ZoneOffset.UTC))
            .eventType(type)
            .payload(payload)
            .orderExternalId("ext1")
            .build()
    }

    private fun segmentCreatedPayload(): ReturnSegmentCreatedPayload {
        return ReturnSegmentCreatedPayload().setId(15).setShipmentFieldsInfo(
            ShipmentFieldsInfo.builder()
                .shipmentTime(LocalDateTime.of(2021, 11, 14, 12, 0, 0).toInstant(ZoneOffset.UTC))
                .destinationInfo(
                    ShipmentFieldsInfo.DestinationInfo.builder().type(ShipmentDestinationType.SORTING_CENTER)
                        .partnerId(345).logisticPointId(200).name("склад сц").returnSegmentId(2).build()
                )
                .recipient(
                    ShipmentFieldsInfo.RecipientInfo.builder().partnerId(1005372)
                        .partnerType(ShipmentRecipientType.DELIVERY_SERVICE_WITH_COURIER)
                        .name("Доставка до ПВЗ").courier(
                            ShipmentFieldsInfo.CourierInfo.builder().id(123).uid(234).name("courier").carNumber("car")
                                .carDescription("reno logan 20go veka chernogo zveta").phoneNumber("+7-000-000-00-00")
                                .build()
                        )
                        .build()
                ).build()
        ).setLogisticPointInfo(
            LogisticPointInfo.builder().logisticPointExternalId("13").logisticPointId(200)
                .type(LogisticPointInfo.LogisticPointType.DROPOFF).partnerId(10L).build()
        ).setBoxExternalId("ext-box-id")
    }

    private fun createdPayloadWithNullFields(): ReturnSegmentCreatedPayload {
        return ReturnSegmentCreatedPayload().setId(15).setShipmentFieldsInfo(
            ShipmentFieldsInfo.builder()
                .shipmentTime(LocalDateTime.of(2021, 11, 14, 12, 0, 0).toInstant(ZoneOffset.UTC))
                .build()
        ).setLogisticPointInfo(
            LogisticPointInfo.builder().build()
        )
    }

    fun segmentStatusChangedPayload(): ReturnSegmentStatusChangedPayload {
        return ReturnSegmentStatusChangedPayload().setId(1).setShipmentFieldsInfo(
            ShipmentFieldsInfo.builder()
                .shipmentTime(LocalDateTime.of(2021, 11, 14, 12, 0, 0).toInstant(ZoneOffset.UTC))
                .destinationInfo(
                    ShipmentFieldsInfo.DestinationInfo.builder().type(ShipmentDestinationType.SORTING_CENTER)
                        .partnerId(345).logisticPointId(200).name("склад сц").returnSegmentId(2).build()
                )
                .recipient(
                    ShipmentFieldsInfo.RecipientInfo.builder().partnerId(1005372)
                        .partnerType(ShipmentRecipientType.DELIVERY_SERVICE_WITH_COURIER)
                        .name("Доставка до ПВЗ").courier(
                            ShipmentFieldsInfo.CourierInfo.builder().id(123).uid(234).name("courier").carNumber("car")
                                .carDescription("reno logan 20go veka chernogo zveta").phoneNumber("+7-000-000-00-00")
                                .build()
                        )
                        .build()
                ).build()
        ).setLogisticPointInfo(
            LogisticPointInfo.builder().logisticPointExternalId("1").logisticPointId(1)
                .type(LogisticPointInfo.LogisticPointType.DROPOFF).partnerId(1L).build()
        ).setStatus(ReturnSegmentStatus.IN)
    }

    private fun returnCommittedPayload(): ReturnCommittedPayload =
        ReturnCommittedPayload().setExternalId("666").setSource(ReturnSource.CLIENT).setItems(
            listOf(
                ReturnItem.builder().supplierId(1).vendorCode("vendorCode").instances(mapOf("kek" to "lol"))
                    .boxExternalId("box").returnReason("returnReason").returnSubreason(
                        ReturnSubreason.BAD_PACKAGE
                    )
                    .returnReasonType(ReturnReasonType.BAD_QUALITY)
                    .build()
            )
        ).setBoxes(listOf(ReturnBox.builder().externalId("box").build()))

    private fun returnStatusChangedPayload(): ReturnStatusChangedPayload = ReturnStatusChangedPayload().setStatus(ReturnStatus.CANCELLED)
}
