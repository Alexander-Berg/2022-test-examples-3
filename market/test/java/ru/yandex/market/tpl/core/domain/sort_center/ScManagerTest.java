package ru.yandex.market.tpl.core.domain.sort_center;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Courier;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderItems;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.PartnerCode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Place;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.sc.FulfillmentLgwDtoFactory;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdate;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdateResult;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.sc.ScOrderMapper;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrder;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrderRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserService;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.external.delivery.sc.ScLgwClient;
import ru.yandex.market.tpl.core.test.AssertionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE;
import static ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType.ORDER_CREATED;
import static ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO;
import static ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType.ORDER_SHIPPED_TO_SO;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_EMAIL_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_PHONE_PERSONAL_ID;

/**
 * @author kukabara
 */
@Slf4j
@CoreTest
class ScManagerTest {

    private static final long COURIER_1 = 10031L;
    private static final long COURIER_2 = 10032L;
    private static final long COURIER_3 = 10033L;
    private static final long COURIER_4 = 10034L;
    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long SORT_CENTER_ID = 47819L;
    private static final String G_CLIENT_ID = "970095AB-E0B5-C87F-B910-9719A9D2347A";
    private static final String G_IMPORTANCE = "9deb314e-1bd6-1ee0-4eb2-ac621ba09b74";

    private static final String SC_ORDER_ID = "SC ORDER ID";

    private static final String PLACE_YANDEX_ID = "placeYandexId";
    private static final String WAREHOUSE_ID = "172";
    private static final String PLACE_BARCODE = "placeBarcode";

    @Autowired
    private Clock clock;

    @Autowired
    private ScManager scManager;
    @Autowired
    private OrderGenerateService orderGenerateService;
    @Autowired
    private ScOrderRepository scOrderRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ScOrderMapper scOrderMapper;

    @Autowired
    private TestUserHelper userHelper;
    @Autowired
    private ShiftManager shiftManager;

    @Autowired
    private OrderCommandService orderCommandService;
    @Autowired
    private DbQueueTestUtil dbQueueTestUtil;
    @Autowired
    private UserShiftRepository userShiftRepository;
    @Autowired
    private UserShiftReassignManager userShiftReassignManager;
    @Autowired
    private PartnerRepository<DeliveryService> deliveryServiceRepository;
    @Autowired
    @SpyBean
    private SortingCenterService sortingCenterService;
    @SpyBean
    private UserService userService;

    @MockBean
    private ScLgwClient scLgwClient;

    private User user;
    private Order order;

    @Nonnull
    private static Stream<Arguments> argumentsStream() {
        return List.<Pair<String, Consumer<ScManagerTest>>>of(
                        Pair.of("cancelOrReturnOrders", ScManagerTest::cancelOrReturnOrders),
                        Pair.of("cancelOrder", ScManagerTest::cancelOrder),
                        Pair.of("returnOrder", ScManagerTest::returnOrder),
                        Pair.of("updateOrderItems", ScManagerTest::updateOrderItems)
                )
                .stream()
                .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Test
    void createOrders() {
        createOrderAndCheckState();
    }

    private ScOrder createOrderAndCheckState() {
        ScOrder scOrder = createOrder(OrderFlowStatus.CREATED);
        checkState(scOrder);
        return scOrder;
    }

    private ScOrder createOrderAndCheckState(Order order) {
        ScOrder scOrder = createOrder(order);
        return scOrder;
    }

    private void checkState(ScOrder scOrder) {
        assertThat(scOrder.getStatus()).isEqualTo(OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED_FF);
        assertThat(scOrder.getOrderId()).isEqualTo(order.getId());
        assertThat(scOrder.getOrderPartnerId()).isNull();
        assertThat(scOrder.getPartnerId()).isEqualTo(SORT_CENTER_ID);
        assertThat(orderRepository.findById(order.getId()).get().getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.START_CREATE_SORTING_CENTER);

        assertThat(dbQueueTestUtil.getQueue(QueueType.CREATE_ORDER)).contains(scOrder.getYandexId());
    }

    @NotNull
    private ScOrder createOrder(OrderFlowStatus orderFlowStatus) {
        List<OrderPlaceDto> places = List.of(OrderPlaceDto.builder()
                .yandexId(PLACE_YANDEX_ID)
                .barcode(new OrderPlaceBarcode(WAREHOUSE_ID, PLACE_BARCODE))
                .build());
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .flowStatus(orderFlowStatus)
                        .places(places)
                        .build()
        );
        log.info("Created order {},", order.getExternalOrderId());
        assertThat(order.getOrderFlowStatus()).isEqualTo(orderFlowStatus);
        return createOrder(order);
    }

    @NotNull
    private ScOrder createOrder(Order order) {
        this.order = order;
        jdbcTemplate.update("UPDATE orders SET created_at = created_at - (? ||' minutes')::interval WHERE id = ?",
                scManager.getDelayToCreateOrderInMinutes() + 1, order.getId());

        scManager.createOrders();

        Long scId = deliveryServiceRepository.findByIdOrThrow(order.getDeliveryServiceId()).getSortingCenter().getId();

        return AssertionUtils.assertPresent(
                scOrderRepository.findByYandexIdAndPartnerId(order.getExternalOrderId(),
                        scId)
        );
    }

    @Test
    void updateWhenCreated() {
        ScOrder order = createOrderAndCheckState();

        scManager.updateWhenCreatedOrder(order.getYandexId(), SC_ORDER_ID, order.getPartnerId());

        ScOrder scOrder = AssertionUtils.assertPresent(scOrderRepository.findByYandexIdAndPartnerId(order.getYandexId(),
                SORT_CENTER_ID));
        assertThat(scOrder.getStatus()).isEqualTo(OrderStatusType.ORDER_CREATED_FF);
        assertThat(scOrder.getOrderPartnerId()).isEqualTo(SC_ORDER_ID);
    }

    @Test
    void map() {
        ScOrder order = createOrderAndCheckState();
        String yandexId = order.getYandexId();

        var lgwOrder = scOrderMapper.mapOrder(yandexId, null);
        assertThat(lgwOrder.getOrderId().getYandexId()).isEqualTo(yandexId);
        assertThat(lgwOrder.getExternalId().getYandexId()).isEqualTo(yandexId);
        assertThat(lgwOrder.getDelivery().getCourier()).isEqualTo(FulfillmentLgwDtoFactory.WITHOUT_COURIER);
        assertThat(lgwOrder.getDelivery().getCourier().getPersons().get(0).getId()).isNotNull();

        assignUserForOrder(order.getOrderId(), COURIER_1);
        assignUserForOrder(order.getOrderId(), COURIER_2);

        lgwOrder = scOrderMapper.mapOrder(yandexId, null);
        assertThat(lgwOrder.getDelivery().getCourier()).isNotEqualTo(FulfillmentLgwDtoFactory.WITHOUT_COURIER);
        assertThat(lgwOrder.getDelivery().getCourier().getPersons().get(0).getId()).isEqualTo(COURIER_2);
        assertThat(lgwOrder.getDelivery().getCourier().getLegalEntity().getName()).isEqualTo(user.getCompany().getName());
        assertThat(lgwOrder.getPersonalRecipient().getPersonalEmailId()).isEqualTo(DEFAULT_EMAIL_PERSONAL_ID);
        assertThat(lgwOrder.getPersonalRecipient().getPersonalPhones()).hasSize(1);
        assertThat(lgwOrder.getPersonalRecipient().getPersonalPhones().get(0).getPersonalPhoneId())
                .isEqualTo(DEFAULT_PHONE_PERSONAL_ID);
    }

    @Test
    void mapOrderWithPlaces() {
        ScOrder order = createOrderAndCheckState();
        String yandexId = order.getYandexId();

        var lgwOrder = scOrderMapper.mapOrder(yandexId, null);
        assertThat(lgwOrder.getOrderId().getYandexId()).isEqualTo(yandexId);
        assertThat(lgwOrder.getExternalId().getYandexId()).isEqualTo(yandexId);
        assertThat(lgwOrder.getDelivery().getCourier()).isEqualTo(FulfillmentLgwDtoFactory.WITHOUT_COURIER);
        assertThat(lgwOrder.getDelivery().getCourier().getPersons().get(0).getId()).isNotNull();
        assertThat(lgwOrder.getPlaces()).hasSize(1);
        Place place = lgwOrder.getPlaces().iterator().next();
        assertThat(place.getPlaceId().getYandexId()).isEqualTo(PLACE_YANDEX_ID);
        assertThat(place.getPartnerCodes()).hasSize(1);
        assertThat(place.getPartnerCodes())
                .extracting(PartnerCode::getPartnerCodeValue)
                .containsExactly(PLACE_BARCODE);
    }

    /**
     * После назначения курьера вызываем update для заказа в СЦ.
     */
    @Test
    void createAndUpdate() throws GatewayApiException {
        ScOrder scOrder = createOrderAndCheckState();

        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), SC_ORDER_ID, scOrder.getPartnerId());

        UserShift userShift = assignUserForOrder(scOrder.getOrderId(), COURIER_4);

        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER)).contains(scOrder.getYandexId());
        dbQueueTestUtil.executeSingleQueueItem(QueueType.UPDATE_ORDER);

        verifyUpdate(scOrder.getYandexId());
        ScOrder updatedOrder = scManager.findByYandexId(scOrder.getYandexId()).get(0);
        assertThat(updatedOrder.getCourier()).isEqualTo(userShift.getUser().getFullName());
        assertThat(updatedOrder.getDeliveryDate()).isEqualTo(
                orderRepository.findByIdOrThrow(scOrder.getOrderId()).getDelivery().getDeliveryIntervalFrom());
    }

    /**
     * Если
     * 1. заказ был назначен на курьера
     * 2. заказ не прибыл на СЦ.
     * 3. курьер завершил приемку своих заказов
     * то скинуть назначение курьера в СЦ
     */
    @Test
    void updateToWithoutCourier() throws GatewayApiException {
        ScOrder scOrder = createOrderAndCheckState();

        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), SC_ORDER_ID, scOrder.getPartnerId());

        UserShift userShift = assignUserForOrder(scOrder.getOrderId(), COURIER_4);

        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER)).contains(scOrder.getYandexId());
        dbQueueTestUtil.executeSingleQueueItem(QueueType.UPDATE_ORDER);

        verifyUpdate(scOrder.getYandexId());
        ScOrder updatedOrder = scManager.findByYandexId(scOrder.getYandexId()).stream().findFirst().orElseThrow();
        assertThat(updatedOrder.getCourier()).isEqualTo(userShift.getUser().getFullName());
        assertThat(updatedOrder.getDeliveryDate()).isEqualTo(
                orderRepository.findByIdOrThrow(scOrder.getOrderId()).getDelivery().getDeliveryIntervalFrom());
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_CREATED);
        reset(scLgwClient);

        userHelper.checkinAndFinishPickup(userShift, List.of(), List.of(), true, true);

        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER)).contains(scOrder.getYandexId());
        dbQueueTestUtil.executeSingleQueueItem(QueueType.UPDATE_ORDER);

        // verify update to WITHOUT_COURIER
        ArgumentCaptor<ru.yandex.market.logistic.gateway.common.model.fulfillment.Order> orderCaptor =
                ArgumentCaptor.forClass(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order.class);
        verify(scLgwClient, times(1)).updateOrder(orderCaptor.capture(), anyLong());
        assertThat(orderCaptor.getValue().getOrderId().getYandexId()).isEqualTo(order.getExternalOrderId());
        Courier courier = orderCaptor.getValue().getDelivery().getCourier();
        assertThat(courier).isEqualTo(FulfillmentLgwDtoFactory.WITHOUT_COURIER);
    }

    /**
     * После обновления грузовых мест - обновить данные в СЦ.
     */
    @Test
    void updateOrderPlaces() {
        ScOrder scOrder = createOrderAndCheckState();

        List<OrderPlaceDto> places = List.of(
                OrderPlaceDto.builder()
                        .yandexId(PLACE_YANDEX_ID + 1)
                        .barcode(new OrderPlaceBarcode(WAREHOUSE_ID, PLACE_BARCODE + "-1"))
                        .build(),
                OrderPlaceDto.builder()
                        .yandexId(PLACE_YANDEX_ID + 2)
                        .barcode(new OrderPlaceBarcode(WAREHOUSE_ID, PLACE_BARCODE + "-2"))
                        .build()
        );

        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER)).isEmpty();

        orderCommandService.updatePlaces(
                OrderCommand.UpdateOrderPlaces.builder()
                        .orderId(scOrder.getOrderId())
                        .dimensions(OrderGenerateService.DEFAULT_DIMENSIONS)
                        .places(places)
                        .build()
        );

        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER)).contains(scOrder.getYandexId());
    }

    private void verifyUpdate(String yandexId) throws GatewayApiException {
        verifyUpdate(yandexId, 1);
    }

    private void verifyUpdate(String yandexId, int cnt) throws GatewayApiException {
        ArgumentCaptor<ru.yandex.market.logistic.gateway.common.model.fulfillment.Order> orderCaptor =
                ArgumentCaptor.forClass(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order.class);
        verify(scLgwClient, times(cnt)).updateOrder(orderCaptor.capture(), anyLong());
        assertThat(orderCaptor.getValue().getOrderId().getYandexId()).isEqualTo(yandexId);
    }

    private void verifyOrderItemsUpdate(String yandexId, int cnt) throws GatewayApiException {
        ArgumentCaptor<OrderItems> orderItemsCaptor =
                ArgumentCaptor.forClass(OrderItems.class);
        verify(scLgwClient, times(cnt)).updateOrderItems(orderItemsCaptor.capture(), anyLong());
        assertThat(orderItemsCaptor.getValue().getOrderId().getYandexId()).isEqualTo(yandexId);
    }

    @Test
    void retryUpdate() throws GatewayApiException {
        ScOrder scOrder = createOrderAndCheckState();
        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), SC_ORDER_ID, scOrder.getPartnerId());

        int cnt = (int) scManager.getRetryCount();
        for (long attempt = 0; attempt < cnt; attempt++) {
            scManager.retryWhenError(
                    scOrder.getYandexId(),
                    scOrder.getPartnerId(),
                    ScOrder::getUpdateAttempt, ScOrder::setUpdateAttempt,
                    "updateOrder", () -> scManager.updateOrder(scOrder.getYandexId(), scOrder.getPartnerId(),
                            null, null));

            assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER)).contains(scOrder.getYandexId());
            dbQueueTestUtil.executeSingleQueueItem(QueueType.UPDATE_ORDER);
        }
        assertThat(scOrder.getUpdateAttempt()).isEqualTo(cnt);
        verifyUpdate(scOrder.getYandexId(), cnt);
        int size = dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER).size();

        // retry exceeded
        scManager.retryWhenError(
                scOrder.getYandexId(),
                scOrder.getPartnerId(),
                ScOrder::getUpdateAttempt, ScOrder::setUpdateAttempt,
                "updateOrder", () -> scManager.updateOrder(scOrder.getYandexId(), scOrder.getPartnerId(), null, null));
        verifyUpdate(scOrder.getYandexId(), cnt);
        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER)).hasSize(size);
    }

    @Test
    void retryUpdateOrderItem() throws GatewayApiException {
        ScOrder scOrder = createOrderAndCheckState();
        scManager.updateOrderItems(scOrder.getYandexId());

        int cnt = (int) scManager.getRetryCount();
        for (long attempt = 0; attempt < cnt; attempt++) {
            scManager.retryWhenError(
                    scOrder.getYandexId(),
                    scOrder.getPartnerId(),
                    ScOrder::getUpdateOrderItemsAttempt, ScOrder::setUpdateOrderItemsAttempt,
                    "updateOrderItemsAttempt", () -> scManager.updateOrderItems(scOrder.getYandexId()));

            assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER_ITEMS)).contains(scOrder.getYandexId());
            dbQueueTestUtil.executeSingleQueueItem(QueueType.UPDATE_ORDER_ITEMS);
        }
        assertThat(scOrder.getUpdateOrderItemsAttempt()).isEqualTo(cnt);
        verifyOrderItemsUpdate(scOrder.getYandexId(), cnt);
        int size = dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER_ITEMS).size();

        scManager.retryWhenError(
                scOrder.getYandexId(),
                scOrder.getPartnerId(),
                ScOrder::getUpdateOrderItemsAttempt, ScOrder::setUpdateOrderItemsAttempt,
                "updateOrderItemsAttempt", () -> scManager.updateOrderItems(scOrder.getYandexId()));
        verifyOrderItemsUpdate(scOrder.getYandexId(), cnt);
        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER_ITEMS)).hasSize(size);
    }

    @Test
    void updateOrderWithKnownInAdvanceCourier() throws GatewayApiException {
        LocalDate date = LocalDate.now(clock);
        user = userHelper.findOrCreateUserForSc(COURIER_2, date, SORT_CENTER_ID);

        ScOrder scOrder = createOrderAndCheckState();

        scManager.updateOrder(order.getExternalOrderId(), scOrder.getPartnerId(), null, user.getId());
        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER)).contains(scOrder.getYandexId());
        dbQueueTestUtil.executeSingleQueueItem(QueueType.UPDATE_ORDER);

        ArgumentCaptor<ru.yandex.market.logistic.gateway.common.model.fulfillment.Order> orderCaptor =
                ArgumentCaptor.forClass(ru.yandex.market.logistic.gateway.common.model.fulfillment.Order.class);
        verify(scLgwClient, times(1)).updateOrder(orderCaptor.capture(), anyLong());
        assertThat(orderCaptor.getValue().getOrderId().getYandexId()).isEqualTo(order.getExternalOrderId());
        assertThat(orderCaptor.getValue().getDelivery().getCourier().getPersons().get(0).getId()).isEqualTo(COURIER_2);
    }

    @Test
    void findScCreatedOrderToUpdate() {
        LocalDate date = LocalDate.now(clock);
        user = userHelper.findOrCreateUserForSc(COURIER_2, date, SORT_CENTER_ID);

        ScOrder scOrder = createOrder(OrderFlowStatus.SORTING_CENTER_CREATED);

        assertThat(scOrderRepository.findOrdersToRefresh()).isNotEmpty();
        assertDoesNotThrow(() -> scManager.refreshOrders());
    }

    @Test
    void updateOrderStatusesFromCancelledToReturnedToSortingCenter() {
        ScOrder scOrder = createOrderAndCheckState();
        orderCommandService.updateFlowStatus(new OrderCommand.UpdateFlowStatus(order.getId(),
                OrderFlowStatus.CANCELLED), Source.SYSTEM);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.CANCELLED);
        scManager.updateOrderStatuses(scOrder.getYandexId(), scOrder.getPartnerId(),
                List.of(
                        new OrderStatusUpdate(OrderStatusType.RETURNED_ORDER_AT_SO_WAREHOUSE.getCode(),
                                Instant.now(clock)),
                        new OrderStatusUpdate(OrderStatusType.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM.getCode(),
                                Instant.now(clock).plusSeconds(1))
                ));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.RETURNED_HAS_ARRIVED_TO_SORTING_CENTER);
    }

    @Test
    void updateOrderStatusesSortingCenterPrepared() {
        ScOrder scOrder = createOrderAndCheckState();
        assertThat(order.getOrderFlowStatus()).isNotEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        scManager.updateOrderStatuses(scOrder.getYandexId(), scOrder.getPartnerId(),
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), Instant.now(clock)),
                        new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(),
                                Instant.now(clock).plusSeconds(2)),
                        new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(),
                                Instant.now(clock).plusSeconds(3))
                ));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
    }

    @Test
    void updateOrderStatusesFromSortingCenterPreparedToSortingCenterCreatedIgnored() {
        ScOrder scOrder = createOrderAndCheckState();
        scManager.updateOrderStatuses(scOrder.getYandexId(), scOrder.getPartnerId(),
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), Instant.now(clock)),
                        new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(),
                                Instant.now(clock).plusSeconds(2)),
                        new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(),
                                Instant.now(clock).plusSeconds(3))
                ));
        scManager.updateOrderStatuses(scOrder.getYandexId(), scOrder.getPartnerId(),
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(),
                                Instant.now(clock).plusSeconds(4))
                ));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
    }

    @Test
    void updateOrderStatusesSortingCenterArrived() {
        ScOrder scOrder = createOrderAndCheckState();
        assertThat(order.getOrderFlowStatus()).isNotEqualTo(OrderFlowStatus.SORTING_CENTER_ARRIVED);
        scManager.updateOrderStatuses(scOrder.getYandexId(), scOrder.getPartnerId(),
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), Instant.now(clock)),
                        new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(),
                                Instant.now(clock).plusSeconds(2))
                ));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_ARRIVED);
    }

    @Test
    void updateOrderStatusesFromSortingCenterArrivedToSortingCenterCreatedIgnored() {
        ScOrder scOrder = createOrderAndCheckState();
        scManager.updateOrderStatuses(scOrder.getYandexId(), scOrder.getPartnerId(),
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), Instant.now(clock)),
                        new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(),
                                Instant.now(clock).plusSeconds(2))
                ));
        scManager.updateOrderStatuses(scOrder.getYandexId(), scOrder.getPartnerId(),
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(),
                                Instant.now(clock).plusSeconds(3))
                ));
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_ARRIVED);
    }

    @Test
    void updateOrderStatusesFromDeliveredToRecipientIgnored() {
        ScOrder scOrder = createOrderAndCheckState();
        scManager.updateOrderStatuses(scOrder.getYandexId(), scOrder.getPartnerId(),
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), Instant.now(clock)),
                        new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(),
                                Instant.now(clock).plusSeconds(2)),
                        new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(),
                                Instant.now(clock).plusSeconds(3))
                ));
        orderCommandService.updateFlowStatus(new OrderCommand.UpdateFlowStatus(order.getId(),
                OrderFlowStatus.TRANSPORTATION_RECIPIENT), Source.SYSTEM);
        orderCommandService.updateFlowStatus(new OrderCommand.UpdateFlowStatus(order.getId(),
                OrderFlowStatus.TRANSMITTED_TO_RECIPIENT), Source.SYSTEM);
        orderCommandService.updateFlowStatus(new OrderCommand.UpdateFlowStatus(order.getId(),
                OrderFlowStatus.DELIVERED_TO_RECIPIENT), Source.SYSTEM);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_RECIPIENT);
        OrderStatusUpdateResult orderStatusUpdateResult = scManager.updateOrderStatuses(scOrder.getYandexId(),
                scOrder.getPartnerId(),
                List.of(
                        new OrderStatusUpdate(OrderStatusType.RETURNED_ORDER_DELIVERED_TO_IM.getCode(),
                                Instant.now(clock).plusSeconds(4))
                ));
        assertThat(orderStatusUpdateResult).isEqualTo(OrderStatusUpdateResult.NOT_SUPPORTED);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_RECIPIENT);
    }

    @Test
    void updateOrderStatuses_orderInUnsupportedStatus() {
        ScOrder scOrder = createOrder(OrderFlowStatus.LOST);

        OrderStatusUpdateResult orderStatusUpdateResult = scManager.updateOrderStatuses(scOrder.getYandexId(),
                scOrder.getPartnerId(),
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), Instant.now(clock)),
                        new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(),
                                Instant.now(clock).plusSeconds(2)),
                        new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(),
                                Instant.now(clock).plusSeconds(3))
                ));

        assertThat(orderStatusUpdateResult).isEqualTo(OrderStatusUpdateResult.NOT_SUPPORTED);
    }

    /**
     * Заказ приехал в СЦ, после обновления должны изменить OrderFlowStatus.
     */
    @Test
    @Disabled
    void arrived() {
        ScOrder scOrder = createOrderAndCheckState();
        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), SC_ORDER_ID, scOrder.getPartnerId());
        assertThat(scManager.findByYandexId(scOrder.getYandexId()).get(0).getStatus()).isEqualTo(OrderStatusType.ORDER_CREATED_FF);

        mockGetOrderHistory(scOrder.getYandexId(),
                ORDER_ARRIVED_TO_SO_WAREHOUSE);
        scManager.refreshOrderStatus(scOrder.getYandexId());

        ScOrder updatedOrder = scManager.findByYandexId(scOrder.getYandexId()).get(0);
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(orderRepository.findByIdOrThrow(scOrder.getOrderId()).getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_ARRIVED);
    }

    @Test
    @Disabled
    void assignAfterArrived() throws GatewayApiException {
        ScOrder scOrder = createOrderAndCheckState();
        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), SC_ORDER_ID, scOrder.getPartnerId());
        mockGetOrderHistory(scOrder.getYandexId(),
                ORDER_ARRIVED_TO_SO_WAREHOUSE,
                ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType.ORDER_AWAITING_CLARIFICATION_FF);

        scManager.refreshOrderStatus(scOrder.getYandexId());

        UserShift userShift = assignUserForOrder(scOrder.getOrderId(), COURIER_3);
        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER))
                .describedAs("Не вызываем updateOrder в статусе 110")
                .doesNotContain(scOrder.getYandexId());

        scManager.refreshOrderStatus(scOrder.getYandexId());
        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER))
                .describedAs("После перехода в 117 статус вызываем updateOrder")
                .contains(scOrder.getYandexId());
        dbQueueTestUtil.executeSingleQueueItem(QueueType.UPDATE_ORDER);

        verifyUpdate(scOrder.getYandexId());
        ScOrder updatedOrder = scManager.findByYandexId(scOrder.getYandexId()).get(0);
        assertThat(updatedOrder.getCourier()).isEqualTo(userShift.getUser().getFullName());
        Order order = orderRepository.findByIdOrThrow(scOrder.getOrderId());
        assertThat(updatedOrder.getDeliveryDate()).isEqualTo(order.getDelivery().getDeliveryIntervalFrom());
    }

    @Test
    void cancelBeforeArrived() {
        ScOrder scOrder = createOrderAndCheckState();
        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), SC_ORDER_ID, scOrder.getPartnerId());

        orderCommandService.cancelOrReturn(new OrderCommand.CancelOrReturn(
                scOrder.getOrderId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, null, null, Source.SYSTEM),
                null,
                false)
        );

        assertThat(dbQueueTestUtil.getQueue(QueueType.CANCEL_ORDER)).contains(scOrder.getYandexId());
    }

    @Test
    void cancelBeforeScCreated() {
        ScOrder scOrder = createOrderAndCheckState();

        orderCommandService.cancelOrReturn(new OrderCommand.CancelOrReturn(
                scOrder.getOrderId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, null, null, Source.SYSTEM),
                null,
                false
        ));
        assertThat(dbQueueTestUtil.getQueue(QueueType.CANCEL_ORDER)).isEmpty();

        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), SC_ORDER_ID, scOrder.getPartnerId());

        assertThat(dbQueueTestUtil.getQueue(QueueType.CANCEL_ORDER)).contains(scOrder.getYandexId());
    }

    @Test
    @Disabled
    void cancelAfterArrived() {
        ScOrder scOrder = createOrderAndCheckState();
        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), SC_ORDER_ID, scOrder.getPartnerId());

        mockGetOrderHistory(scOrder.getYandexId(),
                ORDER_ARRIVED_TO_SO_WAREHOUSE,
                ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType.ORDER_AWAITING_CLARIFICATION_FF);
        scManager.refreshOrderStatus(scOrder.getYandexId());
        orderCommandService.cancelOrReturn(new OrderCommand.CancelOrReturn(
                scOrder.getOrderId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, null, null, Source.COURIER),
                null,
                false
        ));

        assertThat(dbQueueTestUtil.getQueue(QueueType.RETURN_ORDER))
                .describedAs("Не вызываем returnOrder в статусе 110")
                .doesNotContain(scOrder.getYandexId());

        scManager.refreshOrderStatus(scOrder.getYandexId());
        assertThat(dbQueueTestUtil.getQueue(QueueType.RETURN_ORDER))
                .describedAs("После перехода в 117 статус вызываем returnOrder")
                .anyMatch(e -> e.startsWith(scOrder.getYandexId()));
    }

    @Test
    void refresh() {
        ScOrder scOrder = createOrderAndCheckState();
        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), SC_ORDER_ID, scOrder.getPartnerId());
        LocalDateTime now = LocalDateTime.now();
        when(scLgwClient.getOrderHistory(any(), anyLong())).thenReturn(
                List.of(
                        new OrderStatus(
                                ORDER_CREATED,
                                DateTime.fromLocalDateTime(now),
                                "created"
                        ),
                        new OrderStatus(
                                ORDER_ARRIVED_TO_SO_WAREHOUSE,
                                DateTime.fromLocalDateTime(now.plusSeconds(1L)),
                                "arrived"
                        ),
                        new OrderStatus(
                                ORDER_READY_TO_BE_SEND_TO_SO,
                                DateTime.fromLocalDateTime(now.plusSeconds(2L)),
                                "ready"
                        ))
        );

        scManager.refreshOrderStatus(scOrder.getYandexId());

        assertThat(orderRepository.findByIdOrThrow(scOrder.getOrderId()).getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
    }

    private void mockGetOrderHistory(String yandexId,
                                     ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType... answerStatuses
    ) {
        LocalDateTime now = LocalDateTime.now();
        List<List<OrderStatus>> answers = EntryStream.of(answerStatuses)
                .map(entry -> {
                    var status = entry.getValue();
                    return List.of(
                            new OrderStatus(
                                    status,
                                    DateTime.fromLocalDateTime(now.plusSeconds(entry.getKey() + 1)),
                                    status.getCode() + ' ' + status.name()
                            ));
                })
                .collect(Collectors.toList());

        var answer = answers.remove(0);
        when(scLgwClient.getOrderHistory(any(), anyLong())).thenReturn(answer, answers.toArray(new List[0]));
    }

    private UserShift assignUserForOrder(long orderId, long uid) {
        LocalDate date = LocalDate.now(clock);
        user = userHelper.findOrCreateUserForSc(uid, date, SORT_CENTER_ID);
        Shift shift = shiftManager.findOrCreate(date, SORT_CENTER_ID);
        shiftManager.openShift(shift.getId());
        userShiftReassignManager.reassignOrders(Set.of(orderId), Set.of(), Set.of(), user.getId());
        return userShiftRepository.findByShiftIdAndUserId(shift.getId(), user.getId()).orElseThrow();
    }

    @Test
    @DisplayName("Добавление задачи в очередь RETURN_ORDER с включенным флагом")
    void testUpdateOrderFlowOnReadyForReturnAndIncrementSizeDbQueueFlagEnabled() {
        updateOrderFlowOnReadyForReturnAndIncrementSizeDbQueue(1);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("argumentsStream")
    @DisplayName("Отмена DBS заказа не создает таски на отмену или возврат в СЦ")
    void deliveryBySellerCancelOrderDoesNotProduceAnySortingCenterTasks(
            String testCaseName,
            Consumer<ScManagerTest> consumer
    ) {
        List<OrderPlaceDto> places = List.of(OrderPlaceDto.builder()
                .yandexId(PLACE_YANDEX_ID)
                .barcode(new OrderPlaceBarcode(WAREHOUSE_ID, PLACE_BARCODE))
                .build());
        userHelper.createOrFindDbsDeliveryService();
        doReturn(true).when(sortingCenterService).usePvz(any());
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(TestUserHelper.DBS_DELIVERY_SERVICE_ID)
                        .flowStatus(OrderFlowStatus.CREATED)
                        .places(places)
                        .build()
        );
        ScOrder scOrder = createOrderAndCheckState(order);
        consumer.accept(this);
        dbQueueTestUtil.assertQueueHasSize(QueueType.CANCEL_ORDER, 0);
        dbQueueTestUtil.assertQueueHasSize(QueueType.RETURN_ORDER, 0);
        dbQueueTestUtil.assertQueueHasSize(QueueType.UPDATE_ORDER, 0);
        dbQueueTestUtil.assertQueueHasSize(QueueType.UPDATE_ORDER_ITEMS, 0);
        Mockito.reset(userService);
        Mockito.reset(sortingCenterService);
    }

    @Test
    @DisplayName("Когда пришел createOrderError, то пересоздаем еще раз, когда sc_order не создан ")
    void callbackOnErrorCreateScOrderWhenScOrderNotExists() {
        order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .flowStatus(OrderFlowStatus.CREATED)
                        .externalOrderId("someExternalOrderId")
                        .build()
        );
        scManager.createScOrderAgainAfterError("someExternalOrderId", DELIVERY_SERVICE_ID);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_ORDER, 1);
    }

    @Test
    @DisplayName("Когда пришел createOrderError, то пересоздаем еще раз, когда sc_order создан")
    void callbackOnErrorCreateScOrderWhenScOrderxists() {
        ScOrder scOrder = createOrderAndCheckState();
        scManager.createScOrderAgainAfterError(scOrder.getYandexId(), scOrder.getPartnerId());

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_ORDER, 2);
    }

    void cancelOrReturnOrders() {
        scManager.cancelOrReturnOrders(order.getExternalOrderId(), false);
    }

    void cancelOrder() {
        scManager.cancelOrder(order.getExternalOrderId());
    }

    void returnOrder() {
        scManager.returnOrder(order.getExternalOrderId());
    }

    void updateOrderItems() {
        scManager.updateOrderItems(order.getExternalOrderId());
    }

    void updateOrderFlowOnReadyForReturnAndIncrementSizeDbQueue(int expectedSizeQueue) {
        ScOrder scOrder = createOrderAndCheckState();
        Order order1 = orderRepository.findByIdOrThrow(scOrder.getOrderId());
        order1.setOrderFlowStatus(OrderFlowStatus.TRANSPORTATION_RECIPIENT);

        scOrderRepository.findByYandexId(order1.getExternalOrderId())
                .forEach(scOrder1 -> {
                    scOrder1.setStatus(OrderStatusType.ORDER_SHIPPED_TO_SO_FF);
                    scOrder1.setOrderPartnerId(
                            sortingCenterService.findSortCenterForDs(order1.getDeliveryServiceId()).getId().toString());
                    scOrderRepository.save(scOrder1);
                });

        mockGetOrderHistory(
                ORDER_CREATED, ORDER_ARRIVED_TO_SO_WAREHOUSE, ORDER_READY_TO_BE_SEND_TO_SO, ORDER_SHIPPED_TO_SO);

        orderCommandService.updateFlowStatus(new OrderCommand.UpdateFlowStatus(order1.getId(),
                OrderFlowStatus.READY_FOR_RETURN), Source.COURIER);

        assertThat(dbQueueTestUtil.getQueue(QueueType.RETURN_ORDER).size()).isEqualTo(expectedSizeQueue);
    }

    private void mockGetOrderHistory(
            ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType... statuses) {
        LocalDateTime now = LocalDateTime.now();
        AtomicInteger counter = new AtomicInteger();

        List<OrderStatus> orderStatuses = Arrays.stream(statuses)
                .map(status -> new OrderStatus(status,
                        DateTime.fromLocalDateTime(now.plusSeconds(counter.getAndIncrement())), ""))
                .collect(Collectors.toList());

        when(scLgwClient.getOrderHistory(any(), anyLong())).thenReturn(orderStatuses);
    }
}
