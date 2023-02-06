package ru.yandex.market.delivery.mdbapp.integration.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.logging.json.LomEventsFailLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.json.LomEventsSuccessLogger;
import ru.yandex.market.delivery.mdbapp.components.queue.cancel.result.CancelResultDto;
import ru.yandex.market.delivery.mdbapp.components.queue.cancel.result.CancellationResultEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.changetoondemand.OrderChangeToOnDemandRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.changetoondemand.OrderChangeToOnDemandRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.CheckouterSetOrderStatusEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.SetOrderStatusDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.changedbychangerequest.DeliveryDateChangeRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.changedbychangerequest.DeliveryDateChangeRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.recalculateroutedates.RecalculateRouteDatesDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.recalculateroutedates.RecalculateRouteDatesEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileFromPickupToPickupRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileFromPickupToPickupRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileToCourierRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileToCourierRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileToPickupRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileToPickupRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.LastMileChangeRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.LastMileChangeRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.option.OrderChangeDeliveryOptionDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.option.OrderChangeDeliveryOptionEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.recipient.changedbychangerequset.RecipientChangeRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.recipient.changedbychangerequset.RecipientChangeRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.verification.OrderVerificationCodeUpdateDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.verification.OrderVerificationCodeUpdateService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.ChangedItemDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbychangerequest.OrderChangedByPartnerChangeRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbychangerequest.OrderChangedByPartnerChangeRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbyitemnotfound.OrderChangedByItemNotFoundRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbyitemnotfound.OrderChangedByItemNotFoundRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.instances.OrderItemInstanceKeys;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.instances.OrderItemInstancesUpdateDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.instances.OrderItemInstancesUpdateService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.notsupplied.OrderItemIsNotSuppliedRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.notsupplied.OrderItemIsNotSuppliedRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.notsupplied.OrderItemIsNotSuppliedRequestPayloadDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.notsupplied.OrderItemIsNotSuppliedRequestPayloadWrapperDto;
import ru.yandex.market.delivery.mdbapp.components.queue.track.add.AddTrackFromLomEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.track.add.dto.AddTrackDto;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.configuration.LockerCodeProperties;
import ru.yandex.market.delivery.mdbapp.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.MissingItemsCancellationOrderRequestReasonDetailsDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.lom.model.enums.ItemChangeReason;
import ru.yandex.market.logistics.lom.model.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.tags.WaybillSegmentTag;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.params.ParameterizedTest.ARGUMENTS_PLACEHOLDER;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.market.delivery.mdbapp.testutils.ResourceUtils.getFileContent;
import static ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason.MISSING_ITEM;
import static ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus.REQUIRED_SEGMENT_SUCCESS;

@ParametersAreNonnullByDefault
@DisplayName("Чтение событий LOMa")
class LomOrderEventServiceTest extends AllMockContextualTest {
    private static final SortedMap<SegmentStatus, StatusAndSubstatus> MOVEMENT_SUBSTATUSES = EntryStream.of(
        SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION, StatusAndSubstatus.of(OrderStatus.DELIVERY, null),
        SegmentStatus.IN, StatusAndSubstatus.of(OrderStatus.DELIVERY, null)
    )
        .toSortedMap();
    private static final SortedMap<SegmentStatus, StatusAndSubstatus> MOVEMENT_EXPRESS_SUBSTATUSES = EntryStream.of(
        SegmentStatus.TRANSIT_COURIER_SEARCH, StatusAndSubstatus.of(
            OrderStatus.PROCESSING,
            OrderSubstatus.COURIER_SEARCH
        ),
        SegmentStatus.TRANSIT_COURIER_FOUND, StatusAndSubstatus.of(
            OrderStatus.PROCESSING,
            OrderSubstatus.COURIER_FOUND
        ),
        SegmentStatus.TRANSIT_COURIER_IN_TRANSIT_TO_SENDER, StatusAndSubstatus.of(
            OrderStatus.PROCESSING,
            OrderSubstatus.COURIER_IN_TRANSIT_TO_SENDER
        ),
        SegmentStatus.TRANSIT_COURIER_ARRIVED_TO_SENDER, StatusAndSubstatus.of(
            OrderStatus.PROCESSING,
            OrderSubstatus.COURIER_ARRIVED_TO_SENDER
        ),
        SegmentStatus.TRANSIT_COURIER_RECEIVED, StatusAndSubstatus.of(
            OrderStatus.DELIVERY,
            OrderSubstatus.COURIER_RECEIVED
        )
    )
        .toSortedMap();
    private static final SortedMap<SegmentStatus, StatusAndSubstatus> MOVEMENT_PICKUP_NOT_EXPRESS_SUBSTATUSES =
        EntryStream.of(
            SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT, StatusAndSubstatus.of(
                OrderStatus.PICKUP,
                OrderSubstatus.PICKUP_USER_RECEIVED
            )
        )
            .toSortedMap();
    private static final SortedSet<Pair<ChangeOrderRequestStatus, ChangeOrderRequestStatus>>
        APPROPRIATE_RDD_REQUEST_STATUS_TRANSITIONS =
        StreamEx.of(
            Pair.of(ChangeOrderRequestStatus.INFO_RECEIVED, ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS),
            Pair.of(ChangeOrderRequestStatus.INFO_RECEIVED, ChangeOrderRequestStatus.SUCCESS),
            Pair.of(ChangeOrderRequestStatus.CREATED, ChangeOrderRequestStatus.SUCCESS)
        )
            .collect(Collectors.toCollection(TreeSet::new));

    private static final long LOM_ORDER_ID = 12345L;
    private static final long LOM_WAYBILL_SEGMENT_ID = 11111L;
    private static final long CHECKOUTER_ORDER_ID = 1232131L;
    private static final EnqueueParams<SetOrderStatusDto> ENQUEUE_PARAMS = EnqueueParams.create(
        SetOrderStatusDto.builder()
            .orderId(CHECKOUTER_ORDER_ID)
            .status(OrderStatus.PROCESSING)
            .substatus(OrderSubstatus.SHIPPED)
            .build()
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private QueueProducer<AddTrackDto> addTrackDtoQueueProducer;
    @Mock
    private QueueProducer<CancelResultDto> cancelResultDtoQueueProducer;
    @Mock
    private QueueProducer<OrderItemIsNotSuppliedRequestDto> orderItemIsNotSuppliedRequestDtoQueueProducer;
    @Mock
    private QueueProducer<OrderChangedByPartnerChangeRequestDto> orderChangedByPartnerChangeRequestDtoQueueProducer;
    @Mock
    private QueueProducer<SetOrderStatusDto> setOrderStatusDtoQueueProducer;
    @Mock
    private QueueProducer<OrderChangeDeliveryOptionDto> orderChangeDeliveryOptionDtoQueueProducer;
    @Mock
    private QueueProducer<OrderChangedByItemNotFoundRequestDto> orderChangedByItemNotFoundRequestDtoQueueProducer;
    @Mock
    private QueueProducer<OrderItemInstancesUpdateDto> orderItemInstancesUpdateDtoQueueProducer;
    @Mock
    private QueueProducer<DeliveryDateChangeRequestDto> deliveryDateChangeRequestDtoQueueProducer;
    @Mock
    private QueueProducer<RecipientChangeRequestDto> recipientChangeRequestDtoQueueProducer;
    @Mock
    private QueueProducer<LastMileChangeRequestDto> lastMileChangeRequestDtoQueueProducer;
    @Mock
    private QueueProducer<ChangeLastMileToCourierRequestDto> changeLastMileToCourierRequestDtoQueueProducer;
    @Mock
    private QueueProducer<ChangeLastMileToPickupRequestDto> changeLastMileToPickupRequestDtoQueueProducer;
    @Mock
    private QueueProducer<ChangeLastMileFromPickupToPickupRequestDto>
        changeLastMileFromPickupToPickupRequestDtoQueueProducer;
    @Mock
    private QueueProducer<OrderVerificationCodeUpdateDto> orderVerificationCodeUpdateDtoQueueProducer;
    @Mock
    private QueueProducer<OrderChangeToOnDemandRequestDto> orderChangeToOnDemandRequestDtoQueueProducer;
    @Mock
    private QueueProducer<RecalculateRouteDatesDto> recalculateRouteDatesDtoQueueProducer;
    @Mock
    private OrderErrorService orderErrorService;
    @Mock
    private LomEventsSuccessLogger lomEventsSuccessLogger;
    @Mock
    private LomEventsFailLogger lomEventsFailLogger;
    @Autowired
    private LockerCodeProperties lockerCodeProperties;

    private LomOrderEventService lomOrderEventService;

    private FeatureProperties featureProperties;

    @BeforeEach
    public void beforeTest() {
        featureProperties = new FeatureProperties();
        objectMapper.registerModule(new JavaTimeModule());
        lomOrderEventService = new LomOrderEventService(
            new AddTrackFromLomEnqueueService(addTrackDtoQueueProducer),
            new CancellationResultEnqueueService(cancelResultDtoQueueProducer),
            new OrderItemIsNotSuppliedRequestEnqueueService(orderItemIsNotSuppliedRequestDtoQueueProducer),
            new OrderChangedByPartnerChangeRequestEnqueueService(orderChangedByPartnerChangeRequestDtoQueueProducer),
            new OrderChangeDeliveryOptionEnqueueService(orderChangeDeliveryOptionDtoQueueProducer),
            new OrderChangedByItemNotFoundRequestEnqueueService(orderChangedByItemNotFoundRequestDtoQueueProducer),
            new DeliveryDateChangeRequestEnqueueService(deliveryDateChangeRequestDtoQueueProducer),
            new RecipientChangeRequestEnqueueService(recipientChangeRequestDtoQueueProducer),
            new LastMileChangeRequestEnqueueService(lastMileChangeRequestDtoQueueProducer),
            new ChangeLastMileToCourierRequestEnqueueService(changeLastMileToCourierRequestDtoQueueProducer),
            new ChangeLastMileToPickupRequestEnqueueService(changeLastMileToPickupRequestDtoQueueProducer),
            new ChangeLastMileFromPickupToPickupRequestEnqueueService(
                changeLastMileFromPickupToPickupRequestDtoQueueProducer
            ),
            new OrderChangeToOnDemandRequestEnqueueService(orderChangeToOnDemandRequestDtoQueueProducer),
            new RecalculateRouteDatesEnqueueService(recalculateRouteDatesDtoQueueProducer),
            objectMapper,
            orderErrorService,
            new OrderItemInstancesUpdateService(orderItemInstancesUpdateDtoQueueProducer),
            new OrderVerificationCodeUpdateService(orderVerificationCodeUpdateDtoQueueProducer),
            lomEventsSuccessLogger,
            lomEventsFailLogger,
            lockerCodeProperties,
            featureProperties,
            new CheckouterSetOrderStatusEnqueueService(setOrderStatusDtoQueueProducer)
        );
    }

    @AfterEach
    public void afterTest() {
        lockerCodeProperties.setEnabled(false);
        lockerCodeProperties.setStartOrderId(0);
        featureProperties.setDeliveryToStoreStatusFeatureEnabled(false);

        verifyNoMoreInteractions(
            addTrackDtoQueueProducer,
            cancelResultDtoQueueProducer,
            orderItemIsNotSuppliedRequestDtoQueueProducer,
            orderChangedByPartnerChangeRequestDtoQueueProducer,
            setOrderStatusDtoQueueProducer,
            orderChangeDeliveryOptionDtoQueueProducer,
            orderChangedByItemNotFoundRequestDtoQueueProducer,
            orderItemInstancesUpdateDtoQueueProducer,
            deliveryDateChangeRequestDtoQueueProducer,
            recipientChangeRequestDtoQueueProducer,
            lastMileChangeRequestDtoQueueProducer,
            orderVerificationCodeUpdateDtoQueueProducer,
            orderChangeToOnDemandRequestDtoQueueProducer,
            recalculateRouteDatesDtoQueueProducer,
            orderErrorService,
            lomEventsSuccessLogger,
            lomEventsFailLogger
        );
    }

    @Test
    public void newTrack() {
        ObjectNode diff = objectMapper.createObjectNode();
        diff.put("op", "replace");
        diff.put("path", "/waybill/0/externalId");

        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(diff);

        accept(List.of(createEventDto(arrayNode, createSnapshot())));

        var enqueueParams = EnqueueParams.create(
            new AddTrackDto(LOM_ORDER_ID, LOM_WAYBILL_SEGMENT_ID)
        );
        verify(addTrackDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void newTrackWithWithAdditionalOperations() {
        ObjectNode diff = objectMapper.createObjectNode();
        diff.put("op", "replace");
        diff.put("path", "/waybill/0/externalId");

        ObjectNode additionalDiff = objectMapper.createObjectNode();
        additionalDiff.put("op", "someOperation");
        additionalDiff.put("path", "/some/path");

        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(additionalDiff);
        arrayNode.add(diff);

        accept(List.of(createEventDto(arrayNode, createSnapshot())));

        var enqueueParams = EnqueueParams.create(
            new AddTrackDto(LOM_ORDER_ID, LOM_WAYBILL_SEGMENT_ID)
        );
        verify(addTrackDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void cancellation() throws IOException {
        ArrayNode diff = objectMapper.readValue(
            "[{\"op\": \"replace\", "
                + " \"path\": \"/cancellationOrderRequests/0/status\","
                + " \"value\": \"REQUIRED_SEGMENT_SUCCESS\", "
                + " \"fromValue\": \"PROCESSING\"}]",
            ArrayNode.class
        );
        JsonNode snapshot = objectMapper.readValue(
            "{" +
                "\"id\": " + LOM_ORDER_ID + ",\n" +
                "\"platformClientId\": 1,\n" +
                "\"externalId\": 1001,\n" +
                "\"cancellationOrderRequests\": [\n" +
                "  {\n" +
                "    \"status\": \"REQUIRED_SEGMENT_SUCCESS\",\n" +
                "    \"cancellationErrorMessage\": null,\n" +
                "    \"cancellationOrderReason\": \"MISSING_ITEM\",\n" +
                "          \"cancellationOrderRequestReasonDetails\": {\n" +
                "            \"items\": [\n" +
                "              {\n" +
                "                \"vendorId\": 1,\n" +
                "                \"article\": \"article\",\n" +
                "                \"count\": 1,\n" +
                "                \"reason\": \"ITEM_IS_NOT_SUPPLIED\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }," +
                "    \"cancellationSegmentRequests\": [\n" +
                "      {\n" +
                "        \"status\": \"WAITING_CHECKPOINTS\",\n" +
                "        \"required\": false,\n" +
                "        \"partnerId\": 47796,\n" +
                "        \"sufficient\": false\n" +
                "      },\n" +
                "      {\n" +
                "        \"status\": \"WAITING_CHECKPOINTS\",\n" +
                "        \"required\": false,\n" +
                "        \"partnerId\": 1003937,\n" +
                "        \"sufficient\": false\n" +
                "      },\n" +
                "      {\n" +
                "        \"status\": \"FAIL\",\n" +
                "        \"required\": false,\n" +
                "        \"partnerId\": 74,\n" +
                "        \"sufficient\": false\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]\n" +
                " }",
            JsonNode.class
        );

        accept(List.of(createEventDto(diff, snapshot)));

        var enqueueParams = EnqueueParams.create(new CancelResultDto(
                LOM_ORDER_ID,
                REQUIRED_SEGMENT_SUCCESS,
                MISSING_ITEM,
                new MissingItemsCancellationOrderRequestReasonDetailsDto()
                    .setItems(List.of(
                        ru.yandex.market.logistics.lom.model.dto.ChangedItemDto.builder()
                            .article("article")
                            .vendorId(1L)
                            .count(1L)
                            .reason(ItemChangeReason.ITEM_IS_NOT_SUPPLIED)
                            .build()
                    ))
            )
        );
        verify(cancelResultDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void cancellationWithoutReason() throws IOException {
        ArrayNode diff = objectMapper.readValue(
            "[{\"op\": \"replace\", "
                + " \"path\": \"/cancellationOrderRequests/0/status\","
                + " \"value\": \"REQUIRED_SEGMENT_SUCCESS\", "
                + " \"fromValue\": \"PROCESSING\"}]",
            ArrayNode.class
        );
        JsonNode snapshot = objectMapper.readValue(
            "{" +
                "\"id\": " + LOM_ORDER_ID + ",\n" +
                "\"platformClientId\": 1,\n" +
                "\"externalId\": 1001,\n" +
                "\"cancellationOrderRequests\": [\n" +
                "  {\n" +
                "    \"status\": \"REQUIRED_SEGMENT_SUCCESS\",\n" +
                "    \"cancellationErrorMessage\": null,\n" +
                "    \"cancellationOrderRequestReasonDetails\":null,\n" +
                "    \"cancellationSegmentRequests\": [\n" +
                "      {\n" +
                "        \"status\": \"WAITING_CHECKPOINTS\",\n" +
                "        \"required\": false,\n" +
                "        \"partnerId\": 47796,\n" +
                "        \"sufficient\": false\n" +
                "      },\n" +
                "      {\n" +
                "        \"status\": \"WAITING_CHECKPOINTS\",\n" +
                "        \"required\": false,\n" +
                "        \"partnerId\": 1003937,\n" +
                "        \"sufficient\": false\n" +
                "      },\n" +
                "      {\n" +
                "        \"status\": \"FAIL\",\n" +
                "        \"required\": false,\n" +
                "        \"partnerId\": 74,\n" +
                "        \"sufficient\": false\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]\n" +
                " }",
            JsonNode.class
        );

        accept(List.of(createEventDto(diff, snapshot)));

        var enqueueParams = EnqueueParams.create(new CancelResultDto(
                LOM_ORDER_ID,
                REQUIRED_SEGMENT_SUCCESS,
                null,
                null
            )
        );
        verify(cancelResultDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void postamatOrderNoDelivery() {
        var arrayNode = objectMapper.createArrayNode();
        var checkouterId = 1232131L;
        var orderDto = createOrderDto(PlatformClient.BERU, String.valueOf(checkouterId));

        accept(List.of(createEventDto(arrayNode, createSnapshot(orderDto))));

        var enqueueParams = EnqueueParams
            .create(SetOrderStatusDto.builder().orderId(checkouterId).status(OrderStatus.DELIVERY).build());
        verify(setOrderStatusDtoQueueProducer, Mockito.never()).enqueue(enqueueParams);
    }

    @Test
    public void postamatOrderAndDelivery() {
        var checkouterId = 1232131L;
        processOrder(PlatformClient.BERU, String.valueOf(checkouterId));

        var enqueueParams = EnqueueParams
            .create(SetOrderStatusDto.builder().orderId(checkouterId).status(OrderStatus.DELIVERY).build());
        verify(setOrderStatusDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void lavkaOrderChangeCheckouterStatus() {
        var checkouterId = 1232131L;
        processOrder(PlatformClient.BERU, String.valueOf(checkouterId));

        var enqueueParams = EnqueueParams
            .create(SetOrderStatusDto.builder().orderId(checkouterId).status(OrderStatus.DELIVERY).build());
        verify(setOrderStatusDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void postamatOrderAndDeliveryNotBeru() {
        var checkouterId = 1232131L;
        processOrder(PlatformClient.YANDEX_MARKET, String.valueOf(checkouterId));

        Mockito.verifyNoMoreInteractions(setOrderStatusDtoQueueProducer);
    }

    @Test
    public void nonNumberOrderIdBeru() {
        String exceptionMessage = "ExternalId = 22-1 of order with platformClientId = 1 "
            + "expected to be parsed as Long value";

        softly.assertThatThrownBy(() -> processOrder(PlatformClient.BERU, "22-1", SegmentStatus.IN))
            .isInstanceOf(RuntimeException.class)
            .hasMessage(exceptionMessage);

        ArgumentCaptor<RuntimeException> exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);

        Mockito.verify(lomEventsFailLogger).logEvent(
            Mockito.eq(createEventDto(
                createStatusDiff(SegmentStatus.IN, 1),
                createSnapshot(createOrderDto(PlatformClient.BERU, "22-1"))
            )),
            exceptionCaptor.capture()
        );

        softly.assertThat(exceptionCaptor.getValue().getMessage()).isEqualTo(exceptionMessage);

        Mockito.verifyNoMoreInteractions(setOrderStatusDtoQueueProducer);
    }

    @Test
    public void diffIsNotArray() {
        EventDto eventDto = createEventDto(
            objectMapper.createObjectNode(),
            createSnapshot(createOrderDto(PlatformClient.BERU, "1"))
        );
        String exceptionMessage = "Process event diff is not array, entity id " + LOM_ORDER_ID;

        softly.assertThatThrownBy(() -> accept(List.of(eventDto)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage(exceptionMessage);

        ArgumentCaptor<RuntimeException> exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);
        Mockito.verify(lomEventsFailLogger).logEvent(Mockito.eq(eventDto), exceptionCaptor.capture());
        softly.assertThat(exceptionCaptor.getValue().getMessage()).isEqualTo(exceptionMessage);

        Mockito.verifyNoMoreInteractions(setOrderStatusDtoQueueProducer);
    }

    @Test
    public void errorParsingSnapshot() {
        EventDto eventDto = createEventDto(createStatusDiff(SegmentStatus.IN, 1), objectMapper.createArrayNode());
        String exceptionMessage = "Error parsing order snapshot, order id " + LOM_ORDER_ID;

        softly.assertThatThrownBy(() -> accept(List.of(eventDto)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining(exceptionMessage);

        ArgumentCaptor<RuntimeException> exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);
        Mockito.verify(lomEventsFailLogger).logEvent(Mockito.eq(eventDto), exceptionCaptor.capture());
        softly.assertThat(exceptionCaptor.getValue().getMessage()).contains(exceptionMessage);

        Mockito.verifyNoMoreInteractions(setOrderStatusDtoQueueProducer);
    }

    @Test
    public void nonNumberOrderIdNonBeru() {
        processOrder(PlatformClient.YANDEX_MARKET, "22-1");

        Mockito.verifyNoMoreInteractions(setOrderStatusDtoQueueProducer);
    }

    @ParameterizedTest
    @EnumSource(value = PlatformClient.class, names = {"DBS", "BERU"}, mode = EnumSource.Mode.EXCLUDE)
    public void invalidPlatformClient(PlatformClient platformClient) {
        processOrder(platformClient, "123");

        Mockito.verifyNoMoreInteractions(setOrderStatusDtoQueueProducer);
    }

    @Nonnull
    private ArrayNode createStatusDiff(SegmentStatus segmentStatus, int changedSegment) {
        ObjectNode segmentStatusDiff = objectMapper.createObjectNode();
        segmentStatusDiff.put("op", "replace");
        segmentStatusDiff.put("path", "/waybill/" + changedSegment + "/segmentStatus");
        segmentStatusDiff.put("value", segmentStatus.name());

        return objectMapper.createArrayNode().add(segmentStatusDiff);
    }

    @Nonnull
    private OrderDto createOrderDto(PlatformClient beru, String externalId) {
        return createOrderDto(beru, externalId, false);
    }

    @Nonnull
    private OrderDto createOrderDto(PlatformClient beru, String externalId, boolean isLocker) {
        return new OrderDto()
            .setId(1001L)
            .setPlatformClientId(beru.getId())
            .setDeliveryType(DeliveryType.PICKUP)
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.FULFILLMENT)
                    .partnerId(172L)
                    .build(),
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.MOVEMENT)
                    .partnerId(1005471L)
                    .build(),
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.PICKUP)
                    .partnerId(1003375L)
                    .partnerType(PartnerType.DELIVERY)
                    .partnerSubtype(isLocker ? PartnerSubtype.MARKET_LOCKER : PartnerSubtype.MARKET_OWN_PICKUP_POINT)
                    .build()
            ))
            .setExternalId(externalId);
    }

    @Test
    public void orderItemIsNotSuppliedAdd() throws IOException {
        ArrayNode diff = objectMapper.readValue(
            "[\n" +
                "  {\n" +
                "    \"op\": \"add\",\n" +
                "    \"path\": \"/changeOrderRequests/0\",\n" +
                "    \"value\": {\n" +
                "      \"id\": 13,\n" +
                "      \"status\": \"CREATED\",\n" +
                "      \"comment\": null,\n" +
                "      \"requestType\": \"ORDER_ITEM_IS_NOT_SUPPLIED\",\n" +
                "      \"changeOrderRequestPayloads\": [\n" +
                "        {\n" +
                "          \"id\": 13,\n" +
                "          \"payload\": {\n" +
                "            \"items\": [\n" +
                "              {\n" +
                "                \"count\": 0,\n" +
                "                \"reason\": \"ITEM_IS_NOT_SUPPLIED\",\n" +
                "                \"article\": \"Cross2.100256629594\",\n" +
                "                \"vendorId\": 10313340\n" +
                "              }\n" +
                "            ],\n" +
                "            \"barcode\": \"7111039\"\n" +
                "          },\n" +
                "          \"changeOrderRequestStatus\": \"CREATED\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "]",
            ArrayNode.class
        );

        JsonNode snapshot = objectMapper.readValue(
            "{\n" +
                "  \"id\": " + LOM_ORDER_ID + ",\n" +
                "  \"platformClientId\": 1,\n" +
                "  \"externalId\": 1001,\n" +
                "  \"changeOrderRequests\": [\n" +
                "    {\n" +
                "      \"status\": \"CREATED\",\n" +
                "      \"payloads\": [\n" +
                "        {\n" +
                "          \"status\": \"CREATED\",\n" +
                "          \"payload\": {\n" +
                "            \"items\": [\n" +
                "              {\n" +
                "                \"count\": 0,\n" +
                "                \"reason\": \"ITEM_IS_NOT_SUPPLIED\",\n" +
                "                \"article\": \"Cross2.100256629594\",\n" +
                "                \"vendorId\": 10313340\n" +
                "              }\n" +
                "            ],\n" +
                "            \"barcode\": \"7111039\"\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"requestType\": \"ORDER_ITEM_IS_NOT_SUPPLIED\"\n" +
                "    }\n" +
                "  ]\n" +
                "}",
            JsonNode.class
        );

        accept(List.of(createEventDto(diff, snapshot)));

        var enqueueParams = EnqueueParams.create(new OrderItemIsNotSuppliedRequestDto(
            LOM_ORDER_ID,
            ChangeOrderRequestType.ORDER_ITEM_IS_NOT_SUPPLIED,
            ChangeOrderRequestStatus.CREATED,
            Set.of(new OrderItemIsNotSuppliedRequestPayloadWrapperDto(
                ChangeOrderRequestStatus.CREATED,
                new OrderItemIsNotSuppliedRequestPayloadDto(
                    List.of(
                        new ChangedItemDto(
                            10313340L,
                            "Cross2.100256629594",
                            0,
                            "ITEM_IS_NOT_SUPPLIED"
                        )
                    ),
                    "7111039"
                )
            ))
        ));
        verify(orderItemIsNotSuppliedRequestDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void instancesWithEmptyItems() throws JsonProcessingException {
        ArrayNode diff = objectMapper.readValue(
            "[{\"op\": \"replace\", "
                + " \"path\": \"/items/0/instances\","
                + " \"value\": [{\"cis\": \"123abc\"}], "
                + " \"fromValue\": null}]",
            ArrayNode.class
        );

        accept(List.of(createEventDto(diff, createSnapshot())));

        var enqueueParams = EnqueueParams.create(
            new OrderItemInstancesUpdateDto(LOM_ORDER_ID)
        );
        verify(orderItemInstancesUpdateDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void instancesWithNonEmptyItems() throws JsonProcessingException {
        ArrayNode diff = objectMapper.readValue(
            "[{\"op\": \"replace\", "
                + " \"path\": \"/items/0/instances\","
                + " \"value\": [{\"cis\": \"123abc\", \"cisFull\": \"123abc-def\", \"sn\" : \"123sn\"}], "
                + " \"fromValue\": null}]",
            ArrayNode.class
        );

        String cis = "123abc";
        String cisFull = "123abc-def";
        String uit = "321cba";
        String sn = "123sn";
        String imei = "123imei";

        OrderDto snapshot = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId("1001")
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setItems(
                List.of(
                    ItemDto.builder()
                        .instances(List.of(Map.of(
                            OrderItemInstanceKeys.CIS, cis,
                            OrderItemInstanceKeys.CIS_FULL, cisFull,
                            OrderItemInstanceKeys.UIT, uit,
                            OrderItemInstanceKeys.SN, sn,
                            OrderItemInstanceKeys.IMEI, imei
                        )))
                        .build()
                )
            );

        accept(List.of(createEventDto(diff, createSnapshot(snapshot))));

        var enqueueParams = EnqueueParams.create(new OrderItemInstancesUpdateDto(LOM_ORDER_ID));
        verify(orderItemInstancesUpdateDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void instancesWithItemsAddAndReplace() throws JsonProcessingException {
        ArrayNode diff = objectMapper.readValue(
            "[" +
                "{\n" +
                "    \"op\": \"add\",\n" +
                "    \"path\": \"/items/0\",\n" +
                "    \"value\": {\n" +
                "      \"name\": \"Смартфон ASUS ZenFone 3 ZE520KL 32GB золотистый\",\n" +
                "      \"article\": \"10122\",\n" +
                "      \"vendorId\": 10264538,\n" +
                "      \"instances\": null,\n" +
                "      \"cargoTypes\": [\n" +
                "        \"TECH_AND_ELECTRONICS\",\n" +
                "        \"CIS_REQUIRED\"\n" +
                "      ]\n" +
                "    }\n" +
                "  }," +
                "{\n" +
                "    \"op\": \"add\",\n" +
                "    \"path\": \"/items/2\",\n" +
                "    \"value\": {\n" +
                "      \"name\": \"Смартфон ASUS ZenFone 2\",\n" +
                "      \"article\": \"10122\",\n" +
                "      \"vendorId\": 10264538,\n" +
                "      \"instances\": [{\"cis\": \"111abc\"}],\n" +
                "      \"cargoTypes\": [\n" +
                "        \"TECH_AND_ELECTRONICS\",\n" +
                "        \"CIS_REQUIRED\"\n" +
                "      ]\n" +
                "    }\n" +
                "  }," +
                "{\"op\": \"replace\", "
                + " \"path\": \"/items/1/instances\","
                + " \"value\": [{\"cis\": \"123abc\"}], "
                + " \"fromValue\": null}"
                + "]",

            ArrayNode.class
        );

        OrderDto snapshot = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId("1001")
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setItems(
                List.of(
                    ItemDto.builder().build(),
                    ItemDto.builder().instances(List.of(Map.of(OrderItemInstanceKeys.CIS, "111abc"))).build(),
                    ItemDto.builder().instances(List.of(Map.of(OrderItemInstanceKeys.CIS, "123abc"))).build()
                )
            );

        accept(List.of(createEventDto(diff, createSnapshot(snapshot))));

        var enqueueParams = EnqueueParams.create(new OrderItemInstancesUpdateDto(LOM_ORDER_ID));
        verify(orderItemInstancesUpdateDtoQueueProducer).enqueue(enqueueParams);
    }

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "] " + "Постамат: " + ARGUMENTS_PLACEHOLDER)
    @ValueSource(booleans = {true, false})
    @DisplayName("Обновление кода в заказе")
    public void recipientVerificationCode(boolean isLocker) throws JsonProcessingException {
        ArrayNode diff = objectMapper.readValue(
            "[{\"op\": \"replace\", "
                + " \"path\": \"/recipientVerificationCode\", "
                + " \"value\": \"54321\", "
                + " \"fromValue\": \"12345\"}]",
            ArrayNode.class
        );

        OrderDto snapshot = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setDeliveryType(DeliveryType.PICKUP)
            .setExternalId("1001")
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setRecipientVerificationCode("54321")
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.FULFILLMENT)
                    .partnerId(172L)
                    .partnerType(PartnerType.FULFILLMENT)
                    .build(),
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.MOVEMENT)
                    .partnerId(1005471L)
                    .partnerType(PartnerType.DELIVERY)
                    .build(),
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.PICKUP)
                    .partnerId(1003375L)
                    .partnerType(PartnerType.DELIVERY)
                    .partnerSubtype(isLocker ? PartnerSubtype.MARKET_LOCKER : PartnerSubtype.MARKET_OWN_PICKUP_POINT)
                    .build()
            ));

        accept(List.of(createEventDto(diff, createSnapshot(snapshot))));

        var enqueueParams = EnqueueParams.create(
            new OrderVerificationCodeUpdateDto(1001L, "54321", isLocker)
        );
        verify(orderVerificationCodeUpdateDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void orderValidationOrProcessingError() throws Exception {
        ArrayNode validationDiff = objectMapper.readValue(
            "[{\"op\": \"replace\", "
                + " \"path\": \"/status\","
                + " \"value\": \"VALIDATION_ERROR\", "
                + " \"fromValue\": \"VALIDATING\"}]",
            ArrayNode.class
        );

        ArrayNode processingDiff = objectMapper.readValue(
            "[\n" +
                "  {\n" +
                "    \"op\": \"replace\",\n" +
                "    \"path\": \"/status\",\n" +
                "    \"value\": \"PROCESSING_ERROR\",\n" +
                "    \"fromValue\": \"PROCESSING\"\n" +
                "  }\n" +
                "]",
            ArrayNode.class
        );

        JsonNode processingSnapshot = objectMapper.readValue(
            getFileContent("/data/lom/blue_postamat_snapshot.json"),
            JsonNode.class
        );

        var checkouterId = 123L;
        var beruOrderDto = new OrderDto()
            .setId(checkouterId)
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setExternalId(String.valueOf(checkouterId));
        var marketOrderDto = new OrderDto()
            .setId(checkouterId)
            .setPlatformClientId(PlatformClient.YANDEX_MARKET.getId())
            .setExternalId(String.valueOf(checkouterId));

        accept(List.of(
            createEventDto(validationDiff, createSnapshot(beruOrderDto)),
            createEventDto(processingDiff, processingSnapshot),
            createEventDto(validationDiff, createSnapshot(marketOrderDto)),
            createEventDto(processingDiff, createSnapshot(marketOrderDto))
        ));

        verify(orderErrorService, times(2)).setParcelStatusToError(eq(checkouterId), isNull(), anyString());
    }

    @Test
    public void orderChangeDeliveryOptionEventProcessing() throws Exception {
        ArrayNode addChangeOrderDeliveryOptionRequest = objectMapper.readValue(
            "[\n" +
                "  {\n" +
                "    \"op\": \"add\",\n" +
                "    \"path\": \"/changeOrderRequests/0\",\n" +
                "    \"value\": {\n" +
                "      \"requestType\": \"DELIVERY_OPTION\",\n" +
                "      \"status\": \"PROCESSING\"\n" +
                "    },\n" +
                "    \"fromValue\": \"VALIDATING\"\n" +
                "  }\n" +
                "]\n",
            ArrayNode.class
        );

        long checkouterId = 123;
        OrderDto beruOrderDto = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setChangeOrderRequests(List.of(
                ChangeOrderRequestDto.builder()
                    .id(1L)
                    .requestType(ChangeOrderRequestType.DELIVERY_OPTION)
                    .status(ChangeOrderRequestStatus.PROCESSING)
                    .build()
            ))
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setExternalId(String.valueOf(checkouterId));
        accept(List.of(createEventDto(addChangeOrderDeliveryOptionRequest, createSnapshot(beruOrderDto))));

        EnqueueParams<OrderChangeDeliveryOptionDto> enqueueParams = EnqueueParams.create(
            OrderChangeDeliveryOptionDto.builder().lomOrderId(LOM_ORDER_ID).checkouterOrderId(checkouterId).build()
        );
        verify(orderChangeDeliveryOptionDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void orderChangedByPartnerChangeRequestEventProcessing() throws Exception {
        ArrayNode diff = objectMapper.readValue(
            "[\n" +
                "  {\n" +
                "    \"op\": \"replace\",\n" +
                "    \"path\": \"/changeOrderRequests/0\",\n" +
                "    \"value\": \"INFO_RECEIVED\",\n" +
                "    \"fromValue\": \"CREATED\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"op\": \"add\",\n" +
                "    \"path\": \"/changeOrderRequests/0/changeOrderRequestPayloads/0\",\n" +
                "    \"value\": {\n" +
                "      \"id\": 5966,\n" +
                "      \"payload\": [],\n" +
                "      \"changeOrderRequestStatus\": \"INFO_RECEIVED\"\n" +
                "    }\n" +
                "  }\n" +
                "]",
            ArrayNode.class
        );

        JsonNode snapshot = objectMapper.readValue(
            "{" +
                "\"id\": " + LOM_ORDER_ID + ",\n" +
                "\"platformClientId\": 1,\n" +
                "\"externalId\": 1001,\n" +
                "  \"changeOrderRequests\": [\n" +
                "    {\n" +
                "      \"id\": 1,\n" +
                "      \"status\": \"INFO_RECEIVED\",\n" +
                "      \"payloads\": [\n" +
                "        {\n" +
                "          \"status\": \"INFO_RECEIVED\",\n" +
                "          \"payload\": []\n" +
                "        }\n" +
                "      ],\n" +
                "      \"requestType\": \"ORDER_CHANGED_BY_PARTNER\"\n" +
                "    }\n" +
                "  ]" +
                " }",
            JsonNode.class
        );

        accept(List.of(createEventDto(diff, snapshot)));

        EnqueueParams<OrderChangedByPartnerChangeRequestDto> enqueueParams = EnqueueParams.create(
            new OrderChangedByPartnerChangeRequestDto(1L, LOM_ORDER_ID, 1001L)
        );
        verify(orderChangedByPartnerChangeRequestDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void orderChangedByItemNotFoundRequestEventProcessing() throws Exception {
        ArrayNode diff = objectMapper.readValue(
            "[\n" +
                "  {\n" +
                "    \"op\": \"replace\",\n" +
                "    \"path\": \"/changeOrderRequests/0\",\n" +
                "    \"value\": \"INFO_RECEIVED\",\n" +
                "    \"fromValue\": \"CREATED\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"op\": \"add\",\n" +
                "    \"path\": \"/changeOrderRequests/0/changeOrderRequestPayloads/0\",\n" +
                "    \"value\": {\n" +
                "      \"id\": 5966,\n" +
                "      \"payload\": [],\n" +
                "      \"changeOrderRequestStatus\": \"INFO_RECEIVED\"\n" +
                "    }\n" +
                "  }\n" +
                "]",
            ArrayNode.class
        );

        JsonNode snapshot = objectMapper.readValue(
            "{" +
                "\"id\": " + LOM_ORDER_ID + ",\n" +
                "\"platformClientId\": 1,\n" +
                "\"externalId\": 1001,\n" +
                "  \"changeOrderRequests\": [\n" +
                "    {\n" +
                "      \"id\": 1,\n" +
                "      \"status\": \"INFO_RECEIVED\",\n" +
                "      \"payloads\": [\n" +
                "        {\n" +
                "          \"status\": \"INFO_RECEIVED\",\n" +
                "          \"payload\": []\n" +
                "        }\n" +
                "      ],\n" +
                "      \"requestType\": \"ITEM_NOT_FOUND\"\n" +
                "    }\n" +
                "  ]" +
                " }",
            JsonNode.class
        );

        accept(List.of(createEventDto(diff, snapshot)));

        EnqueueParams<OrderChangedByItemNotFoundRequestDto> enqueueParams = EnqueueParams.create(
            new OrderChangedByItemNotFoundRequestDto(1L, LOM_ORDER_ID, 1001L)
        );

        verify(orderChangedByItemNotFoundRequestDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void deliveryDateRequestEventProcessing() throws Exception {
        ArrayNode diff = objectMapper.readValue(
            "[\n" +
                "  {\n" +
                "    \"op\": \"replace\",\n" +
                "    \"path\": \"/changeOrderRequests/0\",\n" +
                "    \"value\": \"INFO_RECEIVED\",\n" +
                "    \"fromValue\": \"CREATED\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"op\": \"add\",\n" +
                "    \"path\": \"/changeOrderRequests/0/changeOrderRequestPayloads/0\",\n" +
                "    \"value\": {\n" +
                "      \"id\": 5966,\n" +
                "      \"payload\": {\n" +
                "        \"barcode\": 1001,\n" +
                "        \"dateMin\": \"2021-03-01\",\n" +
                "        \"dateMax\": \"2021-03-01\",\n" +
                "        \"reason\": \"DELIVERY_DATE_UPDATED_BY_DELIVERY\"\n" +
                "      },\n" +
                "      \"changeOrderRequestStatus\": \"INFO_RECEIVED\"\n" +
                "    }\n" +
                "  }\n" +
                "]",
            ArrayNode.class
        );

        JsonNode snapshot = objectMapper.readValue(
            "{" +
                "\"id\": " + LOM_ORDER_ID + ",\n" +
                "\"platformClientId\": 1,\n" +
                "\"externalId\": 1001,\n" +
                "\"changeOrderRequests\": [\n" +
                "  {\n" +
                "    \"id\": 1,\n" +
                "    \"status\": \"INFO_RECEIVED\",\n" +
                "    \"payloads\": [\n" +
                "      {\n" +
                "        \"status\": \"INFO_RECEIVED\",\n" +
                "        \"payload\": {\n" +
                "          \"barcode\": 1001,\n" +
                "          \"dateMin\": \"2021-03-01\",\n" +
                "          \"dateMax\": \"2021-03-01\",\n" +
                "          \"reason\": \"DELIVERY_DATE_UPDATED_BY_DELIVERY\"\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"requestType\": \"DELIVERY_DATE\"\n" +
                "  }\n" +
                "]" +
                "}",
            JsonNode.class
        );

        accept(List.of(createEventDto(diff, snapshot)));

        EnqueueParams<DeliveryDateChangeRequestDto> enqueueParams = EnqueueParams.create(
            new DeliveryDateChangeRequestDto(1L, LOM_ORDER_ID)
        );

        verify(deliveryDateChangeRequestDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void recipientRequestEventProcessing() throws Exception {
        ArrayNode diff = objectMapper.readValue(
            "[\n" +
                "  {\n" +
                "    \"op\": \"replace\",\n" +
                "    \"path\": \"/changeOrderRequests/0\",\n" +
                "    \"value\": \"INFO_RECEIVED\",\n" +
                "    \"fromValue\": \"CREATED\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"op\": \"add\",\n" +
                "    \"path\": \"/changeOrderRequests/0/changeOrderRequestPayloads/0\",\n" +
                "    \"value\": {\n" +
                "      \"id\": 5966,\n" +
                "      \"payload\": {\n" +
                "           \"barcode\": 234,\n" +
                "           \"checkouterRequestId\": 1,\n" +
                "           \"contact\": {\n" +
                "               \"phone\": \"73334448888\",\n" +
                "               \"firstName\": \"firstName\",\n" +
                "               \"middleName\": \"middleName\",\n" +
                "               \"lastName\": \"lastName\"\n" +
                "           }\n   " +
                "   },\n" +
                "    \"changeOrderRequestStatus\": \"INFO_RECEIVED\"\n" +
                "    }\n" +
                "  }\n" +
                "]",
            ArrayNode.class
        );

        JsonNode snapshot = objectMapper.readValue(
            "{" +
                "\"id\": " + LOM_ORDER_ID + ",\n" +
                "\"platformClientId\": 1,\n" +
                "\"externalId\": 1001,\n" +
                "\"changeOrderRequests\": [\n" +
                "  {\n" +
                "    \"id\": 1,\n" +
                "    \"status\": \"INFO_RECEIVED\",\n" +
                "    \"payloads\": [\n" +
                "      {\n" +
                "        \"status\": \"INFO_RECEIVED\",\n" +
                "        \"payload\": {\n" +
                "           \"barcode\": 234,\n" +
                "           \"checkouterRequestId\": 1,\n" +
                "           \"contact\": {\n" +
                "               \"firstName\": \"firstName\",\n" +
                "               \"middleName\": \"middleName\",\n" +
                "               \"lastName\": \"lastName\",\n" +
                "               \"phone\": \"73334448888\"\n" +
                "           }\n   " +
                "       }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"requestType\": \"RECIPIENT\"\n" +
                "  }\n" +
                "]" +
                "}",
            JsonNode.class
        );

        accept(List.of(createEventDto(diff, snapshot)));

        EnqueueParams<RecipientChangeRequestDto> enqueueParams = EnqueueParams.create(
            RecipientChangeRequestDto.builder()
                .changeRequestId(1L)
                .lomOrderId(LOM_ORDER_ID)
                .build()
        );

        verify(recipientChangeRequestDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    public void shippingDelayedEventProcessing() throws Exception {
        ArrayNode diff = objectMapper.readValue(
            "[\n" +
                "  {\n" +
                "    \"op\": \"add\",\n" +
                "    \"path\": \"/changeOrderRequests/0\",\n" +
                "    \"value\": {\n" +
                "      \"id\": 1,\n" +
                "      \"reason\": \"SHIPPING_DELAYED\",\n" +
                "      \"status\": \"CREATED\",\n" +
                "      \"comment\": null,\n" +
                "      \"requestType\": \"DELIVERY_DATE\",\n" +
                "      \"changeOrderRequestPayloads\": []\n" +
                "    }\n" +
                "  }\n" +
                "]",
            ArrayNode.class
        );

        JsonNode snapshot = objectMapper.readValue(
            "{" +
                "\"id\": " + LOM_ORDER_ID + ",\n" +
                "\"platformClientId\": 1,\n" +
                "\"externalId\": 1001,\n" +
                "\"changeOrderRequests\": [\n" +
                "  {\n" +
                "    \"id\": 1,\n" +
                "    \"reason\": \"SHIPPING_DELAYED\",\n" +
                "    \"status\": \"CREATED\",\n" +
                "    \"comment\": null,\n" +
                "    \"requestType\": \"DELIVERY_DATE\",\n" +
                "    \"changeOrderRequestPayloads\": []\n" +
                "  }\n" +
                "]" +
                "}",
            JsonNode.class
        );

        accept(List.of(createEventDto(diff, snapshot)));

        EnqueueParams<DeliveryDateChangeRequestDto> enqueueParams = EnqueueParams.create(
            new DeliveryDateChangeRequestDto(1L, LOM_ORDER_ID)
        );

        verify(deliveryDateChangeRequestDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    @DisplayName("Процессинг новой заявки на преобразование заказа в заказ с доставкой по клику")
    public void orderChangeToOnDemandEventProcessing() throws Exception {
        long checkouterOrderId = 123;
        long lomChangeOrderRequestId = 456;

        ArrayNode diff = objectMapper.readValue(
            "[\n" +
                "  {\n" +
                "    \"op\": \"add\",\n" +
                "    \"path\": \"/changeOrderRequests/0\",\n" +
                "    \"value\": {\n" +
                "      \"id\": 456,\n" +
                "      \"reason\": \"SHIPPING_DELAYED\",\n" +
                "      \"status\": \"CREATED\",\n" +
                "      \"comment\": null,\n" +
                "      \"requestType\": \"CHANGE_TO_ON_DEMAND\",\n" +
                "      \"changeOrderRequestPayloads\": []\n" +
                "    }\n" +
                "  }\n" +
                "]",
            ArrayNode.class
        );

        JsonNode snapshot = objectMapper.readValue(
            "{" +
                "\"id\": " + LOM_ORDER_ID + ",\n" +
                "\"platformClientId\": 1,\n" +
                "\"externalId\": " + checkouterOrderId + ",\n" +
                "\"changeOrderRequests\": [\n" +
                "  {\n" +
                "    \"id\": 456,\n" +
                "    \"reason\": \"SHIPPING_DELAYED\",\n" +
                "    \"status\": \"CREATED\",\n" +
                "    \"comment\": null,\n" +
                "    \"requestType\": \"CHANGE_TO_ON_DEMAND\",\n" +
                "    \"changeOrderRequestPayloads\": []\n" +
                "  }\n" +
                "]" +
                "}",
            JsonNode.class
        );

        accept(List.of(createEventDto(diff, snapshot)));

        EnqueueParams<OrderChangeToOnDemandRequestDto> enqueueParams = EnqueueParams.create(
            new OrderChangeToOnDemandRequestDto(
                checkouterOrderId,
                lomChangeOrderRequestId,
                ChangeOrderRequestReason.SHIPPING_DELAYED
            )
        );

        verify(orderChangeToOnDemandRequestDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    @DisplayName("Изменение статуса заявки на преобразование заказа в заказ с доставкой по клику - процессинг не нужен")
    public void orderChangeToOnDemandRequestStatusChangeShouldNotBeProcessed() throws Exception {
        long checkouterOrderId = 123;

        ArrayNode diff = objectMapper.readValue(
            "[\n" +
                "  {\n" +
                "    \"op\": \"replace\",\n" +
                "    \"path\": \"/changeOrderRequests/0/status\",\n" +
                "    \"fromValue\": \"CREATED\",\n" +
                "    \"value\": \"PROCESSING\"\n" +
                "  }\n" +
                "]",
            ArrayNode.class
        );

        JsonNode snapshot = objectMapper.readValue(
            "{\n" +
                "\"id\": " + LOM_ORDER_ID + ",\n" +
                "\"platformClientId\": 1,\n" +
                "\"externalId\": " + checkouterOrderId + ",\n" +
                "\"changeOrderRequests\": [\n" +
                "  {\n" +
                "    \"id\": 456,\n" +
                "    \"reason\": \"SHIPPING_DELAYED\",\n" +
                "    \"status\": \"PROCESSING\",\n" +
                "    \"comment\": null,\n" +
                "    \"requestType\": \"CHANGE_TO_ON_DEMAND\",\n" +
                "    \"changeOrderRequestPayloads\": []\n" +
                "  }\n" +
                "]\n" +
                "}",
            JsonNode.class
        );

        accept(List.of(createEventDto(diff, snapshot)));

        verifyZeroInteractions(orderChangeToOnDemandRequestDtoQueueProducer);
    }

    @Test
    @DisplayName("Вызов таски на пересчёт даты доставки")
    public void recalculateRouteDates() throws Exception {
        ArrayNode diff = objectMapper.readValue(
            getBody("/data/integration/service/lomorderevents/recalculate_route_dates_diff.json"),
            ArrayNode.class
        );
        JsonNode snapshot = objectMapper.readValue(
            getBody("/data/integration/service/lomorderevents/recalculate_route_dates_snapshot.json"),
            JsonNode.class
        );
        accept(List.of(createEventDto(diff, snapshot)));

        EnqueueParams<RecalculateRouteDatesDto> enqueueParams = EnqueueParams.create(
            new RecalculateRouteDatesDto(2L, 12345L)
        );

        verify(recalculateRouteDatesDtoQueueProducer).enqueue(enqueueParams);
    }

    @Test
    @DisplayName("Таска на пересчёт даты доставки не создается, т.к. нет изменения статуса заявки")
    public void recalculateRouteDatesNoStatusTransition() throws Exception {
        ArrayNode diff = objectMapper.readValue(
            getBody("/data/integration/service/lomorderevents/recalculate_route_dates_diff_status_not_changed.json"),
            ArrayNode.class
        );
        JsonNode snapshot = objectMapper.readValue(
            getBody("/data/integration/service/lomorderevents/recalculate_route_dates_snapshot.json"),
            JsonNode.class
        );
        accept(List.of(createEventDto(diff, snapshot)));
    }

    @ParameterizedTest
    @MethodSource("notAppropriateStatusTransitions")
    @DisplayName("Таска на пересчёт даты доставки не создается, т.к. нет триггера на переход статуса")
    public void recalculateRouteDatesNotAppropriateStatusTransition(
        ChangeOrderRequestStatus fromStatus,
        ChangeOrderRequestStatus toStatus
    ) throws Exception {
        ArrayNode diff = objectMapper.readValue(
            "[" +
                "  {" +
                "    \"op\": \"replace\"," +
                "    \"fromValue\": \"" + fromStatus + "\"," +
                "    \"path\": \"/changeOrderRequests/0/status\"," +
                "    \"value\": \"" + toStatus + "\"" +
                "  }" +
                "]",
            ArrayNode.class
        );
        JsonNode snapshot = objectMapper.readValue(
            getBody("/data/integration/service/lomorderevents/recalculate_route_dates_snapshot.json"),
            JsonNode.class
        );
        accept(List.of(createEventDto(diff, snapshot)));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("orderFromDrophsipToShippedStatus")
    @SuppressWarnings("unused")
    public void orderChangeCheckouterStatusOnSCExpress(
        String name,
        SegmentStatus segmentStatus,
        List<WaybillSegmentDto> waybillSegmentsDto,
        boolean isEnqueued
    ) {
        processOrder(orderDtoDropshipFFDeliveryWaybills(waybillSegmentsDto), segmentStatus, 1);
        if (isEnqueued) {
            verify(setOrderStatusDtoQueueProducer).enqueue(ENQUEUE_PARAMS);
        }
    }

    @Test
    public void orderChangeCheckouterStatusOnSCExpress() {
        processOrder(orderDtoDropshipFFDeliveryWaybills(dropshipToScWaybillSegmentsDto(null)), SegmentStatus.IN, 0);
    }

    @Test
    public void testDeliveryToStoreStartedFeature() {
        featureProperties.setDeliveryToStoreStatusFeatureEnabled(true);

        ObjectNode diff = objectMapper.createObjectNode();
        diff.put("op", "replace");
        diff.put("path", "/waybill/0/segmentStatus");
        diff.put("value", "TRANSIT_DELIVERY_TRANSPORTATION");
        ArrayNode arrayNode = objectMapper.createArrayNode();
        arrayNode.add(diff);

        OrderDto snapshot = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setDeliveryType(DeliveryType.MOVEMENT)
            .setExternalId("1001")
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setRecipientVerificationCode("54321")
            .setWaybill(
                List.of(
                    WaybillSegmentDto.builder()
                        .id(1L)
                        .segmentType(SegmentType.MOVEMENT)
                        .partnerId(172L)
                        .partnerType(PartnerType.OWN_DELIVERY)
                        .build(),
                    WaybillSegmentDto.builder()
                        .id(2L)
                        .segmentType(SegmentType.GO_PLATFORM)
                        .partnerId(1005471L)
                        .partnerType(PartnerType.DELIVERY)
                        .waybillSegmentTags(List.of(WaybillSegmentTag.DEFERRED_COURIER))
                        .build()
                )
            );

        accept(List.of(createEventDto(arrayNode, createSnapshot(snapshot))));

        verify(setOrderStatusDtoQueueProducer).enqueue(
            EnqueueParams.create(
                SetOrderStatusDto.builder()
                    .orderId(1001L)
                    .status(OrderStatus.DELIVERY)
                    .substatus(null)
                    .build()
            )
        );

        verify(setOrderStatusDtoQueueProducer).enqueue(
            EnqueueParams.create(
                SetOrderStatusDto.builder()
                    .orderId(1001L)
                    .status(OrderStatus.DELIVERY)
                    .substatus(OrderSubstatus.DELIVERY_TO_STORE_STARTED)
                    .build()
            )
        );
    }

    @Nonnull
    private OrderDto orderDtoDropshipFFDeliveryWaybills(List<WaybillSegmentDto> dropshipToWaybillSegment) {
        OrderDto orderDto = createOrderDto(PlatformClient.BERU, String.valueOf(CHECKOUTER_ORDER_ID));
        orderDto.setWaybill(List.of(
            dropshipToWaybillSegment.get(0),
            dropshipToWaybillSegment.get(1),
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.PICKUP)
                .partnerId(1003375L)
                .partnerType(PartnerType.DELIVERY)
                .build()
        ));
        return orderDto;
    }

    @Nonnull
    private static Stream<Arguments> orderFromDrophsipToShippedStatus() {
        return Stream.of(
            Arguments.of(
                "Получение 110 ЧП от СЦ",
                SegmentStatus.IN,
                dropshipToScWaybillSegmentsDto(null),
                true
            ),
            Arguments.of(
                "Получение 35 ЧП, для экспресса",
                SegmentStatus.TRANSIT_COURIER_RECEIVED,
                dropshipToScWaybillSegmentsDto(List.of(WaybillSegmentTag.EXPRESS)),
                false
            ),
            Arguments.of(
                "Получение 35 ЧП, не экспресс",
                SegmentStatus.TRANSIT_COURIER_RECEIVED,
                dropshipToScWaybillSegmentsDto(null),
                false
            ),
            Arguments.of(
                "Получение 110 ЧП, для экспресса",
                SegmentStatus.IN,
                dropshipToScWaybillSegmentsDto(List.of(WaybillSegmentTag.EXPRESS)),
                true
            ),
            Arguments.of(
                "Получение 110 ЧП, не от СЦ",
                SegmentStatus.IN,
                List.of(
                    WaybillSegmentDto.builder()
                        .segmentType(SegmentType.FULFILLMENT)
                        .partnerId(172L)
                        .partnerType(PartnerType.DROPSHIP)
                        .build(),
                    WaybillSegmentDto.builder()
                        .segmentType(SegmentType.FULFILLMENT)
                        .partnerId(172L)
                        .partnerType(PartnerType.DELIVERY)
                        .build()
                ),
                false
            )
        );
    }

    @Nonnull
    private static List<WaybillSegmentDto> dropshipToScWaybillSegmentsDto(@Nullable List<WaybillSegmentTag> tags) {
        return List.of(
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.FULFILLMENT)
                .partnerId(172L)
                .partnerType(PartnerType.DROPSHIP)
                .waybillSegmentTags(tags)
                .build(),
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.SORTING_CENTER)
                .partnerId(172L)
                .partnerType(PartnerType.SORTING_CENTER)
                .waybillSegmentTags(tags)
                .build()
        );
    }

    @Nonnull
    private static Stream<Arguments> notAppropriateStatusTransitions() {
        List<ChangeOrderRequestStatus> allStatuses = List.of(ChangeOrderRequestStatus.values());
        return StreamEx.cartesianPower(2, allStatuses)
            .map(transition -> Pair.of(transition.get(0), transition.get(1)))
            .filter(transition -> !APPROPRIATE_RDD_REQUEST_STATUS_TRANSITIONS.contains(transition))
            .map(transition -> Arguments.of(transition.getLeft(), transition.getRight()));
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Обновление сабстатуса заказа в чекаутере по чекпоинту не экспресс MOVEMENT сегмента")
    public void deliverySubstatusUpdate(
        SegmentStatus segmentStatus,
        OrderStatus orderStatus,
        OrderSubstatus orderSubstatus
    ) {
        var checkouterId = 1232131L;
        processOrder(createOrderDto(PlatformClient.BERU, String.valueOf(checkouterId)), segmentStatus, 1);

        var enqueueParams = EnqueueParams.create(
            SetOrderStatusDto.builder()
                .orderId(checkouterId)
                .status(orderStatus)
                .substatus(orderSubstatus)
                .build()
        );
        verify(setOrderStatusDtoQueueProducer).enqueue(enqueueParams);
    }

    @Nonnull
    private static Stream<Arguments> deliverySubstatusUpdate() {
        return StreamEx
            .of(MOVEMENT_SUBSTATUSES.entrySet())
            .append(MOVEMENT_PICKUP_NOT_EXPRESS_SUBSTATUSES.entrySet())
            .map(entry -> Arguments.of(
                entry.getKey(),
                entry.getValue().getStatus(),
                entry.getValue().getSubstatus()
            ));
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName(
        "Сабстатус заказа в чекаутере не обновляется для неподдерживаемых чекпоинтов не экспресс MOVEMENT сегмента"
    )
    public void deliverySubstatusNotUpdate(SegmentStatus segmentStatus) {
        var checkouterId = 1232131L;
        processOrder(createOrderDto(PlatformClient.BERU, String.valueOf(checkouterId)), segmentStatus, 1);

        verifyZeroInteractions(setOrderStatusDtoQueueProducer);
    }

    @Nonnull
    private static Stream<Arguments> deliverySubstatusNotUpdate() {
        return Stream.of(SegmentStatus.values())
            .filter(segmentStatus -> !MOVEMENT_SUBSTATUSES.containsKey(segmentStatus))
            .filter(segmentStatus -> !MOVEMENT_PICKUP_NOT_EXPRESS_SUBSTATUSES.containsKey(segmentStatus))
            .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Обновление сабстатуса заказа в чекаутере по чекпоинту экспресс MOVEMENT сегмента")
    public void deliveryExpressSubstatusUpdate(
        SegmentStatus segmentStatus,
        OrderStatus orderStatus,
        OrderSubstatus orderSubstatus
    ) {
        var checkouterId = 1232131L;
        processOrder(createExpressOrderDto(String.valueOf(checkouterId)), segmentStatus, 1);

        var enqueueParams = EnqueueParams.create(
            SetOrderStatusDto.builder()
                .orderId(checkouterId)
                .status(orderStatus)
                .substatus(orderSubstatus)
                .build()
        );
        verify(setOrderStatusDtoQueueProducer).enqueue(enqueueParams);
    }

    @Nonnull
    private static Stream<Arguments> deliveryExpressSubstatusUpdate() {
        return MOVEMENT_EXPRESS_SUBSTATUSES.entrySet().stream().map(entry -> Arguments.of(
            entry.getKey(),
            entry.getValue().getStatus(),
            entry.getValue().getSubstatus()
        ));
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName(
        "Сабстатус заказа в чекаутере не обновляется для неподдерживаемых чекпоинтов экспресс MOVEMENT сегмента"
    )
    public void deliveryExpressSubstatusNotUpdate(SegmentStatus segmentStatus) {
        var checkouterId = 1232131L;
        processOrder(createExpressOrderDto(String.valueOf(checkouterId)), segmentStatus, 1);

        verifyZeroInteractions(setOrderStatusDtoQueueProducer);
    }

    @Nonnull
    private static Stream<Arguments> deliveryExpressSubstatusNotUpdate() {
        return Stream.of(SegmentStatus.values())
            .filter(segmentStatus -> !MOVEMENT_EXPRESS_SUBSTATUSES.containsKey(segmentStatus))
            .map(Arguments::of);
    }

    @Nonnull
    private OrderDto createExpressOrderDto(String externalId) {
        return new OrderDto()
            .setId(1001L)
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setDeliveryType(DeliveryType.PICKUP)
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.FULFILLMENT)
                    .partnerId(172L)
                    .build(),
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.MOVEMENT)
                    .partnerId(1005471L)
                    .waybillSegmentTags(List.of(WaybillSegmentTag.CALL_COURIER))
                    .build(),
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.PICKUP)
                    .partnerId(1003375L)
                    .partnerType(PartnerType.DELIVERY)
                    .build()
            ))
            .setExternalId(externalId);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Обновление кода ПВЗ")
    @MethodSource("updatePickupStatus")
    public void updatePickupStatusPvz(
        @SuppressWarnings("unused") String displayName,
        boolean lockerCodeEnabled,
        long lockerCodeStartOrderId
    ) {
        lockerCodeProperties.setEnabled(lockerCodeEnabled);
        lockerCodeProperties.setStartOrderId(lockerCodeStartOrderId);

        long checkouterId = 1001;
        processOrder(
            createOrderDto(PlatformClient.BERU, String.valueOf(checkouterId), false),
            SegmentStatus.TRANSIT_PICKUP,
            2
        );

        var enqueueParams = EnqueueParams.create(
            SetOrderStatusDto.builder()
                .orderId(checkouterId)
                .status(OrderStatus.PICKUP)
                .substatus(OrderSubstatus.PICKUP_SERVICE_RECEIVED)
                .build()
        );
        verify(setOrderStatusDtoQueueProducer).enqueue(enqueueParams);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Обновление кода Постамата")
    @MethodSource("updatePickupStatus")
    public void updatePickupStatusLocker(
        @SuppressWarnings("unused") String displayName,
        boolean lockerCodeEnabled,
        long lockerCodeStartOrderId
    ) {
        lockerCodeProperties.setEnabled(lockerCodeEnabled);
        lockerCodeProperties.setStartOrderId(lockerCodeStartOrderId);

        long checkouterId = 1001;
        processOrder(
            createOrderDto(PlatformClient.BERU, String.valueOf(checkouterId), true),
            SegmentStatus.TRANSIT_PICKUP,
            2
        );

        var enqueueParams = EnqueueParams.create(
            SetOrderStatusDto.builder()
                .orderId(checkouterId)
                .status(OrderStatus.PICKUP)
                .substatus(OrderSubstatus.PICKUP_SERVICE_RECEIVED)
                .build()
        );

        if (
            !lockerCodeEnabled
                || lockerCodeStartOrderId == 0
                || checkouterId < lockerCodeStartOrderId
        ) {
            verify(setOrderStatusDtoQueueProducer).enqueue(enqueueParams);
        }
    }

    private static Stream<Arguments> updatePickupStatus() {
        return Stream.of(
            Arguments.of("Проброс кода постамата: отключен", false, 0),
            Arguments.of("Проброс кода постамата: только логирование", false, 1),
            Arguments.of("Проброс кода постамата: включен, заказ после переключения", true, 1),
            Arguments.of("Проброс кода постамата: включен, заказ перед переключением", true, 1002)
        );
    }

    @Nonnull
    private String getBody(@Nonnull String filePath) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(filePath);
        return IOUtils.toString(inputStream, UTF_8);
    }

    private void accept(List<EventDto> eventDto) {
        eventDto.forEach(e -> {
            lomOrderEventService.processEvent(e);
            verify(lomEventsSuccessLogger).logEvent(e);
        });
    }

    private void processOrder(OrderDto orderDto, SegmentStatus segmentStatus, Integer segmentIndex) {
        accept(List.of(createEventDto(createStatusDiff(segmentStatus, segmentIndex), createSnapshot(orderDto))));
        verifyNoMoreInteractions(orderErrorService);
    }

    private void processOrder(PlatformClient beru, String externalId) {
        processOrder(createOrderDto(beru, externalId), SegmentStatus.IN, 1);
    }

    private void processOrder(PlatformClient beru, String externalId, SegmentStatus status) {
        processOrder(createOrderDto(beru, externalId), status, 1);
    }

    @Nonnull
    private EventDto createEventDto(JsonNode diff, JsonNode snapshot) {
        return new EventDto()
            .setEntityId(LOM_ORDER_ID)
            .setEntityType(EntityType.ORDER)
            .setDiff(diff)
            .setSnapshot(snapshot);
    }

    @Nonnull
    @SneakyThrows
    private JsonNode createSnapshot(OrderDto orderDto) {
        return objectMapper.valueToTree(orderDto);
    }

    @Nonnull
    @SneakyThrows
    private JsonNode createSnapshot() {
        var order = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId("1001")
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setWaybill(
                List.of(
                    WaybillSegmentDto.builder()
                        .id(LOM_WAYBILL_SEGMENT_ID)
                        .build()
                )
            );
        return createSnapshot(order);
    }
}
