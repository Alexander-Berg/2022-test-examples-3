package ru.yandex.market.sc.api.features;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.AcceptReturnedOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Флоу работы с фешн возвратными заказами
 * Продуктовая задача: https://st.yandex-team.ru/OPSPROJECT-3120
 *
 * @author: merak1t
 * @date: 9/22/21
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FashionFlowTest extends BaseApiControllerTest {

    private static final String FSN_PREFIX =
            ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getAnyPrefix();

    @MockBean
    Clock clock;
    SortingCenter sortingCenter;
    Warehouse warehouse;
    Cell cell;
    private final ScanService scanService;
    private final TestFactory testFactory;
    private User user;
    private TestControllerCaller caller;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        warehouse = testFactory.storedWarehouse();
        cell = testFactory.storedActiveCell(sortingCenter);
        user = testFactory.storedUser(sortingCenter, UID, UserRole.STOCKMAN);
        testFactory.setupMockClock(clock);
        testFactory.increaseScOrderId();
        caller = TestControllerCaller.createCaller(mockMvc);
    }

    @Test
    @SneakyThrows
    @DisplayName("создаем и принимаем возвратный fashion заказ")
    void createFashionClientReturnOnAcceptReturn() {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns(
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId()
        );
        var requestDto = new AcceptReturnedOrderRequestDto(FSN_PREFIX + "1", null, null);
        ApiOrderDto orderDto = scanService.acceptReturnedOrder(requestDto, new ScContext(user));
        assertThat(orderDto.getId()).isNotNull();
        assertThat(orderDto.getStatus()).isEqualTo(ApiOrderStatus.SORT_TO_WAREHOUSE);
    }

    @Test
    @SneakyThrows
    @DisplayName("fail fashion заказ не создается тк включена пропертя ConfigurationProperties.DISABLE_FSN_CREATE ")
    void disableFashionCreate() {
        testFactory.setConfiguration(ConfigurationProperties.DISABLE_FSN_CREATE, true);
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns(
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId()
        );
        var requestDto = new AcceptReturnedOrderRequestDto(FSN_PREFIX + "1", null, null);
        ApiOrderDto orderDto = scanService.acceptReturnedOrder(requestDto, new ScContext(user));
        assertThat(orderDto.getId()).isNull();
        assertThat(orderDto.getStatus()).isEqualTo(ApiOrderStatus.ERROR);
    }

    /**
     * принимаем и сортируем возвратный fashion заказ через приемку возвратов,
     * а потом снова сканируем через приемку возвратов и получаем ответ, что заказ уже в своей ячейке
     */
    @Test
    @SneakyThrows
    @DisplayName("принимаем и сортируем возвратный fashion заказ через приемку возвратов")
    void acceptAndSortFashionReturnOrder() {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns(
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId()
        );
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, false);
        var defaultReturnCell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN, CellSubType.DEFAULT,
                "666");
        var clientReturnCell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN, CellSubType.CLIENT_RETURN
                , "client_return_cell");
        var courierCell = testFactory.storedActiveCell(sortingCenter, CellType.COURIER, "321");
        var bufferCell = testFactory.storedActiveCell(sortingCenter, CellType.BUFFER, "222");
        var bufferReturnCell = testFactory.storedCell(sortingCenter, CellType.BUFFER, CellSubType.BUFFER_RETURNS, null);

        var order = testFactory.createClientReturnForToday(sortingCenter, testFactory.defaultCourier(),
                        FSN_PREFIX + "1")
                .get();

        caller.acceptReturn(new AcceptOrderRequestDto(order.getExternalId(), order.getExternalId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}"))
                .andExpect(content().json("{\"availableCells\":[{\"id\":" + defaultReturnCell.getId()
                        + ",\"number\":\"" + defaultReturnCell.getScNumber() + "\"," +
                        "\"status\":\"ACTIVE\",\"type\":\"RETURN\",\"subType\":\"DEFAULT\"}]}"));
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        long cellId = Objects.requireNonNull(testFactory.determineRouteCell(route, order)).getId();
        assertEquals(cellId, defaultReturnCell.getId());
        for (var curCell : List.of(cell, clientReturnCell, courierCell, bufferCell, bufferReturnCell)) {
            caller.sortableBetaSort(new SortableSortRequestDto(
                            order.getExternalId(),
                            order.getExternalId(),
                            String.valueOf(curCell.getId())))
                    .andExpect(status().is4xxClientError());
        }

        caller.sortableBetaSort(new SortableSortRequestDto(
                        order.getExternalId(),
                        order.getExternalId(),
                        String.valueOf(cellId)))
                .andExpect(status().isOk());

        caller.acceptReturn(new AcceptOrderRequestDto(order.getExternalId(), order.getExternalId()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().
                        json("{\"status\":400,\"error\":\"ORDER_FROM_STRAIGHT_STREAM\"}", false));
    }

    /**
     * отгружаем возвратный fashion заказ
     */
    @Test
    @SneakyThrows
    @DisplayName("отгружаем возвратный fashion заказ")
    void shipFashionReturnOrder() {
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns(
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId()
        );
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, false);
        var defaultReturnCell = testFactory.storedActiveCell(sortingCenter, CellType.RETURN, CellSubType.DEFAULT,
                "666");

        var place = testFactory.createClientReturnForToday(sortingCenter, testFactory.defaultCourier(),
                        FSN_PREFIX + "1")
                .getPlace();

        caller.acceptReturn(new AcceptOrderRequestDto(place.getExternalId(), place.getExternalId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}"))
                .andExpect(content().json("{\"availableCells\":[{\"id\":" + defaultReturnCell.getId()
                        + ",\"number\":\"" + defaultReturnCell.getScNumber() + "\"," +
                        "\"status\":\"ACTIVE\",\"type\":\"RETURN\",\"subType\":\"DEFAULT\"}]}"));
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();
        long cellId = Objects.requireNonNull(testFactory.determineRouteCell(route, place)).getId();
        assertEquals(cellId, defaultReturnCell.getId());

        caller.sortableBetaSort(new SortableSortRequestDto(
                        place.getExternalId(),
                        place.getExternalId(),
                        String.valueOf(cellId)))
                .andExpect(status().isOk());

        caller.ship(testFactory.getRouteIdForSortableFlow(route), new FinishRouteRequestDto(
                null, null, cellId, false, null, null
        )).andExpect(status().isOk());

        place = testFactory.updated(place);
        assertThat(place.getCellId()).isEqualTo(Optional.empty());
        assertThat(place.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    private String acceptBody(String externalId, @Nullable String placeExternalId) {
        return "{\"externalId\":\"" + externalId + "\""
                + (placeExternalId == null ? "" : ",\"placeExternalId\":\"" + placeExternalId + "\"") + "}";
    }
}
