package ru.yandex.market.sc.core.domain.task;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.route.model.RouteCategory;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.task.model.RouteTaskService;
import ru.yandex.market.sc.core.domain.task.model.TaskNextCellResponseDto;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseProperty;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RouteTaskServiceTest {
    private final RouteTaskService routeTaskService;
    private final TestFactory testFactory;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    Courier courier;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        courier = testFactory.storedCourier();
        user = testFactory.storedUser(sortingCenter, 1L, UserRole.STOCKMAN);

        testFactory.setupMockClock(clock);
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, false);
        setUpScProperty(sortingCenter);

        // мерчант
        var whShop = testFactory.storedWarehouse("whShop-1", WarehouseType.SHOP);

        // склад с включенное поддержкой сортировки через аддресное хранение
        var whWarehouse = testFactory.storedWarehouse("whWarehouse-1", WarehouseType.SORTING_CENTER);
        testFactory.setWarehouseProperty(String.valueOf(whWarehouse.getYandexId()),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS,
                "true");
    }

    private void setUpScProperty(SortingCenter sc) {
        testFactory.setSortingCenterProperty(sc, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
    }


    private static Set<String> getWarehouseReturnId() {
        return Set.of("whShop-1", "whWarehouse-1");
    }

    private List<ApiRouteTaskEntryDto> getApiOutgoingRouteTaskList(SortingCenter sortingCenter, Pageable pageable) {
        return getApiOutgoingRouteTaskList(sortingCenter, null, pageable);
    }

    private List<ApiRouteTaskEntryDto> getApiOutgoingRouteTaskList(SortingCenter sortingCenter, @Nullable String text,
                                                                   Pageable pageable) {
        return routeTaskService
                .getRouteTaskList(RouteType.OUTGOING_WAREHOUSE, RouteCategory.SHOP, text, sortingCenter, pageable)
                .get().toList();
    }

    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getApiOutgoingRouteTaskList(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").warehouseReturnId(whReturnYandexId).places("o1p1", "o1p2").build())
                .acceptPlaces("o1p1", "o1p2").cancel()
                .keepPlaces(bufferCell.getId(), "o1p1", "o1p2")
                .sortPlaces(returnCell.getId(), "o1p1", "o1p2")
                .get();

        var order2 = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell.getId(), "o2p1", "o2p2")
                .get();
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o3").warehouseReturnId(whReturnYandexId).places("o3p1", "o3p2").build())
                .acceptPlaces("o3p1").cancel()
                .keepPlaces(bufferCell.getId(), "o3p1")
                .get();

        var order5 = testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .cancel().acceptPlaces("o5p1", "o5p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();
        var route2 = testFactory.findOutgoingWarehouseRoute(order5).orElseThrow();

        assertThat(getApiOutgoingRouteTaskList(sortingCenter, Pageable.unpaged()))
                .isEqualTo(List.of(new ApiRouteTaskEntryDto(
                                testFactory.getRouteIdForSortableFlow(route),
                                Objects.requireNonNull(route.getWarehouseTo()).getShopId() + " "
                                        + Objects.requireNonNull(route.getWarehouseTo()).getIncorporation()),
                        new ApiRouteTaskEntryDto(
                                testFactory.getRouteIdForSortableFlow(route2),
                                Objects.requireNonNull(route2.getWarehouseTo()).getIncorporation()
                        )
                ));
    }

    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getApiOutgoingRouteTaskListPageable(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").warehouseReturnId(whReturnYandexId).places("o1p1", "o1p2").build())
                .acceptPlaces("o1p1", "o1p2").cancel()
                .keepPlaces(bufferCell.getId(), "o1p1", "o1p2")
                .sortPlaces(returnCell.getId(), "o1p1")
                .get();

        var order = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell.getId(), "o2p1")
                .get();

        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .cancel().acceptPlaces("o5p1", "o5p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var route2 = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();

        assertThat(getApiOutgoingRouteTaskList(sortingCenter, PageRequest.of(0, 1)))
                .isEqualTo(List.of(new ApiRouteTaskEntryDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        Objects.requireNonNull(route.getWarehouseTo()).getShopId() + " "
                                + Objects.requireNonNull(route.getWarehouseTo()).getIncorporation())
                ));
    }

    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getApiOutgoingRouteTaskListWithText(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").warehouseReturnId(whReturnYandexId).places("o1p1", "o1p2").build())
                .acceptPlaces("o1p1", "o1p2").cancel()
                .keepPlaces(bufferCell.getId(), "o1p1", "o1p2")
                .sortPlaces(returnCell.getId(), "o1p1", "o1p2")
                .get();

        var order2 = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell.getId(), "o2p1", "o2p2")
                .get();
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o3").warehouseReturnId(whReturnYandexId).places("o3p1", "o3p2").build())
                .acceptPlaces("o3p1").cancel()
                .keepPlaces(bufferCell.getId(), "o3p1")
                .get();

        var order5 = testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .cancel().acceptPlaces("o5p1", "o5p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();
        var route2 = testFactory.findOutgoingWarehouseRoute(order5).orElseThrow();

        assertThat(getApiOutgoingRouteTaskList(sortingCenter, "wh", Pageable.unpaged()))
                .isEqualTo(List.of(new ApiRouteTaskEntryDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        Objects.requireNonNull(route.getWarehouseTo()).getShopId() + " "
                                + Objects.requireNonNull(route.getWarehouseTo()).getIncorporation())
                ));
    }

    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getApiOutgoingRouteTaskListWithEmptyText(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").warehouseReturnId(whReturnYandexId).places("o1p1", "o1p2").build())
                .acceptPlaces("o1p1", "o1p2").cancel()
                .keepPlaces(bufferCell.getId(), "o1p1", "o1p2")
                .sortPlaces(returnCell.getId(), "o1p1")
                .get();

        var order = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell.getId(), "o2p1")
                .get();

        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .cancel().acceptPlaces("o5p1", "o5p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var route2 = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();

        assertThat(getApiOutgoingRouteTaskList(sortingCenter, "", Pageable.unpaged()))
                .isEqualTo(List.of(new ApiRouteTaskEntryDto(
                                testFactory.getRouteIdForSortableFlow(route),
                                Objects.requireNonNull(route.getWarehouseTo()).getShopId() + " "
                                        + Objects.requireNonNull(route.getWarehouseTo()).getIncorporation()),
                        new ApiRouteTaskEntryDto(
                                testFactory.getRouteIdForSortableFlow(route2),
                                Objects.requireNonNull(route2.getWarehouseTo()).getIncorporation()
                        )
                ));
    }

    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getApiOutgoingRouteTaskListPageableWithText(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").warehouseReturnId(whReturnYandexId).places("o1p1", "o1p2").build())
                .acceptPlaces("o1p1", "o1p2").cancel()
                .keepPlaces(bufferCell.getId(), "o1p1", "o1p2")
                .sortPlaces(returnCell.getId(), "o1p1")
                .get();

        var order = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell.getId(), "o2p1")
                .get();

        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .cancel().acceptPlaces("o5p1", "o5p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var route2 = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();

        assertThat(getApiOutgoingRouteTaskList(sortingCenter, "wh", PageRequest.of(0, 1)))
                .isEqualTo(List.of(new ApiRouteTaskEntryDto(
                        testFactory.getRouteIdForSortableFlow(route),
                        Objects.requireNonNull(route.getWarehouseTo()).getShopId() + " "
                                + Objects.requireNonNull(route.getWarehouseTo()).getIncorporation())
                ));
    }

    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getNextCellTest(String whReturnYandexId) {
        var bufferCell = testFactory
                .storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS, 1L);
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").warehouseReturnId(whReturnYandexId).places("o1p1", "o1p2").build())
                .acceptPlaces("o1p1", "o1p2").cancel()
                .keepPlaces(bufferCell.getId(), "o1p1", "o1p2")
                .sortPlaces(returnCell.getId(), "o1p1")
                .get();

        var order = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell.getId(), "o2p1")
                .get();

        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .cancel().acceptPlaces("o5p1", "o5p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var route2 = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();

        assertThat(routeTaskService.getNextCell(testFactory.getRouteIdForSortableFlow(route), user))
                .isEqualTo(new TaskNextCellResponseDto(bufferCell, 1, 1));
    }

    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getNextCellWithFinishTest(String whReturnYandexId) {
        var bufferCell = testFactory
                .storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS, 1L);
        var bufferCell2 = testFactory
                .storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS, 2L);
        var bufferCell3 = testFactory
                .storedCell(sortingCenter, "br3", CellType.BUFFER, CellSubType.BUFFER_RETURNS, 3L);

        var bufferCell4 = testFactory
                .storedCell(sortingCenter, "br4", CellType.BUFFER, CellSubType.BUFFER_RETURNS, 4L);

        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").warehouseReturnId(whReturnYandexId).places("o1p1", "o1p2").build())
                .acceptPlaces("o1p1", "o1p2").cancel()
                .keepPlaces(bufferCell.getId(), "o1p1", "o1p2")
                .sortPlaces(returnCell.getId(), "o1p1")
                .get();

        var order = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell2.getId(), "o2p1")
                .keepPlaces(bufferCell3.getId(), "o2p2")
                .get();

        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .cancel().acceptPlaces("o5p1", "o5p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var route2 = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();

        assertThat(routeTaskService.getNextCell(testFactory.getRouteIdForSortableFlow(route), user))
                .isEqualTo(new TaskNextCellResponseDto(bufferCell, 1, 3));
        routeTaskService.finishCell(testFactory.getRouteIdForSortableFlow(route), bufferCell.getId(), user);
        assertThat(routeTaskService.getNextCell(testFactory.getRouteIdForSortableFlow(route), user))
                .isEqualTo(new TaskNextCellResponseDto(bufferCell2, 2, 3));
        routeTaskService.finishCell(testFactory.getRouteIdForSortableFlow(route), bufferCell2.getId(), user);
        assertThat(routeTaskService.getNextCell(testFactory.getRouteIdForSortableFlow(route), user))
                .isEqualTo(new TaskNextCellResponseDto(bufferCell3, 3, 3));
        routeTaskService.finishCell(testFactory.getRouteIdForSortableFlow(route), bufferCell3.getId(), user);
        assertThat(routeTaskService.getNextCell(testFactory.getRouteIdForSortableFlow(route), user))
                .isEqualTo(new TaskNextCellResponseDto(null, 4, 3));
    }

    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getNextCellWithFinishAndStartAgainTest(String whReturnYandexId) {
        var bufferCell = testFactory
                .storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS, 1L);
        var bufferCell2 = testFactory
                .storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS, 2L);

        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        var order0 = testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").warehouseReturnId(whReturnYandexId).places("o1p1", "o1p2").build())
                .acceptPlaces("o1p1", "o1p2").cancel()
                .keepPlaces(bufferCell.getId(), "o1p1", "o1p2")
                .sortPlaces(returnCell.getId(), "o1p1").get();

        var order = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell2.getId(), "o2p1", "o2p2")
                .get();

        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .cancel().acceptPlaces("o5p1", "o5p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var route2 = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();

        assertThat(routeTaskService.getNextCell(testFactory.getRouteIdForSortableFlow(route), user))
                .isEqualTo(new TaskNextCellResponseDto(bufferCell, 1, 2));
        testFactory.sortPlace(order0, "o1p2");
        routeTaskService.finishCell(testFactory.getRouteIdForSortableFlow(route), bufferCell.getId(), user);
        assertThat(routeTaskService.getNextCell(testFactory.getRouteIdForSortableFlow(route), user))
                .isEqualTo(new TaskNextCellResponseDto(bufferCell2, 2, 2));
        routeTaskService.finishCell(testFactory.getRouteIdForSortableFlow(route), bufferCell2.getId(), user);
        assertThat(routeTaskService.getNextCell(testFactory.getRouteIdForSortableFlow(route), user))
                .isEqualTo(new TaskNextCellResponseDto(null, 3, 2));
        assertThat(routeTaskService.getNextCell(testFactory.getRouteIdForSortableFlow(route), user))
                .isEqualTo(new TaskNextCellResponseDto(bufferCell2, 1, 1));
    }

    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getNextCellWithFailTest(String whReturnYandexId) {
        var bufferCell = testFactory
                .storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);

        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);

        var order = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell.getId(), "o2p1", "o2p2")
                .get();

        var order2 = testFactory.createForToday(
                        order(sortingCenter).externalId("o5").places("o5p1", "o5p2").build()
                )
                .cancel().acceptPlaces("o5p1", "o5p2").get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var route2 = testFactory.findOutgoingWarehouseRoute(order2).orElseThrow();
        assertThatThrownBy(() -> routeTaskService.getNextCell(testFactory.getRouteIdForSortableFlow(route), user))
                .isExactlyInstanceOf(ScException.class);
    }

}
