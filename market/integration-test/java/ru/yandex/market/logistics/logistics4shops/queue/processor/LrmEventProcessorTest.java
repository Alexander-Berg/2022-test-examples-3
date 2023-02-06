package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.protobuf.Timestamp;
import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.event.model.FulfilmentBoxItemsReceived;
import ru.yandex.market.logistics.logistics4shops.event.model.FulfilmentBoxesReceived;
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent;
import ru.yandex.market.logistics.logistics4shops.event.model.RegularReturnStatusData;
import ru.yandex.market.logistics.logistics4shops.event.model.RegularReturnStatusData.Box;
import ru.yandex.market.logistics.logistics4shops.event.model.RegularReturnStatusData.Box.RecipientType;
import ru.yandex.market.logistics.logistics4shops.event.model.RegularReturnStatusData.Item;
import ru.yandex.market.logistics.logistics4shops.event.model.ReturnStatusChangedPayload;
import ru.yandex.market.logistics.logistics4shops.event.model.ReturnStatusChangedPayload.ReturnStatus;
import ru.yandex.market.logistics.logistics4shops.queue.payload.LrmEventPayload;
import ru.yandex.market.logistics.logistics4shops.utils.LogisticEventUtil;
import ru.yandex.market.logistics.lrm.event_model.ReturnEvent;
import ru.yandex.market.logistics.lrm.event_model.ReturnEventType;
import ru.yandex.market.lrm.client.api.ReturnsApi;
import ru.yandex.market.lrm.client.model.FulfilmentReceivedBoxInfo;
import ru.yandex.market.lrm.client.model.GetReturnBox;
import ru.yandex.market.lrm.client.model.GetReturnItem;
import ru.yandex.market.lrm.client.model.GetReturnResponse;
import ru.yandex.market.lrm.client.model.OptionalRequestPart;
import ru.yandex.market.lrm.client.model.ReceivedBoxItem;
import ru.yandex.market.lrm.client.model.ReturnSource;
import ru.yandex.market.lrm.client.model.ShipmentRecipientType;
import ru.yandex.market.lrm.client.model.UnitCountType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@DisplayName("Обработка события от LRM")
class LrmEventProcessorTest extends AbstractIntegrationTest {

    private static final long RETURN_ID = 1;
    private static final Instant EVENT_CREATED = Instant.parse("2022-03-04T05:06:07.00Z");
    private static final Instant NOW = Instant.parse("2022-04-05T06:07:08.00Z");

    @Autowired
    private ReturnsApi returnsApi;

    @Autowired
    private LrmEventProcessor processor;

    @Autowired
    private LogisticEventUtil events;

    @BeforeEach
    void setUp() {
        clock.setFixed(NOW, DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(returnsApi);
    }

    @ParameterizedTest
    @EnumSource(value = ReturnEventType.class, names = "RETURN_STATUS_CHANGED", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Не обрабатываемый тип события")
    void otherEvent(ReturnEventType notProcessingEventType) {
        softly.assertThatCode(() -> processor.execute(
                LrmEventPayload.builder()
                    .event(ReturnEvent.builder().eventType(notProcessingEventType).build())
                    .build()
            ))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Разные orderExternalId в возвратах")
    void orderIdExtraction(String orderExternalId, @Nullable Long expectedOrderId) {
        GetReturnResponse returnResponse = returnResponse(boxes()).orderExternalId(orderExternalId);
        when(returnsApi.getReturn(RETURN_ID, Set.of())).thenReturn(returnResponse);

        if (expectedOrderId == null) {
            softly.assertThatThrownBy(() -> processor.execute(eventPayload(orderExternalId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OrderExternalId = '%s' is not supported".formatted(orderExternalId));
        } else {
            softly.assertThatCode(() -> processor.execute(eventPayload(orderExternalId)))
                .doesNotThrowAnyException();
            softly.assertThat(events.getEventPayload(1))
                .extracting(LogisticEvent::getReturnStatusChangedPayload)
                .extracting(ReturnStatusChangedPayload::getOrderId)
                .isEqualTo(expectedOrderId);
        }

        verify(returnsApi).getReturn(RETURN_ID, Set.of());
    }

    @Nonnull
    private static Stream<Arguments> orderIdExtraction() {
        return Stream.of(
            Arguments.of("12345", 12345L),
            Arguments.of("FF-54321", 54321L),
            Arguments.of("LO-123", null),
            Arguments.of("42-LO-24", null)
        );
    }

    @Test
    @DisplayName("Изменился статус возврата")
    @ExpectedDatabase(
        value = "/queue/processor/lrm_event/return_status_changed/after/regular_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void boxStatusChanged() {
        GetReturnResponse returnResponse = returnResponse(boxes());
        LrmEventPayload eventPayload = eventPayload(
            ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatus.READY_FOR_IM
        );
        executePayload(returnResponse, null, eventPayload);

        softly.assertThat(events.getEventPayload(1))
            .isEqualTo(expectedFiredEvent(
                    defaultReturnStatusChangedPayload()
                        .setReturnStatus(ReturnStatus.READY_FOR_PICKUP)
                        .setRegularReturnStatusData(
                            RegularReturnStatusData.newBuilder()
                                .addAllBoxes(List.of(
                                    Box.newBuilder()
                                        .setExternalId("box-external-id-1")
                                        .setRecipientType(RecipientType.SHOP)
                                        .setDestinationLogisticPointId(123456)
                                        .build(),
                                    Box.newBuilder()
                                        .setExternalId("box-external-id-2")
                                        .setRecipientType(RecipientType.SHOP)
                                        .setDestinationLogisticPointId(123456)
                                        .build()
                                ))
                                .addItems(
                                    Item.newBuilder()
                                        .setSupplierId(654321)
                                        .setVendorCode("item-vendor-code")
                                        .putInstances("CIS", "678-ghj")
                                )
                        )
                        .build()
                )
            );
    }

    @Test
    @DisplayName("Изменился статус возврата на приемку коробок на складе")
    @ExpectedDatabase(
        value = "/queue/processor/lrm_event/return_status_changed/after/regular_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnStatusChangedForFulfilmentReceived() {
        GetReturnResponse returnResponse = returnResponse(boxesWithFulfillmentReceivedInfo());
        LrmEventPayload eventPayload = eventPayload(
            ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatus.FULFILMENT_RECEIVED
        );
        executePayload(returnResponse, Set.of(OptionalRequestPart.RECEIVED_BOX_ITEMS), eventPayload);
        softly.assertThat(events.getEventPayload(1))
            .isEqualTo(expectedFiredEvent(
                defaultReturnStatusChangedPayload()
                    .setReturnStatus(ReturnStatus.FULFILMENT_RECEIVED)
                    .setFulfilmentBoxesReceived(
                        FulfilmentBoxesReceived.newBuilder()
                            .addAllBoxes(fulfilmentBoxesReceived())
                            .build()
                    )
                    .build()
                )
            );
    }

    @Nonnull
    private List<FulfilmentBoxItemsReceived> fulfilmentBoxesReceived() {
        return List.of(
            fulfilmentBoxItemsReceived("box-external-id-1", true),
            fulfilmentBoxItemsReceived("box-external-id-2", false)
        );
    }

    @Nonnull
    private FulfilmentBoxItemsReceived fulfilmentBoxItemsReceived(
        String boxExternalId,
        boolean setupNonRequiredFields
    ) {
        FulfilmentBoxItemsReceived.Builder builder = FulfilmentBoxItemsReceived.newBuilder()
            .setBoxId(boxExternalId)
            .setFfRequestId(123L)
            .setTimestamp(Timestamp.newBuilder().setNanos(NOW.getNano()).setSeconds(NOW.getEpochSecond()).build())
            .setWarehousePartnerId(111L)
            .addAllItems(List.of(
                FulfilmentBoxItemsReceived.BoxItem.newBuilder()
                    .setSupplierId(12345L)
                    .setVendorCode("vendor-code")
                    .setStock(FulfilmentBoxItemsReceived.BoxItem.UnitCountType.DEFECT)
                    .putAllInstances(Map.of("UIT", "123-UIT", "CIS", "987650"))
                    .addAllAttributes(List.of("attr-1", "attr-2"))
                    .build()
            ));
        if (setupNonRequiredFields) {
            builder.setDeliveryServicePartnerId(222L);
        }

        return builder.build();
    }

    @Nonnull
    private LogisticEvent expectedFiredEvent(ReturnStatusChangedPayload statusChangedPayload) {
        return LogisticEvent.newBuilder()
            .setId(1)
            .setRequestId("test-request-id/2")
            .setCreated(Timestamp.newBuilder().setSeconds(NOW.getEpochSecond()))
            .setReturnStatusChangedPayload(statusChangedPayload)
            .build();
    }

    @Nonnull
    private ReturnStatusChangedPayload.Builder defaultReturnStatusChangedPayload() {
        return ReturnStatusChangedPayload.newBuilder()
            .setReturnEventId(123)
            .setReturnEventTimestamp(Timestamp.newBuilder().setSeconds(EVENT_CREATED.getEpochSecond()))
            .setReturnId(RETURN_ID)
            .setOrderId(456)
            .setClientReturnId(789)
            .setReturnSource(ReturnStatusChangedPayload.ReturnSource.CLIENT);
    }

    @Nonnull
    private LrmEventPayload eventPayload(ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatus status) {
        return LrmEventPayload.builder()
            .event(
                returnEventBuilder()
                    .payload(
                        new ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatusChangedPayload()
                            .setStatus(status)
                    )
                    .build()
            )
            .build();
    }

    @Nonnull
    private LrmEventPayload eventPayload(String orderExternalId) {
        return LrmEventPayload.builder()
            .event(returnEventBuilder().orderExternalId(orderExternalId).build())
            .build();
    }

    @Nonnull
    private ReturnEvent.ReturnEventBuilder returnEventBuilder() {
        return ReturnEvent.builder()
            .id(123L)
            .created(EVENT_CREATED)
            .eventType(ReturnEventType.RETURN_STATUS_CHANGED)
            .returnId(RETURN_ID)
            .orderExternalId("456")
            .payload(
                new ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatusChangedPayload()
                    .setStatus(ru.yandex.market.logistics.lrm.event_model.payload.ReturnStatus.READY_FOR_IM)
            );
    }

    @Nonnull
    private GetReturnResponse returnResponse(List<GetReturnBox> boxes) {
        return new GetReturnResponse()
            .source(ReturnSource.CLIENT)
            .externalId("789")
            .boxes(boxes)
            .items(List.of(
                new GetReturnItem()
                    .supplierId(654321L)
                    .vendorCode("item-vendor-code")
                    .instances(Map.of("CIS", "678-ghj"))
            ));
    }

    @Nonnull
    private List<GetReturnBox> boxes() {
        return List.of(
            getReturnBox("box-external-id-1"),
            getReturnBox("box-external-id-2")
        );
    }

    @Nonnull
    private GetReturnBox getReturnBox(String boxExternalId) {
        return new GetReturnBox()
            .externalId(boxExternalId)
            .recipientType(ShipmentRecipientType.SHOP)
            .destinationLogisticPointId(123456L);
    }

    @Nonnull
    private List<GetReturnBox> boxesWithFulfillmentReceivedInfo() {
        return List.of(
            getReturnBox("box-external-id-1")
                .fulfilmentReceivedInfo(fulfilmentReceivedBoxInfo("box-external-id-1")),
            getReturnBox("box-external-id-2")
                .fulfilmentReceivedInfo(
                    fulfilmentReceivedBoxInfo("box-external-id-2").deliveryServicePartnerId(null)
                )
        );
    }

    @Nonnull
    private FulfilmentReceivedBoxInfo fulfilmentReceivedBoxInfo(String boxExternalId) {
        return new FulfilmentReceivedBoxInfo()
            .ffRequestId(123L)
            .boxExternalId(boxExternalId)
            .timestamp(NOW)
            .warehousePartnerId(111L)
            .deliveryServicePartnerId(222L)
            .items(
                List.of(
                    new ReceivedBoxItem()
                        .supplierId(12345L)
                        .vendorCode("vendor-code")
                        .stock(UnitCountType.DEFECT)
                        .instances(Map.of("UIT", "123-UIT", "CIS", "987650"))
                        .attributes(List.of("attr-1", "attr-2"))
                )
            );
    }

    private void executePayload(
        GetReturnResponse response,
        @Nullable Set<OptionalRequestPart> optionalParts,
        LrmEventPayload eventPayload
    ) {
        when(returnsApi.getReturn(RETURN_ID, SetUtils.emptyIfNull(optionalParts))).thenReturn(response);
        softly.assertThatCode(() -> processor.execute(eventPayload)).doesNotThrowAnyException();
        verify(returnsApi).getReturn(RETURN_ID, SetUtils.emptyIfNull(optionalParts));
    }

}
