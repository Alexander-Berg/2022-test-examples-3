package ru.yandex.market.sc.api.features;

import java.time.Clock;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.task.model.TaskNextCellResponseDto;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * Флоу работы с заданиями на адресное хранение: сбор маршрута на ТСД
 * Продуктовая задача: https://st.yandex-team.ru/OPSPROJECT-3689
 *
 * @author: merak1t
 * @date: 11/11/21
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RouteTaskFlowTest extends BaseApiControllerTest {

    @MockBean
    Clock clock;
    SortingCenter sortingCenter;
    Cell cell;

    private TestControllerCaller controllerCaller;


    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        cell = testFactory.storedActiveCell(sortingCenter);
        testFactory.storedUser(sortingCenter, UID, UserRole.STOCKMAN);
        testFactory.setupMockClock(clock);
        setUpScProperty(sortingCenter);
        controllerCaller = TestControllerCaller.createCaller(mockMvc);

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
    @DisplayName("success задание отгрузки мерчу из 1 ячейки")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getApiOutgoingRouteTaskList(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS, 1L);
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
        var routeId = testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(order2).orElseThrow())
                                                                                                        .getId();
        var routeId2 = testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(order5).orElseThrow())
                                                                                                        .getId();

        var getNextCellResp = controllerCaller.getNextCell(routeId)
                .andExpect(status().is2xxSuccessful());
        var nextCellDtoResp = readContentAsClass(getNextCellResp, TaskNextCellResponseDto.class);
        assertThat(nextCellDtoResp).isEqualTo(new TaskNextCellResponseDto(bufferCell, 1, 1));

        var finishCellResp = controllerCaller.finishCell(routeId, nextCellDtoResp.getCell().getId())
                .andExpect(status().is2xxSuccessful());

        var getNextCellResp2 = controllerCaller.getNextCell(routeId)
                .andExpect(status().is2xxSuccessful());
        var nextCellDtoResp2 = readContentAsClass(getNextCellResp2, TaskNextCellResponseDto.class);
        assertThat(nextCellDtoResp2).isEqualTo(new TaskNextCellResponseDto(null, 2, 1));
    }

    @SneakyThrows
    @DisplayName("success задание отгрузки мерчу из 3 ячеек")
    @ParameterizedTest
    @MethodSource("getWarehouseReturnId")
    public void getApiOutgoingRouteTask2List(String whReturnYandexId) {
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS, 1L);
        var bufferCell2 = testFactory.storedCell(sortingCenter, "br2", CellType.BUFFER, CellSubType.BUFFER_RETURNS, 2L);
        var bufferCell3 = testFactory.storedCell(sortingCenter, "br3", CellType.BUFFER, CellSubType.BUFFER_RETURNS, 3L);

        var returnCell = testFactory.storedCell(sortingCenter, "ret", CellType.RETURN, CellSubType.DEFAULT);
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o1").warehouseReturnId(whReturnYandexId).places("o1p1", "o1p2").build())
                .acceptPlaces("o1p1", "o1p2").cancel()
                .keepPlaces(bufferCell.getId(), "o1p1", "o1p2")
                .sortPlaces(returnCell.getId(), "o1p1")
                .get();

        var order2 = testFactory.createOrder(order(sortingCenter)
                        .externalId("o2").warehouseReturnId(whReturnYandexId).places("o2p1", "o2p2").build())
                .acceptPlaces("o2p1", "o2p2").cancel()
                .keepPlaces(bufferCell2.getId(), "o2p1")
                .keepPlaces(bufferCell3.getId(), "o2p2")
                .get();
        testFactory.createOrder(order(sortingCenter)
                        .externalId("o3").warehouseReturnId(whReturnYandexId).places("o3p1", "o3p2").build())
                .acceptPlaces("o3p1").cancel()
                .keepPlaces(bufferCell3.getId(), "o3p1")
                .get();

        var routeId = testFactory.getRoutable(testFactory.findOutgoingWarehouseRoute(order2).orElseThrow())
                                                                                                    .getId();

        var getNextCellResp = controllerCaller.getNextCell(routeId)
                .andExpect(status().is2xxSuccessful());
        var nextCellDtoResp = readContentAsClass(getNextCellResp, TaskNextCellResponseDto.class);
        assertThat(nextCellDtoResp).isEqualTo(new TaskNextCellResponseDto(bufferCell, 1, 3));

        var finishCellResp = controllerCaller.finishCell(routeId, nextCellDtoResp.getCell().getId())
                .andExpect(status().is2xxSuccessful());

        var getNextCellResp2 = controllerCaller.getNextCell(routeId)
                .andExpect(status().is2xxSuccessful());
        var nextCellDtoResp2 = readContentAsClass(getNextCellResp2, TaskNextCellResponseDto.class);
        assertThat(nextCellDtoResp2).isEqualTo(new TaskNextCellResponseDto(bufferCell2, 2, 3));

        var finishCellResp2 = controllerCaller.finishCell(routeId, nextCellDtoResp2.getCell().getId())
                .andExpect(status().is2xxSuccessful());

        var getNextCellResp3 = controllerCaller.getNextCell(routeId)
                .andExpect(status().is2xxSuccessful());
        var nextCellDtoResp3 = readContentAsClass(getNextCellResp3, TaskNextCellResponseDto.class);
        assertThat(nextCellDtoResp3).isEqualTo(new TaskNextCellResponseDto(bufferCell3, 3, 3));

        var finishCellResp25 = controllerCaller.finishCell(routeId, nextCellDtoResp2.getCell().getId())
                .andExpect(status().is4xxClientError());
        var getNextCellResp25 = controllerCaller.getNextCell(routeId)
                .andExpect(status().is2xxSuccessful());
        var nextCellDtoResp25 = readContentAsClass(getNextCellResp25, TaskNextCellResponseDto.class);
        assertThat(nextCellDtoResp25).isEqualTo(new TaskNextCellResponseDto(bufferCell3, 3, 3));

        var finishCellResp3 = controllerCaller.finishCell(routeId, nextCellDtoResp3.getCell().getId())
                .andExpect(status().is2xxSuccessful());

        var getNextCellResp4 = controllerCaller.getNextCell(routeId)
                .andExpect(status().is2xxSuccessful());
        var nextCellDtoResp4 = readContentAsClass(getNextCellResp4, TaskNextCellResponseDto.class);
        assertThat(nextCellDtoResp4).isEqualTo(new TaskNextCellResponseDto(null, 4, 3));

    }
}
