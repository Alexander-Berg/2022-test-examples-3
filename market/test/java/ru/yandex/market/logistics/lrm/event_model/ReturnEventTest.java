package ru.yandex.market.logistics.lrm.event_model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.logistics.lrm.event_model.payload.CustomerOrderCancelledPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.CustomerOrderItem;
import ru.yandex.market.logistics.lrm.event_model.payload.CustomerOrderItemsChangedPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.LogisticPointInfo;
import ru.yandex.market.logistics.lrm.event_model.payload.OrderItemInfo;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnBox;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnBoxStatus;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnBoxStatusChangedPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnBoxesChangedPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnCommittedPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnItem;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnReasonType;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSegmentCreatedPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSegmentStatusChangedPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSource;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatus;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatusChangedPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSubreason;
import ru.yandex.market.logistics.lrm.event_model.payload.ShipmentFieldsInfo;
import ru.yandex.market.logistics.lrm.event_model.payload.ShipmentFieldsInfo.CourierInfo;
import ru.yandex.market.logistics.lrm.event_model.payload.ShipmentFieldsInfo.DestinationInfo;
import ru.yandex.market.logistics.lrm.event_model.payload.ShipmentFieldsInfo.RecipientInfo;
import ru.yandex.market.logistics.lrm.event_model.payload.enums.ReturnSegmentStatus;
import ru.yandex.market.logistics.lrm.event_model.payload.enums.ShipmentDestinationType;
import ru.yandex.market.logistics.lrm.event_model.payload.enums.ShipmentRecipientType;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class ReturnEventTest extends AbstractTest {

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource
    @DisplayName("Сериализация и десериализация событий")
    void success(ReturnEvent event, String jsonPath) throws Exception {
        String content = extractFileContent(jsonPath);
        softly.assertThat(OBJECT_MAPPER.readValue(content, ReturnEvent.class))
            .usingRecursiveComparison()
            .isEqualTo(event);

        JSONAssert.assertEquals(
            content,
            OBJECT_MAPPER.writeValueAsString(event),
            true
        );
    }

    @Nonnull
    static Stream<Arguments> success() {
        return Stream.of(
            Arguments.of(
                ReturnEvent.builder().build(),
                "ru/yandex/market/logistics/lrm/event_model/empty.json"
            ),
            Arguments.of(
                defaultFields()
                    .eventType(ReturnEventType.RETURN_COMMITTED)
                    .payload(
                        new ReturnCommittedPayload()
                            .setSource(ReturnSource.PICKUP_POINT)
                            .setExternalId("129837")
                            .setBoxes(List.of(box()))
                            .setItems(List.of(
                                ReturnItem.builder()
                                    .supplierId(9876L)
                                    .vendorCode("KJH65")
                                    .instances(Map.of("CIS", "ws87fy89we"))
                                    .boxExternalId("box-219874")
                                    .returnReason("reason")
                                    .returnSubreason(ReturnSubreason.BAD_PACKAGE)
                                    .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                    .build()
                            ))
                            .setOrderItemsInfo(List.of(
                                OrderItemInfo.builder()
                                    .supplierId(9876L)
                                    .vendorCode("KJH65")
                                    .instances(List.of(Map.of("UIT", "item-uit")))
                                    .build()
                            ))
                    )
                    .build(),
                "ru/yandex/market/logistics/lrm/event_model/return_committed.json"
            ),
            Arguments.of(
                defaultFields()
                    .eventType(ReturnEventType.CUSTOMER_ORDER_ITEMS_CHANGED)
                    .payload(
                        new CustomerOrderItemsChangedPayload()
                            .setItemsBefore(List.of(
                                CustomerOrderItem.builder()
                                    .id(98712L)
                                    .count(1)
                                    .instances(List.of(Map.of("CIS", "298347")))
                                    .build()
                            ))
                            .setItemsAfter(List.of(
                                CustomerOrderItem.builder()
                                    .id(98234L)
                                    .count(2)
                                    .instances(List.of(Map.of("CIS", "298347"), Map.of("CIS", "68784")))
                                    .build()
                            ))
                    )
                    .build(),
                "ru/yandex/market/logistics/lrm/event_model/customer_order_items_changed.json"
            ),
            Arguments.of(
                defaultFields()
                    .eventType(ReturnEventType.CUSTOMER_ORDER_CANCELLED)
                    .payload(new CustomerOrderCancelledPayload())
                    .build(),
                "ru/yandex/market/logistics/lrm/event_model/customer_order_cancelled.json"
            ),
            Arguments.of(
                defaultFields()
                    .eventType(ReturnEventType.RETURN_BOX_STATUS_CHANGED)
                    .payload(
                        new ReturnBoxStatusChangedPayload()
                            .setBoxExternalId("box-external-id")
                            .setStatus(ReturnBoxStatus.EXPIRED)
                    )
                    .build(),
                "ru/yandex/market/logistics/lrm/event_model/return_box_status_changed.json"
            ),
            Arguments.of(
                defaultFields()
                    .eventType(ReturnEventType.RETURN_STATUS_CHANGED)
                    .payload(
                        new ReturnStatusChangedPayload()
                            .setStatus(ReturnStatus.IN_TRANSIT)
                    )
                    .build(),
                "ru/yandex/market/logistics/lrm/event_model/return_status_changed.json"
            ),
            Arguments.of(
                defaultFields()
                    .eventType(ReturnEventType.RETURN_COMMITTED)
                    .payload(
                        new ReturnSegmentCreatedPayload()
                            .setId(1L)
                            .setBoxExternalId("1")
                            .setShipmentFieldsInfo(shipmentFieldsInfo())
                            .setLogisticPointInfo(logisticPointInfo())
                    ).build(),
                "ru/yandex/market/logistics/lrm/event_model/return_segment_created.json"
            ),
            Arguments.of(
                defaultFields()
                    .eventType(ReturnEventType.RETURN_COMMITTED)
                    .payload(
                        new ReturnSegmentStatusChangedPayload()
                            .setId(1L)
                            .setStatus(ReturnSegmentStatus.CANCELLED)
                            .setShipmentFieldsInfo(shipmentFieldsInfo())
                            .setLogisticPointInfo(logisticPointInfo())
                    ).build(),
                "ru/yandex/market/logistics/lrm/event_model/return_segment_status_changed.json"
            ),
            Arguments.of(
                defaultFields()
                    .eventType(ReturnEventType.RETURN_BOXES_CHANGED)
                    .payload(
                        new ReturnBoxesChangedPayload()
                            .setBoxes(List.of(box()))
                    ).build(),
                "ru/yandex/market/logistics/lrm/event_model/return_boxes_changed.json"
            )
        );
    }

    @Nonnull
    private static ReturnBox box() {
        return ReturnBox.builder().externalId("box-219874").build();
    }

    @Nonnull
    private static ShipmentFieldsInfo shipmentFieldsInfo() {
        return ShipmentFieldsInfo.builder()
            .shipmentTime(Instant.parse("2022-03-04T05:06:07Z"))
            .destinationInfo(
                DestinationInfo.builder()
                    .type(ShipmentDestinationType.DROPOFF)
                    .partnerId(1L)
                    .logisticPointId(2L)
                    .name("destination-name")
                    .returnSegmentId(3L)
                    .build()
            )
            .recipient(
                RecipientInfo.builder()
                    .partnerId(1L)
                    .partnerType(ShipmentRecipientType.DELIVERY_SERVICE)
                    .name("recipient-name")
                    .courier(
                        CourierInfo.builder()
                            .id(10L)
                            .uid(20L)
                            .name("courier-name")
                            .carNumber("car-number")
                            .carDescription("car-description")
                            .phoneNumber("phone-number")
                            .build()
                    )
                    .build()
            )
            .build();
    }

    @Nonnull
    private static LogisticPointInfo logisticPointInfo() {
        return LogisticPointInfo.builder()
            .logisticPointId(1L)
            .logisticPointExternalId("1")
            .partnerId(1L)
            .type(LogisticPointInfo.LogisticPointType.DROPOFF)
            .build();
    }

    @Nonnull
    private static ReturnEvent.ReturnEventBuilder defaultFields() {
        return ReturnEvent.builder()
            .id(1L)
            .requestId("test-request-id")
            .returnId(2L)
            .orderExternalId("3")
            .created(Instant.parse("2021-09-10T11:12:13.00Z"));
    }
}
