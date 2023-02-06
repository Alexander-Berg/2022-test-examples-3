package ru.yandex.market.delivery.mdbapp.integration.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.collections.Triple;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
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
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbychangerequest.OrderChangedByPartnerChangeRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbychangerequest.OrderChangedByPartnerChangeRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbyitemnotfound.OrderChangedByItemNotFoundRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbyitemnotfound.OrderChangedByItemNotFoundRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.instances.OrderItemInstancesUpdateDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.instances.OrderItemInstancesUpdateService;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.notsupplied.OrderItemIsNotSuppliedRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.notsupplied.OrderItemIsNotSuppliedRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.track.add.AddTrackFromLomEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.queue.track.add.dto.AddTrackDto;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.configuration.LockerCodeProperties;
import ru.yandex.market.delivery.mdbapp.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.tags.WaybillSegmentTag;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.delivery.mdbapp.integration.service.LomOrderEventService.PICKUP_SUBSTATUSES_MAP;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class LomOrderEventParameterizedServiceTest extends AllMockContextualTest {

    private static final long LOM_ORDER_ID = 12345L;
    private static final long CHECKOUTER_ORDER_ID = 1232131L;
    private static final int MIDDLE_MILE_INDEX = 1;
    private static final int LAST_MILE_INDEX = 2;
    private static final Set<DeliveryType> SUPPORTED_DELIVERY_TYPES = Set.of(DeliveryType.PICKUP, DeliveryType.POST);
    private static final Set<Pair<SegmentStatus, Integer>> IGNORED = Set.of(
        Pair.of(SegmentStatus.TRANSIT_PICKUP, MIDDLE_MILE_INDEX)
    );
    private static final Set<Pair<SegmentStatus, Integer>> IGNORED_EXPRESS = Set.of(
        Pair.of(SegmentStatus.TRANSIT_PICKUP, MIDDLE_MILE_INDEX),
        Pair.of(SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT, MIDDLE_MILE_INDEX)
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SegmentStatus segmentStatus;

    private final DeliveryType deliveryType;

    private final PlatformClient platformClient;

    private final int waybillSegmentIndex;

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

    public LomOrderEventParameterizedServiceTest(
        SegmentStatus segmentStatus,
        DeliveryType deliveryType,
        PlatformClient platformClient,
        int waybillSegmentIndex
    ) {
        this.segmentStatus = segmentStatus;
        this.deliveryType = deliveryType;
        this.platformClient = platformClient;
        this.waybillSegmentIndex = waybillSegmentIndex;
    }

    @Nonnull
    @Parameters(name = "{index}: status = {0}, deliveryType = {1}, platformClient = {2}, waybillSegmentIndex = {3}")
    public static Collection<Object[]> parameters() {
        return Stream.of(SegmentStatus.TRANSIT_PICKUP, SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT)
            .flatMap(status -> Arrays.stream(DeliveryType.values()).map(delType -> new Pair<>(status, delType)))
            .flatMap(
                pair -> Stream.of(PlatformClient.BERU, PlatformClient.DBS)
                    .map(index -> Triple.of(pair.getFirst(), pair.getSecond(), index))
            )
            .flatMap(
                triple -> Stream.of(MIDDLE_MILE_INDEX, LAST_MILE_INDEX)
                    .map(index -> new Object[]{triple.first, triple.second, triple.third, index})
            )
            .collect(Collectors.toList());
    }

    @Before
    public void beforeTest() {
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
            new FeatureProperties(),
            new CheckouterSetOrderStatusEnqueueService(setOrderStatusDtoQueueProducer)
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(setOrderStatusDtoQueueProducer);
    }

    @Test
    public void orderChangeCheckouterStatusPickupDeliveryTypeCourier() {
        processOrder(String.valueOf(CHECKOUTER_ORDER_ID), List.of());

        var enqueueParams = EnqueueParams
            .create(
                SetOrderStatusDto.builder()
                    .orderId(CHECKOUTER_ORDER_ID)
                    .status(OrderStatus.PICKUP)
                    .substatus(PICKUP_SUBSTATUSES_MAP.get(segmentStatus))
                    .build()
            );

        if (
            SUPPORTED_DELIVERY_TYPES.contains(deliveryType)
                && !IGNORED.contains(Pair.of(segmentStatus, waybillSegmentIndex))
        ) {
            Mockito.verify(setOrderStatusDtoQueueProducer).enqueue(enqueueParams);
        }
    }

    @Test
    public void orderChangeCheckouterStatusPickupDeliveryTypeExpressMovement() {
        processOrder(String.valueOf(CHECKOUTER_ORDER_ID), List.of(WaybillSegmentTag.CALL_COURIER));

        var enqueueParams = EnqueueParams
            .create(
                SetOrderStatusDto.builder()
                    .orderId(CHECKOUTER_ORDER_ID)
                    .status(OrderStatus.PICKUP)
                    .substatus(PICKUP_SUBSTATUSES_MAP.get(segmentStatus))
                    .build()
            );

        if (
            SUPPORTED_DELIVERY_TYPES.contains(deliveryType)
                && !IGNORED_EXPRESS.contains(Pair.of(segmentStatus, waybillSegmentIndex))
        ) {
            Mockito.verify(setOrderStatusDtoQueueProducer).enqueue(enqueueParams);
        }
    }

    private void processOrder(String externalId, List<WaybillSegmentTag> middleMileWaybillSegmentTags) {
        processOrder(createOrderDto(externalId, middleMileWaybillSegmentTags, deliveryType));
    }

    private void processOrder(OrderDto orderDto) {
        ArrayNode arrayNode = createStatusDiff(segmentStatus);
        accept(List.of(createEventDto(arrayNode, createSnapshot(orderDto))));
        Mockito.verifyNoMoreInteractions(orderErrorService);
    }

    @Nonnull
    private ArrayNode createStatusDiff(SegmentStatus segmentStatus) {
        ObjectNode segmentStatusDiff = objectMapper.createObjectNode();
        segmentStatusDiff.put("op", "replace");
        segmentStatusDiff.put("path", "/waybill/" + waybillSegmentIndex + "/segmentStatus");
        segmentStatusDiff.put("value", segmentStatus.name());

        return objectMapper.createArrayNode().add(segmentStatusDiff);
    }

    @Nonnull
    private OrderDto createOrderDto(
        String externalId,
        List<WaybillSegmentTag> middleMileWaybillSegmentTags,
        DeliveryType deliveryType
    ) {
        return new OrderDto()
            .setId(1001L)
            .setPlatformClientId(platformClient.getId())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.FULFILLMENT)
                    .partnerId(172L)
                    .partnerType(PartnerType.FULFILLMENT)
                    .build(),
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.MOVEMENT)
                    .partnerId(1005471L)
                    .partnerType(PartnerType.SORTING_CENTER)
                    .waybillSegmentTags(middleMileWaybillSegmentTags)
                    .build(),
                WaybillSegmentDto.builder()
                    .segmentType(SegmentType.PICKUP)
                    .partnerId(1003375L)
                    .partnerType(PartnerType.DELIVERY)
                    .build()
            ))
            .setDeliveryType(deliveryType)
            .setExternalId(externalId);
    }

    private void accept(List<EventDto> eventDto) {
        eventDto.forEach(e -> {
            lomOrderEventService.processEvent(e);
            Mockito.verify(lomEventsSuccessLogger).logEvent(e);
        });
    }

    @Nonnull
    private EventDto createEventDto(JsonNode diff, JsonNode snapshot) {
        return new EventDto()
            .setEntityId(LOM_ORDER_ID)
            .setEntityType(EntityType.ORDER)
            .setDiff(diff)
            .setSnapshot(snapshot);
    }

    @SneakyThrows
    private JsonNode createSnapshot(OrderDto orderDto) {
        return objectMapper.valueToTree(orderDto);
    }
}
