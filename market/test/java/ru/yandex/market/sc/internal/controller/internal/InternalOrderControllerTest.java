package ru.yandex.market.sc.internal.controller.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.route.RouteCellsCleanupService;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.controller.internal.dto.BatchUpdateOrdersRequest;
import ru.yandex.market.sc.internal.controller.internal.dto.ClientReturnDto;
import ru.yandex.market.sc.internal.controller.internal.dto.InternalCourierDto;
import ru.yandex.market.sc.internal.controller.internal.dto.RevertReturnDto;
import ru.yandex.market.sc.internal.model.ContactDto;
import ru.yandex.market.sc.internal.model.LocationDto;
import ru.yandex.market.sc.internal.model.WarehouseDto;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScIntControllerTest
class InternalOrderControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    Clock clock;
    @Autowired
    ScOrderRepository scOrderRepository;
    @MockBean
    RouteCellsCleanupService routeCellsCleanupService;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setupWarehouseAndDeliveryServiceForClientReturns();
    }

    @Test
    void createClientReturnScSupportsClientReturns() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/internal/courier/clientReturn")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JacksonUtil.toString(getClientReturnDto("VOZVRAT_SF_PS_1234"))))
                .andExpect(status().isOk());
    }

    @Test
    void revertReturn() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().ship().makeReturn().get();
        assertThat(scOrderRepository.findByIdOrThrow(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        performRevertReturn(order.getExternalId());
        assertThat(scOrderRepository.findByIdOrThrow(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
    }

    @SneakyThrows
    private void performRevertReturn(String orderId) {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/internal/orders/revertReturn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(new RevertReturnDto(sortingCenter.getId(), List.of(orderId)))))
                .andExpect(status().isOk());
    }

    @Test
    void clientReturnFromLocker() {
        ClientReturnDto clientReturnDto = getClientReturnDto("VOZVRAT_SF_PS_1234");
        performClientReturn(clientReturnDto);
    }

    private ClientReturnDto getClientReturnDto(String barcode) {
        return new ClientReturnDto(
                new InternalCourierDto(321L, "Курьер с возвратом", null, null, null, null),
                barcode,
                LocalDate.now(clock),
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                null,
                null
        );
    }

    @Test
    void clientReturnFromPvz() {
        ClientReturnDto clientReturnDto = getClientReturnDto("VOZVRAT_SF_PVZ_3456");
        performClientReturn(clientReturnDto);
    }

    @SneakyThrows
    private void performClientReturn(ClientReturnDto clientReturnDto) {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/internal/courier/clientReturn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(clientReturnDto)))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void clientReturnWithUnmatchedBarcode() {
        ClientReturnDto clientReturnDto = getClientReturnDto("VOZVRAT_NOT_CORRECT_3456");
        mockMvc.perform(
                MockMvcRequestBuilders.post("/internal/courier/clientReturn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(clientReturnDto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void successClientReturnWithoutWarehouse() {
        ClientReturnDto clientReturnDto = getClientReturnDto("VOZVRAT_SF_PVZ_3456");
        clientReturnDto.setWarehouse(null);
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/internal/courier/clientReturn")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JacksonUtil.toString(clientReturnDto)))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void successFbsClientReturnWithWarehouse() {
        ClientReturnDto clientReturnDto = getClientReturnDto("VOZVRAT_TAR_3456");
        WarehouseDto warehouse = WarehouseDto.builder()
                .yandexId("yandex-id")
                .partnerId("partner-id")
                .location(
                        LocationDto.builder()
                                .country("country")
                                .locality("locality")
                                .region("region")
                                .locationId(1L)
                                .zipCode("123456")
                                .housing("12")
                                .street("street")
                                .build()
                )
                .incorporation("incorp")
                .phones(List.of())
                .contact(ContactDto.builder().build())
                .shopId("123")
                .instruction("")
                .build();
        clientReturnDto.setWarehouse(warehouse);
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/internal/courier/clientReturn")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JacksonUtil.toString(clientReturnDto)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "externalId": "VOZVRAT_TAR_3456"
                        }""", false
                ));
        assertThat(
                scOrderRepository.findBySortingCenterAndExternalId(sortingCenter, "VOZVRAT_TAR_3456")
                        .get()
                        .getWarehouseReturnYandexId()
                        .get()
        )
                .isEqualTo("yandex-id");
    }

    @Test
    @SneakyThrows
    void batchUpdateOrders() {
        var order = testFactory.createOrder(sortingCenter).get();
        var request = new BatchUpdateOrdersRequest(
                sortingCenter.getId(),
                LocalDate.now(clock),
                List.of(new InternalCourierDto(9L, "Вася", null, null, null, "ООО Мой курьер")),
                1L,
                0L,
                1L,
                Map.of(9L, List.of(order.getExternalId())), sortingCenter.getToken(),
                null
        );
        performBatchUpdateOrder(request);
        verify(routeCellsCleanupService, times(1)).removeCellsFromShippedOutgoingRoutes(sortingCenter);
    }

    @Test
    void batchUpdateOrdersMultiplePartitions() {
        long partitionCount = 3;
        for (long i = 0; i < partitionCount; i++) {
            OrderLike order = testFactory.createOrder(sortingCenter).get();
            BatchUpdateOrdersRequest request = new BatchUpdateOrdersRequest(
                    sortingCenter.getId(),
                    LocalDate.now(clock),
                    List.of(new InternalCourierDto(9L, "Вася", null, null, null, "ООО Мой курьер")),
                    1L,
                    i,
                    partitionCount,
                    Map.of(9L, List.of(order.getExternalId())), sortingCenter.getToken(),
                    null
            );
            performBatchUpdateOrder(request);
            verify(routeCellsCleanupService, times(1)).removeCellsFromShippedOutgoingRoutes(sortingCenter);
        }
    }


    @Test
    void batchUpdateOrders_sortBefore12() {
        var order = testFactory.createOrder(sortingCenter).get();
        LocalDate today = LocalDate.now(clock);
        LocalDate tomorrow = today.plusDays(1);
        var request = new BatchUpdateOrdersRequest(
                sortingCenter.getId(),
                tomorrow, // tomorrow
                List.of(new InternalCourierDto(9L, "Вася", null, null, null, "ООО Мой курьер")),
                1L,
                0L,
                1L,
                Map.of(9L, List.of(order.getExternalId())), sortingCenter.getToken(),
                null
        );
        performBatchUpdateOrder(request);

        Route route = testFactory.findOutgoingCourierRoute(order).orElseThrow();

        List<Cell> tomorrowCells = route.getCells(tomorrow);
        assertThat(tomorrowCells).hasSize(1);

        // в этом суть сортировки до 12 - привязываем завтрашний маршрут на сегодня
        List<Cell> todayCells = route.getCells(today);
        assertThat(todayCells).hasSize(1);

        assertThat(todayCells.get(0)).isSameAs(tomorrowCells.get(0));
    }

    @SneakyThrows
    private void performBatchUpdateOrder(BatchUpdateOrdersRequest request) {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/internal/courier/batchUpdateOrders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(request)))
                .andExpect(status().isOk());
    }

}
