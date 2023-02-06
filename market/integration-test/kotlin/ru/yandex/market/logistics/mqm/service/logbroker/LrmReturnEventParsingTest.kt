package ru.yandex.market.logistics.mqm.service.logbroker

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.lrm.event_model.ReturnEvent
import ru.yandex.market.logistics.lrm.event_model.ReturnEventType
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSegmentCreatedPayload
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSegmentStatusChangedPayload
import ru.yandex.market.logistics.lrm.event_model.payload.ShipmentFieldsInfo
import ru.yandex.market.logistics.lrm.event_model.payload.enums.ReturnSegmentStatus
import ru.yandex.market.logistics.lrm.event_model.payload.enums.ShipmentDestinationType
import ru.yandex.market.logistics.lrm.event_model.payload.enums.ShipmentRecipientType
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.properties.LrmReturnEventConsumerProperties
import ru.yandex.market.logistics.mqm.utils.queue.extractFileContent
import java.time.LocalDateTime
import java.time.ZoneOffset

@DisplayName("Проверка чтения событий из LRM")
class LrmReturnEventParsingTest : AbstractContextualTest() {
    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var consumerProperties: LrmReturnEventConsumerProperties

    @Test
    fun lrmSegmentCreatedEventParsing() {
        val parser = LrmEventDtoLbParser(consumerProperties.entityType, objectMapper)
        val json = extractFileContent("service/logbroker/returns/lrm_return_event_segment_created.json")
        val parsedEntity = parser.parseLine(consumerProperties.entityType, json)
        val validPayload = ReturnSegmentCreatedPayload().setId(1).setShipmentFieldsInfo(
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
                        ).build()
                ).build()
        )
        val validEntity = ReturnEvent.builder().id(1).returnId(1)
            .created(LocalDateTime.of(2007, 12, 3, 10, 15, 30).toInstant(ZoneOffset.UTC))
            .eventType(ReturnEventType.RETURN_SEGMENT_CREATED).payload(validPayload).orderExternalId("ext1").build()
        assertSoftly {
            parsedEntity shouldBe validEntity
        }
    }

    @Test
    fun lrmSegmentStatusChangedEventParsing() {
        val parser = LrmEventDtoLbParser(consumerProperties.entityType, objectMapper)
        val json = extractFileContent("service/logbroker/returns/lrm_return_event_segment_status_changed.json")
        val parsedEntity = parser.parseLine(consumerProperties.entityType, json)
        val validPayload =
            ReturnSegmentStatusChangedPayload().setId(1).setStatus(ReturnSegmentStatus.IN).setShipmentFieldsInfo(
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
                                ShipmentFieldsInfo.CourierInfo.builder().id(123).uid(234).name("courier")
                                    .carNumber("car")
                                    .carDescription("reno logan 20go veka chernogo zveta")
                                    .phoneNumber("+7-000-000-00-00")
                                    .build()
                            ).build()
                    ).build()
            )
        val validEntity = ReturnEvent.builder().id(1).returnId(1)
            .created(LocalDateTime.of(2007, 12, 3, 10, 15, 30).toInstant(ZoneOffset.UTC))
            .eventType(ReturnEventType.RETURN_SEGMENT_STATUS_CHANGED).payload(validPayload).orderExternalId("ext1")
            .build()
        assertSoftly {
            parsedEntity shouldBe validEntity
        }
    }
}
