package ru.yandex.market.pvz.core.domain.order;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnClient;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryRecord;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryRepository;
import ru.yandex.market.pvz.core.domain.order.model.params.EnrichOrderBatchParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.pickup_point.capacity.PickupPointCapacityRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.EXTERNAL_ID_NOT_UNIQUE_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.ON_DEMAND_STORAGE_PERIOD;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.ORDER_IS_KGT_ENRICH_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.PUSH_STATUS_CHANGE_IN_LES_ENABLED;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.CHANGE_ORDER_STATUS_BATCH;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.CHANGE_PICKUP_POINT_CAPACITY_BATCH;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.ENRICH_ORDER_BATCH;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.ENRICH_ORDER_BATCH_NEW;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.GET_RECIPIENT_PHONE_TAIL_BATCH;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.ORDER_IS_KGT_BATCH;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.PUSH_ORDER_STATUSES;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.SET_EXPIRATION_DATE_BATCH;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.ARRIVED_TO_PICKUP_POINT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CANCELLED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CREATED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.READY_FOR_RETURN;
import static ru.yandex.market.pvz.core.test.TestExternalConfiguration.DEFAULT_UID;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderReceiveServiceTest {

    private final TestableClock clock;

    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private final DbQueueTestUtil dbQueueTestUtil;

    private final OrderRepository orderRepository;
    private final OrderQueryService orderQueryService;
    private final OrderHistoryRepository orderHistoryRepository;
    private final PickupPointCapacityRepository pickupPointCapacityRepository;

    private final OrderReceiveService orderReceiveService;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @MockBean
    private CheckouterClient checkouterClient;

    @MockBean
    private CheckouterReturnClient checkouterReturnClient;

    /**
     * период хранения - 3 дня
     * 04.03.21 - createdOrder1 (создание), cancelledOrder1 (создание и отмена через 3 часа)
     * 05.03.21 - createdOrder2 (создание), cancelledOrder2 (создание и отмена через 3 часа)
     * 06.03.21 - приемка
     * 09.03.21 - createdOrder1.expirationDate
     * 09.03.21 - createdOrder2.expirationDate
     * <p>
     * капасити до приеики (от deliveryDate до expirationDate): 07.03.21, 11.03.21 - 1; 08.03.21 - 10.03.21 - 2
     * капасити после приемки (от даты приемки до expirationDate): 06.03.21 - 09.03.21 - 2
     */
    @ParameterizedTest
    @MethodSource("isKgtEnabledAndUniqueExternalIdParamsMethodAndPushInLes")
    void receiveOrders(boolean isKgtEnabled, boolean uniqueExternalId, boolean pushInLES) {
        configurationGlobalCommandService.setValue(ORDER_IS_KGT_ENRICH_ENABLED, isKgtEnabled);
        configurationGlobalCommandService.setValue(EXTERNAL_ID_NOT_UNIQUE_ENABLED, uniqueExternalId);
        configurationGlobalCommandService.setValue(PUSH_STATUS_CHANGE_IN_LES_ENABLED, pushInLES);

        var queueType = pushInLES ? CHANGE_ORDER_STATUS_BATCH : PUSH_ORDER_STATUSES;

        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .storagePeriod(3)
                        .build());
        LocalDateTime createdDateTime = LocalDateTime.of(2022, 3, 4, 13, 15);
        ZoneOffset offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        clock.setFixed(createdDateTime.toInstant(offset), offset);
        var createdOrder1 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("4637211")
                        .deliveryDate(createdDateTime.toLocalDate().plusDays(3))
                        .build())
                .build());
        var cancelledOrder1 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("4637212")
                        .build())
                .build());
        clock.setFixed(createdDateTime.plusHours(3).toInstant(offset), offset);
        orderFactory.cancelOrder(cancelledOrder1.getId());

        clock.setFixed(createdDateTime.plusDays(1).toInstant(offset), offset);
        var createdOrder2 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("4637213")
                        .deliveryDate(createdDateTime.toLocalDate().plusDays(4))
                        .build())
                .build());
        var cancelledOrder2 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("4637214")
                        .build())
                .build());
        clock.setFixed(createdDateTime.plusDays(1).plusHours(3).toInstant(offset), offset);
        orderFactory.cancelOrder(cancelledOrder2.getId());

        dbQueueTestUtil.executeAllQueueItems(CHANGE_PICKUP_POINT_CAPACITY);
        dbQueueTestUtil.executeAllQueueItems(queueType);

        clock.setFixed(createdDateTime.plusDays(2).toInstant(offset), offset);
        List<OrderRepository.OrderForReceive> ordersForReceive = StreamEx.of(orderRepository.findForReceive(
                        pickupPoint.getId(), List.of(
                                createdOrder1.getExternalId(), createdOrder2.getExternalId(),
                                cancelledOrder1.getExternalId(), cancelledOrder2.getExternalId())))
                .sorted(Comparator.comparingLong(OrderRepository.OrderForReceive::getId)).toList();
        assertThat(ordersForReceive).hasSize(4);

        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), DEFAULT_UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());

        orderReceiveService.receive(ordersForReceive, pickupPointRequestData);

        var receivedCreatedOrder1 = orderQueryService.get(createdOrder1.getId());
        assertThat(receivedCreatedOrder1.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
        assertThat(receivedCreatedOrder1.getDsApiCheckpoint()).isEqualTo(ARRIVED_TO_PICKUP_POINT.getCode());
        assertThat(receivedCreatedOrder1.getArrivedAt())
                .isEqualTo(OffsetDateTime.of(createdDateTime.plusDays(2), offset));
        assertThat(receivedCreatedOrder1.getExpirationDate()).isNull();
        var receivedCreatedOrder2 = orderQueryService.get(createdOrder2.getId());
        assertThat(receivedCreatedOrder2.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
        assertThat(receivedCreatedOrder2.getDsApiCheckpoint()).isEqualTo(ARRIVED_TO_PICKUP_POINT.getCode());
        assertThat(receivedCreatedOrder2.getArrivedAt())
                .isEqualTo(OffsetDateTime.of(createdDateTime.plusDays(2), offset));
        assertThat(receivedCreatedOrder2.getExpirationDate()).isNull();
        var receivedCancelledOrder1 = orderQueryService.get(cancelledOrder1.getId());
        assertThat(receivedCancelledOrder1.getStatus()).isEqualTo(READY_FOR_RETURN);
        assertThat(receivedCancelledOrder1.getDsApiCheckpoint()).isEqualTo(READY_FOR_RETURN.getCode());
        assertThat(receivedCancelledOrder1.getArrivedAt())
                .isEqualTo(OffsetDateTime.of(createdDateTime.plusDays(2), offset));
        assertThat(receivedCancelledOrder1.getExpirationDate()).isNull();
        var receivedCancelledOrder2 = orderQueryService.get(cancelledOrder2.getId());
        assertThat(receivedCancelledOrder2.getStatus()).isEqualTo(READY_FOR_RETURN);
        assertThat(receivedCancelledOrder2.getDsApiCheckpoint()).isEqualTo(READY_FOR_RETURN.getCode());
        assertThat(receivedCancelledOrder2.getArrivedAt())
                .isEqualTo(OffsetDateTime.of(createdDateTime.plusDays(2), offset));
        assertThat(receivedCancelledOrder2.getExpirationDate()).isNull();

        var createdOrder1History = StreamEx.of(
                        orderHistoryRepository.findByExternalIdOrderByUpdatedAtAscIdAsc(createdOrder1.getExternalId()))
                .map(OrderHistoryRecord::getStatus).toList();
        assertThat(createdOrder1History).containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT);
        var createdOrder2History = StreamEx.of(
                        orderHistoryRepository.findByExternalIdOrderByUpdatedAtAscIdAsc(createdOrder2.getExternalId()))
                .map(OrderHistoryRecord::getStatus).toList();
        assertThat(createdOrder2History).containsExactly(CREATED, ARRIVED_TO_PICKUP_POINT);
        var cancelledOrder1History = StreamEx.of(
                        orderHistoryRepository.findByExternalIdOrderByUpdatedAtAscIdAsc(cancelledOrder1.getExternalId()))
                .map(OrderHistoryRecord::getStatus).toList();
        assertThat(cancelledOrder1History).containsExactly(CREATED, CANCELLED, ARRIVED_TO_PICKUP_POINT,
                READY_FOR_RETURN);
        var cancelledOrder2History = StreamEx.of(
                        orderHistoryRepository.findByExternalIdOrderByUpdatedAtAscIdAsc(cancelledOrder2.getExternalId()))
                .map(OrderHistoryRecord::getStatus).toList();
        assertThat(cancelledOrder2History).containsExactly(CREATED, CANCELLED, ARRIVED_TO_PICKUP_POINT,
                READY_FOR_RETURN);

        dbQueueTestUtil.assertTasksHasSize(queueType, 1);

        dbQueueTestUtil.assertQueueHasSingleEvent(CHANGE_PICKUP_POINT_CAPACITY_BATCH,
                createdOrder1.getId() + "," + createdOrder2.getId());
        dbQueueTestUtil.assertTasksHasSize(CHANGE_PICKUP_POINT_CAPACITY_BATCH, 1);
        dbQueueTestUtil.executeAllQueueItems(CHANGE_PICKUP_POINT_CAPACITY);
        assertThat(pickupPointCapacityRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), createdDateTime.toLocalDate().plusDays(3)).getOrderCount()).isEqualTo(1);
        assertThat(pickupPointCapacityRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), createdDateTime.toLocalDate().plusDays(4)).getOrderCount()).isEqualTo(2);
        assertThat(pickupPointCapacityRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), createdDateTime.toLocalDate().plusDays(5)).getOrderCount()).isEqualTo(2);
        assertThat(pickupPointCapacityRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), createdDateTime.toLocalDate().plusDays(6)).getOrderCount()).isEqualTo(2);
        assertThat(pickupPointCapacityRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), createdDateTime.toLocalDate().plusDays(7)).getOrderCount()).isEqualTo(1);
        dbQueueTestUtil.executeAllQueueItems(CHANGE_PICKUP_POINT_CAPACITY_BATCH);
        assertThat(pickupPointCapacityRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), createdDateTime.toLocalDate().plusDays(2)).getOrderCount()).isEqualTo(2);
        assertThat(pickupPointCapacityRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), createdDateTime.toLocalDate().plusDays(3)).getOrderCount()).isEqualTo(2);
        assertThat(pickupPointCapacityRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), createdDateTime.toLocalDate().plusDays(4)).getOrderCount()).isEqualTo(2);
        assertThat(pickupPointCapacityRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), createdDateTime.toLocalDate().plusDays(5)).getOrderCount()).isEqualTo(2);
        assertThat(pickupPointCapacityRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), createdDateTime.toLocalDate().plusDays(6)).getOrderCount()).isEqualTo(0);
        assertThat(pickupPointCapacityRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), createdDateTime.toLocalDate().plusDays(7)).getOrderCount()).isEqualTo(0);

        if (uniqueExternalId) {
            dbQueueTestUtil.assertQueueHasSingleEvent(ENRICH_ORDER_BATCH_NEW,
                    new StringBuilder()
                            .append(new EnrichOrderBatchParams(createdOrder1.getExternalId(),
                                    createdOrder1.getPickupPoint().getId()))
                            .append(",")
                            .append(new EnrichOrderBatchParams(createdOrder2.getExternalId(),
                                    createdOrder2.getPickupPoint().getId()))
                            .toString());
            dbQueueTestUtil.assertTasksHasSize(ENRICH_ORDER_BATCH_NEW, 1);
            dbQueueTestUtil.executeAllQueueItems(ENRICH_ORDER_BATCH_NEW);
        } else {
            dbQueueTestUtil.assertQueueHasSingleEvent(ENRICH_ORDER_BATCH,
                    new StringBuilder()
                            .append(createdOrder1.getExternalId())
                            .append(",")
                            .append(createdOrder2.getExternalId())
                            .toString());
            dbQueueTestUtil.assertTasksHasSize(ENRICH_ORDER_BATCH, 1);
            dbQueueTestUtil.executeAllQueueItems(ENRICH_ORDER_BATCH);
        }
        verify(checkouterClient, times(2)).getOrder(any(), any());

        if (isKgtEnabled) {
            dbQueueTestUtil.assertQueueHasSingleEvent(ORDER_IS_KGT_BATCH,
                    createdOrder1.getExternalId() + "," + createdOrder2.getExternalId());
            dbQueueTestUtil.assertTasksHasSize(ORDER_IS_KGT_BATCH, 1);
            dbQueueTestUtil.executeSingleQueueItem(ORDER_IS_KGT_BATCH);
            verify(checkouterReturnClient, times(2)).getReturnableItems(anyLong(), any(), any());
        } else {
            dbQueueTestUtil.assertTasksHasSize(ORDER_IS_KGT_BATCH, 0);
        }

        dbQueueTestUtil.assertQueueHasSingleEvent(SET_EXPIRATION_DATE_BATCH,
                createdOrder1.getId() + "," + createdOrder2.getId());
        assertThat(receivedCreatedOrder1.getExpirationDate()).isNull();
        assertThat(receivedCreatedOrder2.getExpirationDate()).isNull();
        dbQueueTestUtil.assertTasksHasSize(SET_EXPIRATION_DATE_BATCH, 1);
        dbQueueTestUtil.executeSingleQueueItem(SET_EXPIRATION_DATE_BATCH);
        receivedCreatedOrder1 = orderQueryService.get(createdOrder1.getId());
        receivedCreatedOrder2 = orderQueryService.get(createdOrder2.getId());
        assertThat(receivedCreatedOrder1.getExpirationDate()).isEqualTo(createdDateTime.toLocalDate().plusDays(5));
        assertThat(receivedCreatedOrder2.getExpirationDate()).isEqualTo(createdDateTime.toLocalDate().plusDays(5));

        dbQueueTestUtil.assertQueueHasSize(GET_RECIPIENT_PHONE_TAIL_BATCH, 1);
        dbQueueTestUtil.assertQueueHasSingleEvent(GET_RECIPIENT_PHONE_TAIL_BATCH,
                createdOrder1.getId() + "," + createdOrder2.getId());
    }

    private static Stream<Arguments> isKgtEnabledAndUniqueExternalIdParamsMethodAndPushInLes() {
        return Stream.of(
                Arguments.of(true, true, true),
                Arguments.of(true, true, false),
                Arguments.of(true, false, true),
                Arguments.of(true, false, false),
                Arguments.of(false, true, true),
                Arguments.of(false, true, false),
                Arguments.of(false, false, true),
                Arguments.of(false, false, false)
        );
    }

    @Test
    void tryToReceiveOrderInInvalidStatus() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());
        orderFactory.receiveOrder(order.getId());

        List<OrderRepository.OrderForReceive> ordersForReceive = orderRepository.findForReceive(
                pickupPoint.getId(), List.of(order.getExternalId()));
        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), DEFAULT_UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        assertThatThrownBy(() -> orderReceiveService.receive(ordersForReceive, pickupPointRequestData))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void receiveYaDeliveryOrderWithNoEnrichAndCheckingKgt() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("LO-8327462834")
                        .yaDelivery(true)
                        .build())
                .build());

        List<OrderRepository.OrderForReceive> ordersForReceive = orderRepository.findForReceive(
                pickupPoint.getId(), List.of(order.getExternalId()));
        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), DEFAULT_UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        orderReceiveService.receive(ordersForReceive, pickupPointRequestData);

        dbQueueTestUtil.assertTasksHasSize(ENRICH_ORDER_BATCH, 0);
        dbQueueTestUtil.assertTasksHasSize(ORDER_IS_KGT_BATCH, 0);
    }

    /**
     * Проверяем, что срок хранения соответствует настройке для on-demand заказов, а не сроку хранения ПВЗ
     */
    @Test
    void receiveOnDemandOrder() {
        configurationGlobalCommandService.setValue(ON_DEMAND_STORAGE_PERIOD, 5);
        var pickupPoint = pickupPointFactory.createPickupPoint();

        LocalDateTime createdDateTime = LocalDateTime.of(2022, 3, 4, 13, 15);
        ZoneOffset offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        clock.setFixed(createdDateTime.toInstant(offset), offset);
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("8327462834")
                        .type(OrderType.ON_DEMAND)
                        .deliveryDate(createdDateTime.toLocalDate().plusDays(3))
                        .build())
                .build());

        clock.setFixed(createdDateTime.plusDays(2).toInstant(offset), offset);
        List<OrderRepository.OrderForReceive> ordersForReceive = orderRepository.findForReceive(
                pickupPoint.getId(), List.of(order.getExternalId()));
        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), DEFAULT_UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        orderReceiveService.receive(ordersForReceive, pickupPointRequestData);

        var receivedOrder = orderQueryService.get(order.getId());
        dbQueueTestUtil.assertQueueHasSingleEvent(SET_EXPIRATION_DATE_BATCH, String.valueOf(receivedOrder.getId()));
        assertThat(receivedOrder.getExpirationDate()).isNull();
        dbQueueTestUtil.assertTasksHasSize(SET_EXPIRATION_DATE_BATCH, 1);
        dbQueueTestUtil.executeSingleQueueItem(SET_EXPIRATION_DATE_BATCH);
        receivedOrder = orderQueryService.get(order.getId());
        assertThat(receivedOrder.getExpirationDate()).isEqualTo(createdDateTime.toLocalDate().plusDays(7));
    }

    /**
     * Проверяем, что срок хранения отсчитывается от даты приемки, если дата прибытия задана раньше даты приемки
     */
    @Test
    void receiveOrderWithOldDeliveryDate() {
        configurationGlobalCommandService.setValue(ON_DEMAND_STORAGE_PERIOD, 5);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .storagePeriod(3)
                        .build());

        LocalDateTime createdDateTime = LocalDateTime.of(2022, 3, 4, 13, 15);
        ZoneOffset offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        clock.setFixed(createdDateTime.toInstant(offset), offset);
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId("8327462834")
                        .deliveryDate(createdDateTime.toLocalDate().minusMonths(1))
                        .build())
                .build());

        clock.setFixed(createdDateTime.plusDays(2).toInstant(offset), offset);
        List<OrderRepository.OrderForReceive> ordersForReceive = orderRepository.findForReceive(
                pickupPoint.getId(), List.of(order.getExternalId()));
        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), DEFAULT_UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        orderReceiveService.receive(ordersForReceive, pickupPointRequestData);

        var receivedOrder = orderQueryService.get(order.getId());
        dbQueueTestUtil.assertQueueHasSingleEvent(SET_EXPIRATION_DATE_BATCH, String.valueOf(receivedOrder.getId()));
        assertThat(receivedOrder.getExpirationDate()).isNull();
        dbQueueTestUtil.assertTasksHasSize(SET_EXPIRATION_DATE_BATCH, 1);
        dbQueueTestUtil.executeSingleQueueItem(SET_EXPIRATION_DATE_BATCH);
        receivedOrder = orderQueryService.get(order.getId());
        assertThat(receivedOrder.getExpirationDate()).isEqualTo(createdDateTime.toLocalDate().plusDays(5));
    }
}
