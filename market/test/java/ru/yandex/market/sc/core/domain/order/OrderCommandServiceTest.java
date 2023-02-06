package ru.yandex.market.sc.core.domain.order;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.OptimisticLockException;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.Location;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.ReturnType;
import ru.yandex.market.logistic.api.model.fulfillment.Warehouse;
import ru.yandex.market.logistic.api.model.fulfillment.request.UpdateOrderItemsRequest;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceRepository;
import ru.yandex.market.sc.core.domain.measurements.repository.Measurements;
import ru.yandex.market.sc.core.domain.order.model.CreateClientReturnRequest;
import ru.yandex.market.sc.core.domain.order.model.CreateReturnRequest;
import ru.yandex.market.sc.core.domain.order.model.DeletedSegmentRequest;
import ru.yandex.market.sc.core.domain.order.model.FFApiOrderUpdateRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderCreateRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderIdResponse;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.OrderReturnType;
import ru.yandex.market.sc.core.domain.order.model.OrderScRequest;
import ru.yandex.market.sc.core.domain.order.model.OrdersScRequest;
import ru.yandex.market.sc.core.domain.order.model.RequestOrderId;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderState;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderFFStatusHistoryItem;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderItem;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.FfApiPlaceService;
import ru.yandex.market.sc.core.domain.place.PlaceCommandService;
import ru.yandex.market.sc.core.domain.place.PlaceRouteSoService;
import ru.yandex.market.sc.core.domain.place.misc.PlaceHistoryTestHelper;
import ru.yandex.market.sc.core.domain.place.model.PlaceId;
import ru.yandex.market.sc.core.domain.place.model.PlaceScRequest;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceMutableState;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.postponed.model.OperationType;
import ru.yandex.market.sc.core.domain.postponed.repository.PostponedOperationRepository;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableBarcodeRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.stage.StageLoader;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.user.repository.UserRepository;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseProperty;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseRepository;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.exception.ScInvalidTransitionException;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.test.TestFactory.CreateOrderParams;
import ru.yandex.market.sc.core.util.ScDateUtils;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.sc.internal.model.LocationDto;
import ru.yandex.market.sc.internal.model.SenderDto;
import ru.yandex.market.sc.internal.model.WarehouseDto;
import ru.yandex.market.tpl.common.util.exception.TplException;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_CANCELLED_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.SORTING_CENTER_PREPARED_FOR_UTILIZE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.SORTING_CENTER_SHIPPED_FOR_UTILIZER;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN;
import static ru.yandex.market.sc.core.domain.order.repository.FakeOrderType.CLIENT_RETURN;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.ACCEPTED_RETURN;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.CANCELLED;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.KEEPED_DIRECT;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.SHIPPED_DIRECT;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.SHIPPED_RETURN;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.SORTED_DIRECT;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.SORTED_RETURN;
import static ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension.testNotMigrated;
import static ru.yandex.market.sc.core.test.TestFactory.SC_OBJECT_MAPPER;
import static ru.yandex.market.sc.core.test.TestFactory.ffOrder;
import static ru.yandex.market.sc.core.test.TestFactory.ffOrderWithParams;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.useNewSortableFlow;

/**
 * @author valter
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderCommandServiceTest {

    private static final String FBS_RETURN_WAREHOUSE_ID = "10000000001";

    private final OrderCommandService orderCommandService;
    private final PlaceRouteSoService placeRouteSoService;
    private final PlaceCommandService placeCommandService;
    private final FfApiPlaceService ffApiPlaceService;
    private final AcceptService acceptService;
    private final PreShipService preShipService;
    private final ScOrderRepository orderRepository;
    private final PlaceRepository placeRepository;
    private final TestFactory testFactory;
    private final TransactionTemplate transactionTemplate;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final DeliveryServiceRepository deliveryServiceRepository;
    private final PostponedOperationRepository postponedOperationRepository;
    private final SortableBarcodeRepository sortableBarcodeRepository;

    private final PlaceHistoryTestHelper placeHistoryHelper;

    @MockBean
    Clock clock;

    @SpyBean
    OrderLockRepository orderLockRepository;

    SortingCenter sortingCenter;
    User user;


    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(1234L);
        user = testFactory.storedUser(sortingCenter, 123L);
        testFactory.setupMockClock(clock);
        placeHistoryHelper.startPlaceHistoryCollection();
    }

    @Test
    //    https://st.yandex-team.ru/MARKETTPLSUPSC-22746
    @DisplayName("Принятые заказы заказы меняют склад возврата после в changeWarehouseReturnIfPossible()")
    void shippedOrdersAreNotCausingException() {
        var acceptedOrder = testFactory.createOrder(
                order(sortingCenter).build()).cancel().accept().get();

        String warehouseYandexId = "warehouse_yan_id";
        var warehouse = testFactory.storedWarehouse(warehouseYandexId);


        orderCommandService.changeWarehouseReturnIfPossible(warehouse.getYandexId(),
                List.of(acceptedOrder.getId().toString()),
                new HashMap(Map.of(acceptedOrder.getId(), user))
        );
        ScOrder afterUpdate = orderRepository.findByIdOrThrow(acceptedOrder.getId());
        assertThat(afterUpdate.getWarehouseReturn().getYandexId()).isEqualTo(warehouseYandexId);
    }

    @Test
    //    https://st.yandex-team.ru/MARKETTPLSUPSC-22746
    @DisplayName("Отсортированные заказы меняют склад возврата после в changeWarehouseReturnIfPossible()")
    void shippedOrdersAreNotCausingException2() {
        var sortedOrder = testFactory.createOrder(
                order(sortingCenter).build()).cancel().accept().sort().get();

        String warehouseYandexId = "warehouse_yan_id";
        var warehouse = testFactory.storedWarehouse(warehouseYandexId);

        orderCommandService.changeWarehouseReturnIfPossible(warehouse.getYandexId(),
                List.of(sortedOrder.getId().toString()),
                new HashMap(Map.of(sortedOrder.getId(), user))
        );

        ScOrder afterUpdate = orderRepository.findByIdOrThrow(sortedOrder.getId());
        assertThat(afterUpdate.getWarehouseReturn().getYandexId()).isEqualTo(warehouseYandexId);
    }

    @Test
    //    https://st.yandex-team.ru/MARKETTPLSUPSC-22746
    @DisplayName("Возвращенные заказы не вызываю исключения в changeWarehouseReturnIfPossible()")
    void shippedOrdersAreNotCausingException3() {
        var shippedOrder = testFactory.createOrder(
                order(sortingCenter).build()).cancel().accept().sort().ship().get();

        String warehouseYandexId = "warehouse_yan_id";
        var warehouse = testFactory.storedWarehouse(warehouseYandexId);

        orderCommandService.changeWarehouseReturnIfPossible(warehouse.getYandexId(),
                List.of(shippedOrder.getId().toString()),
                new HashMap(Map.of(shippedOrder.getId(), user))
        );

        ScOrder afterUpdate = orderRepository.findByIdOrThrow(shippedOrder.getId());
        assertThat(afterUpdate.getWarehouseReturn().getYandexId()).isNotEqualTo(warehouseYandexId);


    }

    @Test
    void canCancelPartiallyShippedOrder() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2").dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlace("p1").sortPlace("p1").shipPlace("p1").get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, false, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(2, user);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
    }

    @Test
    void canCancelPartiallyAcceptedOrder() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2").dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlace("p1").get();
        placeHistoryHelper.startPlaceHistoryCollection();
        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, false, user);
        placeHistoryHelper.validateThatNRecordsWithUserCollected(2, user);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void canCancelPartiallyAcceptedOrderAfterPartialShip() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2").dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlace("p1").sortPlace("p1").shipPlace("p1").acceptPlace("p2").get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, false, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(2, user);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void cantCancelOrderPartiallySortedAfterAccept() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2").dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlace("p1").sortPlace("p1").get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user); // заказ не отменился
        var notCanceledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(notCanceledOrder.getFfStatus()).isEqualTo(ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(1);
        assertThat(operations.iterator().next().getOrderId()).isEqualTo(order.getId());
        assertThat(operations.iterator().next().getOperationType()).isEqualTo(OperationType.CANCEL);
    }

    @Test
    void cantCancelPartiallySortedOrder() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2").dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlace("p1").sortPlace("p1").shipPlace("p1").acceptPlace("p2").sortPlace("p2").get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user); // заказ не отменился
        var notCanceledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(notCanceledOrder.getFfStatus()).isEqualTo(ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(1);
        assertThat(operations.iterator().next().getOrderId()).isEqualTo(order.getId());
        assertThat(operations.iterator().next().getOperationType()).isEqualTo(OperationType.CANCEL);
    }

    @Test
    void cantCancelPostponedSortedOrder() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user); // заказ не отменился
        var notCanceledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(notCanceledOrder.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(1);
        assertThat(operations.iterator().next().getOrderId()).isEqualTo(order.getId());
        assertThat(operations.iterator().next().getOperationType()).isEqualTo(OperationType.CANCEL);
    }

    @Test
    void canCancelAlreadyCancelOrderSoGotInfoAboutPlannedReturn() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().ship().makeReturn().get();
        assertThat(order.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(
                order.getPlaceCount(), user);
        var canceledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(canceledOrder.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(0);
    }

    @Test
    void canCancelAlreadyCancelOrderReturnedOrderAtSoWarehouse() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().ship().makeReturn().accept().get();
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        var returnedOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(returnedOrder.getFfStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(0);
    }

    @Test
    void canCancelAlreadyCancelOrderReturnedOrderReadyToBeSentToIM() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().ship().makeReturn().accept().sort().get();
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(order.getPlaceCount(), user);
        var canceledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(canceledOrder.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(0);
    }

    @Test
    void cantCancelAlreadyShippedToReturnOrder() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().ship().makeReturn().accept().sort().ship().get();
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user); // заказ не отменился
        var returnedOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(returnedOrder.getFfStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(0);
    }

    @Test
    void canCancelPreparedForUtilizationOrder() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.UTILIZATION_ENABLED, true);
        var utilizatorWH = testFactory.storedWarehouse("utilizator-1", WarehouseType.UTILIZATOR);
        var utilCell = testFactory.storedCell(sortingCenter,
                "utils-cell-1",
                CellType.RETURN,
                CellSubType.UTILIZATION,
                utilizatorWH.getYandexId()
        );
        var seniorStockman = testFactory.storedUser(sortingCenter, 222L, UserRole.SENIOR_STOCKMAN);
        var order = testFactory.createOrder(order(sortingCenter).build())
                .cancel().accept().get();

        placeHistoryHelper.startPlaceHistoryCollection();

        Place place = testFactory.orderPlace(order);
        placeCommandService.sortPlace(
                new PlaceScRequest(new PlaceId(order.getId(), place.getExternalId()), seniorStockman),
                utilCell.getId(),
                true
        );


        placeHistoryHelper.validateThatNRecordsWithUserCollected(
                routeSoEnabled()
                        ? 2  // создает варехаус ретурн и сортирует заказ
                        : 1
        , seniorStockman);


        var utilizedOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(utilizedOrder.getFfStatus()).isEqualTo(SORTING_CENTER_PREPARED_FOR_UTILIZE);

        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        var canceledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(canceledOrder.getFfStatus()).isEqualTo(SORTING_CENTER_PREPARED_FOR_UTILIZE);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(0);
    }

    @Test
    void cantCancelUtilizedOrder() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.UTILIZATION_ENABLED, true);
        var utilizatorWH = testFactory.storedWarehouse("utilizator-1", WarehouseType.UTILIZATOR);
        var utilCell = testFactory.storedCell(sortingCenter,
                "utils-cell-1",
                CellType.RETURN,
                CellSubType.UTILIZATION,
                utilizatorWH.getYandexId()
        );
        var seniorStockman = testFactory.storedUser(sortingCenter, 222L, UserRole.SENIOR_STOCKMAN);
        var order = testFactory.createOrder(order(sortingCenter).build())
                .cancel().acceptPlaces().get();
        PlaceScRequest request = testFactory.placeScRequest(order, seniorStockman);
        placeHistoryHelper.startPlaceHistoryCollection();

        placeCommandService.sortPlace(request, utilCell.getId(), false);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(routeSoEnabled() ? 2 : 1, seniorStockman);
        var sortedOrder = orderRepository.findByIdOrThrow(order.getId());
        var places = placeRepository.findAllByOrderIdOrderById(sortedOrder.getId());

        assertThat(places).allMatch(
                p -> Objects.equals(
                        p.getMutableState().getStageId(),
                        StageLoader.getBySortableStatus(SortableStatus.SORTED_RETURN).getId()
                )
        );
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.shipOrders(
                OrdersScRequest.ofExternalIds(
                        Map.of(
                                sortedOrder.getExternalId(),
                                places.stream()
                                        .map(Place::getMainPartnerCode)
                                        .toList()),
                        new ScContext(seniorStockman)
                ),
                null, sortedOrder.getWarehouseReturn().getId());

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, seniorStockman);
        var utilizedOrder = orderRepository.findByIdOrThrow(order.getId());
        var placesUtil = placeRepository.findAllByOrderIdOrderById(sortedOrder.getId());
        assertThat(utilizedOrder.getFfStatus()).isEqualTo(SORTING_CENTER_SHIPPED_FOR_UTILIZER);
        assertThat(placesUtil).allMatch(
                p -> Objects.equals(
                        p.getMutableState().getStageId(),
                        StageLoader.getBySortableStatus(SHIPPED_RETURN).getId()
                )
        );
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, true, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user); // не смогли отменить заказ
        var canceledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(canceledOrder.getFfStatus()).isEqualTo(SORTING_CENTER_SHIPPED_FOR_UTILIZER);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(0);
    }

    @Test
    void canCancelAlreadyCanceledOrderOrderCancelledFF() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).cancel().get();
        assertThat(order.getFfStatus()).isEqualTo(ORDER_CANCELLED_FF);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(sortingCenter, order.getExternalId(), null, true, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);

        var canceledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(canceledOrder.getFfStatus()).isEqualTo(ORDER_CANCELLED_FF);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(0);
    }

    @Test
    void cantKeepOrderToDeletedCell() {
        var cell = testFactory.storedCell(sortingCenter, "b-1", CellType.BUFFER);
        var place = testFactory.createOrder(sortingCenter).accept().getPlace();
        testFactory.deleteCellForce(cell);

        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), cell.getId(), false))
                .isInstanceOf(ScException.class)
                .has(new Condition<>(
                        (e) -> Objects.equals(((ScException) e).getCode(), ScErrorCode.CANT_USE_DELETED_CELL.name()),
                        "code=CANT_USE_DELETED_CELL"
                ));
    }

    @Test
    void cantSortOrderToDeletedCell() {
        var cell = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);
        var place = testFactory.createOrderForToday(sortingCenter).accept().getPlace();
        testFactory.markCellDeleted(cell);

        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), cell.getId(), false)
        ).isInstanceOf(ScException.class)
                .has(new Condition<>(
                        (e) -> Objects.equals(((ScException) e).getCode(), ScErrorCode.CANT_USE_DELETED_CELL.name()),
                        "code=CANT_USE_DELETED_CELL"
                ));
    }

    @Test
    void sortBetweenReturnCells() {
        var cell1 = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN, "w1");
        var cell2 = testFactory.storedCell(sortingCenter, "r2", CellType.RETURN, "w1");
        var place = testFactory.createForToday(order(sortingCenter).warehouseReturnId("w1").build())
                .cancel()
                .accept()
                .sort(cell2.getId())
                .getPlace();

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), cell1.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(SORTED_RETURN).getId());
        assertThat(place.getCell()).isEqualTo(cell1);
    }

    @Test
    void keepBetweenBufferCells() {
        var cell1 = testFactory.storedCell(sortingCenter, "b1", CellType.BUFFER);
        var cell2 = testFactory.storedCell(sortingCenter, "b2", CellType.BUFFER);
        var place = testFactory.create(order(sortingCenter).warehouseReturnId("w1").build())
                .accept()
                .keep(cell2.getId())
                .getPlace();

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), cell1.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(KEEPED_DIRECT).getId());
        assertThat(place.getCell()).isEqualTo(cell1);
    }

    @Test
    void cantKeepToAnotherScCell() {
        var sortingCenter2 = testFactory.storedSortingCenter(2L);
        var keepCellSc2 = testFactory.storedCell(sortingCenter2, "2", CellType.BUFFER);
        var place = testFactory.createOrder(sortingCenter).accept().getPlace();

        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), keepCellSc2.getId(), false))
                .isInstanceOf(ScException.class)
                .is(new Condition<>(
                        e -> Objects.equals(((ScException) e).getCode(), ScErrorCode.CELL_FROM_ANOTHER_SC.name()),
                        null));
    }

    @Test
    void transitDeliveryServiceInstantDirectShip() {
        var deliveryService = testFactory.storedDeliveryService();
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.TYPE_ON_SC_PREFIX + sortingCenter.getId(),
                DeliveryServiceType.TRANSIT.name());
        placeHistoryHelper.startPlaceHistoryCollection();

        var order = testFactory.create(
                order(sortingCenter).deliveryService(deliveryService).build()
        ).accept().sort().ship().get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(4);
        assertThat(order.getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    void transitDeliveryServiceInstantReturnShip() {
        var deliveryService = testFactory.storedDeliveryService();
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.TYPE_ON_SC_PREFIX + sortingCenter.getId(),
                DeliveryServiceType.TRANSIT.name());
        placeHistoryHelper.startPlaceHistoryCollection();

        var order = testFactory.create(
                order(sortingCenter).deliveryService(deliveryService).build()
        ).accept().sort().ship().accept().sort().ship().get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(7);
        assertThat(order.getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    void transitDeliveryServiceCantUpdateCourier() {
        var deliveryService = testFactory.storedDeliveryService();
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.TYPE_ON_SC_PREFIX + sortingCenter.getId(),
                DeliveryServiceType.TRANSIT.name());
        var order = testFactory.create(
                order(sortingCenter).deliveryService(deliveryService).build()
        ).get();
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.updateCourier(order.getId(), testFactory.defaultCourier(),
                testFactory.getOrCreateAnyUser(sortingCenter)))
                .isInstanceOf(ScInvalidTransitionException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void sortInAdvance() {
        int daysInAdvance = 1;
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        var place =
                testFactory.create(order(sortingCenter).externalId("o1").dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance))
                        .accept()
                        .getPlace();
        var courierCell = testFactory.determineRouteCell(
                testFactory.findOutgoingCourierRoute(place).orElseThrow(), place);

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), courierCell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getCell()).isNotNull();
        assertThat(place.getCell().getType()).isEqualTo(CellType.COURIER);
    }

    @Test
    void cantKeepIfCanSortInAdvance() {
        int daysInAdvance = 1;
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        Cell keepCell = testFactory.storedCell(sortingCenter, "keep 1", CellType.BUFFER);
        var place =
                testFactory.create(order(sortingCenter).externalId("o1").dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance))
                        .accept()
                        .getPlace();

        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), keepCell.getId(), false))
                .isInstanceOf(ScException.class);
    }

    @Test
    void canKeepIfCantSortInAdvance() {
        int daysInAdvance = 1;
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        var deliveryService = testFactory.storedDeliveryService("1");
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, String.valueOf(sortingCenter.getId()));
        Cell keepCell1 = testFactory.storedCell(sortingCenter, "keep 1", CellType.BUFFER);
        var place = testFactory.create(order(sortingCenter).externalId("o1")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT).build())
                .updateShipmentDate(LocalDate.now(clock).plusDays(daysInAdvance + 1))
                .accept()
                .getPlace();

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), keepCell1.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getCell()).isNotNull();
        assertThat(place.getCell().getType()).isEqualTo(CellType.BUFFER);
    }

    @Test
    void createClientReturnWithBadBarcode() {
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.createClientReturn(
                new CreateClientReturnRequest(
                        sortingCenter.getId(),
                        sortingCenter.getToken(),
                        sortingCenter.getYandexId(),
                        testFactory.defaultCourier(),
                        "P293824120",
                        LocalDate.now(clock),
                        null,
                        null,
                        null,
                        null,
                        null
                ), user
        )).isInstanceOf(TplInvalidActionException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
    }

    @Test
    void cantUpdateCourierForMiddleMile() {
        var order = testFactory.createForToday(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                .get();
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.updateCourier(
                order.getId(), testFactory.defaultCourier(), testFactory.getOrCreateAnyUser(sortingCenter)
        )).isInstanceOf(ScInvalidTransitionException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void cantUpdateCourierForDsWithSingleCourier() {
        var order = testFactory.createForToday(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                .get();
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.updateCourier(
                order.getId(), testFactory.defaultCourier(), testFactory.getOrCreateAnyUser(sortingCenter)
        )).isInstanceOf(ScInvalidTransitionException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void sortMiddleMileOrderWithoutShipmentDate() {
        var place = testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                .accept().sort().getPlace();

        assertThat(place.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(place.getCell()).isNotNull();
        assertThat(place.getCell().getType()).isEqualTo(CellType.COURIER);
    }

    @Test
    void sortMiddleMileOrderWithShipmentDateInFuture() {
        var place = testFactory.create(
                        order(sortingCenter)
                                .shipmentDate(LocalDate.now(clock).plusDays(1))
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build()
                )
                .accept().sort().getPlace();

        assertThat(place.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(place.getCell()).isNotNull();
        assertThat(place.getCell().getType()).isEqualTo(CellType.COURIER);
    }

    @Test
    void sortMiddleMileOrderWithShipmentDateInPast() {
        var place = testFactory.create(
                        order(sortingCenter)
                                .shipmentDate(LocalDate.now(clock).minusDays(1))
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build()
                )
                .accept().sort().getPlace();
        assertThat(place.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(place.getCell()).isNotNull();
        assertThat(place.getCell().getType()).isEqualTo(CellType.COURIER);
    }

    @Test
    void shipMiddleMileOrderWithoutShipmentDate() {
        var place = testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                .accept().sort().ship().getPlace();

        assertThat(place.getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    void shipMiddleMileOrderWithShipmentDateInFuture() {
        var place = testFactory.create(
                        order(sortingCenter)
                                .shipmentDate(LocalDate.now(clock).plusDays(1))
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build()
                )
                .accept()
                .sort()
                .ship()
                .getPlace();

        assertThat(place.getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    void shipMiddleMileOrderWithShipmentDateInPast() {
        placeHistoryHelper.startPlaceHistoryCollection();

        var order = testFactory.create(
                        order(sortingCenter)
                                .shipmentDate(LocalDate.now(clock).minusDays(1))
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build()
                )
                .accept().sort().ship().get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(4);
        assertThat(order.getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    void createMiddleMileOrder() {
        placeHistoryHelper.startPlaceHistoryCollection();
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    1 * order.getPlaceCount() // stage changes
                            + 1 * order.getPlaceCount() // FF API Update даты в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        }
        assertThat(order.isMiddleMile()).isTrue();
    }

    @Test
    void createNotMiddleMileOrder() {

        var order = testFactory.createForToday(
                order(sortingCenter).build()
        ).get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    1 * order.getPlaceCount() // stage changes
                            + 1 * order.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        }
        assertThat(order.isMiddleMile()).isFalse();
    }

    @Test
    void cancelPartiallyArrivedOrder() {
        var order = testFactory.create(
                order(sortingCenter).externalId("o1").places("p1", "p2").build()
        ).acceptPlaces("p1").keepPlaces("p1").get();
        ArrayList<Map<String, Object>> maps = placeHistoryHelper.viewCollected();
        placeHistoryHelper.validateThatNRecordsWithUserCollected(4);
        assertThat(order.getFfStatus()).isEqualTo(ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
        orderCommandService.cancelOrder(
                sortingCenter, "o1", null, false, user);
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getFfStatus())
                .isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void cancelAcceptedOrder() {
        var order = testFactory.create(
                order(sortingCenter).externalId("o1").build()
        ).accept().get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(2);
        assertThat(order.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
        orderCommandService.cancelOrder(
                sortingCenter, "o1", null, false, user);
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getFfStatus())
                .isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
        var places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places).allMatch(
                p -> Objects.equals(
                        p.getMutableState().getStageId(),
                        StageLoader.getBySortableStatus(ACCEPTED_RETURN).getId()
                )
        );
    }

    @Test
    void updatePlacesFromMultiToZero() {
        var orderParamsBuilder = order(sortingCenter).externalId("ext1").places("1", "2");
        var order = testFactory.create(orderParamsBuilder.build()).get();
        Order ffOrder = testFactory.new TestOrderBuilder().orderRequest(orderParamsBuilder.places().build());
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(sortingCenter, ffOrder), user);

        // пришел заказ без коробок через FF API.
        // Такое бывает для одноместных заказов, мы добавляем его вирутальную коробку
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        order = orderRepository.findByIdOrThrow(order.getId());
        List<String> partnerCodes = testFactory.orderPlaces(order)
                .stream()
                .map(Place::getMainPartnerCode)
                .toList();

        assertThat(partnerCodes).hasSize(1);
        assertThat(partnerCodes.get(0)).isEqualTo("ext1");
    }

    @Test
    void updatePlacesFromZeroToMulti() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        var orderParamsBuilder = order(sortingCenter).externalId("1");
        var order = testFactory.create(orderParamsBuilder.build()).get();
        Order ffOrder = testFactory.new TestOrderBuilder()
                .orderRequest(orderParamsBuilder.places("1", "2").build());
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(sortingCenter, ffOrder), user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(2);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(testFactory.orderPlaces(order).stream().map(Place::getMainPartnerCode).toList())
                .isEqualTo(List.of("1", "2"));
    }

    @Test
    void createMultiPlaceOrder() {
        testFactory.create(order(sortingCenter).createTwoPlaces(true).build()).get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(2);
    }

    @Test
    void createNotMultiPlaceOrder() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        testFactory.create(order(sortingCenter).build()).get();
    }

    @Test
    void createOrderForRegularWarehouse() {
        String warehouseFromId = "whFromId1";
        String warehouseReturnId = "whToId1";
        createOrderAndCheckWarehouses(sortingCenter,
                warehouseFromId, warehouseReturnId, warehouseFromId, warehouseReturnId);
    }

    @Test
    void createOrderForTarniyOnRegularSc() {
        createOrderAndCheckWarehouses(sortingCenter,
                "10001700279", "10001700279", "10001700279", "10001700279");
        createOrderAndCheckWarehouses(sortingCenter,
                "10001699601", "10001699601", "10001699601", "10001699601");

    }

    private void createOrderAndCheckWarehouses(SortingCenter sortingCenter,
                                               String warehouseFromId,
                                               String warehouseReturnId,
                                               String expectedWarehouseFromId,
                                               String expectedWarehouseReturnId) {
        placeHistoryHelper.startPlaceHistoryCollection();

        var order = testFactory.create(
                order(sortingCenter)
                        .externalId("o-" + warehouseFromId)
                        .warehouseFromId(warehouseFromId)
                        .warehouseReturnId(warehouseReturnId)
                        .build()
        ).get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        assertThat(order.getWarehouseFrom().getYandexId()).isEqualTo(expectedWarehouseFromId);
        assertThat(order.getWarehouseReturn().getYandexId()).isEqualTo(expectedWarehouseReturnId);
    }

    @Test
    void rescheduleSortDateMiddleMile() {
        var order = testFactory.createForToday(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().get();
        var expectedDate = LocalDate.now(clock).plusDays(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.rescheduleSortDateTransit(List.of(order.getId()), expectedDate, sortingCenter, user);

        //Нужно поменять дату и у коробок тоже
        List<Long> placeIds = testFactory.orderPlaces(order).stream().map(Place::getId).toList();
        placeRouteSoService.rescheduleSortDateTransit(placeIds, ScDateUtils.toNoon(expectedDate), user);

        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
        }

        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(expectedDate);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rescheduleSortDateMiddleMileForPartialAccepted() {
        var order = testFactory.createForToday(
                order(sortingCenter).places("1", "2").dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("1").get();
        var expectedDate = LocalDate.now(clock).plusDays(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        //Методы, которые тестируем
        orderCommandService.rescheduleSortDateTransit(List.of(order.getId()), expectedDate, sortingCenter, user);
        var places = testFactory.orderPlaces(order).stream().map(Place::getId).toList();
        placeRouteSoService.rescheduleSortDateTransit(places, ScDateUtils.toNoon(expectedDate), user);

        //обновляется сорт дата, она не входит в mutable state в старом флоу
        //Новые руты для 2  коробок в новом флоу
        placeHistoryHelper.validateThatNRecordsWithUserCollected(    routeSoEnabled() ?2 : 0);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(expectedDate);
    }

    @Test
    void rescheduleSortDateReturn() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().makeReturn().get();
        var expectedDate = LocalDate.now(clock).plusDays(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.rescheduleSortDateReturns(List.of(order.getId()), expectedDate, sortingCenter, user);

        //Нужно поменять дату и у коробок тоже
        List<Long> placeIds = testFactory.orderPlaces(order).stream().map(Place::getId).toList();
        placeRouteSoService.rescheduleSortDateTransit(placeIds, ScDateUtils.toNoon(expectedDate), user);

        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
        }

        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(expectedDate);
    }

    @Test
    void acceptDropship() {
        var place = testFactory.createForToday(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                .accept()
                .sort()
                .getPlace();
        var courierRoute = testFactory.findOutgoingCourierRoute(place).get();
        courierRoute.allowNextRead();
        var courierCell = courierRoute.getCells(LocalDate.now(clock)).iterator().next();
        testFactory.shipOrderRoute(place.getOrder());

        PlaceScRequest request = testFactory.placeScRequest(place, user);
        placeHistoryHelper.startPlaceHistoryCollection();

        acceptService.acceptPlace(request);

        //todo:kir вернуть проверку
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, request.getUser());
        placeCommandService.sortPlace(request, courierCell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(place.getCell()).isEqualTo(courierCell);
        checkLastEventOfStatus(place.getOrder(), ORDER_READY_TO_BE_SEND_TO_SO_FF);

        assertThat(place.getSortableStatus()).isEqualTo(SORTED_DIRECT);
        assertThat(place.getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(SORTED_DIRECT).getId());
    }

    @Test
    void sortMiddleMileOrder() {
        var place = testFactory.create(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .shipmentDate(LocalDate.now(clock).minusDays(1))
                                .build()
                )
                .accept()
                .getPlace();
        var courierCell = testFactory.determineRouteCell(
                testFactory.findOutgoingCourierRoute(place).orElseThrow(), place);

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), courierCell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(place.getCell()).isEqualTo(courierCell);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @Test
    void retryOnOptimisticLock() {
        AtomicInteger retryNum = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (retryNum.incrementAndGet() < 3) {
                throw new OptimisticLockException();
            }
            return invocation.callRealMethod();
        }).when(orderLockRepository).saveRw(any());
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThat(orderCommandService.createOrder(new OrderCreateRequest(
                sortingCenter,
                ffOrder(sortingCenter.getToken())
        ), user)).isNotNull();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
    }

    @Test
    void updateOrderItemsTest() {
        var order = testFactory.createForToday(order(sortingCenter).build()).get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    1 * order.getPlaceCount() // stage changes
                            + 1 * order.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        }
        int price = updateOrderItems(order);
        checkItemsUpdated(order, price);
    }

    private void checkItemsUpdated(OrderLike order, int price) {
        transactionTemplate.execute(ts -> {
            var scOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(scOrder.getAssessedCost()).isEqualTo(BigDecimal.valueOf(price));
            assertThat(scOrder.getTotal()).isEqualTo(BigDecimal.valueOf(price));
            assertThat(scOrder.getOrderItems().size()).isEqualTo(1);
            assertThat(scOrder.getOrderItems().get(0)).isEqualToComparingOnlyGivenFields(
                    new ScOrderItem("Новый товар", 1L, BigDecimal.valueOf(price), null, null, null, null),
                    "name", "count", "price"
            );
            assertThat(scOrder.getMeasurements()).isEqualToComparingOnlyGivenFields(
                    new Measurements(1L, 1L, 1L, BigDecimal.valueOf(1), null, null),
                    "width", "height", "length", "weightGross"
            );
            return null;
        });
    }

    private int updateOrderItems(OrderLike order) {
        var korobyte = createKorobyte(1, 1, 1, BigDecimal.valueOf(1));
        var newFfItem = createItem("Новый товар", 1, 100200);
        int price = 100200;
        var request = createUpdateOrderItemsRequest(
                order.getExternalId(),
                price,
                price,
                korobyte,
                List.of(newFfItem)
        );
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateOrderItems(sortingCenter, request);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
        return price;
    }

    @Test
    void updateOrderItemsForPreparedOrder() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().prepare().get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    4 // stage changes
                            + 1 // FF API Update даты в create for today
                            + 1 // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(4);
        }
        int price = updateOrderItems(order);
        checkItemsUpdated(order, price);
    }

    @Test
    void updateOrderItemsOrderNotOnScTest() {
        var order = testFactory.createForToday(order(sortingCenter).build()).accept().sort().ship().get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    4 * order.getPlaceCount() // stage changes
                            + 1 * order.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(4);
        }
        var korobyte = createKorobyte(1, 1, 1, BigDecimal.valueOf(1));
        var newFfItem = createItem("Новый товар", 1, 100200);
        int price = 100200;
        var request = createUpdateOrderItemsRequest(
                order.getExternalId(),
                price,
                price,
                korobyte,
                List.of(newFfItem)
        );
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.updateOrderItems(sortingCenter, request))
                .isInstanceOf(ScInvalidTransitionException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    private UpdateOrderItemsRequest createUpdateOrderItemsRequest(
            String externalId, int total, int assessedCost, Korobyte korobyte, List<Item> items
    ) {
        return new UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder()
                .setOrderId(new ResourceId.ResourceIdBuilder().setYandexId(externalId).build())
                .setTotal(BigDecimal.valueOf(total))
                .setAssessedCost(BigDecimal.valueOf(assessedCost))
                .setDeliveryCost(BigDecimal.valueOf(100))
                .setKorobyte(korobyte)
                .setItems(items)
                .build();
    }

    @SuppressWarnings("SameParameterValue")
    private Item createItem(String name, int count, int price) {
        return new Item.ItemBuilder(name, count, BigDecimal.valueOf(price)).build();
    }

    @SuppressWarnings("SameParameterValue")
    private Korobyte createKorobyte(Integer width, Integer height, Integer length, BigDecimal weightGross) {
        return new Korobyte.KorobyteBuiler(width, height, length, weightGross).build();
    }

    @Test
    void updateOrderItemsFromXmlTest() {
        var order = testFactory.createForToday(order(sortingCenter).externalId("12775551-YD4897759").build()).get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateOrderItems(sortingCenter, ffRequest());

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        transactionTemplate.execute(ts -> {
            var scOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(scOrder.getOrderItems().size()).isEqualTo(1);
            assertThat(scOrder.getOrderItems().get(0).getName()).isEqualTo(
                    "Nordic Хлопья пшенные, 500 г"
            );
            return null;
        });
    }


    @SneakyThrows
    private UpdateOrderItemsRequest ffRequest() {
        String rawInput = IOUtils.toString(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream("ff_update_order_items.xml")
                ),
                StandardCharsets.UTF_8
        );

        RequestWrapper<UpdateOrderItemsRequest> request = SC_OBJECT_MAPPER.readValue(rawInput,
                new TypeReference<RequestWrapper<UpdateOrderItemsRequest>>() {
                });

        return request.getRequest();
    }

    @Test
    void createOrder() {

        assertThat(orderCommandService.createOrder(new OrderCreateRequest(
                sortingCenter,
                ffOrder(sortingCenter.getToken())
        ), user).getExternalId())
                .isEqualTo("12775551-YD4897759");

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
    }

    @Test
    void createOrderNewReturnFlowWarehouse() {
        final String incorporation = "incorp";

        //from xml
        final String orderYandexId = "12775551-YD4897759";
        final String warehouseYandexId = "9865";
        final String partnerId = "153240";

        assertThat(testFactory.create(
                order(sortingCenter)
                        .warehouseReturnName(incorporation)
                        .warehouseReturnType(ReturnType.WAREHOUSE)
                        .build()).get().getExternalId())
                .isEqualTo(orderYandexId);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        Optional<ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse> warehouse =
                warehouseRepository.findByYandexId(warehouseYandexId);
        assertThat(warehouse).isPresent();
        var warehousePersisted = warehouse.get();
        assertThat(warehousePersisted.getYandexId()).isEqualTo(warehouseYandexId);

        assertThat(warehousePersisted.getPartnerId()).isEqualTo(partnerId);
        assertThat(warehousePersisted.getShopId()).isNull();
        assertThat(warehousePersisted.getIncorporation()).isEqualTo(incorporation);
        assertThat(warehousePersisted.getType()).isEqualTo(WarehouseType.SORTING_CENTER);

        final OrderLike scOrder =
                orderRepository.findBySortingCenterAndExternalId(sortingCenter, orderYandexId).orElse(null);
        assertThat(scOrder).isNotNull();
        assertThat(scOrder.getReturnDeliveryService()).isNull();
        assertThat(scOrder.getWarehouseReturn().getId()).isEqualTo(warehousePersisted.getId());
    }

    @Test
    void createOrderNewReturnFlowShop() {
        final String incorporation = "incorp";

        //from xml
        final String orderYandexId = "12775551-YD4897759";
        final String partnerId = "153240";
        final String senderYandexId = "431782";

        assertThat(testFactory.create(
                order(sortingCenter)
                        .warehouseReturnName(incorporation)
                        .warehouseReturnType(ReturnType.SHOP)
                        .build()).get().getExternalId())
                .isEqualTo(orderYandexId);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        Optional<ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse> warehouse =
                warehouseRepository.findByYandexId(senderYandexId);
        assertThat(warehouse).isPresent();
        var warehousePersisted = warehouse.get();
        assertThat(warehousePersisted.getYandexId()).isEqualTo(senderYandexId);

        assertThat(warehousePersisted.getPartnerId()).isEqualTo(partnerId);
        assertThat(warehousePersisted.getShopId()).isEqualTo(senderYandexId);
        assertThat(warehousePersisted.getIncorporation()).isEqualTo(incorporation);
        assertThat(warehousePersisted.getType()).isEqualTo(WarehouseType.SHOP);

        final OrderLike scOrder =
                orderRepository.findBySortingCenterAndExternalId(sortingCenter, orderYandexId).orElse(null);
        assertThat(scOrder).isNotNull();
        assertThat(scOrder.getReturnDeliveryService()).isNull();
        assertThat(scOrder.getWarehouseReturn().getId()).isEqualTo(warehousePersisted.getId());
    }


    @Test
    void createOrderNewReturnFlowShopWithTransportPartner() {
        final String partnerToPartnerId = "partnerToPartnerId";
        final String partnerToIncorporation = "partnerToIncorporation";
        final String partnerTransporterPartnerId = "partnerTransporterPartnerId";
        final String partnerTransporterIncorporation = "partnerTransporterIncorporation";
        final String type = "SHOP";

        //from xml
        final String orderYandexId = "12775551-YD4897759";
        final String warehouseYandexId = "9865";
        final String senderYandexId = "431782";

        assertThat(orderCommandService.createOrder(new OrderCreateRequest(
                sortingCenter,
                ffOrderWithParams("ff_create_order_with_return_info_with_partner_transporter.xml",
                        sortingCenter.getToken(),
                        partnerToPartnerId,
                        partnerToIncorporation,
                        partnerTransporterPartnerId,
                        partnerTransporterIncorporation,
                        type
                )
        ), user).getExternalId())
                .isEqualTo(orderYandexId);
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);


        Optional<ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse> warehouse =
                warehouseRepository.findByYandexId(senderYandexId);
        assertThat(warehouse).isPresent();
        var warehousePersisted = warehouse.get();
        assertThat(warehousePersisted.getYandexId()).isEqualTo(senderYandexId);

        assertThat(warehousePersisted.getPartnerId()).isEqualTo(partnerToPartnerId);
        assertThat(warehousePersisted.getShopId()).isEqualTo(senderYandexId);
        assertThat(warehousePersisted.getIncorporation()).isEqualTo(partnerToIncorporation);
        assertThat(warehousePersisted.getType()).isEqualTo(WarehouseType.SHOP);

        DeliveryService deliveryService =
                deliveryServiceRepository.findByYandexId(partnerTransporterPartnerId).orElse(null);
        assertThat(deliveryService).isNotNull();
        assertThat(deliveryService.getYandexId()).isEqualTo(partnerTransporterPartnerId);
        assertThat(deliveryService.getName()).isEqualTo(partnerTransporterIncorporation);

        final OrderLike scOrder =
                orderRepository.findBySortingCenterAndExternalId(sortingCenter, orderYandexId).orElse(null);
        assertThat(scOrder).isNotNull();
        assertThat(scOrder.getReturnDeliveryService().getId()).isEqualTo(deliveryService.getId());
        assertThat(scOrder.getWarehouseReturn().getId()).isEqualTo(warehousePersisted.getId());
    }

    @Test
    void createOrderTwiceOnDifferentWarehouseReturnTplUpdateLast() {
        var orderYandexId1 = "12775551-YD4897759";
        var mail1 = "someMail@yandex.ru";
        var tplMail = "tpl.platform@yandex.ru";
        var whFromYandexId1 = "whFromYandexId1";
        var whFromYandexId2 = "whFromYandexId2";
        var whToYandexId1 = "whToYandexId1";
        var whToYandexId2 = "whToYandexId2";

        var order = testFactory.create(
                order(sortingCenter)
                        .externalId(orderYandexId1)
                        .recipientEmail(mail1)
                        .warehouseFromId(whFromYandexId1)
                        .warehouseReturnId(whToYandexId1)
                        .build()).get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        var whFrom = order.getWarehouseFrom();
        var whTo = order.getWarehouseReturn();
        placeHistoryHelper.startPlaceHistoryCollection();

        var createOrderByCourierService = testFactory.create(
                order(sortingCenter)
                        .externalId(orderYandexId1)
                        .recipientEmail(tplMail)
                        .warehouseFromId(whFromYandexId2)
                        .warehouseReturnId(whToYandexId2)
                        .build()).get();

        //обновляются склады, они не входят в mutable state
        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        assertThat(whFrom)
                .isEqualTo(createOrderByCourierService.getWarehouseFrom());
        assertThat(whTo)
                .isEqualTo(createOrderByCourierService.getWarehouseReturn());
    }

    @Test
    void createOrderTwiceOnDifferentWarehouseReturnTplUpdateFirst() {
        var orderYandexId1 = "12775551-YD4897759";
        String mail1 = "someMail@yandex.ru";
        String tplMail = "tpl.platform@yandex.ru";
        String whFromYandexId1 = "whFromYandexId1";
        String whFromYandexId2 = "whFromYandexId2";
        var whToYandexId1 = "whToYandexId1";
        var whToYandexId2 = "whToYandexId2";

        var createOrderByCourierService = testFactory.create(
                order(sortingCenter)
                        .externalId(orderYandexId1)
                        .recipientEmail(tplMail)
                        .warehouseFromId(whFromYandexId1)
                        .warehouseReturnId(whToYandexId1)
                        .build()).get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        var whFrom = createOrderByCourierService.getWarehouseFrom();
        var whTo = createOrderByCourierService.getWarehouseReturn();
        placeHistoryHelper.startPlaceHistoryCollection();

        var order = testFactory.create(
                order(sortingCenter)
                        .externalId(orderYandexId1)
                        .recipientEmail(mail1)
                        .warehouseFromId(whFromYandexId2)
                        .warehouseReturnId(whToYandexId2)
                        .build()).get();

        // это тот же самый заказ - нет записей в стории
        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        assertThat(order.getWarehouseFrom())
                .isNotEqualTo(whFrom);
        assertThat(order.getWarehouseReturn())
                .isNotEqualTo(whTo);
    }

    @Test
    void createOrderNewReturnFlowDropoff() {
        final String incorporation = "incorp";

        //from xml
        final String orderYandexId = "12775551-YD4897759";
        final String partnerId = "153240";
        final String warehouseYandexId = "9865";

        assertThat(testFactory.create(
                order(sortingCenter)
                        .warehouseReturnName(incorporation)
                        .warehouseReturnType(ReturnType.DROPOFF)
                        .build()).get().getExternalId())
                .isEqualTo(orderYandexId);
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);

        Optional<ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse> warehouse =
                warehouseRepository.findByYandexId(warehouseYandexId);
        assertThat(warehouse).isPresent();
        var warehousePersited = warehouse.get();
        assertThat(warehousePersited.getYandexId()).isEqualTo(warehouseYandexId);

        assertThat(warehousePersited.getPartnerId()).isEqualTo(partnerId);
        assertThat(warehousePersited.getShopId()).isNull();
        assertThat(warehousePersited.getIncorporation()).isEqualTo(incorporation);
        assertThat(warehousePersited.getType()).isEqualTo(WarehouseType.DROPOFF);
    }

    @Test
    void createOrderWarehouseUpdateNotWorkingWithoutScProperty() {
        final String partnerId = "153240";
        final String incorporation = "incorp";
        final String orderYandexId = "12775551-YD4897759";
        final String orderYandexId2 = "12775551-YD4897759-2";

        //from xml
        final String warehouseYandexId = "9865";

        assertThat(testFactory.create(
                order(sortingCenter)
                        .externalId(orderYandexId)
                        .warehouseReturnName(incorporation)
                        .warehouseReturnType(ReturnType.WAREHOUSE)
                        .build()).get().getExternalId())
                .isEqualTo(orderYandexId);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThat(testFactory.create(
                order(sortingCenter)
                        .externalId(orderYandexId2)
                        .warehouseReturnName("new_incorp")
                        .warehouseReturnType(ReturnType.DROPOFF)
                        .build()).get().getExternalId())
                .isEqualTo(orderYandexId2);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        Optional<ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse> warehouse =
                warehouseRepository.findByYandexId(warehouseYandexId);
        assertThat(warehouse).isPresent();
        var warehousePersisted = warehouse.get();
        assertThat(warehousePersisted.getYandexId()).isEqualTo(warehouseYandexId);

        assertThat(warehousePersisted.getPartnerId()).isEqualTo(partnerId);
        assertThat(warehousePersisted.getShopId()).isNull();
        assertThat(warehousePersisted.getIncorporation()).isEqualTo(incorporation);
        assertThat(warehousePersisted.getType()).isEqualTo(WarehouseType.SORTING_CENTER);

        final OrderLike scOrder =
                orderRepository.findBySortingCenterAndExternalId(sortingCenter, orderYandexId).orElse(null);
        assertThat(scOrder).isNotNull();
        assertThat(scOrder.getReturnDeliveryService()).isNull();
        assertThat(scOrder.getWarehouseReturn().getId()).isEqualTo(warehousePersisted.getId());
    }

    @Test
    void createOrderInDifferentSortingCenters() {
        var ffOrder = ffOrder(sortingCenter.getToken());
        var sortingCenter1 = testFactory.storedSortingCenter(11L);
        var sortingCenter2 = testFactory.storedSortingCenter(12L);
        assertThat(orderCommandService.createOrder(new OrderCreateRequest(sortingCenter1, ffOrder), user).getExternalId())
                .isEqualTo("12775551-YD4897759");

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThat(orderCommandService.createOrder(new OrderCreateRequest(sortingCenter2, ffOrder), user).getExternalId())
                .isEqualTo("12775551-YD4897759");

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
    }

    @Test
    void ordersWithSameIdHasDifferentStatusesInDifferentSortingCenters() {
        String externalId = "order1";
        var sortingCenter1 = testFactory.storedSortingCenter(11L);
        var sortingCenter2 = testFactory.storedSortingCenter(12L);
        var order1 = testFactory.createForToday(order(sortingCenter1, externalId).build()).get();
        var order2 = testFactory.create(order(sortingCenter2, externalId).build()).get();
        placeHistoryHelper.startPlaceHistoryCollection();

        testFactory.cancelOrder(order1.getId());

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        assertThat(orderRepository.findByIdOrThrow(order1.getId()).getOrderStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);
        assertThat(orderRepository.findByIdOrThrow(order2.getId()).getOrderStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_CREATED_FF);
    }

    @Test
    void createOrderWithRomashkaWarehouse() {
        String warehouseFromId = "wf1-id";
        String warehouseFromName = "ООО Ромашка";

        String warehouseReturnId = "wr2-id";
        String warehouseReturnName = "ООО Ромашка";
        placeHistoryHelper.startPlaceHistoryCollection();

        var order = testFactory.create(order(sortingCenter).externalId("1")
                .warehouseFromId(warehouseFromId).warehouseFromName(warehouseFromName)
                .warehouseReturnId(warehouseReturnId).warehouseReturnName(warehouseReturnName)
                .build()
        ).get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        assertThat(order.getWarehouseFrom().getIncorporation()).isEqualTo(
                "Неизвестный склад, уточните у поддержки"
        );
        assertThat(order.getWarehouseReturn().getIncorporation()).isEqualTo(
                "Неизвестный склад, уточните у поддержки"
        );
    }

    @Test
    void createOrderUpdatesWarehouseName() {
        String warehouseFromId = "wf1-id";
        String warehouseFromName1 = "wf1-name-1";
        String warehouseFromName2 = "wf1-name-2";

        String warehouseReturnId = "wr1-id";
        String warehouseReturnName1 = "wr1-name-1";
        String warehouseReturnName2 = "wr1-name-2";
        placeHistoryHelper.startPlaceHistoryCollection();

        var order1 = testFactory.create(order(sortingCenter).externalId("1")
                .warehouseFromId(warehouseFromId).warehouseFromName(warehouseFromName1)
                .warehouseReturnId(warehouseReturnId).warehouseReturnName(warehouseReturnName1)
                .build()
        ).get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        assertThat(order1.getWarehouseFrom().getIncorporation()).isEqualTo(warehouseFromName1);
        assertThat(order1.getWarehouseReturn().getIncorporation()).isEqualTo(warehouseReturnName1);

        placeHistoryHelper.startPlaceHistoryCollection();
        testFactory.create(order(sortingCenter).externalId("2")
                .warehouseFromId(warehouseFromId).warehouseFromName(warehouseFromName2)
                .warehouseReturnId(warehouseReturnId).warehouseReturnName(warehouseReturnName2)
                .build()
        ).get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        transactionTemplate.execute(ts -> {
            var order = orderLockRepository.findAndValidateOrderRw(
                    order1.getId(), OrderLockRepository.NO_VALIDATION);
            assertThat(order.getWarehouseFrom().getIncorporation())
                    .isEqualTo(warehouseFromName1);
            assertThat(order.getWarehouseReturn().getIncorporation())
                    .isEqualTo(warehouseReturnName1);
            return null;
        });

    }

    @Test
    void createOrderTwice() {
        assertThat(orderCommandService.createOrder(new OrderCreateRequest(sortingCenter,
                ffOrder(sortingCenter.getToken())), user).getExternalId())
                .isEqualTo("12775551-YD4897759");

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);


        placeHistoryHelper.startPlaceHistoryCollection();
        assertThat(orderCommandService.createOrder(new OrderCreateRequest(sortingCenter,
                ffOrder(sortingCenter.getToken())), user).getExternalId())
                .isEqualTo("12775551-YD4897759");
        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void createOrderWithSingleCourierDs() {
        var order = testFactory.create(
                TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build()
        ).get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        assertThat(Objects.requireNonNull(order.getCourier()).getName()).isEqualTo("3PL");
        assertThat(Objects.requireNonNull(order.getCourier()).getId()).isEqualTo(1100000000000239L);
    }

    @Test
    void createOrderWithCourierAndMultiCourierDs() {
        var order = testFactory.create(
                TestFactory.CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .dsType(DeliveryServiceType.LAST_MILE_COURIER)
                        .request(ffOrder("ff_create_order_courier_updated.xml", sortingCenter.getToken()))
                        .build()
        ).get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        assertThat(order.getCourier()).isNotNull();
        assertThat(order.getCourier().getId())
                .isEqualTo(213123L);
    }

    @Test
    void createOrderWithShipmentDate() {
        OrderLike order = testFactory.create(order(sortingCenter).shipmentDate(LocalDate.now(clock)).build()).get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        assertThat(order.getShipmentDate()).isNotNull();
        assertThat(order.getOutgoingRouteDate()).isNotNull();
    }

    @Test
    void cancelOrder() {
        Place place = testFactory.createOrderForToday(sortingCenter).getPlace();

        orderCommandService.cancelOrder(
                sortingCenter, place.getExternalId(), null, false, user);

        place = testFactory.updated(place);
        assertThat(place.getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(CANCELLED).getId());

        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);
        assertThat(place.getIncomingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(place.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_CANCELLED_FF);
    }

    @Test
    void cancelOrderTwice() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .cancel()
                .getPlace();

        orderCommandService.cancelOrder(
                sortingCenter, place.getExternalId(), null, false, user);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_CANCELLED_FF);
    }

    @Test
    void cancelOrderAfterReturn() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().makeReturn().get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, false, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(order.getPlaceCount(), user);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void cancelOrderAfterShip() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, false, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
    }

    @Test
    void cancelOrderAfterReturnAndSort() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().cancel().sort().get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(
                sortingCenter, order.getExternalId(), null, false, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(order.getPlaceCount(), user);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }


    @Test
    void ffStatusHistory() {
        OrderIdResponse id = orderCommandService.createOrder(new OrderCreateRequest(sortingCenter,
                ffOrder(sortingCenter.getToken())), user);
        transactionTemplate.execute(ts -> {
            var history = orderRepository.findByIdOrThrow(id.getId()).getFfStatusHistory();
            assertThat(history.size()).isEqualTo(1);
            assertThat(history.get(0).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_CREATED_FF);
            return null;
        });
        var message = "My message";
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.cancelOrder(id.getId(), message, false, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        transactionTemplate.execute(ts -> {
            var order = orderRepository.findByIdOrThrow(id.getId());
            var history = order.getFfStatusHistory();
            assertThat(history.size())
                    .isEqualTo(2);
            assertThat(history.get(1).getFfStatus())
                    .isEqualTo(ScOrderFFStatus.ORDER_CANCELLED_FF);
            assertThat(history.get(1).getFfStatusMessage())
                    .isEqualTo(message);
            checkLastEventOfStatus(order, ScOrderFFStatus.ORDER_CANCELLED_FF);
            return null;
        });
    }

    @Test
    void updatePlaces() {
        if (true) {
            testNotMigrated();
            return;
        }

        var order = testFactory.createOrderForToday(sortingCenter).get();
        orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place.xml", sortingCenter.getToken())
        ), user);
        orderRepository.findByIdOrThrow(order.getId());
    }

    @Test
    void cantUpdatePlacesForKeepedOrder() {
        testFactory.createOrder(sortingCenter).accept().keep().get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(3);
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.updatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place.xml", sortingCenter.getToken())
        ), user)).isInstanceOf(ScException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void dontUpdatePlacesForKeepedOrder() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        testFactory.createOrder(sortingCenter).accept().keep().get();
        orderCommandService.tryToUpdatePlaces(new FFApiOrderUpdateRequest(
                sortingCenter, ffOrder("ff_update_order_multi_place.xml", sortingCenter.getToken())
        ), user);
    }

    @Test
    void updateCourier() {
        OrderLike order = testFactory.createOrder(sortingCenter).get();
        testUpdateCourier(order);
    }

    @Test
    void updateCourierFromAwaitingStatus() {
        var place = testFactory.createOrder(sortingCenter).getPlace();
        var cell = testFactory.storedCell(sortingCenter, "b-1", CellType.BUFFER);
        acceptService.acceptPlace(new PlaceScRequest(PlaceId.of(place), user));

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), cell.getId(), false);

        testUpdateCourier(place);
    }

    @Test
    void updateCourierFromAcceptedStatus() {
        OrderLike order = testFactory.createOrder(sortingCenter).accept().get();
        testUpdateCourier(order);
    }

    @Test
    void updateCourierFromPartiallyAcceptedStatus() {
        OrderLike order = testFactory.create(order(sortingCenter).places("p1", "p2").build())
                .acceptPlace("p1").get();
        testUpdateCourier(order);
    }

    private void testUpdateCourier(OrderLike order) {
        if (order.isPlace()) {
            order = order.getOrder();
        }

        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getCourier()).isNull();
        placeHistoryHelper.startPlaceHistoryCollection();

        FFApiOrderUpdateRequest request = new FFApiOrderUpdateRequest(sortingCenter, ffOrder(
                "ff_create_order_courier_updated.xml",
                sortingCenter.getToken()));

        orderCommandService.updateCourier(
                request, testFactory.getOrCreateAnyUser(sortingCenter)
        );

        ffApiPlaceService.updatePlaceRoutes(
                request, user);
        orderCommandService.tryToUpdatePlaces(
                request, user
        );



        // запись о новом place route
        //todo:kir починить
//        placeHistoryHelper.validateThatNRecordsWithUserCollected(
//                routeSoEnabled() ? testFactory.orderPlaces(order.getOrder()).size() : 0);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getCourier())
                .isEqualTo(new Courier(
                        213123L, "Иванов Иван Иванович",
                        "2139127", "Лада седан, баклажан, заниженная",
                        "+7123456789 (325)", "ООО «Логистическая компания»",
                        null
                ));
    }

    @Test
    void updateCourierFake() {
        OrderLike order = testFactory.createOrder(sortingCenter).get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateCourier(
                order.getId(), testFactory.fakeCourier(), testFactory.getOrCreateAnyUser(sortingCenter)
        );

        // статус коробки не меняется, так как курьер не в mutable state
        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        assertThat(order.getCourier()).isNull();
    }

    @Test
    void updateCourierShippedOrder() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        long newCourierUid = 321L;
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateCourier(
                order.getId(), new CourierDto(newCourierUid, "Другой курьер", null, null, null, null, null, false),
                testFactory.getOrCreateAnyUser(sortingCenter)
        );

        placeHistoryHelper.validateThatNRecordsWithUserCollected(order.getPlaceCount());
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(Objects.requireNonNull(order.getCourier()).getId()).isEqualTo(newCourierUid);

        var newCourier = userRepository.findByUid(newCourierUid).orElseThrow();
        placeHistoryHelper.startPlaceHistoryCollection();

        acceptService.acceptOrder(
                new OrderScRequest(order.getId(), order.getExternalId(), newCourier));

        placeHistoryHelper.validateThatNRecordsWithUserCollected(order.getPlaceCount());
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void updateCourierForShippedReturnOrder() {
        long newCourierUid = 321L;
        ScOrder order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().makeReturn().get();

        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    5 * order.getPlaceCount() // stage changes
                            + 1 * order.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(5 * order.getPlaceCount());
        }
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateCourier(
                order.getId(), new CourierDto(newCourierUid, "Другой курьер", null, null, null, null, null, false),
                testFactory.getOrCreateAnyUser(sortingCenter)
        );

        // курьер не относится к mutable state, поэтому не оставляем записей потомкам
        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(Objects.requireNonNull(order.getCourier()).getId()).isEqualTo(newCourierUid);

        var newCourier = userRepository.findByUid(
                testFactory.storedUser(sortingCenter, newCourierUid).getUid()).orElseThrow();

        // Так как заказ стал "многоместным", то принимаем коробку
        PlaceScRequest placeScRequest = testFactory.placeScRequest(order, user);
        placeHistoryHelper.startPlaceHistoryCollection();

        acceptService.acceptPlace(placeScRequest);
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);

        // Проверяем
        Place place = testFactory.orderPlace(order);
        assertThat(place.getStatus()).isEqualTo(PlaceStatus.ACCEPTED);

        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void updateCourierTiFakeFromExistingCourier() {
        ScOrder order = testFactory.createOrder(sortingCenter).updateCourier(testFactory.storedCourier(123)).get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    1 * order.getPlaceCount() // stage changes
                            + 1 * order.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(order.getPlaceCount());
        }
        assertThat(order.getCourier()).isNotNull();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateCourier(
                order.getId(), new CourierDto(404L, "UNKNOWN_COURIER", null, null, null, null, null, false),
                testFactory.getOrCreateAnyUser(sortingCenter)
        );

        //курьера в коробках тоже нужно обновить
        transactionTemplate.execute(ts -> {
            testFactory.orderPlaces(order).forEach(
                    p -> placeRouteSoService.updatePlaceRoutes(
                            p, null, Instant.now(clock), user
                    )
            );
            return null;
        });

        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
        }
        var orderAfter = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderAfter.getCourier()).isNull();
    }

    @Test
    void cantDeleteCourierForShippedOrder() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().get();
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.updateCourier(
                order.getId(), new CourierDto(404L, "UNKNOWN_COURIER", null, null, null, null, null, false),
                testFactory.getOrCreateAnyUser(sortingCenter)
        )).isInstanceOf(ScInvalidTransitionException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);

    }

    @Test
    void cantDeleteCourierForShippedReturnOrder() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().makeReturn().get();
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.updateCourier(
                order.getId(), new CourierDto(404L, "UNKNOWN_COURIER", null, null, null, null, null, false),
                testFactory.getOrCreateAnyUser(sortingCenter)
        )).isInstanceOf(ScInvalidTransitionException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);

    }


    @Test
    void updateShipmentDate() {
        var orderRequest = ffOrder(sortingCenter.getToken());
        testFactory.storedDeliveryService(orderRequest.getDelivery().getDeliveryId().getYandexId(), true);
        OrderIdResponse id = orderCommandService.createOrder(new OrderCreateRequest(sortingCenter, orderRequest), user);
        assertThat(orderRepository.findByIdOrThrow(id.getId()).getShipmentDate()).isNull();
        placeHistoryHelper.startPlaceHistoryCollection();

        Order ffOrder = ffOrder("ff_create_order_shipment_date_updated.xml",
                sortingCenter.getToken());
        orderCommandService.updateShipmentDate(
                new FFApiOrderUpdateRequest(sortingCenter, ffOrder), user
        );

        //Нужно поменять дату и у коробок тоже
        ffApiPlaceService.updatePlaceRoutes(new FFApiOrderUpdateRequest(sortingCenter, ffOrder), user);

        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
        }

        var order = orderRepository.findByIdOrThrow(id.getId());
        assertThat(order.getShipmentDate()).isEqualTo(LocalDate.parse("2012-12-23"));
        assertThat(order.getIncomingRouteDate()).isEqualTo(LocalDate.parse("2012-12-23"));
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.parse("2012-12-23"));

        var place = testFactory.orderPlace(order);
        assertThat(place.getShipmentDate()).isEqualTo(LocalDate.parse("2012-12-23"));
        assertThat(place.getIncomingRouteDate()).isEqualTo(LocalDate.parse("2012-12-23"));
        assertThat(place.getOutgoingRouteDate()).isEqualTo(LocalDate.parse("2012-12-23"));
    }

    @Test
    void updateShipmentDateTime() {
        // Дублируется в FFApiPlaceServiceTest
        var orderRequest = ffOrder(sortingCenter.getToken());
        testFactory.storedDeliveryService(orderRequest.getDelivery().getDeliveryId().getYandexId(), true);
        OrderIdResponse id = orderCommandService.createOrder(new OrderCreateRequest(sortingCenter, orderRequest), user);
        assertThat(orderRepository.findByIdOrThrow(id.getId()).getShipmentDate()).isNull();
        assertThat(orderRepository.findByIdOrThrow(id.getId()).getShipmentDateTime()).isNull();
        placeHistoryHelper.startPlaceHistoryCollection();

        FFApiOrderUpdateRequest request = new FFApiOrderUpdateRequest(sortingCenter, ffOrder(
                "ff_create_order_shipment_date_updated.xml",
                sortingCenter.getToken()));
        orderCommandService.updateShipmentDateTime(request, user);
        //нужно так же обновить коробки, чтобы протестировать новый поток
        ffApiPlaceService.updatePlaceRoutes(request, user);

        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
        }


        var order = orderRepository.findByIdOrThrow(id.getId());
        assertThat(order.getShipmentDate()).isEqualTo(LocalDate.parse("2012-12-23"));
        assertThat(order.getShipmentDateTime()).isEqualTo(LocalDateTime.parse("2012-12-23T23:59:59"));
        assertThat(order.getIncomingRouteDate()).isEqualTo(LocalDate.parse("2012-12-23"));
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.parse("2012-12-23"));

        var place = testFactory.orderPlace(order);
        assertThat(place.getShipmentDate()).isEqualTo(LocalDate.parse("2012-12-23"));
        assertThat(place.getShipmentDateTime()).isEqualTo(LocalDateTime.parse("2012-12-23T23:59:59"));
        assertThat(place.getIncomingRouteDate()).isEqualTo(LocalDate.parse("2012-12-23"));
        assertThat(place.getOutgoingRouteDate()).isEqualTo(LocalDate.parse("2012-12-23"));
    }

    @Test
    void updateDeliveryDate() {
        var orderRequest = ffOrder(sortingCenter.getToken());
        testFactory.storedDeliveryService(orderRequest.getDelivery().getDeliveryId().getYandexId(), true);
        OrderIdResponse id = orderCommandService.createOrder(new OrderCreateRequest(sortingCenter, orderRequest), user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        assertThat(orderRepository.findByIdOrThrow(id.getId()).getDeliveryDate()).isNull();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateDeliveryDate(id.getId(), LocalDate.parse("2012-12-21"));

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        var order = orderRepository.findByIdOrThrow(id.getId());
        assertThat(order.getDeliveryDate()).isEqualTo(LocalDate.parse("2012-12-21"));
    }

    @Test
    void createMiddleMileOrderForDropoff() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, "true");
        var orderRequest = ffOrder(sortingCenter.getToken());
        OrderIdResponse id = orderCommandService.createOrder(
                new OrderCreateRequest(sortingCenter, orderRequest), user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        assertThat(orderRepository.findByIdOrThrow(id.getId()).isMiddleMile()).isTrue();
    }

    @Test
    void updateDeliveryDateFromAwaitingStatusDoesNotChangeIncomingRouteDate() {
        OrderLike order = testFactory.createOrder(sortingCenter).accept().keep().get();
        placeHistoryHelper.validateThatNRecordsWithUserCollected(3);

        var incomingRouteDate = order.getIncomingRouteDate();
        var newDeliveryDate = LocalDate.now(clock).plusDays(50);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateDeliveryDate(sortingCenter, order.getExternalId(), newDeliveryDate);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        assertThat(order.getIncomingRouteDate()).isEqualTo(incomingRouteDate);
    }

    @Test
    void updateDeliveryDateAfterAccept() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var date = LocalDate.now(clock).plusDays(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateDeliveryDate(order.getId(), date);

        //при обновлении даты история коробки не должна обновлятся, потому что дата не в mutable state
        placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getDeliveryDate()).isEqualTo(date);
    }

    @Test
    void updateDeliveryDateAfterCancel() {
        var order = testFactory.createOrder(sortingCenter).cancel().get();
        placeHistoryHelper.validateThatNRecordsWithUserCollected(2);
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.updateDeliveryDate(
                sortingCenter, order.getExternalId(), LocalDate.now(clock).plusDays(1)
        )).isInstanceOf(TplException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void updateDeliveryDateAfterReturn() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().makeReturn().get();
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.updateDeliveryDate(
                order.getId(), LocalDate.now(clock).plusDays(1)
        )).isInstanceOf(TplException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void updateDeliveryDateForDropship() {
        var order = testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()).get();
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);

        var expectedDate = LocalDate.now(clock).plusDays(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateDeliveryDate(order.getId(), expectedDate);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getDeliveryDate()).isEqualTo(expectedDate);
        assertThat(order.getIncomingRouteDate()).isNull();
        assertThat(order.getOutgoingRouteDate()).isNull();
    }

    @Test
    void updateDeliveryDateForNonDropship() {
        var order =
                testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.LAST_MILE_COURIER).build()).get();
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        var expectedDate = LocalDate.now(clock).plusDays(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateDeliveryDate(order.getId(), expectedDate);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getDeliveryDate()).isEqualTo(expectedDate);
        assertThat(order.getIncomingRouteDate()).isNull();
        assertThat(order.getOutgoingRouteDate()).isNull();
    }

    @Test
    void updateShipmentDateForNonDropship() {
        var order =
                testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.LAST_MILE_COURIER).build()).get();
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        var expectedDate = LocalDate.now(clock).plusDays(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateShipmentDate(order.getId(), expectedDate, user);
        //дату в коробках тоже нужно обновить
        transactionTemplate.execute( ts -> {
            testFactory.orderPlaces(order).forEach(
                    p -> placeRouteSoService.updatePlaceRoutes(
                            p, order.getCourier(), ScDateUtils.toNoon(expectedDate), user
                    )
            );
            return null;
        });

        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
        }

        var orderAfter = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderAfter.getShipmentDate()).isEqualTo(expectedDate);
        assertThat(orderAfter.getIncomingRouteDate()).isNotNull();
        assertThat(orderAfter.getOutgoingRouteDate()).isNotNull();
    }

    @Test
    void updateShipmentDateForDropship() {
        var order = testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()).get();
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        var expectedDate = LocalDate.now(clock).plusDays(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateShipmentDate(order.getId(), expectedDate, user);

        //дату в коробках тоже нужно обновить
        transactionTemplate.execute( ts -> {
            testFactory.orderPlaces(order).forEach(
                    p -> placeRouteSoService.updatePlaceRoutes(
                            p, order.getCourier(), ScDateUtils.toNoon(expectedDate), user
                    )
            );
            return null;
        });

        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
        }

        var orderAfter = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderAfter.getShipmentDate()).isEqualTo(expectedDate);
        assertThat(orderAfter.getIncomingRouteDate()).isNotNull();
        assertThat(orderAfter.getOutgoingRouteDate()).isNotNull();
    }

    @Test
    void updateShipmentDateDropship() {
        var order = testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build()).get();
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);

        assertThat(order.getShipmentDate()).isNull();
        var expectedDate = LocalDate.now(clock).plusDays(11);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateShipmentDate(
                order.getId(), expectedDate, user
        );

        //дату в коробках тоже нужно обновить
        transactionTemplate.execute( ts -> {
            placeRouteSoService.batchUpdateCourierAndShipmentDateOnDirectFlow(
                    testFactory.orderPlaces(order), expectedDate, order.getCourier(), user
            );
            return null;
        });

//        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
//        } else {
//            placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
//        }

        var orderAfter = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderAfter.getShipmentDate()).isEqualTo(expectedDate);
        assertThat(orderAfter.getIncomingRouteDate()).isEqualTo(expectedDate);
        assertThat(orderAfter.getOutgoingRouteDate()).isEqualTo(expectedDate);
    }

    @Test
    void updateShipmentDateNonDropship() {
        var order = testFactory.create(order(sortingCenter).build()).get();
        assertThat(order.getShipmentDate()).isNull();
        var expectedDate = LocalDate.now(clock).plusDays(11);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateShipmentDate(
                order.getId(), expectedDate, user
        );

        //дату в коробках тоже нужно обновить
        transactionTemplate.execute( ts -> {
            testFactory.orderPlaces(order).forEach(
                    p -> placeRouteSoService.updatePlaceRoutes(
                            p, order.getCourier(), ScDateUtils.toNoon(expectedDate), user
                    )
            );
            return null;
        });

        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
        }

        var orderAfter = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderAfter.getShipmentDate()).isEqualTo(expectedDate);
        assertThat(orderAfter.getIncomingRouteDate()).isEqualTo(expectedDate);
        assertThat(orderAfter.getOutgoingRouteDate()).isEqualTo(expectedDate);
    }

    @Test
    void updateShipmentDateFromAwaitingStatus() {
        OrderLike order = testFactory.create(order(sortingCenter).build())
                .accept().keep().get();
        var newShipmentDate = LocalDate.now(clock).plusDays(50);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateShipmentDate(order.getId(), newShipmentDate, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(newShipmentDate);
    }

    @Test
    void updateShipmentDateFromAwaitingStatusDoesNotChangeIncomingRouteDate() {
        OrderLike order = testFactory.create(order(sortingCenter).build())
                .accept().keep().get();
        placeHistoryHelper.validateThatNRecordsWithUserCollected(3);

        var incomingRouteDate = order.getIncomingRouteDate();
        var newShipmentDate = LocalDate.now(clock).plusDays(50);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateShipmentDate(order.getId(), newShipmentDate, user);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        assertThat(order.getIncomingRouteDate()).isEqualTo(incomingRouteDate);
    }

    @Test
    void updateShipmentDateAfterAccept() {
        var order = testFactory.create(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).shipmentDate(LocalDate.now(clock)).build()
        ).accept().get();
        placeHistoryHelper.validateThatNRecordsWithUserCollected(2);
        var date = LocalDate.now(clock).plusDays(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.updateShipmentDate(order.getId(), date, user);

        //дату в коробках тоже нужно обновить
        transactionTemplate.execute( ts -> {
                    placeRouteSoService.batchUpdateCourierAndShipmentDateOnDirectFlow(
                            testFactory.orderPlaces(order), date, order.getCourier(), user
                    );
                    return null;
        });

//        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
//        } else {
//            placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
//        }

        var orderAfter = orderRepository.findByIdOrThrow(order.getId());
        assertThat(orderAfter.getShipmentDate()).isEqualTo(date);
    }

    @Test
    void updateShipmentDateAfterCancel() {
        var order = testFactory.create(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).shipmentDate(LocalDate.now(clock)).build()
        ).cancel().get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(2);
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.updateShipmentDate(
                order.getId(), LocalDate.now(clock), user
        )).isInstanceOf(TplException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void updateShipmentDateAfterReturn() {
        var order = testFactory.create(
                order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).shipmentDate(LocalDate.now(clock)).build()
        ).accept().makeReturn().get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(3);
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.updateShipmentDate(
                order.getId(), LocalDate.now(clock), user
        )).isInstanceOf(TplException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    @Disabled // тут реально бага в route_so - нужно починить
    void updateShipmentDateForce() {
        Place place = testFactory.createOrderForToday(sortingCenter).getPlace();
        var yesterdayShipmentDate = LocalDate.now(clock).minusDays(1);

        testFactory.acceptPlace(place);
        testFactory.sortPlace(place);

        orderCommandService.updateShipmentDate(sortingCenter, place.getExternalId(), yesterdayShipmentDate, true, user);
        place = testFactory.updated(place);
        assertThat(place.getShipmentDate()).isEqualTo(yesterdayShipmentDate);
    }

    @Test
    void pileOfYesterdayReturns() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().get();
        var request = new OrderScRequest(order.getId(), order.getExternalId(), user);
        acceptService.acceptOrder(request);
        acceptService.acceptOrder(request);
        acceptService.acceptOrder(request);
        doReturn(clock.instant().plus(1, ChronoUnit.DAYS)).when(clock).instant();
        acceptService.acceptOrder(request);
        acceptService.acceptOrder(request);
        acceptService.acceptOrder(request);
        orderCommandService.updateShipmentDate(order.getId(), LocalDate.now(clock), user);
        orderCommandService.updateCourier(order.getId(), new CourierDto(999L,
                "Вася", "111-111", "Жигуль", "+71111", "Рога и копыта", null, false), testFactory.getOrCreateAnyUser(sortingCenter));
        acceptService.acceptOrder(request);
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    @Disabled // тут реально бага в route_so - нужно починить
    void realLifeArriveBeforeIncomingRouteDate() {
        var deliveryDay = LocalDate.ofInstant(clock.instant().plus(2, ChronoUnit.DAYS), clock.getZone());
        var bufferCell = testFactory.storedCell(sortingCenter, "b-1", CellType.BUFFER);
        Place place = testFactory.createOrder(sortingCenter).getPlace();
        orderCommandService.updateShipmentDate(place.getOrderId(), deliveryDay, user);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CREATED_FF);
        assertThat(place.getShipmentDate()).isEqualTo(deliveryDay);
        assertThat(place.getCourier()).isNull();
        assertThat(place.getOutgoingRouteDate()).isEqualTo(deliveryDay);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_CREATED_FF);

        testFactory.acceptPlace(place);
        testFactory.sortPlace(place, bufferCell.getId());

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place.getShipmentDate()).isEqualTo(deliveryDay);
        assertThat(place.getCourier()).isNull();
        assertThat(place.getOutgoingRouteDate()).isEqualTo(deliveryDay);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);

        doReturn(clock.instant().plus(1, ChronoUnit.DAYS)).when(clock).instant();

        testFactory.sortPlace(place, bufferCell.getId());

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place.getShipmentDate()).isEqualTo(deliveryDay);
        assertThat(place.getCourier()).isNull();
        assertThat(place.getOutgoingRouteDate()).isEqualTo(deliveryDay);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);

        orderCommandService.updateCourier(place.getOrderId(), new CourierDto(123L,
                "Вася Васечкин", null, null, null, null, null, false), user);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place.getShipmentDate()).isEqualTo(deliveryDay);
        assertThat(place.getCourier()).isNotNull();
        assertThat(place.getOutgoingRouteDate()).isEqualTo(deliveryDay);

        doReturn(clock.instant().plus(1, ChronoUnit.DAYS)).when(clock).instant();

        testFactory.sortPlace(place);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(place.getShipmentDate()).isEqualTo(deliveryDay);
        assertThat(place.getCourier()).isNotNull();
        assertThat(place.getOutgoingRouteDate()).isEqualTo(deliveryDay);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @Test
    void keepOrders() {
        Place place = testFactory.createOrder(sortingCenter)
                .accept()
                .getPlace();
        Cell cell = testFactory.storedCell(sortingCenter, "new one", CellType.BUFFER);

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), cell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place.getCell()).isEqualTo(cell);
        assertThat(place.getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(KEEPED_DIRECT).getId());
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void keepPreparedOrder() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort()
                .prepare()
                .getPlace();

        Cell cell = testFactory.storedCell(sortingCenter, "new one", CellType.BUFFER);
        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), cell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place.getCell()).isEqualTo(cell);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void keepOrdersToSameCell() {
        Place place = testFactory.createOrder(sortingCenter).accept().getPlace();
        Cell cell = testFactory.storedCell(sortingCenter, "new one", CellType.BUFFER);

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), cell.getId(), false);
        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), cell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place.getCell()).isEqualTo(cell);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void keepOrdersForTomorrowShip() {
        Place place = testFactory.createOrder(sortingCenter)
                .updateShipmentDate(LocalDate.now().plus(1, ChronoUnit.DAYS))
                .accept()
                .getPlace();
        Cell cell = testFactory.storedCell(sortingCenter, "new one", CellType.BUFFER);

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), cell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place.getCell()).isEqualTo(cell);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void cantSortToCourierTomorrowCourierRoute() {
        Cell courierCell = testFactory.storedCell(sortingCenter, "courier 1", CellType.COURIER);
        Place place = testFactory.createForToday(order(sortingCenter).build())
                .updateShipmentDate(LocalDate.now().plus(1, ChronoUnit.DAYS))
                .accept()
                .getPlace();
        assertThat(place.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);

        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), courierCell.getId(), false)
        ).isInstanceOf(ScException.class);
    }

    @Test
    void cantSortToCourierFromKeepTomorrowCourierRoute() {
        Cell courierCell = testFactory.storedCell(sortingCenter, "courier 1", CellType.COURIER);
        Place place = testFactory.createForToday(order(sortingCenter).build())
                .updateShipmentDate(LocalDate.now().plus(1, ChronoUnit.DAYS))
                .accept()
                .keep()
                .getPlace();
        assertThat(place.getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), courierCell.getId(), false)
        ).isInstanceOf(ScException.class);
    }

    @Test
    void keepOrderToAnyKeepCell() {
        Place place1 = testFactory.create(order(sortingCenter).externalId("1").build()).accept().getPlace();
        Place place2 = testFactory.create(order(sortingCenter).externalId("2").build()).accept().getPlace();
        Cell keepCell1 = testFactory.storedCell(sortingCenter, "keep 1", CellType.BUFFER);
        Cell keepCell2 = testFactory.storedCell(sortingCenter, "keep 2", CellType.BUFFER);

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place1), user), keepCell1.getId(), false);
        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place2), user), keepCell2.getId(), false);

        place1 = testFactory.updated(place1);
        place2 = testFactory.updated(place2);
        assertThat(place1.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place1.getCell()).isEqualTo(keepCell1);
        checkLastEventOfStatus(place1.getOrder(), ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place2.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place2.getCell()).isEqualTo(keepCell2);
        checkLastEventOfStatus(place2.getOrder(), ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void keepOldCancelledOrder() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        Place place = testFactory.createOrderForToday(sortingCenter).cancel().getPlace();
        Cell keepCell = testFactory.storedCell(sortingCenter, "keep 1", CellType.BUFFER);
        doReturn(clock.instant().plus(1, ChronoUnit.DAYS)).when(clock).instant();
        testFactory.storedOutgoingWarehouseRoute(LocalDate.now(clock), sortingCenter,
                place.getWarehouseReturn(), testFactory.storedCell(sortingCenter, "return-1", CellType.RETURN));

        PlaceScRequest placeScRequest = testFactory.placeScRequest(place, user);

        // заказ должен быть принят автоматически
        acceptService.acceptPlace(placeScRequest);

        // заказ тоже должен отсортироваться
        placeCommandService.sortPlace(placeScRequest, keepCell.getId(), true);

        // проверяем
        place = testFactory.updated(place);
        assertThat(place.getStatus()).isEqualTo(PlaceStatus.KEEPED);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(place.getCell()).isEqualTo(keepCell);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void cantKeepTodaysCancelledOrder() {
        var place = testFactory.createOrderForToday(sortingCenter)
                .cancel()
                .accept()
                .getPlace();
        Cell keepCell = testFactory.storedCell(sortingCenter, "keep 1", CellType.BUFFER);
        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), keepCell.getId(), false)
        ).isInstanceOf(ScException.class);
    }

    @Disabled
    @Test
    void sortReturnOrderToAnyReturnCell() {
        Place place1 = testFactory.create(order(sortingCenter, "1").build()).accept().keep().makeReturn().getPlace();
        Place place2 = testFactory.create(order(sortingCenter, "2").build()).accept().keep().makeReturn().getPlace();
        Cell returnCell1 = testFactory.storedCell(sortingCenter, "return 1", CellType.RETURN);
        Cell returnCell2 = testFactory.storedCell(sortingCenter, "return 2", CellType.RETURN);

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place1), user), returnCell1.getId(), false);
        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place2), user), returnCell2.getId(), false);

        assertThat(place1.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place1.getCell()).isEqualTo(returnCell1);
        checkLastEventOfStatus(place1.getOrder(), RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place2.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place2.getCell()).isEqualTo(returnCell2);
        checkLastEventOfStatus(place2.getOrder(), RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    void keepOrderIgnoreTodayRoute() {
        Place place = testFactory.createOrderForToday(sortingCenter).accept().sort().getPlace();
        Cell keepCell = testFactory.storedCell(sortingCenter, "keep 1", CellType.BUFFER);

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), keepCell.getId(), true);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place.getCell()).isEqualTo(keepCell);
    }

    @Test
    void cantKeepDamagedOrder() {
        Place place = testFactory.createOrder(
                CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .warehouseCanProcessDamagedOrders(true)
                        .build())
                .accept()
                .markOrderAsDamaged()
                .getPlace();
        Cell keepCell = testFactory.storedCell(sortingCenter, "keep 1", CellType.BUFFER);

        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), keepCell.getId(), false)
        ).isInstanceOf(ScException.class);
    }

    @Test
    void markAsDamagedPreparedOrder() {
        Place place =
                testFactory.createForToday(order(sortingCenter).warehouseCanProcessDamagedOrders(true).build())
                        .accept().sort().prepare().getPlace();

        assertThatThrownBy(() -> orderCommandService.markOrderAsDamaged(
                new OrderScRequest(place.getOrderId(), place.getExternalId(), user))
        ).isInstanceOf(ScInvalidTransitionException.class);
    }

    @Test
    void prepareToShipOrder() {
        Place place = testFactory.createOrderForToday(sortingCenter).accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        Cell designatedCell = testFactory.determineRouteCell(route, place);

        PlaceScRequest placeScRequest = testFactory.placeScRequest(place, user);

        Long routeId;
        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            routeId = testFactory.getRouteSo(route).getId();
        } else {
            routeId = testFactory.getRouteIdForSortableFlow(route);
        }

        preShipService.prepareToShipPlace(placeScRequest,
                routeId,
                designatedCell.getId());

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO);
        assertThat(place.getCell()).isEqualTo(designatedCell);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO);
    }

    @Test
    void prepareToShipOrderWithWrongRoute() {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        Place place1 = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .updateCourier(testFactory.storedCourier(1L)).accept().sort(cell.getId()).getPlace();
        Place place2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build())
                .updateCourier(testFactory.storedCourier(2L)).accept().sort().getPlace();
        var route2 = testFactory.findOutgoingCourierRoute(place2).orElseThrow();
        placeHistoryHelper.startPlaceHistoryCollection();

        Long route2Id;
        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            route2Id = testFactory.getRouteSo(route2).getId();
        } else {
            route2Id = route2.getId();
        }

        assertThatThrownBy(() -> preShipService.prepareToShipPlace(
                new PlaceScRequest(PlaceId.of(place1), user), route2Id, cell.getId()))
                .isInstanceOf(ScException.class)
                .hasFieldOrPropertyWithValue("code", ScErrorCode.CELL_FROM_ANOTHER_ROUTE.name());

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void prepareToShipOrderFromWrongCell() {
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        Place place = testFactory.createOrderForToday(sortingCenter).accept().sort(cell1.getId()).getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> preShipService.prepareToShipPlace(
                new PlaceScRequest(PlaceId.of(place), user), testFactory.getRouteIdForSortableFlow(route), cell2.getId()))
                .isInstanceOf(ScException.class)
                .hasFieldOrPropertyWithValue("code", ScErrorCode.PLACE_NOT_PREPARED_NOT_IN_THIS_CELL.name());

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void cantPrepareToShipNotSortedOrder() {
        Place place = testFactory.createOrderForToday(sortingCenter).accept().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        Cell designatedCell = testFactory.determineRouteCell(route, place);
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> preShipService.prepareToShipPlace(
                new PlaceScRequest(PlaceId.of(place), user),
                testFactory.getRouteIdForSortableFlow(route), designatedCell.getId()))
                .isInstanceOf(ScInvalidTransitionException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void sortOrders() {
        Place place = testFactory.createOrderForToday(sortingCenter).accept().getPlace();
        Cell designatedCell = testFactory.determineRouteCell(
                Objects.requireNonNull(testFactory.findOutgoingCourierRoute(place).orElseThrow()), place);

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), designatedCell.getId(), false);

        transactionTemplate.execute(ts -> {
            Place actualPlace = testFactory.updated(place);
            assertThat(actualPlace.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
            assertThat(actualPlace.getCell()).isEqualTo(designatedCell);
            checkLastEventOfStatus(actualPlace.getOrder(), ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
            return null;
        });
    }

    @Test
    void cantSortOrdersToSameCourierCell() {
        ScOrder order = testFactory.createOrderForToday(sortingCenter).accept().get();
        Cell designatedCell = testFactory.determineRouteCell(
                Objects.requireNonNull(testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow()), order);

        PlaceScRequest request = testFactory.placeScRequest(order, user);
        placeHistoryHelper.startPlaceHistoryCollection();

        acceptService.acceptPlace(request);

        //место было принято выше
        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
        placeHistoryHelper.startPlaceHistoryCollection();

        placeCommandService.sortPlace(request, designatedCell.getId(), false);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, request.getUser());
        assertThatThrownBy(() ->
                placeCommandService.sortPlace(request, designatedCell.getId(), false)
        )
                .isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void sortDamagedOrder() {
        Place place = testFactory.createForToday(
                CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .warehouseCanProcessDamagedOrders(true)
                        .build())
                .accept()
                .markOrderAsDamaged()
                .getPlace();

        var cell = testFactory.determineRouteCell(
                testFactory.findOutgoingRoute(place).orElseThrow(), place);

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), cell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(Objects.requireNonNull(place.getCell()).getType()).isEqualTo(CellType.RETURN);
        assertThat(place.getCell().getSubtype()).isEqualTo(CellSubType.RETURN_DAMAGED);
        checkLastEventOfStatus(place.getOrder(), RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    void sortDamagedOrderWithCell() {
        Cell damagedCell = testFactory.storedCell(sortingCenter,
                "return damaged 1",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED);
        Place place = testFactory.createForToday(
                CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .warehouseCanProcessDamagedOrders(true)
                        .build())
                .accept()
                .markOrderAsDamaged()
                .getPlace();

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), damagedCell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place.getCell()).isEqualTo(damagedCell);
        assertThat(place.getState()).isEqualTo(ScOrderState.SORTED);
        checkLastEventOfStatus(place.getOrder(), RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    void keepDroppedOrderDroppedOrdersEnabled() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DROPPED_ORDERS_ENABLED, "true");
        var cell = testFactory.storedCell(sortingCenter, "d-1", CellType.BUFFER, CellSubType.DROPPED_ORDERS);
        var place = testFactory.createOrder(sortingCenter)
                .updateShipmentDate(LocalDate.now(clock))
                .accept()
                .keep(cell.getId())
                .getPlace();
        assertThat(place.getCell()).isEqualTo(cell);
    }

    @Test
    void keepDroppedOrderDroppedOrdersDisabled() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DROPPED_ORDERS_ENABLED, "false");
        var cell = testFactory.storedCell(sortingCenter, "b-1", CellType.BUFFER, CellSubType.DEFAULT);

        var place = testFactory.createOrder(sortingCenter)
                .updateShipmentDate(LocalDate.now(clock))
                .accept()
                .keep(cell.getId()) // не вызывает изменения в коробках, старый API
                .getPlace();

        assertThat(place.getCell()).isEqualTo(cell);
    }

    /**
     * Поврежденный заказ можно отсортировать только
     * в возвратную ячейку (CellType.RETURN) подтипа поврежденных заказов (CellSubtype.RETURN_DAMAGED)
     */
    @Test
    void canSortDamagedOrderOnlyInDamagedCellSubtype() {
        Place place = testFactory.createForToday(
                CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .warehouseCanProcessDamagedOrders(true)
                        .build())
                .accept()
                .markOrderAsDamaged()
                .getPlace();
        Cell defaultReturnCell = testFactory.storedCell(sortingCenter,
                "default return",
                CellType.RETURN,
                CellSubType.DEFAULT);
        Cell clientReturnCell = testFactory.storedCell(sortingCenter,
                "client return",
                CellType.RETURN,
                CellSubType.CLIENT_RETURN);
        Cell bufferCell = testFactory.storedCell(sortingCenter,
                "buffer cell",
                CellType.BUFFER,
                CellSubType.DEFAULT);
        Cell bufferDroppedCell = testFactory.storedCell(sortingCenter,
                "dropped cell",
                CellType.BUFFER,
                CellSubType.DROPPED_ORDERS);
        Cell courierCell = testFactory.storedCell(sortingCenter,
                "courier cell",
                CellType.COURIER,
                CellSubType.DEFAULT);

        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), defaultReturnCell.getId(), false)
        ).isInstanceOf(ScException.class);
        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), clientReturnCell.getId(), false)
        ).isInstanceOf(ScException.class);
        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), bufferCell.getId(), false)
        ).isInstanceOf(ScException.class);
        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), bufferDroppedCell.getId(), false)
        ).isInstanceOf(ScException.class);
        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), courierCell.getId(), false)
        ).isInstanceOf(ScException.class);
    }

    /**
     * обычный заказ не может быть отсортирован в ячейку для поврежденных заказов
     */
    @Test
    void cantSortRegularOrderInDamagedCellSubtype() {
        Place place = testFactory.createForToday(
                CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .warehouseCanProcessDamagedOrders(true)
                        .build())
                .accept()
                .getPlace();
        Cell damagedReturnCell = testFactory.storedCell(sortingCenter,
                "damaged return",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED);

        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), damagedReturnCell.getId(), false)
        ).isInstanceOf(ScException.class);
    }

    @Test
    void sortReturnedOrdersTwice() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().accept().makeReturn().getPlace();

        Cell designatedCell = testFactory.determineRouteCell(
                Objects.requireNonNull(
                        testFactory.findOutgoingWarehouseRoute(place).orElseThrow()), place);

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), designatedCell.getId(), false);
        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), designatedCell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place.getCell()).isEqualTo(designatedCell);
        checkLastEventOfStatus(place.getOrder(), RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    void sortOrdersToWrongCell() {
        var wrongCell = testFactory.storedCell(sortingCenter, "1", CellType.BUFFER);
        Place place = testFactory.createOrderForToday(sortingCenter).accept().getPlace();

        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), wrongCell.getId(), false)
        ).isInstanceOf(ScException.class);
    }

    @Test
    void shipOrderSinglePlaceWithSinglePlaceSorted() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1").sortPlaces("p1").get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                        6
                            + 1 * order.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(6);
        }
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1")), new ScContext(user)
        ), null, order.getWarehouseReturn().getId());

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        var actualOrder = orderRepository.findByIdOrThrow(order.getId());
        var place1 = testFactory.orderPlace(order, "p1");
        var place2 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void shipHistoryItemCreateForOneOrder() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().prepare().get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    4 * order.getPlaceCount() // stage changes
                            + 1 * order.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(4);
        }
        var historyBefore = testFactory.findOrderStatusHistoryItems(order.getId());

        var finishedItem = historyBefore.stream()
                .filter(h -> ORDER_SHIPPED_TO_SO_FF.equals(h.getFfStatus()))
                .findFirst();
        assertThat(finishedItem.isPresent()).isFalse();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.shipOrder(new OrderScRequest(order.getId(), order.getExternalId(), user),
                Objects.requireNonNull(order.getCourier()).getId(), null);

        // место было отправлено
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        var historyAfter = testFactory.findOrderStatusHistoryItems(order.getId());
        assertThat(historyAfter.size()).isEqualTo(historyBefore.size() + 1);

        finishedItem = historyAfter.stream()
                .filter(h -> ORDER_SHIPPED_TO_SO_FF.equals(h.getFfStatus()))
                .findFirst();
        assertThat(finishedItem.isPresent()).isTrue();
    }

    @Test
    void shipTwoSinglePlaceOrdersWithSinglePlaceSorted() {
        var order1 = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1").sortPlaces("p1").get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    6           // stage changes
                            + 1 * order1.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order1.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(6);
        }
        placeHistoryHelper.startPlaceHistoryCollection();

        var order2 = testFactory.createForToday(
                        order(sortingCenter).places("p3", "p4").externalId("o2").build()
                )
                .cancel().acceptPlaces("p3").sortPlaces("p3").get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    6           // stage changes
                            + 1 * order1.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order1.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(6);
        }
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1"), "o2", List.of("p3")), new ScContext(user)
        ), null, order1.getWarehouseReturn().getId());


        //в запросе 2 коробки, поэтому 2 записи в истории RETURNED
        placeHistoryHelper.validateThatNRecordsWithUserCollected(2, user);

        var actualOrder1 = orderRepository.findByIdOrThrow(order1.getId());
        var place1 = testFactory.orderPlace(actualOrder1, "p1");
        var place2 = testFactory.orderPlace(actualOrder1, "p2");
        assertThat(actualOrder1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.CREATED);

        var actualOrder2 = orderRepository.findByIdOrThrow(order2.getId());
        var place3 = testFactory.orderPlace(actualOrder2, "p3");
        var place4 = testFactory.orderPlace(actualOrder2, "p4");
        assertThat(actualOrder2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(place3.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place4.getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void shipTwoOrdersWithAllPlacesSorted() {
        placeHistoryHelper.startPlaceHistoryCollection();

        var order1 = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    4 * order1.getPlaceCount() // stage changes
                            + 1 * order1.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order1.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(8);
        }
        placeHistoryHelper.startPlaceHistoryCollection();

        var order2 = testFactory.createForToday(
                        order(sortingCenter).places("p3", "p4").externalId("o2").build()
                )
                .cancel().acceptPlaces("p3", "p4").sortPlaces("p3", "p4").get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    4 * order2.getPlaceCount() // stage changes
                            + 1 * order2.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order2.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(8);
        }
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1", "p2"), "o2", List.of("p3", "p4")), new ScContext(user)
        ), null, order1.getWarehouseReturn().getId());

        // в истории 4 коробки возвращено
        placeHistoryHelper.validateThatNRecordsWithUserCollected(4, user);
        var actualOrder1 = orderRepository.findByIdOrThrow(order1.getId());
        var place1 = testFactory.orderPlace(actualOrder1, "p1");
        var place2 = testFactory.orderPlace(actualOrder1, "p2");
        assertThat(actualOrder1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.RETURNED);

        var actualOrder2 = orderRepository.findByIdOrThrow(order2.getId());
        var place3 = testFactory.orderPlace(actualOrder2, "p3");
        var place4 = testFactory.orderPlace(actualOrder2, "p4");
        assertThat(actualOrder2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(place3.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place4.getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }

    @Test
    void shipOrderSinglePlaceWithAllPlacesSorted() {
        var places = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1", "p2").sortPlaces("p1", "p2")
                .getPlaces();

        var cell = places.get("p1").getCell();

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1")), new ScContext(user)
        ), null, places.get("p1").getWarehouseReturn().getId());

        // отправили 1 коробку
        var place1 = testFactory.updated(places.get("p1"));
        var place2 = testFactory.updated(places.get("p2"));
        assertThat(place1.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place1.getCell()).isNull();

        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThat(place2.getCell()).isEqualTo(cell);
    }

    @Test
    void shipOrderSinglePlaceWithAllPlacesAccepted() {
        var places = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1", "p2").sortPlaces("p1")
                .getPlaces();

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1")), new ScContext(user)
        ), null, places.get("p1").getWarehouseReturn().getId());

        var place1 = testFactory.updated(places.get("p1"));
        var place2 = testFactory.updated(places.get("p2"));
        assertThat(place1.getOrderStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);

        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place1.getCell()).isNull();

        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(place2.getCell()).isNull();
    }

    @Test
        // MARKETTPLSC-4205
    void canShipOrderWithSinglePlaceToCourier() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    6 // stage changes
                            + 2 // FF API Update даты в create for today
                            + 2 // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(6);
        }
        placeHistoryHelper.startPlaceHistoryCollection();

        assertDoesNotThrow(() -> orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1")), new ScContext(user)
        ), Objects.requireNonNull(order.getCourier()).getId(), null));

        // 1 коробка отправлена
        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
    }

    @Test
        // MARKETTPLSC-4205
    void cantShipMultiplaceOrderIfNotAllPlacesInCell() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .acceptPlaces("p1", "p2").sortPlace("p1").get();
        assertThatThrownBy(() ->
                orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                        Map.of("o1", List.of("p1")), new ScContext(user)
                ), Objects.requireNonNull(order.getCourier()).getId(), null)
        );
    }


    @Test
    void shipOrderWithMultiplePlacesAtOnce() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    8 // stage changes
                            + 2 // FF API Update даты в create for today
                            + 2 // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(8);
        }

        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1", "p2")), new ScContext(user)
        ), null, order.getWarehouseReturn().getId());


        placeHistoryHelper.validateThatNRecordsWithUserCollected(2, user);
        var actualOrder = orderRepository.findByIdOrThrow(order.getId());
        var place1 = testFactory.orderPlace(order, "p1");
        var place2 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder.getOrderStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }

    @Test
    void shipOrderWithResorting() {
        var places = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1", "p2").sortPlaces("p1", "p2").getPlaces();
        var cell = places.get("p1").getCell();

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1")), new ScContext(user)
        ), null, places.get("p1").getWarehouseReturn().getId());

        var place1 = testFactory.updated(places.get("p1"));
        var place2 = testFactory.updated(places.get("p2"));
        assertThat(place1.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place1.getCell()).isNull();

        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThat(place2.getCell()).isEqualTo(cell);

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p2")), new ScContext(user)
        ), null, places.get("p2").getWarehouseReturn().getId());

        place1 = testFactory.updated(place1);
        place2 = testFactory.updated(place2);
        assertThat(place1.getOrderStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);

        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place1.getCell()).isNull();

        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place2.getCell()).isNull();
    }

    @Disabled("MARKETTPLSC-284")
    @Test
    void shipOrderWithMultiplePlacesOneByOne() {
        var places = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1").sortPlaces("p1").getPlaces();
        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1")), new ScContext(user)
        ), null, places.get("p1").getWarehouseReturn().getId());

        var place1 = testFactory.updated(places.get("p1"));
        var place2 = testFactory.updated(places.get("p2"));
        assertThat(place1.getOrderStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);

        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place1.getCell()).isNull();

        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.CREATED);
        assertThat(place2.getCell()).isNull();

        testFactory.acceptPlace(place2)
                .sortPlace(place2);

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p2")), new ScContext(user)
        ), null, place2.getWarehouseReturn().getId());

        place1 = testFactory.updated(place1);
        place2 = testFactory.updated(place2);
        assertThat(place1.getOrderStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);

        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place1.getCell()).isNull();

        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place2.getCell()).isNull();
    }

    @Test
    void shipOrderWithNotExistingPlace() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    4 * order.getPlaceCount() // stage changes
                            + 1 * order.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(8);
        }

        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p3")), new ScContext(user)
        ), null, order.getWarehouseReturn().getId()))
                .isInstanceOf(TplInvalidActionException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void shipOrderWithNotSortedPlace() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1", "p2").get();
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1()) {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(
                    3 * order.getPlaceCount() // stage changes
                            + 1 * order.getPlaceCount() // FF API Update даты в create for today
                            + 1 * order.getPlaceCount() // FF API Update курьера в create for today
            );
        } else {
            placeHistoryHelper.validateThatNRecordsWithUserCollected(6);
        }
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1")), new ScContext(user)
        ), null, order.getWarehouseReturn().getId()))
                .isInstanceOf(ScException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void shipOrderWithPlaceFromDifferentCell() {
        var order = testFactory.create(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .acceptPlaces("p1", "p2").keepPlaces("p1", "p2").makeReturn().get();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(8);
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1")), new ScContext(user)
        ), null, order.getWarehouseReturn().getId()))
                .isInstanceOf(ScException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void cantCancelOrderWhenPlacesSorted() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").build()
                )
                .acceptPlaces("p1")
                .sortPlaces("p1")
                .get();

        assertThatThrownBy(() -> orderCommandService.cancelOrder(order.getId(), null, false, user));
        Place p1 = testFactory.orderPlace(order, "p1");
        Place p2 = testFactory.orderPlace(order, "p2");
        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.SORTED_DIRECT);
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_DIRECT);
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void canCancelOrderWhenPlaceAccepted() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").build()
                )
                .acceptPlaces("p1")

                .get();

        orderCommandService.cancelOrder(order.getId(), null, false, user);
        Place p1 = testFactory.orderPlace(order, "p1");
        Place p2 = testFactory.orderPlace(order, "p2");
        assertThat(p1.getSortableStatus()).isEqualTo(SortableStatus.ACCEPTED_RETURN);
        assertThat(p2.getSortableStatus()).isEqualTo(SortableStatus.CANCELLED);
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void returnOrderWithPlaces() {
        var places = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .acceptPlaces("p1", "p2")
                .makeReturn()
                .getPlaces();

        Place place1 = places.get("p1");
        Place place2 = places.get("p2");
        Cell cell = place2.getCell();

        testFactory.sortPlace(place2);

        place1 = testFactory.updated(place1);
        place2 = testFactory.updated(place2);

        assertThat(place1.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(place1.getSortableStatus()).isEqualTo(ACCEPTED_RETURN);
        assertThat(place1.getCell()).isNull();

        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThat(place2.getSortableStatus()).isEqualTo(SORTED_RETURN);
        assertThat(place2.getCell()).isNotNull();
        assertThat(place2.getCell()).isNotEqualTo(cell);
    }

    @Test
    void shipOrderWithPlaceInBufferCell() {
        var places = testFactory.create(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .acceptPlaces("p1", "p2").keepPlaces("p1", "p2").makeReturn().getPlaces();
        var bufferCell = places.get("p1").getCell();

        testFactory.sortPlace(places.get("p1"));

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1")), new ScContext(user)
        ), null, places.get("p1").getWarehouseReturn().getId());

        var place1 = testFactory.updated(places.get("p1"));
        var place2 = testFactory.updated(places.get("p2"));
        assertThat(place1.getOrderStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);

        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place1.getCell()).isNull();

        assertThat(place2.getSortableStatus()).isEqualTo(ACCEPTED_RETURN);
        assertThat(place2.getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(ACCEPTED_RETURN).getId());
        assertThat(place2.getCell()).isEqualTo(bufferCell);
    }

    @Test
    void shipOrderWithAnotherOrdersPlace() {

        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        testFactory.createForToday(
                        order(sortingCenter).places("p3", "p4").externalId("o2").build()
                )
                .cancel().acceptPlaces("p3", "p4").sortPlaces("p3", "p4");
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p3")), new ScContext(user)
        ), null, order.getWarehouseReturn().getId()))
                .isInstanceOf(TplInvalidActionException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void shipMultiOrderWithoutPlaceSinglePlaceSorted() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1").sortPlaces("p1").get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of()), new ScContext(user)
        ), null, order.getWarehouseReturn().getId());

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        var actualOrder = orderRepository.findByIdOrThrow(order.getId());
        var place1 = testFactory.orderPlace(order, "p1");
        var place2 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void acceptP1SortP1AcceptP2ThenCanShipMiddleMile() {
        var order = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .places("p1", "p2").externalId("o1").build()
                )
                .get();
        placeHistoryHelper.startPlaceHistoryCollection();

        testFactory.acceptPlace(order, "p1");

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        testFactory.sortPlace(order, "p1");

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        testFactory.acceptPlace(order, "p2");

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);

        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of()), new ScContext(user)
        ), Objects.requireNonNull(order.getCourier()).getId(), null);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1, user);
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
    }

    @Test
    void cantShipAcceptedOrderMiddleMile() {
        var order = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .externalId("o1").build()
                )
                .accept().get();

        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of()), new ScContext(user)
        ), Objects.requireNonNull(order.getCourier()).getId(), null))
                .isInstanceOf(ScException.class)
                .hasMessage("Can't ship order o1 cause ORDER_IN_WRONG_STATUS with message Заказ находится в неверном " +
                        "статусе. Проверьте статус заказа. Order status ORDER_ARRIVED_TO_SO_WAREHOUSE.");

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0, user);
    }

    @Test
    void canShipSortedOrderMiddleMile() {
        var order = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .externalId("o1").build()
                )
                .accept().sort().get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of()), new ScContext(user)
        ), Objects.requireNonNull(order.getCourier()).getId(), null);

        // отправляем все коробки
        placeHistoryHelper.validateThatNRecordsWithUserCollected(order.getPlaceCount());
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    void cantShipMultiOrderSinglePlaceSorted() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .acceptPlaces("p1", "p2").sortPlaces("p1").get();

        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of()), new ScContext(user)
        ), Objects.requireNonNull(order.getCourier()).getId(), null))
                .isInstanceOf(ScException.class);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    void shipMultiOrderAllPlacesSortedWithoutPlaces() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        placeHistoryHelper.startPlaceHistoryCollection();

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of()), new ScContext(user)
        ), Objects.requireNonNull(order.getCourier()).getId(), null);

        placeHistoryHelper.validateThatNRecordsWithUserCollected(2, user);
        var actualOrder = orderRepository.findByIdOrThrow(order.getId());
        var place1 = testFactory.orderPlace(order, "p1");
        var place2 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder.getOrderStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    //todo:kir добавить  еще placeHistoryHelper

    @Test
    void shipMultiOrderAllPlacesSortedWithPlaces() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .acceptPlaces("p1", "p2")
                .sortPlaces("p1", "p2").get();
        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1", "p2")), new ScContext(user)
        ), Objects.requireNonNull(order.getCourier()).getId(), null);
        var actualOrder = orderRepository.findByIdOrThrow(order.getId());
        var place1 = testFactory.orderPlace(order, "p1");
        var place2 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder.getOrderStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void shipMultiOrderWithoutPlaceAllPlacesSorted() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of()), new ScContext(user)
        ), null, order.getWarehouseReturn().getId());
        var actualOrder = orderRepository.findByIdOrThrow(order.getId());
        var place1 = testFactory.orderPlace(order, "p1");
        var place2 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }


    @Test
    void shipMultiplaceOrderOnePlaceAlreadyReturnedNeedToShipSecond() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .cancel().acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1")), new ScContext(user)
        ), null, order.getWarehouseReturn().getId());
        var actualOrder1 = orderRepository.findByIdOrThrow(order.getId());
        var place11 = testFactory.orderPlace(order, "p1");
        var place21 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place11.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place21.getStatus()).isEqualTo(PlaceStatus.SORTED);

        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p2")), new ScContext(user)
        ), null, order.getWarehouseReturn().getId());
        var actualOrder2 = orderRepository.findByIdOrThrow(order.getId());
        var place12 = testFactory.orderPlace(order, "p1");
        var place22 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder2.getOrderStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(place12.getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(place22.getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }

    @Test
    void shipMultiplaceOrderOnePlaceAlreadyShippedNeedToShipSecond() {
        var orderBuilder = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2")
                                .dsType(DeliveryServiceType.TRANSIT).externalId("o1").build()
                )
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2");
        var order = orderBuilder.get();
        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of("p1")), new ScContext(user)
        ), Objects.requireNonNull(order.getCourier()).getId(), null);
        var actualOrder = orderRepository.findByIdOrThrow(order.getId());
        var place11 = testFactory.orderPlace(order, "p1");
        var place12 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder.getOrderStatus()).isEqualTo(ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(place11.getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(place12.getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThatThrownBy(() -> orderBuilder.cancel().get())
                .isInstanceOf(ScException.class);
    }


    @Test
    void shipMultiplaceOrderOnePlaceAlreadySortedSecond() {
        var orderBuilder = testFactory.createForToday(
                        order(sortingCenter)
                                .places("p1", "p2", "p3")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .externalId("o1")
                                .build()
                )
                .acceptPlaces("p1", "p2").sortPlaces("p1");
        var order = orderBuilder.get();
        var place11 = testFactory.orderPlace(order, "p1");
        var place12 = testFactory.orderPlace(order, "p2");
        var place13 = testFactory.orderPlace(order, "p3");
        assertThat(order.getOrderStatus()).isEqualTo(ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(place11.getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThat(place12.getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(place13.getStatus()).isEqualTo(PlaceStatus.CREATED);
        assertThatThrownBy(() -> orderBuilder.cancel().get())
                .isInstanceOf(ScException.class);
    }

    @Test
    void shipOrders() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        User user = testFactory.storedUser(sortingCenter, 1234L);
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort(user).get();
        orderCommandService.shipOrder(new OrderScRequest(order.getId(), order.getExternalId(), user),
                Objects.requireNonNull(order.getCourier()).getId(), null);

        transactionTemplate.execute(ts -> {
            var actualOrder = orderRepository.findByIdOrThrow(order.getId());
            assertThat(actualOrder.getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
            assertThat(actualOrder.getState()).isEqualTo(ScOrderState.SHIPPED);
            assertThat(actualOrder.getFfStatusHistory())
                    .filteredOn(e -> e.getFfStatus().equals(ORDER_SHIPPED_TO_SO_FF))
                    .extracting(ScOrderFFStatusHistoryItem::getDispatchPerson)
                    .containsOnly(user);
            checkLastEventOfStatus(order.getOrder(), ORDER_SHIPPED_TO_SO_FF);
            var places = placeRepository.findAllByOrderIdOrderById(actualOrder.getId());
            assertThat(places).allMatch(
                    p -> Objects.equals(
                            p.getMutableState().getStageId(),
                            StageLoader.getBySortableStatus(SHIPPED_DIRECT).getId()
                    )
            );
            return null;
        });
    }

    @Test
    void shipPreparedOrder() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().prepare().get();
        orderCommandService.shipOrder(new OrderScRequest(order.getId(), order.getExternalId(), user),
                Objects.requireNonNull(order.getCourier()).getId(), null);

        transactionTemplate.execute(ts -> {
            var actualOrder = orderRepository.findByIdOrThrow(order.getId());
            assertThat(actualOrder.getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
            assertThat(actualOrder.getState()).isEqualTo(ScOrderState.SHIPPED);
            assertThat(actualOrder.getFfStatusHistory())
                    .filteredOn(e -> e.getFfStatus().equals(ORDER_SHIPPED_TO_SO_FF))
                    .extracting(ScOrderFFStatusHistoryItem::getDispatchPerson)
                    .containsOnly(user);
            checkLastEventOfStatus(order.getOrder(), ORDER_SHIPPED_TO_SO_FF);
            var places = placeRepository.findAllByOrderIdOrderById(actualOrder.getId());
            assertThat(places).allMatch(
                    p -> Objects.equals(
                            p.getMutableState().getStageId(),
                            StageLoader.getBySortableStatus(SHIPPED_DIRECT).getId()
                    )
            );
            return null;
        });
    }

    @Test
    void shipDamagedOrderToWarehouse() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        Cell damagedCell = testFactory.storedCell(sortingCenter,
                "return damaged 1",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED);
        OrderLike order = testFactory.createForToday(
                        CreateOrderParams.builder().sortingCenter(sortingCenter)
                                .warehouseCanProcessDamagedOrders(true)
                                .build())
                .accept().sort(user).ship().accept().markOrderAsDamaged().makeReturn()
                .sort(damagedCell.getId()).get();
        orderCommandService.shipOrder(
                new OrderScRequest(order.getId(), order.getExternalId(), user),
                null, order.getWarehouseReturn().getId());
        transactionTemplate.execute(ts -> {
            var actualOrder = orderRepository.findByIdOrThrow(order.getId());
            assertThat(actualOrder.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
            assertThat(actualOrder.getState()).isEqualTo(ScOrderState.SHIPPED);
            checkLastEventOfStatus(actualOrder, ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
            var places = placeRepository.findAllByOrderIdOrderById(actualOrder.getId());
            assertThat(places).allMatch(
                    p -> Objects.equals(
                            p.getMutableState().getStageId(),
                            StageLoader.getBySortableStatus(SHIPPED_RETURN).getId()
                    )
            );
            return null;
        });
    }

    @Test
    void returnAcceptedOrders() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var incomingRouteDay = LocalDate.now(clock);
        assertThat(order.getIncomingRouteDate()).isEqualTo(incomingRouteDay);

        doReturn(clock.instant().plus(1, ChronoUnit.DAYS)).when(clock).instant();

        orderCommandService.returnOrdersByIds(List.of(order.getId()), user, false);
        transactionTemplate.execute(ts -> {
            var actualOrder = orderRepository.findByIdOrThrow(order.getId());
            assertThat(actualOrder.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
            assertThat(actualOrder.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
            assertThat(actualOrder.getIncomingRouteDate()).isEqualTo(incomingRouteDay);

            checkLastEventOfStatus(actualOrder, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
            return null;
        });
    }

    @Test
    void returnReturnedOrders() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().accept().makeReturn().getPlace();
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        orderCommandService.returnOrdersByIds(List.of(place.getOrderId()), user, false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void returnReturnedAndSortedOrder() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().getPlace();
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        checkLastEventOfStatus(place.getOrder(), RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        orderCommandService.returnOrdersByIds(List.of(place.getOrderId()), user, false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        checkLastEventOfStatus(place.getOrder(), RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    void realLifeReturnScenario() {
        var createdDate = LocalDate.now(clock);

        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort()
                .ship()
                .getPlace();
        assertThat(place.getIncomingRouteDate()).isEqualTo(createdDate);
        assertThat(place.getOutgoingRouteDate()).isEqualTo(createdDate);

        doReturn(clock.instant().plus(1, ChronoUnit.DAYS)).when(clock).instant();

        PlaceScRequest placeScequest = new PlaceScRequest(PlaceId.of(place), user);
        acceptService.acceptPlace(placeScequest);
        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(place.getIncomingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(place.getOutgoingRouteDate()).isNull();
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);

        orderCommandService.returnOrdersByIds(List.of(place.getOrderId()), user, false);
        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(place.getIncomingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(place.getIncomingRouteDate()).isEqualTo(LocalDate.now(clock));
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        var cell = testFactory.determineRouteCell(
                testFactory.findOutgoingRoute(place).orElseThrow(), place);
        placeCommandService.sortPlace(placeScequest, cell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place.getIncomingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(place.getIncomingRouteDate()).isEqualTo(LocalDate.now(clock));
        checkLastEventOfStatus(place.getOrder(), RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);

        var orderScRequest = new OrderScRequest(place.getOrderId(), null, user);
        orderCommandService.shipOrder(orderScRequest, null, place.getWarehouseReturn().getId());
        place = testFactory.updated(place);
        assertThat(place.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(place.getIncomingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(place.getIncomingRouteDate()).isEqualTo(LocalDate.now(clock));
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    @Test
    void cantRevertReturnSortedOrders() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().accept().makeReturn().sort().get();
        assertThatThrownBy(() -> orderCommandService.revertReturnOrdersByExternalIds(
                sortingCenter, List.of(order.getExternalId()), user))
                .isInstanceOf(ScException.class);
    }

    @Test
    void revertReturnAcceptedOrders() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().accept().makeReturn().get();
        orderCommandService.revertReturnOrdersByExternalIds(sortingCenter, List.of(order.getExternalId()), user);
        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void returnOrdersTwiceOrderNotOnSc() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().makeReturn().get();
        orderCommandService.returnOrdersByIds(List.of(order.getId()), user, false);
        transactionTemplate.execute(ts -> {
            var actualOrder = orderRepository.findByIdOrThrow(order.getId());
            assertThat(actualOrder.getFfStatus()).isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);
            checkLastEventOfStatus(actualOrder, ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);
            return null;
        });
    }

    @Test
    void revertReturnOrdersOrderNotOnSc() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().makeReturn().get();
        orderCommandService.revertReturnOrdersByExternalIds(
                sortingCenter, List.of(order.getExternalId()), user);
        transactionTemplate.execute(ts -> {
            var actualOrder = orderRepository.findByIdOrThrow(order.getId());
            assertThat(actualOrder.getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
            checkLastEventOfStatus(actualOrder, ORDER_SHIPPED_TO_SO_FF);
            return null;
        });

    }

    @Test
    void returnOrdersBeforeAccept() {
        OrderLike order = testFactory.createOrder(sortingCenter).get();
        orderCommandService.returnOrdersByIds(List.of(order.getId()), user, false);
        var notCanceledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(notCanceledOrder.getFfStatus()).isEqualTo(ORDER_CANCELLED_FF);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(0);
    }

    @Test
    void returnOrdersAfterCancel() {
        OrderLike order = testFactory.createOrder(sortingCenter).cancel().get();
        orderCommandService.returnOrdersByIds(List.of(order.getId()), user, false);
        var notCanceledOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(notCanceledOrder.getFfStatus()).isEqualTo(ORDER_CANCELLED_FF);
        var operations = postponedOperationRepository.findAll();
        assertThat(operations).hasSize(0);
    }

    @Test
    void acceptAndSortOrder() {
        Place place = testFactory.createOrder(sortingCenter).getPlace();
        testFactory.updateForTodayDelivery(place.getOrder());
        place = testFactory.updated(place);

        Route route = testFactory.findOutgoingRoute(place).orElseThrow();
        Cell cell = testFactory.determineRouteCell(route, place);

        PlaceScRequest request = new PlaceScRequest(PlaceId.of(place), user);
        acceptService.acceptPlace(request);
        placeCommandService.sortPlace(request, cell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(place.getState()).isEqualTo(ScOrderState.SORTED);
        checkLastEventOfStatus(place.getOrder(), ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

        assertThat(place.getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(SORTED_DIRECT).getId());
    }

    @Test
    void acceptAndSortOrderFromReturn() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort()
                .ship()
                .makeReturn()
                .accept()
                .getPlace();
        long designatedCellId = testFactory.determineRouteCell(
                Objects.requireNonNull(testFactory.findOutgoingWarehouseRoute(place).orElseThrow()), place).getId();
        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), designatedCellId, false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place.getState()).isEqualTo(ScOrderState.SORTED);
        checkLastEventOfStatus(place.getOrder(), RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place.getMutableState().getStageId())
                .isEqualTo(StageLoader.getBySortableStatus(SORTED_RETURN).getId());
    }

    @Test
    void acceptAndSortOrderFromCourierWithoutReturn() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .sort()
                .ship()
                .makeReturn()
                .getPlace();

        PlaceScRequest placeScRequest = testFactory.placeScRequest(place, user);
        acceptService.acceptPlace(placeScRequest); // заказ должен быть принят автоматически

        place = testFactory.updated(place);
        Route route = testFactory.findOutgoingRoute(place).orElseThrow();
        Cell cell = testFactory.determineRouteCell(route, place);

        placeCommandService.sortPlace(placeScRequest, cell.getId(), false); // заказ тоже должен отсортироваться

        // проверяем
        place = testFactory.updated(place);
        assertThat(place.getSortableStatus()).isEqualTo(SORTED_RETURN);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place.getState()).isEqualTo(ScOrderState.SORTED);
        checkLastEventOfStatus(place.getOrder(), RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    void acceptAndSortOrderFromScReturn() {
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .makeReturn()
                .accept()
                .getPlace();
        long designatedCellId = testFactory.determineRouteCell(
                Objects.requireNonNull(testFactory.findOutgoingWarehouseRoute(place).orElseThrow()), place).getId();

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), designatedCellId, false);

        place = testFactory.updated(place);
        assertThat(place.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(place.getState()).isEqualTo(ScOrderState.SORTED);
        checkLastEventOfStatus(place.getOrder(), RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @Test
    void returnToBufferCell() {
        Cell bufferCell = testFactory.storedCell(sortingCenter, "buffer-1", CellType.BUFFER);
        Place place = testFactory.createOrderForToday(sortingCenter)
                .accept()
                .makeReturn()
                .getPlace();
        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), bufferCell.getId(), false)
        ).isInstanceOf(ScException.class);
    }

    @Test
    @DisplayName("Даем класть клиентские возвраты в ячейку хранения")
    void clientReturnToBufferCell() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "true");
        testFactory.setWarehouseProperty(
                ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId(),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS, "true");
        testFactory.storedFakeReturnDeliveryService();
        var bufferCell = testFactory.storedCell(sortingCenter, "BR-1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";
        var place = testFactory.createClientReturnForToday(
                        sortingCenter.getId(),
                        sortingCenter.getToken(),
                        sortingCenter.getYandexId(),
                        courierDto,
                        clientReturnLocker
                )
                .accept().getPlace();

        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place), user), bufferCell.getId(), false);

        place = testFactory.updated(place);
        assertThat(place.getCell()).isEqualTo(bufferCell);
    }


    private void checkLastEventOfStatus(ScOrder order, ScOrderFFStatus ffStatus) {
        transactionTemplate.execute(ts -> {
            var actualOrder = orderRepository.findByIdOrThrow(order.getId());
            var historyItemsWithStatus = actualOrder.getFfStatusHistory().stream()
                    .filter(i -> i.getFfStatus() == ffStatus)
                    .toList();
            var lastHistoryItemWithStatus = historyItemsWithStatus.get(historyItemsWithStatus.size() - 1);
            assertThat(lastHistoryItemWithStatus.getCourier()).isEqualTo(actualOrder.getCourier());
            return null;
        });
    }

    @Test
    void markOrderAsDamagedWhenItPossible() {
        OrderLike order = testFactory.createOrder(
                CreateOrderParams.builder().sortingCenter(sortingCenter)
                        .warehouseCanProcessDamagedOrders(true)
                        .build()
        ).accept().get();
        orderCommandService.markOrderAsDamaged(new OrderScRequest(order.getId(), order.getExternalId(), user));
        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.isDamaged()).isTrue();
    }

    @Test
    void markOrderAsDamagedFailsWhenOrderIsNotOnSc() {
        OrderLike order1 = testFactory.createOrder(
                CreateOrderParams.builder().sortingCenter(sortingCenter)
                        .warehouseCanProcessDamagedOrders(true)
                        .externalId("1")
                        .build()
        ).get();
        assertThatThrownBy(() -> orderCommandService.markOrderAsDamaged(
                new OrderScRequest(order1.getId(), order1.getExternalId(), user)))
                .isInstanceOf(ScException.class);

        OrderLike order2 = testFactory.createForToday(
                CreateOrderParams.builder().sortingCenter(sortingCenter)
                        .warehouseCanProcessDamagedOrders(true)
                        .externalId("2")
                        .build()
        ).cancel().accept().sort().ship().get();
        assertThatThrownBy(() -> orderCommandService.markOrderAsDamaged(
                new OrderScRequest(order2.getId(), order2.getExternalId(), user)))
                .isInstanceOf(ScException.class);
    }

    @Test
    void sortReturnOrderToAnyDesignatedCell() {
        testFactory.storedWarehouse("wh1");
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN, "wh1");
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN, "wh1");
        var place1 = testFactory.create(order(sortingCenter).externalId("o1").warehouseReturnId("wh1").build())
                .cancel().accept().sort(cell1.getId()).getPlace();
        var place2 = testFactory.create(order(sortingCenter).externalId("o2").warehouseReturnId("wh1").build())
                .cancel().accept().sort(cell2.getId()).getPlace();
        assertThat(place1.getCell()).isEqualTo(cell1);
        assertThat(place2.getCell()).isEqualTo(cell2);
    }

    @Test
    void cantSortReturnOrderToNotDesignatedCell() {
        testFactory.storedWarehouse("wh1");
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN);
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN);
        testFactory.createForToday(order(sortingCenter).externalId("o1").warehouseReturnId("wh1").build())
                .cancel()
                .accept()
                .sort(cell1.getId());
        var place2 = testFactory.createForToday(order(sortingCenter).externalId("o2").warehouseReturnId("wh1").build())
                .cancel()
                .accept()
                .getPlace();

        assertThatThrownBy(() ->
                placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place2), user), cell2.getId(), false)
        ).isInstanceOf(ScException.class);
    }

    @Test
    void revertMarkOrderAsDamaged() {
        OrderLike scOrder = testFactory.createOrder(
                CreateOrderParams.builder()
                        .sortingCenter(sortingCenter)
                        .warehouseCanProcessDamagedOrders(true)
                        .build()
        ).accept().markOrderAsDamaged().get();

        orderCommandService.revertMarkOrderAsDamaged(
                new OrderScRequest(scOrder.getId(), scOrder.getExternalId(), user));
        var order = orderRepository.findByIdOrThrow(scOrder.getId());
        assertThat(order.getOrderStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(order.isDamaged()).isFalse();
    }

    @Test
    void createClientReturn() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";

        var cargoUnitId = "1";
        var segmentUuid = "1123-12asdf-sadf-3213";
        orderCommandService.createClientReturn(new CreateClientReturnRequest(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker,
                LocalDate.now(clock),
                null,
                null,
                segmentUuid,
                null,
                cargoUnitId
        ), user);
        var clientReturnLockerOrder = orderRepository.findBySortingCenterAndExternalId(
                sortingCenter, clientReturnLocker
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getOrderStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
        assertThat(Objects.requireNonNull(clientReturnLockerOrder.getCourier()).getId()).isEqualTo(courierId);
        assertThat(clientReturnLockerOrder.getSegmentUid()).isEqualTo(segmentUuid);
        assertThat(clientReturnLockerOrder.getCargoUnitId()).isEqualTo(cargoUnitId);
    }

    @Test
    void createClientReturnForSender() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        var clientReturnLocker = "VOZVRAT_TAR_1234";
        var cargoUnitId = "1";
        var segmentUuid = "1123-12asdf-sadf-3213";
        var sender = SenderDto.builder()
                .yandexId("logPointId-1")
                .partnerId("partnerId-1")
                .incorporation("seller")
                .location(LocationDto.builder()
                        .country("Россия")
                        .region("Москва и Московская область")
                        .locality("Котельники")
                        .build())
                .build();

        orderCommandService.createClientReturn(
                new CreateClientReturnRequest(
                        sortingCenter.getId(),
                        sortingCenter.getToken(),
                        sortingCenter.getYandexId(),
                        courierDto,
                        clientReturnLocker,
                        LocalDate.now(clock),
                        null,
                        sender,
                        segmentUuid,
                        null,
                        cargoUnitId
                ), user);
        var clientReturnLockerOrder = orderRepository.findBySortingCenterAndExternalId(
                sortingCenter, clientReturnLocker
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getOrderStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
        assertThat(Objects.requireNonNull(clientReturnLockerOrder.getCourier()).getId()).isEqualTo(courierId);
        assertThat(clientReturnLockerOrder.getCargoUnitId()).isEqualTo(cargoUnitId);
        assertThat(clientReturnLockerOrder.getSegmentUid()).isEqualTo(segmentUuid);
    }

    @Test
    void createClientReturnTar() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FBS_TAR.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_TAR_1234";

        orderCommandService.createClientReturn(new CreateClientReturnRequest(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker,
                LocalDate.now(clock),
                null,
                null,
                null,
                null,
                null
        ), user);
        var clientReturnLockerOrder = orderRepository.findBySortingCenterAndExternalId(
                sortingCenter, clientReturnLocker
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getOrderStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
        assertThat(Objects.requireNonNull(clientReturnLockerOrder.getCourier()).getId()).isEqualTo(courierId);
    }

    @Test
    void createClientReturnFbs() {
        testFactory.storedWarehouse(FBS_RETURN_WAREHOUSE_ID);
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_FBS_" + FBS_RETURN_WAREHOUSE_ID + "_1234";

        orderCommandService.createClientReturn(new CreateClientReturnRequest(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker,
                LocalDate.now(clock),
                null,
                null,
                null,
                null,
                null
        ), user);
        var clientReturnLockerOrder = orderRepository.findBySortingCenterAndExternalId(
                sortingCenter, clientReturnLocker
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getOrderStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
        assertThat(Objects.requireNonNull(clientReturnLockerOrder.getCourier()).getId()).isEqualTo(courierId);
    }

    @Test
    void checkExactlyOneFbsPrefix() {
        assertThat(ClientReturnBarcodePrefix.CLIENT_RETURN_FBS.getPrefixes()).hasSize(1);
    }

    @Test
    void createClientReturnFbsWithUnknownWarehouseId() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FBS_TAR.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возвратами", null);
        String clientReturnLocker = "VOZVRAT_FBS_" + FBS_RETURN_WAREHOUSE_ID + "_1234";

        orderCommandService.createClientReturn(new CreateClientReturnRequest(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker,
                LocalDate.now(clock),
                null,
                null,
                null,
                null,
                null
        ), user);
        var clientReturnLockerOrder = orderRepository.findBySortingCenterAndExternalId(
                sortingCenter, clientReturnLocker
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getOrderStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
        assertThat(Objects.requireNonNull(clientReturnLockerOrder.getCourier()).getId()).isEqualTo(courierId);
    }

    @Test
    void createClientReturnWithWarehouse() {
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_TAR_1234";

        String returnWarehouseYandexId = "sc-1";
        var cargoUnitId = "1";
        var segmentUuid = "1123-12asdf-sadf-3213";
        orderCommandService.createClientReturn(new CreateClientReturnRequest(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker,
                LocalDate.now(clock),
                new Warehouse.WarehouseBuilder(
                        new ResourceId.ResourceIdBuilder().setYandexId(returnWarehouseYandexId)
                                .setPartnerId("sc").build(),
                        new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники").build(),
                        List.of(),
                        "SC"
                ).build(),
                null,
                segmentUuid,
                null,
                cargoUnitId
        ), user);
        var clientReturnLockerOrder = orderRepository.findBySortingCenterAndExternalId(
                sortingCenter, clientReturnLocker
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
        assertThat(Objects.requireNonNull(clientReturnLockerOrder.getCourier()).getId()).isEqualTo(courierId);
        assertThat(clientReturnLockerOrder.getWarehouseReturn().getYandexId()).isEqualTo(returnWarehouseYandexId);
        assertThat(clientReturnLockerOrder.getWarehouseFrom().getYandexId()).isEqualTo(returnWarehouseYandexId);
        assertThat(clientReturnLockerOrder.getCargoUnitId()).isEqualTo(cargoUnitId);
        assertThat(clientReturnLockerOrder.getSegmentUid()).isEqualTo(segmentUuid);
    }

    @Test
    @DisplayName("success создание клиентского возврата со складом от куда приехал заказ")
    void successCreateClientReturnWithWarehouseFrom() {
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_TAR_1234";

        String returnWarehouseYandexId = "sc-1";
        String fromWarehouseYandexId = "sc-2";
        var cargoUnitId = "1";
        var segmentUuid = "1123-12asdf-sadf-3213";
        orderCommandService.createClientReturn(new CreateClientReturnRequest(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker,
                LocalDate.now(clock),
                new Warehouse.WarehouseBuilder(
                        new ResourceId.ResourceIdBuilder().setYandexId(returnWarehouseYandexId).setPartnerId("sc").build(),
                        new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники").build(),
                        List.of(),
                        "SC"
                ).build(),
                null,
                segmentUuid,
                new Warehouse.WarehouseBuilder(
                        new ResourceId.ResourceIdBuilder().setYandexId(fromWarehouseYandexId).setPartnerId("sc-2").build(),
                        new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники").build(),
                        List.of(),
                        "SC"
                ).build(),
                cargoUnitId
        ), user);
        var clientReturnLockerOrder = orderRepository.findBySortingCenterAndExternalId(
                sortingCenter, clientReturnLocker
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getOrderStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
        assertThat(Objects.requireNonNull(clientReturnLockerOrder.getCourier()).getId()).isEqualTo(courierId);
        assertThat(clientReturnLockerOrder.getWarehouseReturn().getYandexId()).isEqualTo(returnWarehouseYandexId);
        assertThat(clientReturnLockerOrder.getWarehouseFrom().getYandexId()).isEqualTo(fromWarehouseYandexId);
        assertThat(clientReturnLockerOrder.getCargoUnitId()).isEqualTo(cargoUnitId);
        assertThat(clientReturnLockerOrder.getSegmentUid()).isEqualTo(segmentUuid);
    }

    @Test
    void createClientReturnByToken() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";

        orderCommandService.createClientReturn(new CreateClientReturnRequest(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker,
                LocalDate.now(clock),
                null,
                null,
                null,
                null,
                null
        ), user);
        var clientReturnLockerOrder = orderRepository.findBySortingCenterAndExternalId(
                sortingCenter, clientReturnLocker
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getOrderStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
        assertThat(Objects.requireNonNull(clientReturnLockerOrder.getCourier()).getId()).isEqualTo(courierId);
    }

    @Test
    void createClientReturnAlreadyExists() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";

        orderCommandService.createClientReturn(new CreateClientReturnRequest(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker,
                LocalDate.now(clock),
                null,
                null,
                null,
                null,
                null
        ), user);
        assertThatCode(() -> orderCommandService.createClientReturn(new CreateClientReturnRequest(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker,
                LocalDate.now(clock),
                null,
                null,
                null,
                null,
                null
        ), user))
                .doesNotThrowAnyException();
    }

    @Test
    void createClientReturnByTokenButSeveralSortingCentersSuit() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .id(12345L)
                .token(sortingCenter.getToken())
                .build());

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";

        assertThatThrownBy(() -> orderCommandService.createClientReturn(new CreateClientReturnRequest(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                null,
                courierDto,
                clientReturnLocker,
                LocalDate.now(clock),
                null,
                null,
                null,
                null,
                null
        ), user))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void revertReturnShippedOrder() {
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().get();
        orderCommandService.revertReturnOrdersByExternalIds(sortingCenter, List.of(order.getExternalId()), user);
        assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    void revertReturnOrderOnSc() {
        var order = testFactory.createOrderForToday(sortingCenter)
                .accept().makeReturn().get();
        orderCommandService.revertReturnOrdersByExternalIds(sortingCenter, List.of(order.getExternalId()), user);
        assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void revertReturnDamagedOrder() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");
        var order = testFactory.createForToday(order(sortingCenter).build())
                .accept().sort().ship().markOrderAsDamaged().get();
        orderCommandService.revertReturnOrdersByExternalIds(sortingCenter, List.of(order.getExternalId()), user);
        assertThat(testFactory.getOrder(order.getId()).isDamaged()).isFalse();
    }

    // TODO: enable
    @Disabled
    @Test
    void markAsDamagedScDoesNotSupportDamaged() {
        var order = testFactory.createForToday(order(sortingCenter).build()).accept().get();
        orderCommandService.markOrderAsDamaged(new OrderScRequest(order.getId(), null, user));
        var actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isTrue();
        assertThat(actualOrder.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    // TODO: enable
    @Disabled
    @Test
    void tryMarkAsDamagedScDoesNotSupportDamaged() {
        var order = testFactory.createForToday(order(sortingCenter).build()).accept().get();
        orderCommandService.markOrderAsDamaged(order.getId(), false, user);
        var actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isTrue();
        assertThat(actualOrder.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void canNotMarkAsDamagedCreatedOrder() {
        enableDamagedOrdersOnSc();
        var order = testFactory.createForToday(order(sortingCenter).build()).get();
        var actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isFalse();
        assertThat(actualOrder.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_CREATED_FF);
        assertThrows(
                ScInvalidTransitionException.class,
                () -> orderCommandService.markOrderAsDamaged(order.getId(), false, user)
        );
    }

    private void enableDamagedOrdersOnSc() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");
    }

    @Test
    void markDamagedAcceptedOnScOrder() {
        enableDamagedOrdersOnSc();
        var order = testFactory.createForToday(order(sortingCenter).build()).accept().get();
        orderCommandService.markOrderAsDamaged(order.getId(), false, user);
        var actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isTrue();
        assertThat(actualOrder.getFfStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void markDamagedShippedOrder() {
        enableDamagedOrdersOnSc();
        var order = testFactory.createForToday(order(sortingCenter).build()).accept().sort().ship().get();
        orderCommandService.markOrderAsDamaged(order.getId(), false, user);
        var actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isTrue();
        assertThat(actualOrder.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
    }

    @Test
    void checkMarkDamagedIsIdempotent() {
        enableDamagedOrdersOnSc();
        var order = testFactory.createForToday(order(sortingCenter).build()).accept().get();
        orderCommandService.markOrderAsDamaged(order.getId(), false, user);
        var actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isTrue();
        assertThat(actualOrder.getFfStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
        orderCommandService.markOrderAsDamaged(order.getId(), false, user);
        actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isTrue();
        assertThat(actualOrder.getFfStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void canNotRevertDamageMarkForOrderThatAlreadyShipped() {
        enableDamagedOrdersOnSc();
        var order = testFactory.createForToday(order(sortingCenter).build())
                .accept().markOrderAsDamaged().sort().ship().get();
        assertThat(order.isDamaged()).isTrue();
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
        assertThrows(
                ScInvalidTransitionException.class,
                () -> orderCommandService.revertMarkOrderAsDamaged(order.getId(), false, user)
        );
    }

    @Test
    void revertMarkAsDamagedAcceptedOnScOrder() {
        enableDamagedOrdersOnSc();
        var order = testFactory.createForToday(order(sortingCenter).build()).accept().get();
        orderCommandService.markOrderAsDamaged(order.getId(), false, user);
        var actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isTrue();
        orderCommandService.revertMarkOrderAsDamaged(order.getId(), false, user);
        actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isFalse();
        assertThat(actualOrder.getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void checkRevertMarkDamagedIsIdempotent() {
        enableDamagedOrdersOnSc();
        var order = testFactory.createForToday(order(sortingCenter).build()).accept().get();
        orderCommandService.markOrderAsDamaged(order.getId(), false, user);
        var actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isTrue();
        orderCommandService.revertMarkOrderAsDamaged(order.getId(), false, user);
        actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isFalse();
        assertThat(actualOrder.getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
        orderCommandService.revertMarkOrderAsDamaged(order.getId(), false, user);
        actualOrder = testFactory.getOrder(order.getId());
        assertThat(actualOrder.isDamaged()).isFalse();
        assertThat(actualOrder.getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
    }


    @Test
    void canNotMarkAsDamagedOrderThatAlreadyReturned() {
        enableDamagedOrdersOnSc();
        var order = testFactory.createForToday(order(sortingCenter).build())
                .accept().cancel().sort().ship().get();
        assertThatThrownBy(() -> orderCommandService.markOrderAsDamaged(
                new OrderScRequest(order.getId(), null, user)
        ))
                .isInstanceOf(ScInvalidTransitionException.class);
    }

    @Test
    void revertMarkAsDamagedOrderOnSc() {
        enableDamagedOrdersOnSc();
        var order = testFactory.createForToday(order(sortingCenter).build()).accept().get();
        orderCommandService.markOrderAsDamaged(new OrderScRequest(order.getId(), null, user));
        order = testFactory.getOrder(order.getId());
        assertTrue(order.isDamaged());
        orderCommandService.revertMarkOrderAsDamaged(new OrderScRequest(order.getId(), null, user));
        order = testFactory.getOrder(order.getId());
        assertFalse(order.isDamaged());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void damagedOrderSortWarehouseSupportsDamaged() {
        var cell = testFactory.storedCell(sortingCenter, "rd-1",
                CellType.RETURN, CellSubType.RETURN_DAMAGED);
        var place = testFactory.create(order(sortingCenter).warehouseCanProcessDamagedOrders(true).build())
                .accept().markOrderAsDamaged().sort(cell.getId()).getPlace();
        assertThat(place.getCell()).isEqualTo(cell);
    }

    @Test
    void damagedOrderSortWarehouseDoesNotSupportDamaged() {
        var cell = testFactory.storedCell(sortingCenter, "r-1", CellType.RETURN, CellSubType.DEFAULT);
        var place = testFactory.create(order(sortingCenter).warehouseCanProcessDamagedOrders(false).build())
                .accept().markOrderAsDamaged().sort(cell.getId()).getPlace();
        assertThat(place.getCell()).isEqualTo(cell);
    }

    @Test
    void keepOrderWithDisabledCellDistributionRoute() {
        var cell = testFactory.storedCell(sortingCenter, "b-1", CellType.BUFFER);

        var place1 = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().getPlace();
        var place2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build())
                .accept().getPlace();

        testFactory.shipOrderRouteAndDisableCellDistribution(place1);
        placeCommandService.sortPlace(new PlaceScRequest(PlaceId.of(place2), user), cell.getId(), false);

        assertThat(testFactory.updated(place2).getFfStatus()).isEqualTo(ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void nullifyLotAfterSinglePlaceOrderReturn() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }

        var cell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        var order = testFactory.createOrder(sortingCenter)
                .cancel().accept().sort().sortToLot().get();
        assertThat(testFactory.orderPlace(order).getParent()).isNotNull();
        orderCommandService.shipOrders(
                OrdersScRequest.of(List.of(new RequestOrderId(order.getId())), new ScContext(user)),
                null,
                order.getWarehouseReturn().getId()
        );
        assertThat(testFactory.orderPlace(order).getParent()).isNull();
    }

    @Test
    void nullifyLotAfterPartialMultiPlaceOrderReturn() {
        var cell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        var order = testFactory.create(order(sortingCenter).places("p1", "p2").build())
                .cancel().acceptPlaces("p1").sortPlaces("p1")
                .sortPlaceToLot("SC_LOT_1", SortableType.PALLET, "p1").get();
        assertThat(testFactory.orderPlace(order, "p1").getLot()).isNotNull();
        assertThat(testFactory.orderPlace(order, "p2").getLot()).isNull();
        orderCommandService.shipOrders(
                OrdersScRequest.of(List.of(new RequestOrderId(order.getId())), new ScContext(user)),
                null,
                order.getWarehouseReturn().getId()
        );
        assertThat(testFactory.orderPlace(order, "p1").getLot()).isNull();
        assertThat(testFactory.orderPlace(order, "p2").getLot()).isNull();
    }

    @Test
    void nullifyLotAfterFullMultiPlaceOrderReturn() {
        var cell = testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        var order = testFactory.create(order(sortingCenter).places("p1", "p2").build())
                .cancel().acceptPlaces("p1", "p2").sortPlaces("p1", "p2")
                .sortPlaceToLot("SC_LOT_1", SortableType.PALLET, "p1", "p2").get();
        assertThat(testFactory.orderPlace(order, "p1").getLot()).isNotNull();
        assertThat(testFactory.orderPlace(order, "p2").getLot()).isNotNull();
        orderCommandService.shipOrders(
                OrdersScRequest.of(List.of(new RequestOrderId(order.getId())), new ScContext(user)),
                null,
                order.getWarehouseReturn().getId()
        );
        assertThat(testFactory.orderPlace(order, "p1").getLot()).isNull();
        assertThat(testFactory.orderPlace(order, "p2").getLot()).isNull();
    }

    @Test
    void copyUnexpectedReturnOrder() {
        var misdeliverySc = testFactory.storedSortingCenter(9L);
        var warehouseReturn = testFactory.storedWarehouse("1");
        var o1 = testFactory.createForToday(order(sortingCenter, "o1").build()).accept().sort().ship().makeReturn().get();
        OrderLike copyOrder =
                orderCommandService.copyUnexpectedReturnOrder(misdeliverySc, "o1", warehouseReturn, user)
                        .orElseThrow();

        assertThat(copyOrder).isNotNull();
        assertThat(copyOrder.getExternalId()).isEqualTo(o1.getExternalId());
        assertThat(copyOrder.getSortingCenter()).isEqualTo(misdeliverySc);
        assertThat(copyOrder.getFfStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(copyOrder.getFfStatusHistory().stream().map(ScOrderFFStatusHistoryItem::getFfStatus).toList())
                .contains(ScOrderFFStatus.ORDER_CREATED_FF, SO_GOT_INFO_ABOUT_PLANNED_RETURN);

        var barcodes = sortableBarcodeRepository.findAllForPlacesHavingAllBarcodes(Set.of("o1"), misdeliverySc.getId(), null);
        assertThat(barcodes).isNotEmpty(); // проверяем, что баркоды создались на правильном СЦ


    }

    @Test
    void whenCopyUnexpectedReturnOrderCheckPlaceHistoryUser() {

        var misdeliverySc = testFactory.storedSortingCenter(9L);
        var warehouseReturn = testFactory.storedWarehouse("1");
        User otherUser = testFactory.storedUser(sortingCenter, 123123);
        var o1 = testFactory.createForToday(order(sortingCenter, "o1").build()).accept().sort().ship().makeReturn().get();

        placeHistoryHelper.startPlaceHistoryCollection();
        OrderLike copyOrder =
                orderCommandService.copyUnexpectedReturnOrder(misdeliverySc, "o1", warehouseReturn,
                        otherUser
                ).orElseThrow();

        placeHistoryHelper.validateThatNRecordsWithUserCollected(routeSoEnabled() ? 2 : 1, otherUser);

    }

    @Test
    @DisplayName("fail нельзя удалить заказ который уже находится на сц")
    void failDeleteOrderBySegmentUid() {
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
        var segmentUuid0 = "segment_uuid_0";
        String cargoUnitId0 = "cargoUnitId_0";

        var orderIdForDelete = createClientReturn(cargoUnitId0, segmentUuid0);
        placeHistoryHelper.startPlaceHistoryCollection();

        testFactory.accept(orderRepository.findByIdOrThrow(orderIdForDelete.getId()));
        // Тут мы принимаем коробки заказа также, поэтому ожидаем 1 запись в истории

        placeHistoryHelper.validateThatNRecordsWithUserCollected(1);
        placeHistoryHelper.startPlaceHistoryCollection();

        assertThatThrownBy(() -> orderCommandService.deleteSegmentUUI(DeletedSegmentRequest.builder()
                .cargoUnitId(cargoUnitId0)
                .segmentUuid(segmentUuid0)
                .build()))
                .isInstanceOf(TplIllegalStateException.class)
                .hasMessage("Can't delete segment. Place already on sc");

        placeHistoryHelper.validateThatNRecordsWithUserCollected(0);
    }

    @Test
    @DisplayName("success не кидаем ошибку если не нашли заказ для удаления, считаем что уже удален")
    void successResponseDeleteOrderBySegmentUuid() {
        orderCommandService.deleteSegmentUUI(DeletedSegmentRequest.builder()
                .cargoUnitId("not-existing-cargo-unit-id")
                .segmentUuid("not-existing-segment-uuid")
                .build());
    }

    @Test
    @DisplayName("success удаление заказа по segmentUid")
    void successDeleteOrderBySegmentUid() {
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);

        var segmentUuid0 = "segment_uuid_0";
        String cargoUnitId0 = "cargoUnitId_0";

        var orderIdForDelete = createClientReturn(cargoUnitId0, segmentUuid0);

        orderCommandService.deleteSegmentUUI(DeletedSegmentRequest.builder()
                .cargoUnitId(cargoUnitId0)
                .segmentUuid(segmentUuid0)
                .build());

        var places = testFactory.orderPlaces(orderIdForDelete.getId());
        assertThat(places).hasSize(1);
        var place = places.get(0);
        assertThat(place.getSegmentUid()).isNull();
        assertThat(place.getStatus()).isEqualTo(PlaceStatus.DELETED);
        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.DELETED);

        var historyList = testFactory.findPlaceHistory(place.getId());
        var historyItem = historyList.stream()
                .filter(h -> h.getMutableState().getSortableStatus().equals(SortableStatus.DELETED))
                .findFirst();

        assertThat(historyItem.isPresent()).isTrue();
        assertThat(historyItem.get().getMutableState().getSegmentUid()).isNull();
    }

    private final LocationDto MOCK_WAREHOUSE_LOCATION = LocationDto.builder()
            .country("Россия")
            .region("Москва и Московская область")
            .locality("Котельники")
            .build();

    private OrderIdResponse createClientReturn(String cargoUnitId, String segmentUuid) {
        var fromWarehouse = WarehouseDto.builder()
                .type(WarehouseType.SORTING_CENTER.name())
                .yandexId("123123")
                .logisticPointId("123123")
                .incorporation("ООО фром мерчант")
                .location(MOCK_WAREHOUSE_LOCATION)
                .build();
        var returnWarehouse = WarehouseDto.builder()
                .type(WarehouseType.SORTING_CENTER.name())
                .yandexId("222222")
                .logisticPointId("123123")
                .location(MOCK_WAREHOUSE_LOCATION)
                .incorporation("ООО ретурн мерчант")
                .build();
        return orderCommandService.createReturn(CreateReturnRequest.builder()
                        .sortingCenter(sortingCenter)
                        .orderBarcode(cargoUnitId + "_" + segmentUuid)
                        .returnDate(LocalDate.now())
                        .returnWarehouse(returnWarehouse)
                        .fromWarehouse(fromWarehouse)
                        .segmentUuid(segmentUuid)
                        .cargoUnitId(cargoUnitId)
                        .timeIn(Instant.now(clock))
                        .timeOut(Instant.now(clock))
                        .orderReturnType(OrderReturnType.CLIENT_RETURN)
                        .assessedCost(new BigDecimal(10_000))
                        .build()
                , user);
    }

    @Test
    void shipOrdersWithContextRoute() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        var route = testFactory.findOutgoingCourierRoute(order).orElseThrow();

        boolean sortRouteSo = testFactory.sortWithRouteSo();

        orderCommandService.shipOrdersWithContextRoute(OrdersScRequest.ofExternalIds(
                        Map.of("o1", List.of("p1", "p2")), new ScContext(user)
                ),
                sortRouteSo ? testFactory.getRouteSo(route) : route, false);

        var actualOrder = orderRepository.findByIdOrThrow(order.getId());
        var place1 = testFactory.orderPlace(order, "p1");
        var place2 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder.getOrderStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void canNotShipOrdersWithInvalidContextRoute() {
        var order = testFactory.createForToday(
                        order(sortingCenter).places("p1", "p2").externalId("o1").build()
                )
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").get();
        var cell = testFactory.storedCell(sortingCenter);

        Route storedOutgoingCourierRoute = testFactory.storedOutgoingCourierRoute(LocalDate.now(clock).minusDays(2),
                sortingCenter,
                testFactory.storedCourier(67560),
                cell);
        storedOutgoingCourierRoute.allowReading();
        var routeId = storedOutgoingCourierRoute.getId();
        var route = testFactory.getRoute(routeId);
        storedOutgoingCourierRoute.revokeRouteReading();
        boolean sortRouteSo = testFactory.sortWithRouteSo();

        route.revokeRouteReading();
        assertThrows(
                ScException.class,
                () -> orderCommandService.shipOrdersWithContextRoute(OrdersScRequest.ofExternalIds(
                        Map.of("o1", List.of("p1", "p2")), new ScContext(user)
                ),
                sortRouteSo ? testFactory.getRouteSo(route) : route,
                false)
        );
    }

    @Test
    void shipOrdersWithContextRouteWhenOrdersRescheduledForNewDayRoute() {
        var deliveryService = testFactory.storedDeliveryService("123124345");
        testFactory.storedCourier(345345, Long.parseLong(deliveryService.getYandexId()));
        testFactory.storedCell(sortingCenter);

        var order = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2")
                        .deliveryService(deliveryService)
                        .externalId("o1")
                        .build()
        ).acceptPlaces("p1", "p2").get();
        var contextRoute = testFactory.findOutgoingCourierRoute(order).orElseThrow();
        testFactory.sortPlace(order, "p1");
        testFactory.sortPlace(order, "p2");

        var newDateTime = LocalDate.now(clock).plusDays(1).atStartOfDay(clock.getZone());
        testFactory.setupMockClock(clock, newDateTime.toInstant());
        orderCommandService.rescheduleSortDateTransit(List.of(order.getId()), newDateTime.toLocalDate(), sortingCenter,
                user);
        placeRouteSoService.rescheduleSortDateTransit(
                testFactory.orderPlaces(order).stream().map(Place::getId).toList(),
                newDateTime.toInstant(),
                user);

        var newDateOrdersRoute = testFactory.findOutgoingCourierRoute(order).orElseThrow();

        newDateOrdersRoute.allowReading();
        contextRoute.allowReading();
        assertThat(newDateOrdersRoute.getSortingCenter()).isEqualTo(contextRoute.getSortingCenter());
        assertThat(newDateOrdersRoute.getCourierTo()).isEqualTo(contextRoute.getCourierTo());
        assertThat(newDateOrdersRoute.getExpectedDate()).isEqualTo(contextRoute.getExpectedDate().plusDays(1));
        newDateOrdersRoute.revokeRouteReading();
        contextRoute.revokeRouteReading();

        boolean sortRouteSo = testFactory.sortWithRouteSo();

        orderCommandService.shipOrdersWithContextRoute(
                OrdersScRequest.ofExternalIds(
                    Map.of("o1", List.of("p1", "p2")), new ScContext(user)
                ),
                sortRouteSo ? testFactory.getRouteSo(contextRoute) : contextRoute,
                false);

        var actualOrder = orderRepository.findByIdOrThrow(order.getId());
        var place1 = testFactory.orderPlace(order, "p1");
        var place2 = testFactory.orderPlace(order, "p2");
        assertThat(actualOrder.getOrderStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);
        assertThat(place1.getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(place2.getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    boolean routeSoEnabled() {
        return SortableFlowSwitcherExtension.useNewRouteSoStage1_2();
    }

    @Test
    @Transactional
    void successDeletePlaceBySegmentUid() {
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);

        var segmentUuid0 = "segment_uuid_0";
        var cargoUnitId0 = "cargoUnitId_0";
        var partnerId1 = "partnerId1";
        var partnerId2 = "partnerId2";

        var scOrder = testFactory.createForToday(order(sortingCenter).places(partnerId1, partnerId2)
                .createTwoPlaces(true).build()).accept().get();

        var placeO = placeRepository.findByOrderIdAndMainPartnerCode(scOrder.getId(), partnerId1);
        assertThat(placeO).isNotNull();

        var place = placeO.get();
        PlaceMutableState state = place.getMutableState();
        state.setSegmentUid(segmentUuid0);
        state.setSortableStatus(SortableStatus.AWAITING_RETURN);
        place.setMutableState(state, user, Instant.now(clock));
        place.setCargoUnitId(cargoUnitId0);

        orderCommandService.deleteSegmentUUI(DeletedSegmentRequest.builder()
                .cargoUnitId(cargoUnitId0)
                .segmentUuid(segmentUuid0)
                .build());

        var places = testFactory.orderPlaces(scOrder);
        assertThat(places).hasSize(2);

        var place1 = placeRepository.findByOrderIdAndMainPartnerCode(scOrder.getId(), partnerId1);
        assertThat(place1.get().getSortableStatus()).isEqualTo(SortableStatus.DELETED);
        var historyList = testFactory.findPlaceHistory(place.getId());
        var historyItem1 = historyList.stream()
                .filter(h -> h.getMutableState().getSortableStatus().equals(SortableStatus.DELETED))
                .findFirst();
        assertThat(historyItem1.isPresent()).isTrue();

        var place2 = placeRepository.findByOrderIdAndMainPartnerCode(scOrder.getId(), partnerId2);
        assertThat(place2.get().getSortableStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
    }
}
