package ru.yandex.market.sc.api.features;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.place.PlaceRouteSoService;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.exception.ScWrongCellSortException;
import ru.yandex.market.sc.core.util.ScDateUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * Процесс работы с обезличенной ячейкой
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ImpermanentCellFlowTest extends BaseApiControllerTest {

    private final OrderCommandService orderCommandService;
    private final PlaceRouteSoService placeRouteSoService;
    private final PlaceRepository placeRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    User user;
    SortingCenter sortingCenter;
    TestControllerCaller caller;

    Cell bufferCell;
    Cell impermanenceCell;
    Cell returnCell;

    Warehouse warehouseIvanov;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, UID);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IMPERMANENT_ENABLED, true);

        caller = TestControllerCaller.createCaller(mockMvc);
        warehouseIvanov = testFactory.storedWarehouse("merchant-ivanov-id", WarehouseType.SHOP);
        bufferCell = testFactory.storedCell(
                sortingCenter, "b-1", CellType.BUFFER, CellSubType.BUFFER_RETURNS, warehouseIvanov.getYandexId());
        impermanenceCell = testFactory.storedCell(
                sortingCenter, "i-1", CellType.RETURN, CellSubType.IMPERMANENT, warehouseIvanov.getYandexId());
        returnCell = testFactory.storedCell(
                sortingCenter, "r-1", CellType.RETURN, CellSubType.DEFAULT, warehouseIvanov.getYandexId());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    @DisplayName("Сортировка заказов мерчанта из ячейки АХ в обезличенную ячейку")
    void sortMerchantOrderFromBufferReturnCellToImpermanenceCell(int count) {
        IntStream.range(0, count).forEach(i -> {
            var place = createAndSortReturnOrderToBufferReturnCell("o" + i, bufferCell);
            assertThat(place.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
            assertThat(place.getCell()).isEqualTo(bufferCell);

            place = sortPlaceToImpermanenceCellSilently(place, impermanenceCell);
            assertThat(place.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
            assertThat(place.getCell()).isEqualTo(impermanenceCell);
        });
        assertThat(testFactory.findPlacesInCell(bufferCell)).hasSize(0);
        assertThat(testFactory.findPlacesInCell(impermanenceCell)).hasSize(count);
    }

    @Test
    @DisplayName("Ошибка при сортировке в обезличенную ячейку привязанную к другому мерчанту")
    void sortMerchantOrderFromBufferReturnCellToImpermanenceCellAnotherMerchant() {
        var placeIvanov = createAndSortReturnOrderToBufferReturnCell("o1", bufferCell);
        placeIvanov = sortPlaceToImpermanenceCellSilently(placeIvanov, impermanenceCell);
        assertThat(placeIvanov.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(placeIvanov.getCell()).isEqualTo(impermanenceCell);

        var merchantPetrov = testFactory.storedWarehouse("merchant-petrov-id", WarehouseType.SHOP);
        var bufferCellPetrov = testFactory.storedCell(
                sortingCenter, "b-2", CellType.BUFFER, CellSubType.BUFFER_RETURNS, merchantPetrov.getYandexId());
        var placePetrov = createAndSortReturnOrderToBufferReturnCell("o2", bufferCellPetrov);
        assertThatThrownBy(() -> sortPlaceToImpermanenceCell(placePetrov, impermanenceCell))
                .isInstanceOf(ScException.class)
                .hasMessage(ScErrorCode.IMPERMANENT_CELL_ALREADY_ASSIGNED_ON_MERCHANT.getMessage());
    }

    @Disabled
    @Test
    @DisplayName("Сортировка заказа не из возвратной ячейки в обезличенную ячейку")
    void sortOrderFromNotBufferReturnCellToImpermanenceCell() {
        var merchantPetrov = testFactory.storedWarehouse("merchant-petrov-id", WarehouseType.SHOP);
        var placePetrov = testFactory.createForToday(
                        order(sortingCenter, "o1")
                                .warehouseReturnId(merchantPetrov.getYandexId())
                                .build())
                .accept().sort().ship()
                .makeReturn().accept().sort()
                .getPlace();

        assertThatThrownBy(() -> sortPlaceToImpermanenceCell(placePetrov, impermanenceCell))
                .isInstanceOf(ScWrongCellSortException.class)
                .hasMessage(ScErrorCode.CELL_FROM_ANOTHER_ROUTE.getMessage());
    }

    @SneakyThrows
    @Test
    @DisplayName("Сортировка многоместного заказа в обезличенную ячейку")
    void sortMultiplaceOrderFromBufferReturnCellToImpermanentCell() {
        var order = createAndSortMultiplaceOrderToBufferReturns("o1");

        // Отсортировал в обезличенную ячейку
        SortableSortRequestDto request1 = new SortableSortRequestDto(
                order.getExternalId(),
                "o1-1",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request1)
                .andExpect(status().isOk());
        SortableSortRequestDto request2 = new SortableSortRequestDto(
                order.getExternalId(),
                "o1-2",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request2)
                .andExpect(status().isOk());

        order = testFactory.getOrder(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(testFactory.orderPlace(order, "o1-1").getCell()).isEqualTo(impermanenceCell);
        assertThat(testFactory.orderPlace(order, "o1-2").getCell()).isEqualTo(impermanenceCell);
    }

    @SneakyThrows
    @Test
    @DisplayName("Сортировка многоместного заказа в разные об. ячейки")
    void sortMultiplaceOrderDifferentImpermanentCells() {
        var order = createAndSortMultiplaceOrderToBufferReturns("o2");

        var impermanenceCell2 = testFactory.storedCell(
                sortingCenter, "i-2", CellType.RETURN, CellSubType.IMPERMANENT, warehouseIvanov.getYandexId());

        SortableSortRequestDto request1 = new SortableSortRequestDto(
                order.getExternalId(),
                "o2-1",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request1)
                .andExpect(status().isOk());

        SortableSortRequestDto request2 = new SortableSortRequestDto(
                order.getExternalId(),
                "o2-2",
                String.valueOf(impermanenceCell2.getId()));
        caller.sortableBetaSort(request2)
                .andExpect(status().isOk());

        order = testFactory.getOrder(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(testFactory.orderPlace(order, "o2-1").getCell()).isEqualTo(impermanenceCell);
        assertThat(testFactory.orderPlace(order, "o2-2").getCell()).isEqualTo(impermanenceCell2);
    }

    @SneakyThrows
    @Test
    @DisplayName("[Сортировка] Одна посылка в возвратной ячейке, другая в обезличенной ячейке")
    void sortPlacesToReturnAndImpermanentCells() {
        var order = createAndSortMultiplaceOrderToBufferReturns("o3");

        SortableSortRequestDto request1 = new SortableSortRequestDto(
                order.getExternalId(),
                "o3-1",
                String.valueOf(returnCell.getId()));
        caller.sortableBetaSort(request1)
                .andExpect(status().isOk());

        SortableSortRequestDto request = new SortableSortRequestDto(
                order.getExternalId(),
                "o3-2",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().isOk());

        order = testFactory.getOrder(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(testFactory.orderPlace(order, "o3-1").getCell()).isEqualTo(returnCell);
        assertThat(testFactory.orderPlace(order, "o3-2").getCell()).isEqualTo(impermanenceCell);
    }

    @SneakyThrows
    @Test
    @DisplayName("Отгрузка многоместного заказа (2 посылки)")
    void shipMultiplaceOrderFromImpermanentCell() {
        var order = createAndSortMultiplaceOrderToBufferReturns("o4");

        SortableSortRequestDto request1 = new SortableSortRequestDto(
                order.getExternalId(),
                "o4-1",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request1)
                .andExpect(status().isOk());

        SortableSortRequestDto request2 = new SortableSortRequestDto(
                order.getExternalId(),
                "o4-2",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request2)
                .andExpect(status().isOk());

        var route = testFactory.findOutgoingRoute(order).orElseThrow();

        caller.ship(testFactory.getRouteIdForSortableFlow(route), FinishRouteRequestDto.builder().cellId(impermanenceCell.getId()).build())
                .andExpect(status().isOk());

        order = testFactory.getOrder(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(testFactory.orderPlace(order, "o4-1").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "o4-2").getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }

    @SneakyThrows
    @Test
    @DisplayName("Отгрузка раздельно многоместного заказа из об. ячейки")
    void shipSplitMultiplaceOrderFromImpermanentCell() {
        var order = createAndSortMultiplaceOrderToBufferReturns("o4");

        SortableSortRequestDto request1 = new SortableSortRequestDto(
                order.getExternalId(),
                "o4-1",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request1)
                .andExpect(status().isOk());


        var route = testFactory.findOutgoingRoute(order).orElseThrow();
        caller.ship(testFactory.getRouteIdForSortableFlow(route), FinishRouteRequestDto.builder().cellId(impermanenceCell.getId()).build())
                .andExpect(status().is4xxClientError());

        order = testFactory.getOrder(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(testFactory.orderPlace(order, "o4-1").getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThat(testFactory.orderPlace(order, "o4-2").getStatus()).isEqualTo(PlaceStatus.KEEPED);

        SortableSortRequestDto request2 = new SortableSortRequestDto(
                order.getExternalId(),
                "o4-2",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request2)
                .andExpect(status().isOk());

        route = testFactory.findOutgoingRoute(order).orElseThrow();
        caller.ship(testFactory.getRouteIdForSortableFlow(route), FinishRouteRequestDto.builder().cellId(impermanenceCell.getId()).build())
                .andExpect(status().isOk());

        order = testFactory.getOrder(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(testFactory.orderPlace(order, "o4-1").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "o4-2").getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }

    @SneakyThrows
    @Test
    @DisplayName("Отгрузка по одной посылке из обезличенной ячейки")
    void shipMultiplaceOrderSequantialByPlace() {
        var order = createAndSortMultiplaceOrderToBufferReturns("o4");

        SortableSortRequestDto request1 = new SortableSortRequestDto(
                order.getExternalId(),
                "o4-1",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request1)
                .andExpect(status().isOk());
        Long routableId = testFactory.getRouteIdForSortableFlow(
                testFactory.findOutgoingRoute(order).orElseThrow().getId()
        );
        caller.ship(routableId,
                        FinishRouteRequestDto.builder().cellId(impermanenceCell.getId()).build())
                .andExpect(status().is4xxClientError());

        order = testFactory.getOrder(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(testFactory.orderPlace(order, "o4-1").getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThat(testFactory.orderPlace(order, "o4-2").getStatus()).isEqualTo(PlaceStatus.KEEPED);

        SortableSortRequestDto request2 = new SortableSortRequestDto(
                order.getExternalId(),
                "o4-2",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request2)
                .andExpect(status().isOk());

        caller.ship(routableId,
                        FinishRouteRequestDto.builder().cellId(impermanenceCell.getId()).build())
                .andExpect(status().isOk());

        order = testFactory.getOrder(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(testFactory.orderPlace(order, "o4-1").getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(testFactory.orderPlace(order, "o4-2").getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }

    @SneakyThrows
    @Test
    @DisplayName("Отгрузка многоместного заказа из разных обезличенных ячеек")
    void shipMultiplaceOrderFromDifferentImpermanentCells() {
        var order = createAndSortMultiplaceOrderToBufferReturns("o4");
        var impermanenceCell2 = testFactory.storedCell(
                sortingCenter, "i-2", CellType.RETURN, CellSubType.IMPERMANENT, warehouseIvanov.getYandexId());

        SortableSortRequestDto request1 = new SortableSortRequestDto(
                order.getExternalId(),
                "o4-1",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request1)
                .andExpect(status().isOk());

        SortableSortRequestDto request2 = new SortableSortRequestDto(
                order.getExternalId(),
                "o4-2",
                String.valueOf(impermanenceCell2.getId()));
        caller.sortableBetaSort(request2)
                .andExpect(status().isOk());

        caller.ship(testFactory.findOutgoingRoute(order).orElseThrow().getId(),
                        FinishRouteRequestDto.builder().cellId(impermanenceCell.getId()).build())
                .andExpect(status().is4xxClientError());
        caller.ship(testFactory.findOutgoingRoute(order).orElseThrow().getId(),
                        FinishRouteRequestDto.builder().cellId(impermanenceCell2.getId()).build())
                .andExpect(status().is4xxClientError());

        order = testFactory.getOrder(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(testFactory.orderPlace(order, "o4-1").getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThat(testFactory.orderPlace(order, "o4-2").getStatus()).isEqualTo(PlaceStatus.SORTED);
    }

    @SneakyThrows
    @Test
    @DisplayName("Отгрузка одной посылки из возвратной ячейки, другой из об. ячейки")
    void shipMultiplaceOrderFromReturnCellAndImpermanentCell() {
        var order = createAndSortMultiplaceOrderToBufferReturns("o4");

        SortableSortRequestDto request1 = new SortableSortRequestDto(
                order.getExternalId(),
                "o4-1",
                String.valueOf(returnCell.getId()));
        caller.sortableBetaSort(request1)
                .andExpect(status().isOk());

        SortableSortRequestDto request2 = new SortableSortRequestDto(
                order.getExternalId(),
                "o4-2",
                String.valueOf(impermanenceCell.getId()));
        caller.sortableBetaSort(request2)
                .andExpect(status().isOk());

        caller.ship(testFactory.findOutgoingRoute(order).orElseThrow().getId(),
                        FinishRouteRequestDto.builder().cellId(returnCell.getId()).build())
                .andExpect(status().is4xxClientError());
        caller.ship(testFactory.findOutgoingRoute(order).orElseThrow().getId(),
                        FinishRouteRequestDto.builder().cellId(impermanenceCell.getId()).build())
                .andExpect(status().is4xxClientError());

        order = testFactory.getOrder(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(testFactory.orderPlace(order, "o4-1").getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThat(testFactory.orderPlace(order, "o4-2").getStatus()).isEqualTo(PlaceStatus.SORTED);
    }

    @SneakyThrows
    private ScOrder createAndSortMultiplaceOrderToBufferReturns(String orderExternalId) {
        var order = testFactory.createForToday(
                        order(sortingCenter, orderExternalId)
                                .places(orderExternalId + "-1", orderExternalId + "-2")
                                .warehouseReturnId(bufferCell.getWarehouseYandexId())
                                .build())
                .acceptPlaces()
                .sortPlaces()
                .ship()
                .makeReturn()
                .get();

        caller.acceptReturn(new AcceptOrderRequestDto(orderExternalId, orderExternalId + "-1"))
                .andExpect(status().isOk());
        caller.acceptReturn(new AcceptOrderRequestDto(orderExternalId, orderExternalId + "-2"))
                .andExpect(status().isOk());

        // Отсортировал в буферную ячейку
        SortableSortRequestDto request1 = new SortableSortRequestDto(
                orderExternalId,
                orderExternalId + "-1",
                String.valueOf(bufferCell.getId()));
        caller.sortableBetaSort(request1)
                .andExpect(status().isOk());

        SortableSortRequestDto request2 = new SortableSortRequestDto(
                orderExternalId,
                orderExternalId + "-2",
                String.valueOf(bufferCell.getId()));
        caller.sortableBetaSort(request2)
                .andExpect(status().isOk());

        return order;
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    @DisplayName("Отгрузка обезличенной ячейки (отвязываем ячейку от мерчанта и маршрута)")
    void shipRouteWithImpermanenceCell(int count) {
        IntStream.range(0, count).forEach(i -> {
            var place = createAndSortReturnOrderToBufferReturnCell("o" + i, bufferCell);
            sortPlaceToImpermanenceCellSilently(place, impermanenceCell);
        });

        var routable = testFactory.findCellActiveRoute(impermanenceCell.getId(), sortingCenter);
        var places = testFactory.findPlacesInCell(impermanenceCell);
        caller.ship(routable.getId(), FinishRouteRequestDto.builder().cellId(impermanenceCell.getId()).build());

        assertThat(testFactory.findRoutesCell(impermanenceCell)).hasSize(0);
        transactionTemplate.execute( t ->
            assertThat(testFactory.getRoutable(routable.getId(), sortingCenter).getAllRouteFinishOrders())
                .allMatch(routeFinishOrder -> routeFinishOrder.getStatus()
                        .equals(RETURNED_ORDER_DELIVERED_TO_IM))
        );
        assertThat(places)
                .map(place -> testFactory.updated(place))
                .allMatch(p -> p.getFfStatus() == RETURNED_ORDER_DELIVERED_TO_IM
                        && p.getCell() == null);
    }

    @Test
    @DisplayName("Пере-привязка обезличенной ячейки к маршруту нового дня")
    void rebindImpermanenceCellToRouteNewDay() {
        var placeIvanov = createAndSortReturnOrderToBufferReturnCell("o1", bufferCell);


        placeIvanov = sortPlaceToImpermanenceCellSilently(placeIvanov, impermanenceCell);
        var oldRoute = testFactory.findOutgoingRoute(placeIvanov).orElseThrow();

        // перенос маршрута на новый день
        LocalDate newSortDate = LocalDate.now(clock).plusDays(1);
        orderCommandService.rescheduleSortDateReturns(
                List.of(placeIvanov.getOrderId()), newSortDate, placeIvanov.getSortingCenter(), user);

        //Нужно поменять дату и у коробок тоже
        placeRouteSoService.rescheduleSortDateTransit(List.of(placeIvanov.getId()), ScDateUtils.toNoon(newSortDate), user);


        placeIvanov = testFactory.updated(placeIvanov);
        var newRoute = testFactory.findOutgoingRoute(placeIvanov).orElseThrow();

        assertThat(placeIvanov.getOutgoingRouteDate()).isEqualTo(newSortDate);
        assertThat(newRoute).isNotEqualTo(oldRoute);
        assertThat(newRoute.getRouteCells())
                .anyMatch(rc -> rc.isReservedOnDate(newSortDate) &&
                        rc.getCell().equals(impermanenceCell));
    }

    @ParameterizedTest
    @ValueSource(ints = {3})
    @DisplayName("Отвязываем обезличенную ячейку после изъятия последнего заказа (возвратная ячейка)")
    void detachImpermanenceCellFromRouteAfterRemoveLastOrderReturnCell(int count) {
        // сортируем в обезличенную ячейку
        IntStream.range(0, count).forEach(i -> {
            var place = createAndSortReturnOrderToBufferReturnCell("o" + i, bufferCell);
            sortPlaceToImpermanenceCellSilently(place, impermanenceCell);
        });
        List<Place> places = testFactory.findPlacesInCell(impermanenceCell);
        assertThat(places).hasSize(count);

        // сортируем в возвратную ячейку
        // и отвязываем от маршрута
        var route = testFactory.findOutgoingRoute(places.get(0)).orElseThrow();
        var returnCell = StreamEx.of(route.getRouteCells()).map(RouteCell::getCell)
                .filterBy(Cell::getSubtype, CellSubType.DEFAULT)
                .findFirst().orElseThrow();
        places.forEach(place -> sortPlaceToImpermanenceCellSilently(place, returnCell));
        route = testFactory.findOutgoingRoute(places.get(0)).orElseThrow();

        assertThat(testFactory.findPlacesInCell(impermanenceCell)).hasSize(0);
        assertThat(route.getCells(LocalDate.now(clock))).noneMatch(cell -> cell.equals(impermanenceCell));
        assertThat(placeRepository.countAllByMutableStateCellAndOutgoingRouteDate(impermanenceCell, LocalDate.now(clock)))
                .isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(ints = {3})
    @DisplayName("Отвязываем обезличенную ячейку после изъятия последнего заказа (ячейка АХ)")
    void detachImpermanenceCellFromRouteAfterRemoveLastOrderBufferCell(int count) {
        // сортируем в обезличенную ячейку
        IntStream.range(0, count).forEach(i -> {
            var place = createAndSortReturnOrderToBufferReturnCell("o" + i, bufferCell);
            sortPlaceToImpermanenceCellSilently(place, impermanenceCell);
        });
        List<Place> places = testFactory.findPlacesInCell(impermanenceCell);
        assertThat(places).hasSize(count);

        // сортируем обратно в буфер
        // и отвязываем от маршрута
        var route = testFactory.findOutgoingRoute(places.get(0)).orElseThrow();
        places.forEach(order -> sortPlaceToImpermanenceCellSilently(order, bufferCell));
        route = testFactory.findOutgoingRoute(places.get(0)).orElseThrow();

        assertThat(testFactory.findPlacesInCell(impermanenceCell)).hasSize(0);
        assertThat(route.getCells(LocalDate.now(clock))).noneMatch(cell -> cell.equals(impermanenceCell));
        assertThat(placeRepository.countAllByMutableStateCellAndOutgoingRouteDate(impermanenceCell, LocalDate.now(clock)))
                .isEqualTo(0);
    }

    @SneakyThrows
    @Test
    @DisplayName("Отгрузка нескольких мерчантов через обезличенную ячейку")
    void shipSeveralMerchantImpermanenceCell() {
        var placeIvanov = createAndSortReturnOrderToBufferReturnCell("o1", bufferCell);
        placeIvanov = sortPlaceToImpermanenceCellSilently(placeIvanov, impermanenceCell);
        var routeIvanov = testFactory.findOutgoingRoute(placeIvanov).orElseThrow();

        caller.ship(routeIvanov.getId(), FinishRouteRequestDto.builder().cellId(impermanenceCell.getId()).build());
        assertThat(testFactory.findRoutesCell(impermanenceCell)).hasSize(0);

        var placePetrov = createAndSortReturnOrderToBufferReturnCell("o2", bufferCell);
        placePetrov = sortPlaceToImpermanenceCellSilently(placePetrov, impermanenceCell);
        var routePetrov = testFactory.findOutgoingRoute(placePetrov).orElseThrow();

        caller.ship(routePetrov.getId(), FinishRouteRequestDto.builder().cellId(impermanenceCell.getId()).build());
        assertThat(testFactory.findRoutesCell(impermanenceCell)).hasSize(0);
    }

    @SneakyThrows
    private Place createAndSortReturnOrderToBufferReturnCell(String externalId, Cell cell) {
        var place = testFactory.createForToday(
                        order(sortingCenter, externalId)
                                .warehouseReturnId(cell.getWarehouseYandexId())
                                .build())
                .accept().sort().ship()
                .makeReturn()
                .getPlace();

        caller.acceptReturn(new AcceptOrderRequestDto(externalId, null))
                .andExpect(status().isOk());

        caller.sortableBetaSort(new SortableSortRequestDto(place, cell))
                .andExpect(status().isOk());

        return testFactory.getPlace(place.getId());
    }

    @SneakyThrows
    private Place sortPlaceToImpermanenceCellSilently(Place place, Cell cell) {
        return sortPlaceToImpermanenceCell(place, cell);
    }

    private Place sortPlaceToImpermanenceCell(Place place, Cell cell) throws Exception {
        SortableSortRequestDto request = new SortableSortRequestDto(
                place.getExternalId(),
                place.getMainPartnerCode(),
                String.valueOf(cell.getId()));
        MvcResult result = caller.sortableBetaSort(request).andReturn();
        if (result.getResponse().getStatus() != 200) {
            throw Objects.requireNonNull(result.getResolvedException());
        }
        return testFactory.getPlace(place.getId());
    }
}
