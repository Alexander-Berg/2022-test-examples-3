package ru.yandex.market.sc.api.features;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.CellField;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellDto;
import ru.yandex.market.sc.core.domain.cell.model.CellCargoType;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseProperty;
import ru.yandex.market.sc.core.exception.ScErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.USE_ZONE_FOR_BUFFER_RETURN_CELLS;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * Флоу работы с ячейками адресного хранения
 * Продуктовая задача: https://st.yandex-team.ru/OPSPROJECT-2540
 *
 * @author: dbryndin, merak1t
 * @date: 9/14/21
 */
public class BufferReturnCellFlowTest extends BaseApiControllerTest {

    @MockBean
    private Clock clock;
    private SortingCenter sortingCenter;
    private Cell cell;
    private TestControllerCaller caller;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        cell = testFactory.storedActiveCell(sortingCenter);
        testFactory.storedUser(sortingCenter, UID, UserRole.STOCKMAN);
        testFactory.setupMockClock(clock);
        setUpScProperty(sortingCenter);

        caller = TestControllerCaller.createCaller(mockMvc, UID);

        // мерчант
        var whShop = testFactory.storedWarehouse("whShop-1", WarehouseType.SHOP);

        // склад с включенное поддержкой сортировки через аддресное хранение
        var whWarehouse = testFactory.storedWarehouse("whWarehouse-1", WarehouseType.SORTING_CENTER);
        testFactory.setWarehouseProperty(String.valueOf(whWarehouse.getYandexId()),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS,
                "true");
    }

    /**
     * Список настроенных warehouse, для которых работает адресное хранение. Настройка происходит в init()
     * warehouse с типом {@link WarehouseType#SHOP}
     * или
     * warehouse для которого есть проперти WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS
     */
    private static Set<String> getWarehouseReturnId() {
        return Set.of("whShop-1", "whWarehouse-1");
    }

    public void setUpScProperty(SortingCenter sc) {
        testFactory.setSortingCenterProperty(sc, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
    }


    @SneakyThrows
    @DisplayName("fail сортировка посылок возврата на возврат мимо адресного хранения")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void cantSortNotFullShopReturnToReturnCell(String whReturnYandexId) {
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        var places = List.of("p1", "p2");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .acceptPlaces(places)
                .cancel().get();
        for (String place : places) {
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(returnCell.getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is4xxClientError());
        }
    }

    @SneakyThrows
    @DisplayName("fail сортировка посылок возврата не на адресное хранение")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void cantSortNotFullShopReturnToDefaultCell(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "buf", CellType.BUFFER, CellSubType.DEFAULT);
        var places = List.of("p1", "p2");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .acceptPlaces(places)
                .cancel().get();
        for (String place : places) {
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(bufferCell.getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is4xxClientError());
        }
    }

    @SneakyThrows
    @DisplayName("success сортировка посылок возврата на адресное хранение")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void canSortNotFullShopReturnToBufferCell(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var places = List.of("p1", "p2");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .acceptPlaces(places)
                .cancel().get();
        for (String place : places) {
            // проверяем что место из заказа сортируется в ячейку хранения
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(bufferCell.getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());

            var actualPlace = testFactory.orderPlace(order, place);
            assertThat(actualPlace.getCellId()).isEqualTo(Optional.of(bufferCell.getId()));
            assertThat(actualPlace.isKeeped()).isTrue();
        }
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @SneakyThrows
    @DisplayName("success сортировка одной посылки возврата на адресное хранение")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void canSortNotFullShopReturnToBufferCellSinglePlace(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var places = List.of("p1", "p2");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .acceptPlaces(places)
                .cancel().get();
        String place = "p1";
        // проверяем что место из заказа сортируется в ячейку хранения
        SortableSortRequestDto request = new SortableSortRequestDto(
                order.getExternalId(),
                place,
                String.valueOf(bufferCell.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());

        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }


    @SneakyThrows
    @DisplayName("success сортировка посылок возврата на возврат после адресного хранения")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void canSortNotFullShopReturnToReturnFromBufferCell(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        var places = List.of("p1", "p2");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .acceptPlaces(places).cancel().keepPlaces(bufferCell.getId(), places.toArray(new String[]{}))
                .get();
        for (String place : places) {
            // проверяем что место из заказа сортируется в ячейку хранения
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(returnCell.getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());

            var actualPlace = testFactory.orderPlace(order, place);
            assertThat(actualPlace.getCellId()).isEqualTo(Optional.of(returnCell.getId()));
            assertThat(actualPlace.isSortedToShipmentCell()).isTrue();
        }
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @SneakyThrows
    @DisplayName("fail отгрузка одной посылки возврата")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void cantShipNotFullShopReturn(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        var places = List.of("p1", "p2");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .acceptPlaces(places).cancel()
                .keepPlaces()
                .keepPlaces(bufferCell.getId(), places.toArray(new String[]{}))
                .sortPlace("p1", returnCell.getId())
                .get();
        String place = "p1";
        // проверяем что место из заказа сортируется в ячейку хранения
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        caller.ship(testFactory.getRouteIdForSortableFlow(route), new FinishRouteRequestDto(
                null, null, returnCell.getId(), false, null, null
        )).andExpect(status().is4xxClientError());
        var actualPlace = testFactory.orderPlace(order, place);
        assertThat(actualPlace.getCellId()).isEqualTo(Optional.of(returnCell.getId()));
        assertThat(actualPlace.isSortedToShipmentCell()).isTrue();
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
    }

    @SneakyThrows
    @DisplayName("success отгрузка всех посылок возврата")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void canShipFullShopReturn(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        var places = List.of("p1", "p2");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .acceptPlaces(places).cancel()
                .keepPlaces(bufferCell.getId(), places.toArray(new String[]{}))
                .sortPlaces(returnCell.getId(), places.toArray(new String[]{}))
                .get();
        // проверяем что место из заказа сортируется в ячейку хранения
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        caller.ship(testFactory.getRouteIdForSortableFlow(route), new FinishRouteRequestDto(
                null, null, returnCell.getId(), false, null, null
        )).andExpect(status().is2xxSuccessful());
        for (String place : places) {
            var actualPlace = testFactory.orderPlace(order, place);
            assertThat(actualPlace.getCellId()).isEmpty();
            assertThat(actualPlace.isReturned()).isTrue();
        }
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);
    }

    /**
     * <li/>при первом сканировании возврата он должен проситься в ячейку хранения возврата
     * {@link CellSubType#BUFFER_RETURNS} если включены свойства для сц и склада
     * <li/> после того как был отсортирован в ячейку хранния, при сканировании должен проситься в ячейку возврата по
     * маршруту
     */
    @DisplayName("success сортировка одноместного возврата через адресное хранение")
    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void acceptToBufferReturnAndAfterSortToReturnsCell(String whReturnYandexId) {
        var brCell1 = testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var order = testFactory.createOrder(order(sortingCenter).warehouseReturnId(whReturnYandexId).build())
                .cancel().get();

        // проверяем что коробка должна проситься в BUFFER_RETURNS
        var acceptOrderRes0 = caller.acceptOrder(new AcceptOrderRequestDto(
                        order.getExternalId(),
                        null))
                .andExpect(status().is2xxSuccessful());
        var acceptOrderDtoResponse0 = readContentAsClass(acceptOrderRes0, ApiOrderDto.class);
        assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResponse0.getStatus());

        // сортируем в ячейку хранения
        SortableSortRequestDto request = new SortableSortRequestDto(
                order.getExternalId(),
                order.getExternalId(),
                String.valueOf(brCell1.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());

        // тут уже коробка должна проситься в ячейку возврата своего маршрута
        var acceptResponse1 = caller
                .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), null))
                .andExpect(status().is2xxSuccessful());

        var acceptOrderDtoResponse1 = readContentAsClass(acceptResponse1, ApiOrderDto.class);
        var returnCell = StreamEx
                .of(acceptOrderDtoResponse1.getAvailableCells())
                .nonNull()
                .filter(c -> c.getType() == CellType.RETURN)
                .findFirst();
        assertEquals(acceptOrderDtoResponse1.getStatus(), ApiOrderStatus.SORT_TO_WAREHOUSE);
        assertFalse(returnCell.isEmpty());

        // сортируем в ячейку возврата
        SortableSortRequestDto request1 = new SortableSortRequestDto(
                order.getExternalId(),
                order.getExternalId(),
                String.valueOf(returnCell.get().getId()));
        caller.sortableBetaSort(request1)
                .andExpect(status().is2xxSuccessful());

        // проверяем что после того как коробка попала в ячейку возврата,
        // она больше не просится в ячейку хранения
        var acceptResponse2 = caller.acceptOrder(new AcceptOrderRequestDto(
                        order.getExternalId(),
                        null))
                .andExpect(status().is2xxSuccessful());
        assertFalse(StreamEx
                .of(readContentAsClass(acceptResponse2, ApiOrderDto.class).getAvailableCells())
                .nonNull()
                .filter(c -> c.getType() == CellType.RETURN)
                .findFirst()
                .isEmpty());
    }

    /**
     * Проверям что в ответ возвращается ячейки и колличество в них заказов мерча
     */
    @SneakyThrows
    @DisplayName("success в ответе возвращается список ячеек хранения возврата где лежат заказы мерча и их колличество")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void checkOrderCountInBufferReturnCell(String whReturnYandexId) {
        var expectedCountInBrCell1 = 10;
        var expectedCountInBrCell2 = 5;

        var brCell1 = testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var brCell2 = testFactory.storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS);

        // кладем заказы мерча в brCell1
        for (int i = 0; i < expectedCountInBrCell1; i++) {
            testFactory.createOrder(order(sortingCenter, "br1-ex-" + i)
                            .warehouseReturnId(whReturnYandexId).build())
                    .accept().cancel().keep(brCell1.getId()).get();
        }

        // кладем заказы мерча в brCell2
        for (int i = 0; i < expectedCountInBrCell2; i++) {
            testFactory.createOrder(order(sortingCenter, "br2-ex-" + i)
                            .warehouseReturnId(whReturnYandexId).build())
                    .accept().cancel().keep(brCell2.getId()).get();
        }

        var order = testFactory.createOrder(order(sortingCenter).warehouseReturnId(whReturnYandexId).build())
                .cancel().get();

        // проверяем что коробка должна проситься в BUFFER_RETURNS
        var acceptOrderRes0 = caller.acceptOrder(new AcceptOrderRequestDto(
                        order.getExternalId(),
                        null))
                .andExpect(status().is2xxSuccessful());

        var acceptOrderDtoResp0 = readContentAsClass(acceptOrderRes0, ApiOrderDto.class);
        assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp0.getStatus());
        var brCell1Response = StreamEx
                .of(acceptOrderDtoResp0.getAvailableCells())
                .nonNull()
                .filter(c -> c.getNumber().equals(brCell1.getScNumber()))
                .findFirst();
        var brCell2Response = StreamEx
                .of(acceptOrderDtoResp0.getAvailableCells())
                .nonNull()
                .filter(c -> c.getNumber().equals(brCell2.getScNumber()))
                .findFirst();
        assertEquals(expectedCountInBrCell1, brCell1Response.get().getOrderCount());
        assertEquals(expectedCountInBrCell2, brCell2Response.get().getOrderCount());
    }


    /**
     * Успешный кейс когда все посылки сортируются сначала в ячейку хранения возврата потом в ячейку возврата и
     * отгружаются
     * <p>
     * <li>2 посылки сортируются в разные ячейки хранения возврата
     * <li>2 посылки сортируются в ячейку возврата
     * <li>маршрут отгружается
     */
    @SneakyThrows
    @DisplayName("success сортировка всех посылок заказа через разные ячейки хранения возврата и отгрузка")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void multiplaceFlow22(String whReturnYandexId) {
        var cell1 = testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS,
                null, null, 2L);
        var cell2 = testFactory.storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS,
                null, null, 1L);
        var place1 = "p1";
        var place2 = "p2";
        var places = List.of(place1, place2);

        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .cancel().get();

        // place1
        {
            // проверяем что место из заказа просится в ячейку хранения возвратов
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place1))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertTrue(hasCell(acceptOrderDtoResp, cell1), "buffer return cell must be not empty");
            assertThat(acceptOrderDtoResp.getAvailableCells()).extracting(ApiCellDto::getNumber)
                    .containsExactly("br2", "br1"); // Ячейки идут в порядке обхода

            // проверяем что место из заказа сортируется в ячейку хранения
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place1,
                    String.valueOf(cell1.getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }
        // place2
        {
            // проверяем что место из заказа просится в ячейку хранения возвратов
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place2))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());

            // проверяем что место из заказа сортируется в ячейку хранения
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place2,
                    String.valueOf(cell2.getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());

        }
        var curOrder = testFactory.getOrder(order.getId());
        assertEquals(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, curOrder.getFfStatus());
        // сортируем места из заказа в ячейку возврата
        for (String place : places) {
            var acceptOrderRes = caller.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var returnCell = findFirstCellByType(acceptOrderDtoResp, CellType.RETURN, CellSubType.DEFAULT);
            assertFalse(returnCell.isEmpty(), "return cell must be not empty");

            // сортируем место из заказа в ячейку возврата
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(returnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }


        // отгружаем ячейку с посылками заказа
        Place sortedPlace = testFactory.orderPlace(order, place1);
        assertEquals(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, sortedPlace.getFfStatus());
        var shipDtoReq = new FinishRouteRequestDto();
        shipDtoReq.setCellId(sortedPlace.getCellId().get());
        Long routableId = testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(sortedPlace)
                .orElseThrow()).getId();
        caller.ship(routableId, shipDtoReq)
                .andExpect(status().is2xxSuccessful());
        var shippedOrder = testFactory.getOrder(order.getId());
        assertEquals(RETURNED_ORDER_DELIVERED_TO_IM, shippedOrder.getOrderStatus());

        // проверяем что все посылки заказа отгрузились
        var shippedPlace = testFactory.orderPlaces(shippedOrder.getId());
        shippedPlace.forEach(place ->
                assertThat(place.isReturned()).isTrue()
        );
    }

    /**
     * Успешный кейс когда все посылки сортируются сначала в ячейку хранения возврата потом в ячейку возврата и
     * отгружаются
     * <p>
     * <li>3 посылки сортируются в ячейку хранения возврата
     * <li>3 посылки сортируются в ячейку возврата
     * <li>маршрут отгружается
     */
    @SneakyThrows
    @DisplayName("success сортировка всех посылок заказа через ячейку хранения возврата и отгрузка")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void multiplaceFlow0(String whReturnYandexId) {
        testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        testFactory.storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var places = List.of("p1", "p2", "p3");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .cancel().get();

        for (String place : places) {
            // проверяем что место из заказа просится в ячейку хранения возвратов
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");

            // проверяем что место из заказа сортируется в ячейку хранения
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(bufferReturnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }
        var curOrder = testFactory.getOrder(order.getId());
        assertEquals(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, curOrder.getFfStatus());
        // сортируем места из заказа в ячейку возврата
        for (String place : places) {
            var acceptOrderRes = caller.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var returnCell = findFirstCellByType(acceptOrderDtoResp, CellType.RETURN, CellSubType.DEFAULT);
            assertFalse(returnCell.isEmpty(), "return cell must be not empty");

            // сортируем место из заказа в ячейку возврата
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(returnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }


        // отгружаем ячейку с посылками заказа
        Place sortedPlace = testFactory.anyOrderPlace(order);
        assertEquals(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, sortedPlace.getFfStatus());
        var shipDtoReq = new FinishRouteRequestDto();
        shipDtoReq.setCellId(sortedPlace.getCellId().orElseThrow());
        Long routableId = testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(sortedPlace)
                .orElseThrow()).getId();
        caller.ship(routableId, shipDtoReq)
                .andExpect(status().is2xxSuccessful());
        var shippedOrder = testFactory.getOrder(order.getId());
        assertEquals(RETURNED_ORDER_DELIVERED_TO_IM, shippedOrder.getOrderStatus());

        // проверяем что все посылки заказа отгрузились
        var shippedPlace = testFactory.orderPlaces(shippedOrder.getId());
        shippedPlace.forEach(place ->
                assertThat(place.isReturned()).isTrue()
        );
    }


    /**
     * кейс когда все посылки сортируются сначала в ячейку хранения возврата потом часть в ячейку возврата и
     * попытка отгрузить
     * <p>
     * <li>3 посылки сортируются в ячейку хранения возврата
     * <li>2 посылки сортируются в ячейку возврата
     * <li>маршрут не отгружается
     */
    @SneakyThrows
    @DisplayName("success сортировка части посылок заказа через ячейку хранения возврата и отгрузка")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void multiplaceFlowWithWrongShip(String whReturnYandexId) {
        testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        testFactory.storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var places = List.of("p1", "p2", "p3");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .cancel().get();

        for (String place : places) {
            // проверяем что место из заказа просится в ячейку хранения возвратов
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");

            // проверяем что место из заказа сортируется в ячейку хранения
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(bufferReturnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }
        var curOrder = testFactory.getOrder(order.getId());
        assertEquals(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, curOrder.getFfStatus());
        // сортируем места из заказа в ячейку возврата
        for (String place : List.of("p1", "p2")) {
            var acceptOrderRes = caller.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var returnCell = findFirstCellByType(acceptOrderDtoResp, CellType.RETURN, CellSubType.DEFAULT);
            assertFalse(returnCell.isEmpty(), "return cell must be not empty");

            // сортируем место из заказа в ячейку возврата
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(returnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }


        // пытаемся отгрузить ячейку с посылками заказа
        Place sortedPlace = testFactory.anyOrderPlace(order);
        assertEquals(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, sortedPlace.getFfStatus());
        var shipDtoReq = new FinishRouteRequestDto();
        shipDtoReq.setCellId(sortedPlace.getCellId().get());
        Long routableId = testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(sortedPlace)
                .orElseThrow()).getId();
        caller.ship(routableId, shipDtoReq)
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("error").value("CANT_SHIP_PARTIALLY_ORDER"))
                .andExpect(jsonPath("message").value("Can't ship order "
                        + sortedPlace.getExternalId() + " cause CANT_SHIP_PARTIALLY_ORDER with " +
                        "message Нельзя отгрузить ячейку с неполным многоместным мерчу. " +
                        "Places [p3] not found. Please remove order from current cell or add missing places"));
        var shippedOrder = testFactory.getOrder(order.getId());
        assertEquals(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, shippedOrder.getOrderStatus());

        // проверяем что все посылки заказа не отгрузились
        var shippedPlaces = testFactory.orderPlaces(shippedOrder.getId());
        shippedPlaces.forEach(place ->
                assertThat(place.isReturned()).isFalse()
        );
    }

    @SneakyThrows
    @DisplayName("success сортировка части посылок заказа через ячейку хранения возврата и отгрузка")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void multiplaceFlow123(String whReturnYandexId) {
        testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        testFactory.storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var places = List.of("p1", "p2", "p3");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .cancel().get();

        {
            var place = places.get(0);
            // проверяем что место из заказа просится в ячейку хранения возвратов
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");

        }

        {
            var place = places.get(1);
            // проверяем что место из заказа просится в ячейку хранения возвратов
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");

            // проверяем что место из заказа сортируется в ячейку хранения
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(bufferReturnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }

        {
            var place = places.get(2);
            // проверяем что место из заказа просится в ячейку хранения возвратов
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");
            // проверяем что место из заказа сортируется в ячейку хранения
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(bufferReturnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }
        var curOrder = testFactory.getOrder(order.getId());
        // сортируем места из заказа в ячейку возврата
        {
            var place = places.get(1);
            checkNotEmptyAndOnlyBufferAvailableCellsOnAccept(order, place);
            var returnCell = testFactory.storedCell(sortingCenter, CellType.RETURN, CellSubType.DEFAULT, null);

            // сортируем место из заказа в ячейку возврата
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(returnCell.getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("error").value("ORDER_IN_WRONG_STATUS"))
                    .andExpect(jsonPath("message").value("Can't sortPlace from buffer_return cell from status " +
                            "KEEPED_RETURN"));
        }

    }

    @SneakyThrows
    @DisplayName("success сортировка части посылок заказа через ячейку хранения возврата и попытка отгрузки")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void multiplaceFlowForce(String whReturnYandexId) {
        testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        testFactory.storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var places = List.of("p1", "p2", "p3");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .cancel().get();

        {
            // первую посылку просто принимаем без сортировки
            var place = places.get(0);
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");

        }

        {
            // вторую принимаем и кладем в адресное хранение
            var place = places.get(1);
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");

            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(bufferReturnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }

        {
            // третью принимаем и кладем в адресное хранение
            var place = places.get(2);
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");

            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(bufferReturnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }

        // вот тут вот кладовщик думает что не надо искать последнюю посылку, мерч то уже приехал
        // он берет и принудительно сортирует в ячейку возврата к которой привязан маршрут
        // а мы ему не разрешаем это делать, так как посылки одного заказа могут перемещаться из АХ только все вместе
        {
            var place = places.get(1);
            var acceptOrderRes = caller.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());

            // 4L это ячейка маршрута, у тебя может быть другая
            var routeCell = testFactory.findOutgoingRoute(order).orElseThrow().getCells(order.getOutgoingRouteDate());

            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(routeCell.get(0).getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("error").value("ORDER_IN_WRONG_STATUS"))
                    .andExpect(jsonPath("message").value("Can't sortPlace from buffer_return cell from status KEEPED_RETURN"));
        }

        {
            // а вот та посылка которая завалялась
            // пикает ее
            var place = places.get(0);
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());

            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(bufferReturnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }
    }

    /**
     * Принятие первой посылки до того как заказ был отменен, после отмены первая и все остальные посылки должны идти
     * через ячейку хранения
     * <p>
     * <li> 3 посылки у заказа
     * <li> 1 посылку принимают
     * <li> отменяем заказ
     * <li> 1 посылка сканят еще раз и отправялют в ячейку хранения возвратов
     * <li> 2 остальные послыки принимают и отправляют в ячейку хранения
     * <li> все посылки сканят и отправляют в ячейку возврата
     * <li> маршрут отгружается
     */
    @SneakyThrows
    @DisplayName("success сортировка одной из посылок заказа который был отменен после того как посылку приняли")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void multiplaceFlow1(String whReturnYandexId) {
        testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        testFactory.storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS);

        // создаем еще не отмененный многоместный заказа
        var p1 = "p1";
        var p2 = "p2";
        var p3 = "p3";
        var places = List.of(p1, p2, p3);
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId)
                        .places(places)
                        .build())
                .get();

        // принимаем p1 первую посылку
        {
            caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), p1))
                    .andExpect(status().is2xxSuccessful());

            assertEquals(PlaceStatus.ACCEPTED, testFactory.orderPlaces(order.getId()).stream()
                    .filter(p -> Objects.equals(p.getYandexId(), p1))
                    .findFirst()
                    .get().getStatus());
        }

        // отменяем заказ
        testFactory.cancelOrder(order.getId());

        // при сканировании p1 первой посылки она должна просится в хранение возвратов
        // сортируем в ячейку хранения возвратов
        {
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), p1))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");

            // проверяем что место из заказа сортируется в ячейку хранения
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    p1,
                    String.valueOf(bufferReturnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }

        {
            for (String place : List.of(p2, p3)) {
                // проверяем что место из заказа просится в ячейку хранения возвратов
                var acceptOrderRes = caller
                        .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                        .andExpect(status().is2xxSuccessful());
                var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
                var bufferReturnCell = findFirstCellByType(
                        acceptOrderDtoResp,
                        CellType.BUFFER,
                        CellSubType.BUFFER_RETURNS
                );
                assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
                assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");

                // проверяем что место из заказа сортируется в ячейку хранения
                SortableSortRequestDto request = new SortableSortRequestDto(
                        order.getExternalId(),
                        place,
                        String.valueOf(bufferReturnCell.get().getId()));
                caller.sortableBetaSort(request)
                        .andExpect(status().is2xxSuccessful());
            }
        }

        // сортируем места из заказа в ячейку возврата
        for (String place : places) {
            // проверяем что место из заказа просится в ячейку возврата
            var acceptOrderRes = caller.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var returnCell = findFirstCellByType(acceptOrderDtoResp, CellType.RETURN, CellSubType.DEFAULT);

            assertFalse(returnCell.isEmpty(), "return cell must be not empty");

            // сортируем место из заказа в ячейку возврата
            SortableSortRequestDto request = new SortableSortRequestDto(
                    order.getExternalId(),
                    place,
                    String.valueOf(returnCell.get().getId()));
            caller.sortableBetaSort(request)
                    .andExpect(status().is2xxSuccessful());
        }

        // отгружаем ячейку с посылками заказа
        Place sortedPlace = testFactory.anyOrderPlace(order);

        var shipDtoReq = new FinishRouteRequestDto();
        shipDtoReq.setCellId(sortedPlace.getCellId().get());
        Long routableId = testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(sortedPlace)
                .orElseThrow()).getId();
        caller.ship(routableId, shipDtoReq)
                .andExpect(status().is2xxSuccessful());

        var shippedOrder = testFactory.getOrder(order.getId());
        assertEquals(RETURNED_ORDER_DELIVERED_TO_IM, shippedOrder.getOrderStatus());

        // проверяем что все посылки заказа отгрузились
        var shippedPlaces = testFactory.orderPlaces(shippedOrder.getId());
        shippedPlaces.forEach(place ->
                assertThat(place.isReturned()).isTrue()
        );
    }

    /**
     * Принятие, сортировка и отгрузка всех посылок друг за другом
     * <p>
     * <li> 3 посылки у заказа
     * <li> 3 посылки принимают
     * <li> каждую посылку сортируют в ячейку хранение возвратов потом в ячейку вовзрата
     * <li> маршрут отгружается после каждой посылки
     * <li> заказ должен перейти в {@link ScOrderFFStatus#RETURNED_ORDER_DELIVERED_TO_IM} статус
     * <li> все посылки которые приняты на сц должны быть в статусе {@link PlaceStatus#RETURNED}
     */
    @SneakyThrows
    @DisplayName("success сортировка и отгрузка посылок одна за другой (все посылки на сц)")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void multiplaceFlow2(String whReturnYandexId) {
        testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        testFactory.storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS);

        var p1 = "p1";
        var p2 = "p2";
        var p3 = "p3";
        var places = List.of(p1, p2, p3);
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId)
                        .places(places)
                        .build())
                .cancel()
                .get();

        // принимаем посылки на сц
        for (String place : places) {
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");
        }

        for (String place : List.of(p1, p2)) {
            // сортируем посылку в ячейку хранения
            // сортируем посылку в ячейку возвратов
            successAcceptAndSortToBufferReturnCell(order, place);
            checkNotEmptyAndOnlyBufferAvailableCellsOnAccept(order, place);

            Place sortedPlace = testFactory.orderPlace(order, place);

            var shipDtoReq = new FinishRouteRequestDto();
            shipDtoReq.setCellId(sortedPlace.getCellId().get());
            // Посылки одного заказа могут перемещаться из адресного хранения только все вместе
            caller.ship(testFactory.findOutgoingWarehouseRoute(sortedPlace)
                            .orElseThrow().getId(), shipDtoReq)
                    .andExpect(status().is4xxClientError());
        }

        // сортируем последнюю посылку в ячейку хранения
        // сортируем последнюю посылку в ячейку возвратов
        // отгружаем
        successAcceptAndSortToBufferReturnCell(order, p3);
        for (var place : places) {
            successAcceptAndSortToReturnDefaultCell(order, place);
        }

        Place sortedPlace = testFactory.anyOrderPlace(order);

        var shipDtoReq = new FinishRouteRequestDto();
        shipDtoReq.setCellId(sortedPlace.getCellId().get());
        Long routableId = testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(sortedPlace)
                .orElseThrow()).getId();
        caller.ship(routableId, shipDtoReq)
                .andExpect(status().is2xxSuccessful());
        var shippedOrder = testFactory.getOrder(order.getId());
        assertEquals(RETURNED_ORDER_DELIVERED_TO_IM, shippedOrder.getOrderStatus());
        var orderPlaces = testFactory.orderPlaces(shippedOrder.getId());
        assertThat(orderPlaces).allMatch(p -> p.getSortableStatus() == SortableStatus.SHIPPED_RETURN);
    }

    /**
     * Проверяем что если 2 из 3 посылок приняли на сц, то они сортируются и отгружаются,
     * если принимают 3 посылку после отгрузки, то падаем с ошибкой
     * <p>
     * <li> 3 посылки у заказа
     * <li> 1'ую посылку принимают
     * <li> 1'ую посылку сортируют в ячейку хранение возвратов потом в ячейку возврата
     * <li> маршрут отгружается
     * <li> 2'ую посылку принимают
     * <li> 2'ую посылку сортируют в ячейку хранение возвратов потом в ячейку возврата
     * <li> маршрут отгружается
     * <li> 3'ая посылка при приемке будет падать с ошибкой, что заказ уже в статусе
     * {@link ScOrderFFStatus#RETURNED_ORDER_DELIVERED_TO_IM}
     */
    @SneakyThrows
    @DisplayName("negative сортировка и отгрузка посылок одна за другой(когда не все посылки на сц)")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void multiplaceFlow3(String whReturnYandexId) {
        testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        testFactory.storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS);

        var p1 = "p1";
        var p2 = "p2";
        var p3 = "p3";
        var places = List.of(p1, p2, p3);
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId)
                        .places(places)
                        .build())
                .cancel()
                .get();

        // принимаем 2 посылки из 3
        for (String place : List.of(p1, p2)) {
            var acceptOrderRes = caller
                    .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                    .andExpect(status().is2xxSuccessful());
            var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
            var bufferReturnCell = findFirstCellByType(
                    acceptOrderDtoResp,
                    CellType.BUFFER,
                    CellSubType.BUFFER_RETURNS
            );
            assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
            assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");
        }

        // сортируем посылку в ячейку хранения
        // сортируем посылку в ячейку возвратов
        // отгружаем
        for (String place : List.of(p1, p2)) {
            successAcceptAndSortToBufferReturnCell(order, place);
            checkNotEmptyAndOnlyBufferAvailableCellsOnAccept(order, place);

            Place sortedPlace = testFactory.orderPlace(order, place);


            var shipDtoReq = new FinishRouteRequestDto();
            shipDtoReq.setCellId(sortedPlace.getCellId().get());
            assertEquals(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, sortedPlace.getFfStatus());

            // Посылки одного заказа могут перемещаться из адресного хранения только все вместе
            Long routableId = testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(sortedPlace)
                    .orElseThrow()).getId();
            caller.ship(routableId, shipDtoReq)
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("error").value("CELL_FROM_ANOTHER_ROUTE"));

        }
        var sortedOrder = testFactory.getOrder(order.getId());

        // проверяем что приемке посылки p3 на сц упадет с ошибкой
        assertEquals(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, sortedOrder.getOrderStatus());
        caller
                .acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), p3))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @SneakyThrows
    @DisplayName("success warehouse не поддерживает ячейки ах")
    void whNotSupportBufferReturnCell() {
        var wh = testFactory.storedWarehouse("wh_not_support_buffer_return_cell");
        testFactory.setWarehouseProperty(wh.getYandexId(),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS,
                "false");
        var order = testFactory.createOrder(order(sortingCenter).warehouseReturnId(wh.getYandexId()).build())
                .cancel().get();
        // проверяем что коробка должна проситься в обычную ячейку возврата
        var acceptOrderRes0 = caller.acceptOrder(new AcceptOrderRequestDto(
                        order.getExternalId(),
                        null))
                .andExpect(status().is2xxSuccessful());
        var acceptOrderDtoResponse0 = readContentAsClass(acceptOrderRes0, ApiOrderDto.class);

        assertEquals(ApiOrderStatus.SORT_TO_WAREHOUSE, acceptOrderDtoResponse0.getStatus());
        Optional<ApiCellDto> returnCell = findFirstCellByType(acceptOrderDtoResponse0, CellType.RETURN,
                CellSubType.DEFAULT);
        assertTrue(returnCell.isPresent());
        assertEquals(returnCell.get().getType(), CellType.RETURN);
    }

    @DisplayName("success сортировка одноместного поврежденного возврата через адресное хранение")
    @SneakyThrows
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void acceptAndSortDamageOrder(String whReturnYandexId) {
        var brCell1 = testFactory.storedCell(sortingCenter, "br1", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var place = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId)
                        .warehouseCanProcessDamagedOrders(true)
                        .build())
                .cancel()
                .accept()
                .markOrderAsDamaged()
                .getPlace();

        // сортируем в ах
        successAcceptAndSortToBufferReturnCell(place);
        // сортируем в ячейку возврата
        successAcceptAndSortToReturnCell(place, CellSubType.RETURN_DAMAGED);
    }

    @Test
    @DisplayName("предлагать только одну ячейку АХ из каждой зоны, разрешать сортировку только в предложенные")
    void oneCellByCargoTypeForBufferReturns() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCells = List.of(
                testFactory.storedCell(sortingCenter, "br1kgt",
                        cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L)),
                testFactory.storedCell(sortingCenter, "br2kgt",
                        cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L)),
                testFactory.storedCell(sortingCenter, "br3kgt",
                        cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(3L))
        );

        var mgtCells = List.of(
                testFactory.storedCell(sortingCenter, "br1mgt",
                        cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(4L)),
                testFactory.storedCell(sortingCenter, "br2mgt",
                        cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(5L)),
                testFactory.storedCell(sortingCenter, "br3mgt",
                        cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(6L))
        );

        var place1 = testFactory.createOrder(order(sortingCenter).externalId("o1")
                .warehouseReturnId(testFactory.storedWarehouse().getYandexId()).build())
                .cancel()
                .accept()
                .getPlace();
        var place2 = testFactory.createOrder(order(sortingCenter).externalId("o2")
                .warehouseReturnId(testFactory.storedWarehouse().getYandexId()).build())
                .cancel()
                .accept()
                .getPlace();

        assertGetOrderAndSort(kgtCells.get(0), mgtCells.get(0), mgtCells.get(0), place1, true);
        assertGetOrderAndSort(kgtCells.get(0), mgtCells.get(0), mgtCells.get(1), place2, false);
    }

    @SneakyThrows
    private void assertGetOrderAndSort(Cell kgtCell, Cell mgtCell, Cell sortToCell, Place place, boolean expect2xx) {
        var caller = new TestControllerCaller(mockMvc);
        caller.getOrder(place.getExternalId(), null, null)
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("availableCells").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("availableCells[*].number").value(Matchers.hasItem(kgtCell.getScNumber())))
                .andExpect(jsonPath("availableCells[*].number").value(Matchers.hasItem(mgtCell.getScNumber())));
        caller.sortableBetaSort(new SortableSortRequestDto(place, sortToCell))
                .andExpect(expect2xx ? status().is2xxSuccessful() : status().is4xxClientError());
    }

    @SneakyThrows
    @Test
    @DisplayName("предлагать только одну ячейку АХ из каждой зоны, пропускать полные ячейки")
    void oneCellByCargoTypeForBufferReturnsSkipFull() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        var kgtCell2 = testFactory.storedCell(sortingCenter, "kgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L));
        var kgtOrder = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("o" + kgtCell1.getScNumber())
                        .warehouseReturnId(testFactory.storedWarehouse().getYandexId())
                        .build()).cancel().accept().get();

        var mgtCell1 = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(3L));

        SortableSortRequestDto request = new SortableSortRequestDto(
                kgtOrder.getExternalId(),
                kgtOrder.getExternalId(),
                String.valueOf(kgtCell1.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());

        caller.markCellFilled(kgtCell1.getId(), true);

        var place = testFactory.createOrder(order(sortingCenter).externalId("o")
                .warehouseReturnId(testFactory.storedWarehouse().getYandexId()).build())
                .cancel()
                .accept()
                .getPlace();

        assertGetOrderAndSort(kgtCell2, mgtCell1, kgtCell1, place, false);
        assertGetOrderAndSort(kgtCell2, mgtCell1, kgtCell2, place, true);
    }

    @SneakyThrows
    @Test
    @DisplayName("В адресном хранении разрешать отсканировать ту же ячейку, в которой лежит заказ")
    void oneCellByCargoTypeForBufferReturnsSortTheSameCellAgain() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        var kgtCell2 = testFactory.storedCell(sortingCenter, "kgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L));
        var kgtOrder = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("o" + kgtCell1.getScNumber())
                        .warehouseReturnId(testFactory.storedWarehouse().getYandexId())
                        .build()).cancel().accept().get();

        var mgtCell1 = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(3L));

        SortableSortRequestDto request = new SortableSortRequestDto(
                kgtOrder.getExternalId(),
                kgtOrder.getExternalId(),
                String.valueOf(kgtCell1.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());
        caller.markCellFilled(kgtCell1.getId(), true);

        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());
    }


    @SneakyThrows
    @Test
    @DisplayName("AX 1.5 если закончились ячейки хотя бы в одной зоне, кидать ошибку")
    void oneCellByCargoTypeForBufferReturnsUseDefaultCells() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        var mgtCell = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(2L));
        var kgtOrder = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("o" + kgtCell1.getScNumber())
                        .warehouseReturnId(testFactory.storedWarehouse().getYandexId())
                        .build()).cancel().accept().get();

        SortableSortRequestDto request = new SortableSortRequestDto(
                kgtOrder.getExternalId(),
                kgtOrder.getExternalId(),
                String.valueOf(kgtCell1.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());

        caller.markCellFilled(kgtCell1.getId(), true);

        var order = testFactory.createOrder(order(sortingCenter).externalId("o")
                .warehouseReturnId(testFactory.storedWarehouse().getYandexId()).build()).cancel().accept().get();

        caller.getOrder(order.getExternalId(), null, null)
                .andExpect(status().is4xxClientError());
    }

    @SneakyThrows
    @Test
    @DisplayName("AX 1.5 если есть ячейки с заказами мерча, то предлагать их")
    void oneCellByCargoTypeForBufferReturnsUseCellWithWarehouseOrders() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        var kgtCell2 = testFactory.storedCell(sortingCenter, "kgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L));
        var mgtCell1 = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(3L));
        var mgtCell2 = testFactory.storedCell(sortingCenter, "mgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(4L));

        var warehouse1 = testFactory.storedWarehouse("w1", WarehouseType.SHOP);
        var warehouse2 = testFactory.storedWarehouse("w2", WarehouseType.SHOP);

        var o11 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell1, "o11");
        var o12 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell1, "o12");
        var o21 = createAndSortOrderToBufferReturnCell(warehouse2, kgtCell2, "o21");
        var o22 = createAndSortOrderToBufferReturnCell(warehouse2, mgtCell2, "o22");

        var kgtPlace1 = testFactory.createOrder(
                order(sortingCenter).externalId("kgtOrder1")
                        .places(List.of("kgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var kgtOrder2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("kgtOrder2")
                        .places(List.of("kgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace1 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder1")
                        .places(List.of("mgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder2")
                        .places(List.of("mgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();

        assertGetOrderAndSort(kgtCell1, mgtCell1, kgtCell1, kgtPlace1, true);
        assertGetOrderAndSort(kgtCell1, mgtCell1, kgtCell2, kgtOrder2, false);
        assertGetOrderAndSort(kgtCell1, mgtCell1, mgtCell1, mgtPlace1, true);
        assertGetOrderAndSort(kgtCell1, mgtCell1, mgtCell2, mgtPlace2, false);
    }

    @SneakyThrows
    @Test
    @DisplayName("AX 1.5 если есть несколько ячеек с заказами мерча, то предлагать ячейки с наибольшим числом заказов")
    void oneCellByCargoTypeForBufferReturnsUseCellWithMaxOrdersCnt() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        var kgtCell2 = testFactory.storedCell(sortingCenter, "kgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L));
        var mgtCell1 = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(3L));
        var mgtCell2 = testFactory.storedCell(sortingCenter, "mgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(4L));

        var warehouse1 = testFactory.storedWarehouse("w1", WarehouseType.SHOP);

        var o11 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell1, "o1");
        var o12 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell1, "o2");
        var o21 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell2, "o3");
        var o22 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell2, "o4");
        var o23 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell2, "o5");
        var o24 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell2, "o6");

        var kgtPlace1 = testFactory.createOrder(
                order(sortingCenter).externalId("kgtOrder1")
                        .places(List.of("kgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var kgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("kgtOrder2")
                        .places(List.of("kgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace1 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder1")
                        .places(List.of("mgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder2")
                        .places(List.of("mgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();

        assertGetOrderAndSort(kgtCell2, mgtCell2, kgtCell2, kgtPlace1, true);
        assertGetOrderAndSort(kgtCell2, mgtCell2, kgtCell1, kgtPlace2, false);
        assertGetOrderAndSort(kgtCell2, mgtCell2, mgtCell2, mgtPlace1, true);
        assertGetOrderAndSort(kgtCell2, mgtCell2, mgtCell1, mgtPlace2, false);
    }

    @SneakyThrows
    @Test
    @DisplayName("AX 1.5 если есть несколько ячеек с заказами и у нее не стоит признак занятости, " +
            "то предлагать ячейку без заказов  и не занятую")
    void oneCellByCargoTypeForBufferReturnsDontUseCellsWithPlaces() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        var kgtCell2 = testFactory.storedCell(sortingCenter, "kgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L));
        var mgtCell1 = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(3L));
        var mgtCell2 = testFactory.storedCell(sortingCenter, "mgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(4L));
        var warehouse1 = testFactory.storedWarehouse("w1", WarehouseType.SHOP);
        var warehouse2 = testFactory.storedWarehouse("w2", WarehouseType.SHOP);

        var o11 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell1, "o1");
        var o12 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell1, "o2");

        var kgtPlace1 = testFactory.createOrder(
                order(sortingCenter).externalId("kgtOrder1")
                        .places(List.of("kgtOrder1_0"))
                        .warehouseReturnId(warehouse2.getYandexId())
                        .build()).cancel().accept().getPlace();
        var kgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("kgtOrder2")
                        .places(List.of("kgtOrder2_0"))
                        .warehouseReturnId(warehouse2.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace1 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder1")
                        .places(List.of("mgtOrder1_0"))
                        .warehouseReturnId(warehouse2.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder2")
                        .places(List.of("mgtOrder2_0"))
                        .warehouseReturnId(warehouse2.getYandexId())
                        .build()).cancel().accept().getPlace();

        assertGetOrderAndSort(kgtCell2, mgtCell2, kgtCell2, kgtPlace1, true);
        assertGetOrderAndSort(kgtCell2, mgtCell2, kgtCell1, kgtPlace2, false);
        assertGetOrderAndSort(kgtCell2, mgtCell2, mgtCell2, mgtPlace1, true);
        assertGetOrderAndSort(kgtCell2, mgtCell2, mgtCell1, mgtPlace2, false);
    }

    @SneakyThrows
    @Test
    @DisplayName("AX 1.5 " +
            "если есть несколько ячеек с заказами мерча и она занята, то предлагать другую ячейку с заказами мерча")
    void oneCellByCargoTypeForBufferReturnsUseNotFullCells() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        var kgtCell2 = testFactory.storedCell(sortingCenter, "kgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L));
        var mgtCell1 = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(3L));
        var mgtCell2 = testFactory.storedCell(sortingCenter, "mgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(4L));

        var warehouse1 = testFactory.storedWarehouse("w1", WarehouseType.SHOP);

        var o11 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell1, "o1");
        var o21 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell1, "o2");
        testFactory.setFullnessToCell(kgtCell1.getId(), true);
        var o12 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell1, "o3");
        var o23 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell1, "o4");
        testFactory.setFullnessToCell(mgtCell1.getId(), true);
        var o22 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell2, "o5");
        var o24 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell2, "o6");

        var kgtPlace1 = testFactory.createOrder(
                order(sortingCenter).externalId("kgtOrder1")
                        .places(List.of("kgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var kgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("kgtOrder2")
                        .places(List.of("kgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace1 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder1")
                        .places(List.of("mgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder2")
                        .places(List.of("mgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();

        assertGetOrderAndSort(kgtCell2, mgtCell2, kgtCell2, kgtPlace1, true);
        assertGetOrderAndSort(kgtCell2, mgtCell2, kgtCell1, kgtPlace2, false);
        assertGetOrderAndSort(kgtCell2, mgtCell2, mgtCell2, mgtPlace1, true);
        assertGetOrderAndSort(kgtCell2, mgtCell2, mgtCell1, mgtPlace2, false);

    }

    @SneakyThrows
    @Test
    @DisplayName("AX 1.5 если есть ячейка с заказами мерча и она занята, то выбрать пустую ячейку")
    void oneCellByCargoTypeForBufferReturnsUseEmptyCells() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        var kgtCell2 = testFactory.storedCell(sortingCenter, "kgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L));
        var kgtCell3 = testFactory.storedCell(sortingCenter, "kgtCell3",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(3L));
        var mgtCell1 = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(4L));
        var mgtCell2 = testFactory.storedCell(sortingCenter, "mgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(5L));
        var mgtCell3 = testFactory.storedCell(sortingCenter, "mgtCell3",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(6L));

        var warehouse1 = testFactory.storedWarehouse("w1", WarehouseType.SHOP);
        var warehouse2 = testFactory.storedWarehouse("w2", WarehouseType.SHOP);

        var o11 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell1, "o11");
        testFactory.setFullnessToCell(kgtCell1.getId(), true);
        var o12 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell1, "o12");
        testFactory.setFullnessToCell(mgtCell1.getId(), true);
        var o21 = createAndSortOrderToBufferReturnCell(warehouse2, kgtCell2, "o21");
        var o22 = createAndSortOrderToBufferReturnCell(warehouse2, mgtCell2, "o22");

        var kgtPlace1 = testFactory.createOrder(
                order(sortingCenter).externalId("kgtOrder1")
                        .places(List.of("kgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var kgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("kgtOrder2")
                        .places(List.of("kgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace1 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder1")
                        .places(List.of("mgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder2")
                        .places(List.of("mgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();

        assertGetOrderAndSort(kgtCell3, mgtCell3, kgtCell3, kgtPlace1, true);
        assertGetOrderAndSort(kgtCell3, mgtCell3, kgtCell1, kgtPlace2, false);
        assertGetOrderAndSort(kgtCell3, mgtCell3, mgtCell3, mgtPlace1, true);
        assertGetOrderAndSort(kgtCell3, mgtCell3, mgtCell1, mgtPlace2, false);
    }

    @SneakyThrows
    @Test
    @DisplayName("AX 1.5 если есть ячейка с заказами мерча и она занята, то выбрать пустую ячейку в порядке обхода")
    void oneCellByCargoTypeForBufferReturnsUseEmptyCellsOrderBySeqNumber() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        var kgtCell2 = testFactory.storedCell(sortingCenter, "kgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L));
        var kgtCell3 = testFactory.storedCell(sortingCenter, "kgtCell3",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(3L));
        var kgtCell4 = testFactory.storedCell(sortingCenter, "kgtCell4",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(4L));
        var kgtCell5 = testFactory.storedCell(sortingCenter, "kgtCell5",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(5L));

        var mgtCell1 = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(10L));
        var mgtCell2 = testFactory.storedCell(sortingCenter, "mgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(9L));
        var mgtCell3 = testFactory.storedCell(sortingCenter, "mgtCell3",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(8L));
        var mgtCell4 = testFactory.storedCell(sortingCenter, "mgtCell4",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(7L));
        var mgtCell5 = testFactory.storedCell(sortingCenter, "mgtCell5",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(6L));

        var warehouse1 = testFactory.storedWarehouse("w1", WarehouseType.SHOP);
        var warehouse2 = testFactory.storedWarehouse("w2", WarehouseType.SHOP);

        var o11 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell3, "o11");
        testFactory.setFullnessToCell(kgtCell3.getId(), true);
        var o12 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell3, "o12");
        testFactory.setFullnessToCell(mgtCell3.getId(), true);
        var o21 = createAndSortOrderToBufferReturnCell(warehouse2, kgtCell2, "o21");
        var o22 = createAndSortOrderToBufferReturnCell(warehouse2, kgtCell4, "o22");
        var o23 = createAndSortOrderToBufferReturnCell(warehouse2, mgtCell2, "o23");
        var o24 = createAndSortOrderToBufferReturnCell(warehouse2, mgtCell4, "o24");

        var kgtPlace1 = testFactory.createOrder(
                order(sortingCenter).externalId("kgtOrder1")
                        .places(List.of("kgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var kgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("kgtOrder2")
                        .places(List.of("kgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace1 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder1")
                        .places(List.of("mgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder2")
                        .places(List.of("mgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();

        assertGetOrderAndSort(kgtCell1, mgtCell5, kgtCell1, kgtPlace1, true);
        assertGetOrderAndSort(kgtCell1, mgtCell5, kgtCell5, kgtPlace2, false);
        assertGetOrderAndSort(kgtCell1, mgtCell5, mgtCell5, mgtPlace1, true);
        assertGetOrderAndSort(kgtCell1, mgtCell5, mgtCell1, mgtPlace2, false);
    }

    @SneakyThrows
    @Test
    @DisplayName("AX 1.5 если нет пустых ячеек, то выбрать ячейку с наименьшим количеством мерчей в порядке обхода")
    void oneCellByCargoTypeForBufferReturnsUseCellWithMinWarehouseNumber() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        var kgtCell2 = testFactory.storedCell(sortingCenter, "kgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L));
        var mgtCell1 = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(3L));
        var mgtCell2 = testFactory.storedCell(sortingCenter, "mgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(4L));

        var warehouse1 = testFactory.storedWarehouse("w1", WarehouseType.SHOP);
        var warehouse2 = testFactory.storedWarehouse("w2", WarehouseType.SHOP);
        var warehouse3 = testFactory.storedWarehouse("w3", WarehouseType.SHOP);

        var o11 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell1, "o11");
        var o12 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell1, "o12");
        var o21 = createAndSortOrderToBufferReturnCell(warehouse2, kgtCell2, "o21");
        var o22 = createAndSortOrderToBufferReturnCell(warehouse2, mgtCell2, "o22");

        var kgtPlace1 = testFactory.createOrder(
                order(sortingCenter).externalId("kgtOrder1")
                        .places(List.of("kgtOrder1_0"))
                        .warehouseReturnId(warehouse3.getYandexId())
                        .build()).cancel().accept().getPlace();
        var kgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("kgtOrder2")
                        .places(List.of("kgtOrder2_0"))
                        .warehouseReturnId(warehouse3.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace1 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder1")
                        .places(List.of("mgtOrder1_0"))
                        .warehouseReturnId(warehouse3.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder2")
                        .places(List.of("mgtOrder2_0"))
                        .warehouseReturnId(warehouse3.getYandexId())
                        .build()).cancel().accept().getPlace();

        assertGetOrderAndSort(kgtCell1, mgtCell1, kgtCell1, kgtPlace1, true);
        assertGetOrderAndSort(kgtCell1, mgtCell1, kgtCell2, kgtPlace2, false);
        assertGetOrderAndSort(kgtCell1, mgtCell1, mgtCell1, mgtPlace1, true);
        assertGetOrderAndSort(kgtCell1, mgtCell1, mgtCell2, mgtPlace2, false);
    }

    @SneakyThrows
    @Test
    @DisplayName("AX 1.5 Даже при наличии пустых ячеек, для заказов мерча использовать ячейки с другими заказами мерча")
    void oneCellByCargoTypeForBufferReturnsUseCellWithWarehousePlacesEvenThereAreEmptyCells() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell2 = testFactory.storedCell(sortingCenter, "kgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L));

        var mgtCell2 = testFactory.storedCell(sortingCenter, "mgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(4L));

        var warehouse1 = testFactory.storedWarehouse("w1", WarehouseType.SHOP);
        var warehouse2 = testFactory.storedWarehouse("w2", WarehouseType.SHOP);

        var o11 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell2, "o11");
        var o12 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell2, "o12");
        var o21 = createAndSortOrderToBufferReturnCell(warehouse2, kgtCell2, "o21");
        var o22 = createAndSortOrderToBufferReturnCell(warehouse2, mgtCell2, "o22");

        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        var mgtCell1 = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(3L));

        var kgtPlace1 = testFactory.createOrder(
                order(sortingCenter).externalId("kgtOrder1")
                        .places(List.of("kgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var kgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("kgtOrder2")
                        .places(List.of("kgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace1 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder1")
                        .places(List.of("mgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder2")
                        .places(List.of("mgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();

        assertGetOrderAndSort(kgtCell2, mgtCell2, kgtCell2, kgtPlace1, true);
        assertGetOrderAndSort(kgtCell2, mgtCell2, kgtCell1, kgtPlace2, false);
        assertGetOrderAndSort(kgtCell2, mgtCell2, mgtCell2, mgtPlace1, true);
        assertGetOrderAndSort(kgtCell2, mgtCell2, mgtCell1, mgtPlace2, false);
    }

    @SneakyThrows
    @Test
    @DisplayName("AX 1.5 При равном количестве заказов мерчей в ячейках выбирать ячейку с наименьшим порядком обхода")
    void oneCellByCargoTypeForBufferReturnsUseCellWithWarehousePlacesOrderBySequenceNumber() {
        testFactory.setSortingCenterProperty(sortingCenter, ONE_CELL_PER_ZONE_FOR_BUFFER_RETURN_CELLS, true);
        testFactory.setSortingCenterProperty(sortingCenter, USE_ZONE_FOR_BUFFER_RETURN_CELLS, true);

        CellField.CellFieldBuilder cellFieldBuilder = CellField.builder()
                .type(CellType.BUFFER)
                .subType(CellSubType.BUFFER_RETURNS);
        var kgtCell1 = testFactory.storedCell(sortingCenter, "kgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(2L));
        var mgtCell1 = testFactory.storedCell(sortingCenter, "mgtCell1",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(4L));

        var warehouse1 = testFactory.storedWarehouse("w1", WarehouseType.SHOP);
        var warehouse2 = testFactory.storedWarehouse("w2", WarehouseType.SHOP);

        var o11 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell1, "o11");
        var o12 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell1, "o12");
        var o21 = createAndSortOrderToBufferReturnCell(warehouse2, kgtCell1, "o21");
        var kgtCell2 = testFactory.storedCell(sortingCenter, "kgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.KGT).sequenceNumber(1L));
        testFactory.setFullnessToCell(kgtCell1.getId(), true);
        var o13 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell2, "o13");
        var o14 = createAndSortOrderToBufferReturnCell(warehouse1, kgtCell2, "o14");
        testFactory.setFullnessToCell(kgtCell1.getId(), false);

        var o15 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell1, "o15");
        var o16 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell1, "o16");
        var o22 = createAndSortOrderToBufferReturnCell(warehouse2, mgtCell1, "o22");
        var mgtCell2 = testFactory.storedCell(sortingCenter, "mgtCell2",
                cellFieldBuilder.cargoType(CellCargoType.MGT).sequenceNumber(3L));
        testFactory.setFullnessToCell(mgtCell1.getId(), true);
        var o17 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell2, "o17");
        var o18 = createAndSortOrderToBufferReturnCell(warehouse1, mgtCell2, "o18");
        testFactory.setFullnessToCell(mgtCell1.getId(), false);

        var kgtPlace1 = testFactory.createOrder(
                order(sortingCenter).externalId("kgtOrder1")
                        .places(List.of("kgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var kgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("kgtOrder2")
                        .places(List.of("kgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace1 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder1")
                        .places(List.of("mgtOrder1_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();
        var mgtPlace2 = testFactory.createOrder(
                order(sortingCenter)
                        .externalId("mgtOrder2")
                        .places(List.of("mgtOrder2_0"))
                        .warehouseReturnId(warehouse1.getYandexId())
                        .build()).cancel().accept().getPlace();

        assertGetOrderAndSort(kgtCell2, mgtCell2, kgtCell2, kgtPlace1, true);
        assertGetOrderAndSort(kgtCell2, mgtCell2, kgtCell1, kgtPlace2, false);
        assertGetOrderAndSort(kgtCell2, mgtCell2, mgtCell2, mgtPlace1, true);
        assertGetOrderAndSort(kgtCell2, mgtCell2, mgtCell1, mgtPlace2, false);
    }

    private ScOrder createAndSortOrderToBufferReturnCell(Warehouse warehouse, Cell cell, String externalOrderId) {
        String placeId = externalOrderId + "_0";
        return testFactory.createOrder(
                        order(sortingCenter)
                                .externalId(externalOrderId)
                                .places(List.of(placeId))
                                .warehouseReturnId(warehouse.getYandexId())
                                .build()).cancel().accept().sortPlace(placeId, cell.getId())
                .get();
    }

    @SneakyThrows
    @DisplayName("Не переводим из 175 в 170 чп, при перемещении АХ -> ячейка мерча -> АХ, если все посылки на СЦ")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    void freeze175StateIfPlacesOnSc(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        var places = List.of("p1", "p2");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId(whReturnYandexId).places(places).build())
                .acceptPlaces(places)
                .cancel()
                .keepPlaces(bufferCell.getId(), places.toArray(new String[0]))
                .get();

        // отсортировали в ячейку мерча
        SortableSortRequestDto request = new SortableSortRequestDto(
                order.getExternalId(),
                "p1",
                String.valueOf(returnCell.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());

        request = new SortableSortRequestDto(
                order.getExternalId(),
                "p2",
                String.valueOf(returnCell.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());

        // отсортировали обратно в АХ
        request = new SortableSortRequestDto(
                order.getExternalId(),
                "p1",
                String.valueOf(bufferCell.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());

        assertThat(testFactory.getOrder(order.getId()).getFfStatus()).isEqualTo(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(testFactory.orderPlace(order, "p1").getPlaceStatus()).isEqualTo(PlaceStatus.KEEPED);
        assertThat(testFactory.orderPlace(order, "p2").getPlaceStatus()).isEqualTo(PlaceStatus.SORTED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Отключение АХ для мерчанта")
    void disabledBufferReturnsWarehouseShop() {
        // мерчант
        var whShop = testFactory.storedWarehouse("whShop-1", WarehouseType.SHOP);
        testFactory.setWarehouseProperty(String.valueOf(whShop.getYandexId()),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS, "false");

        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId("whShop-1").places("p1", "p2").build())
                .acceptPlaces()
                .cancel()
                .get();

        SortableSortRequestDto request = new SortableSortRequestDto(
                order.getExternalId(),
                "p1",
                String.valueOf(bufferCell.getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.error", is(ScErrorCode.NOT_SUPPORTED_CELL_SUBTYPE.name())))
                .andExpect(jsonPath("$.message", is("Cell " + bufferCell + " has wrong subtype BUFFER_RETURNS. " +
                        "Expected: DEFAULT")));
    }

    @Test
    @SneakyThrows
    @DisplayName("Сортировка отмененного заказа (средней мили) из АХ в возвратную ячейку мерчанта")
    void sortMiddleMileOrderCanceledFromBufferReturnsToReturnMerch() {
        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT, "whShop-1");
        var order = testFactory.createOrder(order(sortingCenter)
                        .warehouseReturnId("whShop-1")
                        .places("p1", "p2", "p3")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2")
                .cancel()
                .keepPlaces("p1", "p2")
                .acceptPlaces("p3")
                .keepPlaces("p3")
                .get();

        caller.sortableBetaSort(new SortableSortRequestDto(order.getExternalId(), "p3", returnCell.getId().toString()))
                .andExpect(status().isOk());
        assertThat(testFactory.orderPlace(order, "p3").getCell()).isEqualTo(returnCell);
    }

    @SneakyThrows
    private void checkNotEmptyAndOnlyBufferAvailableCellsOnAccept(OrderLike order, String place) {
        var acceptOrderRes = caller.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), place))
                .andExpect(status().is2xxSuccessful());
        var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
        var availableCells = Objects.requireNonNull(acceptOrderDtoResp.getAvailableCells());
        assertThat(availableCells).isNotEmpty();
        assertThat(availableCells)
                .allMatch(it -> it.getType() == CellType.BUFFER && it.getSubType() == CellSubType.BUFFER_RETURNS);
    }

    private void successAcceptAndSortToBufferReturnCell(ScOrder order, String placeExternalId) throws Exception {
        successAcceptAndSortToBufferReturnCell(testFactory.orderPlace(order, placeExternalId));
    }

    private void successAcceptAndSortToBufferReturnCell(Place place) throws Exception {
        var acceptOrderRes = caller.acceptOrder(new AcceptOrderRequestDto(place))
                .andExpect(status().is2xxSuccessful());
        var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
        var bufferReturnCell = findFirstCellByType(
                acceptOrderDtoResp,
                CellType.BUFFER,
                CellSubType.BUFFER_RETURNS
        );
        assertEquals(ApiOrderStatus.KEEP_TO_WAREHOUSE, acceptOrderDtoResp.getStatus());
        assertFalse(bufferReturnCell.isEmpty(), "buffer return cell must be not empty");

        // проверяем что место из заказа сортируется в ячейку хранения
        SortableSortRequestDto request = new SortableSortRequestDto(
                place.getExternalId(),
                place.getMainPartnerCode(),
                String.valueOf(bufferReturnCell.get().getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());
    }

    private void successAcceptAndSortToReturnDefaultCell(ScOrder order, String placeExternalId) throws Exception {
        Place place = testFactory.orderPlace(order, placeExternalId);
        successAcceptAndSortToReturnCell(place, CellSubType.DEFAULT);
    }

    private void successAcceptAndSortToReturnCell(Place place, CellSubType cellSubType) throws Exception {
        // проверяем что место из заказа просится в ячейку возврата
        var acceptOrderRes = caller.acceptOrder(new AcceptOrderRequestDto(place))
                .andExpect(status().is2xxSuccessful());
        var acceptOrderDtoResp = readContentAsClass(acceptOrderRes, ApiOrderDto.class);
        var returnCell = findFirstCellByType(acceptOrderDtoResp, CellType.RETURN, cellSubType);

        assertFalse(returnCell.isEmpty(), "return cell must be not empty");

        // сортируем место из заказа в ячейку возврата
        SortableSortRequestDto request = new SortableSortRequestDto(
                place.getExternalId(),
                place.getMainPartnerCode(),
                String.valueOf(returnCell.get().getId()));
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful());
    }

    private Optional<ApiCellDto> findFirstCellByType(ApiOrderDto apiOrderDto, CellType cellType, CellSubType subType) {
        assertNotNull(apiOrderDto, "apiOrderDto must be not null ");
        assertNotNull(apiOrderDto.getAvailableCells(), "apiOrderDto.availableCells must be not null");
        return StreamEx
                .of(apiOrderDto.getAvailableCells())
                .nonNull()
                .filter(c -> c.getType() == cellType && c.getSubType() == subType)
                .findFirst();
    }

    private boolean hasCell(ApiOrderDto apiOrderDto, Cell cell) {
        assertNotNull(apiOrderDto, "apiOrderDto must be not null ");
        assertNotNull(apiOrderDto.getAvailableCells(), "apiOrderDto.availableCells must be not null");
        return StreamEx
                .of(apiOrderDto.getAvailableCells())
                .nonNull()
                .anyMatch(c -> c.getId() == cell.getId());
    }

    @Test
    @SneakyThrows
    @DisplayName("Одна из посылок в АХ. Должен приходить routeTo при сканировании заказа")
    void routeToExistsWhenAcceptOrder() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);

        var order = testFactory.createForToday(
                        order(sortingCenter, "o1")
                                .warehouseReturnId("whShop-1")
                                .places("p1", "p2")
                                .build()
                ).cancel()
                .acceptPlaces("p1")
                .keepPlaces(bufferCell.getId(), "p1")
                .get();

        var caller = TestControllerCaller.createCaller(mockMvc);
        caller.acceptOrder(new AcceptOrderRequestDto(order.getExternalId(), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeTo").exists());
    }
}
