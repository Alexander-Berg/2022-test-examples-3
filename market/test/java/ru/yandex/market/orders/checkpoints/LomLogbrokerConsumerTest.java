package ru.yandex.market.orders.checkpoints;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.billing.OrderCheckpointDao;
import ru.yandex.market.core.order.model.OrderCheckpoint;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.utils.logbroker.MessageBatchBuilder;
import ru.yandex.market.utils.logbroker.MessageBatchItem;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.order.model.CheckpointType.CLAIM_PAID;
import static ru.yandex.market.core.order.model.CheckpointType.CLAIM_STARTED;
import static ru.yandex.market.core.order.model.CheckpointType.DELIVERY_STORAGE_PERIOD_EXPIRED;
import static ru.yandex.market.core.order.model.CheckpointType.LOST;
import static ru.yandex.market.core.order.model.CheckpointType.ORDER_SORTING_CENTER_RETURN_PREPARED_FOR_UTILIZE;
import static ru.yandex.market.core.order.model.CheckpointType.ORDER_SORTING_CENTER_RETURN_SHIPPED_FOR_UTILIZER;
import static ru.yandex.market.core.order.model.CheckpointType.RETURN_PREPARING;
import static ru.yandex.market.core.order.model.CheckpointType.RETURN_TRANSMITTED_FULFILMENT;
import static ru.yandex.market.core.order.model.CheckpointType.SORTING_CENTER_RETURN_ARRIVED;

/**
 * Тест для {@link LomLogbrokerConsumer}.
 */
class LomLogbrokerConsumerTest extends FunctionalTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private LomLogbrokerConsumer lomLogbrokerConsumer;

    @Autowired
    private OrderCheckpointDao orderCheckpointDao;

    @BeforeEach
    void before() {
        orderCheckpointDao = Mockito.spy(orderCheckpointDao);
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));
        lomLogbrokerConsumer = new LomLogbrokerConsumer(orderCheckpointDao, retryTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCorrectImport() {
        lomLogbrokerConsumer.process(
                new MessageBatchBuilder<OrderItem>()
                        .addItem(new OrderItem(new OrderDto()
                                .setPlatformClientId(PlatformClient.BERU.getId())
                                .setId(123L)
                                .setExternalId("1")
                                .setReturnSortingCenterId(333L)
                                .setWaybill(List.of(
                                        WaybillSegmentDto.builder()
                                                .partnerType(PartnerType.SORTING_CENTER)
                                                .segmentType(SegmentType.SORTING_CENTER)
                                                .partnerId(776L) // SORTING_CENTER.RETURN_PREPARING - not process
                                                .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                        .id(10L)
                                                        .status(SegmentStatus.RETURN_PREPARING)
                                                        .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                        .build()))
                                                .warehouseLocation(LocationDto.builder()
                                                        .warehouseId(1000000333L)
                                                        .build())
                                                .build(), WaybillSegmentDto.builder()
                                                .partnerType(PartnerType.SORTING_CENTER)
                                                .segmentType(SegmentType.SORTING_CENTER)
                                                .partnerId(777L) // dropship wharehouse id != returnSortingCenterId
                                                .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                        .id(11L)
                                                        .status(SegmentStatus.RETURN_ARRIVED)
                                                        .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                        .build()))
                                                .warehouseLocation(LocationDto.builder()
                                                        .warehouseId(1000000333L)
                                                        .build())
                                                .build(),
                                        WaybillSegmentDto.builder()
                                                .partnerType(PartnerType.SORTING_CENTER)
                                                .segmentType(SegmentType.SORTING_CENTER)
                                                .partnerId(333L)
                                                .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                        .id(12L)
                                                        .status(SegmentStatus.RETURN_ARRIVED)
                                                        .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                        .build()))
                                                .warehouseLocation(LocationDto.builder()
                                                        .warehouseId(1000000333L)
                                                        .build())
                                                .build())
                                )
                        ))
                        .addItem(new OrderItem(new OrderDto()
                                .setPlatformClientId(PlatformClient.BERU.getId())
                                .setId(108L)
                                .setExternalId("1")
                                .setReturnSortingCenterId(333L)
                                .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                        .partnerType(PartnerType.DELIVERY)
                                        .segmentType(SegmentType.PICKUP)
                                        .partnerId(133L)
                                        .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                .id(13L)
                                                .status(SegmentStatus.RETURNED)
                                                .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                .build()))
                                        .build()))
                        ))
                        .addItem(new OrderItem(new OrderDto()
                                .setPlatformClientId(PlatformClient.BERU.getId())
                                .setId(109L)
                                .setExternalId("1")
                                .setReturnSortingCenterId(333L)
                                .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                        .partnerType(PartnerType.DELIVERY)
                                        .segmentType(SegmentType.COURIER)
                                        .partnerId(133L)
                                        .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                .id(14L)
                                                .status(SegmentStatus.RETURN_PREPARING)
                                                .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                .build()))
                                        .build()))
                        ))
                        .addItem(new OrderItem(new OrderDto()
                                .setPlatformClientId(PlatformClient.BERU.getId())
                                .setId(111L)
                                .setExternalId("1")
                                .setReturnSortingCenterId(333L)
                                .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                        .partnerType(PartnerType.DELIVERY)
                                        .segmentType(SegmentType.POST)
                                        .partnerId(133L)
                                        .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                .id(15L)
                                                .status(SegmentStatus.ERROR_LOST)
                                                .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                .build()))
                                        .build()))
                        ))
                        .addItem(new OrderItem(new OrderDto()
                                .setPlatformClientId(PlatformClient.BERU.getId())
                                .setId(222L)
                                .setExternalId("1")
                                .setReturnSortingCenterId(333L)
                                .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                        .partnerType(PartnerType.SORTING_CENTER)
                                        .segmentType(SegmentType.SORTING_CENTER)
                                        .partnerId(333L)
                                        .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                .id(16L)
                                                .status(SegmentStatus.ERROR_LOST)
                                                .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                .build()))
                                        .build()))
                        ))
                        .addItem(new OrderItem(new OrderDto()
                                .setPlatformClientId(PlatformClient.BERU.getId())
                                .setId(223L)
                                .setExternalId("1")
                                .setReturnSortingCenterId(335L)
                                .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                        .partnerType(PartnerType.SORTING_CENTER)
                                        .segmentType(SegmentType.SORTING_CENTER)
                                        .partnerId(333L)
                                        .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                .id(17L)
                                                .status(SegmentStatus.ERROR_LOST)
                                                .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                .build()))
                                        .build()))
                        ))
                        .addItem(new OrderItem(new OrderDto()
                                .setPlatformClientId(PlatformClient.BERU.getId())
                                .setId(224L)
                                .setExternalId("1")
                                .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                        .partnerType(PartnerType.DELIVERY)
                                        .segmentType(SegmentType.COURIER)
                                        .partnerId(333L)
                                        .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                .id(18L)
                                                .status(SegmentStatus.CLAIM_STARTED)
                                                .date(LocalDateTime.of(2021, 9, 21, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                .build()))
                                        .build()))
                        ))
                        .addItem(new OrderItem(new OrderDto()
                                .setPlatformClientId(PlatformClient.BERU.getId())
                                .setId(225L)
                                .setExternalId("1")
                                .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                        .partnerType(PartnerType.DELIVERY)
                                        .segmentType(SegmentType.COURIER)
                                        .partnerId(333L)
                                        .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                .id(19L)
                                                .status(SegmentStatus.CLAIM_PAID)
                                                .date(LocalDateTime.of(2021, 9, 25, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                .build()))
                                        .build()))
                        ))
                        .addItem(new OrderItem(new OrderDto()
                                .setPlatformClientId(PlatformClient.DBS.getId())
                                .setId(226L)
                                .setExternalId("1")
                                .setReturnSortingCenterId(133L)
                                .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                        .partnerType(PartnerType.DELIVERY)
                                        .segmentType(SegmentType.PICKUP)
                                        .partnerId(133L)
                                        .shipment(WaybillSegmentDto.ShipmentDto.builder()
                                                .locationTo(LocationDto.builder().warehouseId(144L).build())
                                                .build())
                                        .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                .id(20L)
                                                .status(SegmentStatus.TRANSIT_STORAGE_PERIOD_EXPIRED)
                                                .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                .build()))
                                        .build()))
                        ))
                        .addItem(new OrderItem(new OrderDto()
                                .setPlatformClientId(PlatformClient.BERU.getId())
                                .setId(226L)
                                .setExternalId("1")
                                .setReturnSortingCenterId(333L)
                                .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                        .partnerType(PartnerType.SORTING_CENTER)
                                        .segmentType(SegmentType.SORTING_CENTER)
                                        .partnerId(333L)
                                        .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                .id(21L)
                                                .status(SegmentStatus.RETURN_PREPARED_FOR_UTILIZE)
                                                .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                .build()))
                                        .build()))
                        ))
                        .addItem(new OrderItem(new OrderDto()
                                .setPlatformClientId(PlatformClient.BERU.getId())
                                .setId(227L)
                                .setExternalId("1")
                                .setReturnSortingCenterId(333L)
                                .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                        .partnerType(PartnerType.SORTING_CENTER)
                                        .segmentType(SegmentType.SORTING_CENTER)
                                        .partnerId(333L)
                                        .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                                .id(22L)
                                                .status(SegmentStatus.RETURN_SHIPPED_FOR_UTILIZER)
                                                .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                                .build()))
                                        .build()))
                        ))
                        .build()
        );

        ArgumentCaptor<Collection<OrderCheckpoint>> argumentCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(orderCheckpointDao).mergeCheckpoints(argumentCaptor.capture());
        Collection<OrderCheckpoint> capturedArgument = argumentCaptor.getValue();
        assertThat(capturedArgument).hasSize(10);
        assertThat(capturedArgument)
                .element(0)
                .asInstanceOf(InstanceOfAssertFactories.type(OrderCheckpoint.class))
                .extracting(OrderCheckpoint::getId, OrderCheckpoint::getOrderId, OrderCheckpoint::getPartnerId,
                        OrderCheckpoint::getPartnerType, OrderCheckpoint::getCheckpointType,
                        OrderCheckpoint::isPickupable, OrderCheckpoint::getPickupLogisticPointId)
                .containsExactly(12L, 1L, 333L, PartnerType.SORTING_CENTER, SORTING_CENTER_RETURN_ARRIVED,
                        true, 1000000333L);
        assertThat(capturedArgument)
                .element(1)
                .asInstanceOf(InstanceOfAssertFactories.type(OrderCheckpoint.class))
                .extracting(OrderCheckpoint::getId, OrderCheckpoint::getOrderId, OrderCheckpoint::getPartnerId,
                        OrderCheckpoint::getPartnerType, OrderCheckpoint::getCheckpointType)
                .containsExactly(13L, 1L, 133L, PartnerType.DELIVERY, RETURN_TRANSMITTED_FULFILMENT);
        assertThat(capturedArgument)
                .element(2)
                .asInstanceOf(InstanceOfAssertFactories.type(OrderCheckpoint.class))
                .extracting(OrderCheckpoint::getId, OrderCheckpoint::getOrderId, OrderCheckpoint::getPartnerId,
                        OrderCheckpoint::getPartnerType, OrderCheckpoint::getCheckpointType)
                .containsExactly(14L, 1L, 133L, PartnerType.DELIVERY, RETURN_PREPARING);
        assertThat(capturedArgument)
                .element(3)
                .asInstanceOf(InstanceOfAssertFactories.type(OrderCheckpoint.class))
                .extracting(OrderCheckpoint::getId, OrderCheckpoint::getOrderId, OrderCheckpoint::getPartnerId,
                        OrderCheckpoint::getPartnerType, OrderCheckpoint::getCheckpointType)
                .containsExactly(15L, 1L, 133L, PartnerType.DELIVERY, LOST);
        assertThat(capturedArgument)
                .element(4)
                .asInstanceOf(InstanceOfAssertFactories.type(OrderCheckpoint.class))
                .extracting(OrderCheckpoint::getId, OrderCheckpoint::getOrderId, OrderCheckpoint::getPartnerId,
                        OrderCheckpoint::getPartnerType, OrderCheckpoint::getCheckpointType)
                .containsExactly(16L, 1L, 333L, PartnerType.SORTING_CENTER, LOST);
        assertThat(capturedArgument)
                .element(5)
                .asInstanceOf(InstanceOfAssertFactories.type(OrderCheckpoint.class))
                .extracting(OrderCheckpoint::getId, OrderCheckpoint::getOrderId, OrderCheckpoint::getPartnerId,
                        OrderCheckpoint::getPartnerType, OrderCheckpoint::getCheckpointType)
                .containsExactly(18L, 1L, 333L, PartnerType.DELIVERY, CLAIM_STARTED);
        assertThat(capturedArgument)
                .element(6)
                .asInstanceOf(InstanceOfAssertFactories.type(OrderCheckpoint.class))
                .extracting(OrderCheckpoint::getId, OrderCheckpoint::getOrderId, OrderCheckpoint::getPartnerId,
                        OrderCheckpoint::getPartnerType, OrderCheckpoint::getCheckpointType)
                .containsExactly(19L, 1L, 333L, PartnerType.DELIVERY, CLAIM_PAID);
        assertThat(capturedArgument)
                .element(7)
                .asInstanceOf(InstanceOfAssertFactories.type(OrderCheckpoint.class))
                .extracting(OrderCheckpoint::getId, OrderCheckpoint::getOrderId, OrderCheckpoint::getPartnerId,
                        OrderCheckpoint::getPartnerType, OrderCheckpoint::getCheckpointType,
                        OrderCheckpoint::getPickupLogisticPointId)
                .containsExactly(20L, 1L, 133L, PartnerType.DELIVERY, DELIVERY_STORAGE_PERIOD_EXPIRED, 144L);
        assertThat(capturedArgument)
                .element(8)
                .asInstanceOf(InstanceOfAssertFactories.type(OrderCheckpoint.class))
                .extracting(OrderCheckpoint::getId, OrderCheckpoint::getOrderId, OrderCheckpoint::getPartnerId,
                        OrderCheckpoint::getPartnerType, OrderCheckpoint::getCheckpointType)
                .containsExactly(21L, 1L, 333L, PartnerType.SORTING_CENTER,
                        ORDER_SORTING_CENTER_RETURN_PREPARED_FOR_UTILIZE);
        assertThat(capturedArgument)
                .element(9)
                .asInstanceOf(InstanceOfAssertFactories.type(OrderCheckpoint.class))
                .extracting(OrderCheckpoint::getId, OrderCheckpoint::getOrderId, OrderCheckpoint::getPartnerId,
                        OrderCheckpoint::getPartnerType, OrderCheckpoint::getCheckpointType)
                .containsExactly(22L, 1L, 333L, PartnerType.SORTING_CENTER,
                        ORDER_SORTING_CENTER_RETURN_SHIPPED_FOR_UTILIZER);
        Assertions.assertTrue(LomLogbrokerSolomonMetrics.returnArrivedRate(PartnerType.SORTING_CENTER).get() > 0);
    }

    @Test
    void testUnrecognizedField() {
        OrderDto orderDto = new OrderDto()
                .setPlatformClientId(PlatformClient.BERU.getId())
                .setId(123L)
                .setExternalId("1")
                .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                        .partnerType(PartnerType.SORTING_CENTER)
                        .segmentType(SegmentType.SORTING_CENTER)
                        .partnerId(133L)
                        .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                .id(10L)
                                .status(SegmentStatus.RETURN_ARRIVED)
                                .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                .build()))
                        .build()));
        EventDto eventDto = new EventDto();
        eventDto.setEntityType(EntityType.ORDER);
        ObjectNode node = OBJECT_MAPPER.convertValue(orderDto, ObjectNode.class);
        node.put("unrecogField", "value");
        eventDto.setSnapshot(node);

        Assertions.assertDoesNotThrow(() ->
                lomLogbrokerConsumer.process(
                        new MessageBatchBuilder<OrderItemStr>()
                                .addItem(new OrderItemStr(OBJECT_MAPPER.convertValue(eventDto, JsonNode.class).toString()))
                                .build()
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "LomLogbrokerConsumerTest.before.csv", after = "LomLogbrokerConsumerTest.after.csv")
    void testSaveCheckpointWithoutExistingOrder() {
        Set<OrderItem> orderItems = Set.of(
                new OrderItem(new OrderDto()
                        .setPlatformClientId(PlatformClient.BERU.getId())
                        .setId(123L)
                        .setExternalId("1")
                        .setReturnSortingCenterId(333L)
                        .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                .partnerType(PartnerType.DELIVERY)
                                .segmentType(SegmentType.SORTING_CENTER)
                                .partnerId(333L)
                                .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                        .id(10L)
                                        .status(SegmentStatus.RETURN_ARRIVED)
                                        .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                        .build()))
                                .build()))
                ),
                new OrderItem(new OrderDto()
                        .setPlatformClientId(PlatformClient.BERU.getId())
                        .setId(108L)
                        .setExternalId("2")
                        .setReturnSortingCenterId(333L)
                        .setWaybill(Collections.singletonList(WaybillSegmentDto.builder()
                                .partnerType(PartnerType.DELIVERY)
                                .segmentType(SegmentType.PICKUP)
                                .partnerId(133L)
                                .waybillSegmentStatusHistory(Collections.singletonList(WaybillSegmentStatusHistoryDto.builder()
                                        .id(11L)
                                        .status(SegmentStatus.RETURNED)
                                        .date(LocalDateTime.of(2020, 6, 19, 12, 0, 0).toInstant(ZoneOffset.UTC))
                                        .build()))
                                .build()))
                )
        );

        lomLogbrokerConsumer.process(new MessageBatchBuilder<OrderItem>().addAllMessages(orderItems).build());

        ArgumentCaptor<Collection<OrderCheckpoint>> argumentCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(orderCheckpointDao).mergeCheckpoints(argumentCaptor.capture());
        Collection<OrderCheckpoint> capturedArgument = argumentCaptor.getValue();
        assertThat(capturedArgument).hasSize(orderItems.size());
    }

    private static class OrderItem implements MessageBatchItem {

        private EventDto eventDto;

        public OrderItem(OrderDto orderDto) {
            EventDto eventDto = new EventDto();
            eventDto.setEntityType(EntityType.ORDER);
            eventDto.setSnapshot(OBJECT_MAPPER.convertValue(orderDto, JsonNode.class));
            this.eventDto = eventDto;
        }

        @Override
        public byte[] toByteArray() {
            try {
                return OBJECT_MAPPER.writeValueAsBytes(eventDto);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static class OrderItemStr implements MessageBatchItem {

        private String eventDtoJson;

        public OrderItemStr(String eventDtoJson) {
            this.eventDtoJson = eventDtoJson;
        }

        @Override
        public byte[] toByteArray() {
            return eventDtoJson.getBytes();
        }
    }
}
