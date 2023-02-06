package ru.yandex.market.sc.internal.controller.partner;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.stage.Stages;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.controller.dto.PartnerSortableTypeDto;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerSortableControllerTest {

    private final TestFactory testFactory;
    private final ScIntControllerCaller caller;
    private final Clock clock;

    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    @DisplayName("Лимитирование выборки без фильтров")
    void getSortableReportWithoutFilters() {
        testFactory.createForToday(order(sortingCenter, "o2")
                        .places("o2-1", "o2-2", "o2-3", "o2-4", "o2-5", "o2-6", "o2-7").build())
                .cancel()
                .acceptPlaces("o2-2", "o2-3")
                .sortPlace("o2-3")
                .enableSortMiddleMileToLot()
                .sortPlaceToLot("SC_LOT_1", SortableType.PALLET, "o2-2");

        caller.getSortableReport(sortingCenter.getPartnerId(), null, PageRequest.of(0, 5))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*]", hasSize(5)));
    }

    @Test
    @Disabled
    @DisplayName("Проверка размера страницы")
    void checkPageableSortableReport() {
        caller.getSortableReport(sortingCenter.getPartnerId(), null)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*]", hasSize(20)))
                .andExpect(jsonPath("$.pageable.sort.sorted", is(true)))
                .andExpect(jsonPath("$.pageable.offset", is(0)))
                .andExpect(jsonPath("$.pageable.pageNumber", is(0)))
                .andExpect(jsonPath("$.pageable.pageSize", is(20)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.totalElements", is(30)));
    }

    @Nested
    @DisplayName("Проверка фильтров на странице отчета")
    class SortableReportFilter {

        @Test
        @DisplayName("Фильтрация по коду посылки")
        void checkPlaceCodeFilter() {
            testFactory.createForToday(order(sortingCenter, "o1").places("o1-1", "o1-2", "o1-3").build())
                    .cancel().get();
            caller.getSortableReport(sortingCenter.getPartnerId(), "sortableBarcode=o1-2")
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].sortableBarcode", is("o1-2")))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @DisplayName("Фильтрация по типу грузоместа")
        void checkSortableTypeFilter() {
            testFactory.createForToday(order(sortingCenter, "o1").places("o1-1", "o1-2", "o1-3").build())
                    .cancel().get();
            caller.getSortableReport(sortingCenter.getPartnerId(), "sortableTypes=PLACE")
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(3)))
                    .andExpect(jsonPath("$.totalElements", is(3)));
        }

        @Test
        @DisplayName("Фильтрация по имени ячейки")
        void checkCellNameFilter() {
            var courier = testFactory.storedCourier(2, "Ololosha");
            var courierCell = testFactory.storedCell(sortingCenter, "cellOlolosha", CellType.COURIER, courier.getId());
            testFactory.createForToday(order(sortingCenter, "o3")
                            .places(IntStream.range(0, 4).mapToObj(String::valueOf).toList())
                            .build())
                    .updateCourier(courier)
                    .acceptPlaces().sortPlaces().get();

            testFactory.createForToday(order(sortingCenter, "o4").places("o4-1", "o4-2").build())
                    .cancel().get();

            caller.getSortableReport(sortingCenter.getPartnerId(),
                            "cellName=" + courierCell.getCellName().orElseThrow())
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(4)))
                    .andExpect(jsonPath("$.totalElements", is(4)));
        }

        @Test
        @DisplayName("Фильтрация по адресу ячейки (зона)")
        @Disabled
            // TODO поправить ошибку сортировки
        void checkCellAddressByZoneIdFilter() {
            Cell cell = prepareOrderCellAddress("Ololosha");
            prepareOrderCellAddress("Ololoyka");

            caller.getSortableReport(sortingCenter.getPartnerId(),
                            "cellZoneId=" + Objects.requireNonNull(cell.getZone()).getId())
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(3)))
                    .andExpect(jsonPath("$.totalElements", is(3)));
        }

        @Test
        @DisplayName("Фильтрация по адресу ячейки (уровень)")
        @Disabled
            // TODO поправить ошибку сортировки
        void checkCellAddressByLevelNumberFilter() {
            Cell cell = prepareOrderCellAddress("Ololosha");
            prepareOrderCellAddress("Ololoyka");

            caller.getSortableReport(sortingCenter.getPartnerId(),
                            "cellLevelNumber=" + cell.getLevelNumber())
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(3)))
                    .andExpect(jsonPath("$.totalElements", is(3)));
        }

        @Test
        @DisplayName("Фильтрация по адресу ячейки (аллея)")
        @Disabled
            // TODO поправить ошибку сортировки
        void checkCellAddressByAlleyNumberFilter() {
            Cell cell = prepareOrderCellAddress("Ololosha");
            prepareOrderCellAddress("Ololoyka");

            caller.getSortableReport(sortingCenter.getPartnerId(),
                            "cellLevelNumber=" + cell.getAlleyNumber())
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(3)))
                    .andExpect(jsonPath("$.totalElements", is(3)));
        }

        @Test
        @DisplayName("Фильтрация по адресу ячейки (секция)")
        @Disabled
            // TODO поправить ошибку сортировки
        void checkCellAddressBySectionNumberFilter() {
            Cell cell = prepareOrderCellAddress("Ololosha");
            prepareOrderCellAddress("Ololoyka");

            caller.getSortableReport(sortingCenter.getPartnerId(),
                            "cellLevelNumber=" + cell.getSectionNumber())
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(3)))
                    .andExpect(jsonPath("$.totalElements", is(3)));
        }

        private Cell prepareOrderCellAddress(String merchant) {
            var zone = testFactory.storedZone(sortingCenter, "zone" + merchant);
            var warehouse = testFactory.storedWarehouse("warehouse-" + merchant);
            var cell = testFactory.storedCell(sortingCenter, "cell-" + merchant,
                    CellType.RETURN, CellSubType.DEFAULT, warehouse.getYandexId(), zone);
            testFactory.createForToday(order(sortingCenter, "o5-" + merchant)
                            .places("o5-1" + merchant, "o5-2" + merchant, "o5-3" + merchant)
                            .warehouseReturnId(String.valueOf(warehouse.getId()))
                            .build())
                    .acceptPlaces().sortPlaces().shipPlaces()
                    .makeReturn()
                    .acceptPlaces()
                    .sortPlace("o5-1" + merchant, cell.getId())
                    .sortPlace("o5-2" + merchant, cell.getId())
                    .sortPlace("o5-3" + merchant, cell.getId())
                    .get();
            return cell;
        }

        @Test
        @DisplayName("Фильтрация по номеру заказа")
        void checkOrderExternalIdFilter() {
            var o = testFactory.createForToday(order(sortingCenter, "o5")
                            .places(IntStream.range(0, 4).mapToObj(String::valueOf).toList())
                            .build())
                    .acceptPlaces().sortPlaces().get();
            testFactory.createForToday(order(sortingCenter, "o6")
                            .places(IntStream.range(0, 2).mapToObj(String::valueOf).toList())
                            .build())
                    .acceptPlaces().sortPlaces().get();

            caller.getSortableReport(sortingCenter.getPartnerId(), "orderExternalId=" + o.getExternalId())
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(4)))
                    .andExpect(jsonPath("$.totalElements", is(4)));
        }

        @Test
        @DisplayName("Фильтрация по sortable статусу")
        void checkSortableStatusFilter() {
            var o = testFactory.createForToday(order(sortingCenter, "o6")
                            .places(IntStream.range(0, 4).mapToObj(String::valueOf).toList())
                            .build())
                    .cancel()
                    .acceptPlaces()
                    .sortPlaces("2")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot("SC_LOT_1", SortableType.PALLET, "3")
                    .prepareToShipLot()
                    .shipLot()
                    .get();

            caller.getSortableReport(sortingCenter.getPartnerId(), "sortableStatuses=" + SortableStatus.SORTED_RETURN +
                            "&sortableStatuses=" + SortableStatus.SHIPPED_RETURN)
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(2)));
        }

        @Test
        @DisplayName("Фильтрация по расширенному статусу (стейджу)")
        void checkStageFilter() {
            testFactory.createForToday(order(sortingCenter, "o6")
                            .places(IntStream.range(0, 5).mapToObj(String::valueOf).toList())
                            .build())
                    .cancel()
                    .acceptPlaces()
                    .sortPlaces("0")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot("SC_LOT_1", SortableType.PALLET, "4")
                    .prepareToShipLot()
                    .shipLot()
                    .get();

            caller.getSortableReport(sortingCenter.getPartnerId(),
                            "stages=" + Stages.SORTED_RETURN + "&stages=" + Stages.SHIPPED_RETURN)
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(2)));
        }

        @Test
        @DisplayName("Фильтрация по дате начала/завершения приемки")
        void checkArrivedDateFromToFilter1() {
            testFactory.createForToday(order(sortingCenter, "o6").places("o6-1", "o6-2", "o6-3").build())
                    .acceptPlaces("o6-2")
                    .get();
            caller.getSortableReport(sortingCenter.getPartnerId(),
                            "arrivedDateFrom=" + LocalDate.now(clock).minusDays(1) +
                                    "&arrivedDateTo=" + LocalDate.now(clock).plusDays(1))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].sortableBarcode", is("o6-2")))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @DisplayName("Фильтрация по дате начала/завершения приемки")
        void checkArrivedDateFromToFilter2() {
            testFactory.createForToday(order(sortingCenter, "o6").places("o6-1", "o6-2", "o6-3").build())
                    .acceptPlaces("o6-2")
                    .get();
            caller.getSortableReport(sortingCenter.getPartnerId(),
                            "arrivedDateFrom=" + LocalDate.now(clock) +
                                    "&arrivedDateTo=" + LocalDate.now(clock))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].sortableBarcode", is("o6-2")))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @Disabled
        @DisplayName("Фильтрация по дате начала/завершения отгрузки")
        void checkShippedDateFromToFilter() {
            var o = testFactory.createForToday(order(sortingCenter, "o7")
                            .dsType(DeliveryServiceType.TRANSIT)
                            .places("o7-1", "o7-2", "o7-3")
                            .build())
                    .cancel()
                    .acceptPlaces("o7-1", "o7-3")
                    .enableSortMiddleMileToLot()
                    .sortPlaceToLot("SC_LOT_1", SortableType.PALLET, "o7-1", "o7-3")
                    .prepareToShipLot(1)
                    .get();
            var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(o))
                    .orElseThrow();
            testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);
            caller.getSortableReport(sortingCenter.getPartnerId(),
                            "shippedDateFrom=" + LocalDate.now(clock).minusDays(1) +
                                    "&shippedDateTo=" + LocalDate.now(clock).plusDays(1))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(2)))
                    .andExpect(jsonPath("$.content[*].sortableBarcode", containsInAnyOrder("o7-1", "o7-3")))
                    .andExpect(jsonPath("$.totalElements", is(2)));
        }

        @Test
        @DisplayName("Фильтрация по курьеру")
        void checkCourierFilter() {
            var courier = testFactory.storedCourier(13L, "Ololosha");
            testFactory.createForToday(order(sortingCenter, "o7")
                            .places(IntStream.range(0, 4).mapToObj(String::valueOf).toList())
                            .build())
                    .updateCourier(courier)
                    .acceptPlaces().get();

            testFactory.createForToday(order(sortingCenter, "o8")
                            .places(IntStream.range(0, 6).mapToObj(String::valueOf).toList())
                            .build())
                    .acceptPlaces().get();

            caller.getSortableReport(sortingCenter.getPartnerId(), "courierId=" + courier.getId())
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(4)))
                    .andExpect(jsonPath("$.totalElements", is(4)));
        }

        @Test
        @DisplayName("Фильтрация по складу поставки")
        void checkWarehouseFromFilter() {
            testFactory.createForToday(order(sortingCenter, "o11").places("o11-1", "o11-2", "o11-3").build()).get();
            var warehouseFrom = testFactory.storedWarehouse("Tarniy");
            testFactory.createForToday(order(sortingCenter, "o12")
                            .places("o12-1", "o12-2", "o12-3")
                            .warehouseFromId(warehouseFrom.getYandexId())
                            .build())
                    .cancel()
                    .acceptPlaces().get();

            caller.getSortableReport(sortingCenter.getPartnerId(), "warehouseFromId=" + warehouseFrom.getId())
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(3)))
                    .andExpect(jsonPath("$.content[*].sortableBarcode", containsInAnyOrder("o12-1", "o12-2", "o12-3")))
                    .andExpect(jsonPath("$.totalElements", is(3)));
        }

        @Test
        @DisplayName("Фильтрация по складу возврата")
        void checkWarehouseReturnFilter() {
            var courier = testFactory.storedCourier(13L);
            testFactory.createForToday(order(sortingCenter, "o9")
                            .places("o10-1", "o10-2", "o10-3")
                            .build())
                    .updateCourier(courier)
                    .acceptPlaces().get();

            var warehouseReturn = testFactory.storedWarehouse("Sofino");
            testFactory.createForToday(order(sortingCenter, "o10")
                            .places("o10-1", "o10-2")
                            .warehouseReturnId(warehouseReturn.getYandexId())
                            .build())
                    .cancel()
                    .acceptPlaces().get();

            caller.getSortableReport(sortingCenter.getPartnerId(), "warehouseReturnId=" + warehouseReturn.getId())
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*]", hasSize(2)))
                    .andExpect(jsonPath("$.content[*].sortableBarcode", containsInAnyOrder("o10-1", "o10-2")))
                    .andExpect(jsonPath("$.totalElements", is(2)));
        }
    }

    @Test
    void getSortablesSortByCellAddress() {
        var courier1 = testFactory.storedCourier(1);
        var courier2 = testFactory.storedCourier(2);
        var courier3 = testFactory.storedCourier(3);
        var courier4 = testFactory.storedCourier(4);
        var courier5 = testFactory.storedCourier(5);
        var courier6 = testFactory.storedCourier(6);
        var zoneA = testFactory.storedZone(sortingCenter, "A");
        var zoneI = testFactory.storedZone(sortingCenter, "I");
        var cell1 = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);
        testFactory.createForToday(order(sortingCenter, "o1").places("o1-1").build())
                .acceptPlaces("o1-1").sortPlaces(cell1.getId(), "o1-1")
                .get();

        var cell2 = testFactory.storedCell(sortingCenter, "c-2", CellType.COURIER, CellSubType.DEFAULT,
                courier2, zoneA, 3L, 4L, 5L, 1L);

        testFactory.createForToday(order(sortingCenter, "o2").places("o2-2").build()).updateCourier(courier2)
                .acceptPlaces("o2-2").sortPlaces(cell2.getId(), ("o2-2"))
                .get();

        var cell3 = testFactory.storedCell(sortingCenter, "c-3", CellType.COURIER, CellSubType.DEFAULT,
                courier3, zoneI, 1L, 2L, 1L, 1L);

        testFactory.createForToday(order(sortingCenter, "o3").places("o3-1", "o3-2").build()).updateCourier(courier3)
                .acceptPlaces("o3-1").sortPlaces(cell3.getId(), ("o3-1"))
                .get();

        var cell4 = testFactory.storedCell(sortingCenter, "c-4", CellType.COURIER, CellSubType.DEFAULT,
                courier4, zoneA, 1L, 4L, 3L, 1L);

        testFactory.createForToday(order(sortingCenter, "o4").places("o4-1").build()).updateCourier(courier4)
                .acceptPlaces("o4-1").sortPlaces(cell4.getId(), "o4-1")
                .get();

        var cell5 = testFactory.storedCell(sortingCenter, "c-5", CellType.COURIER, CellSubType.DEFAULT,
                courier5, zoneA, 1L, 1L, 1L, 1L);

        testFactory.createForToday(order(sortingCenter, "o5").places("o5-1").build()).updateCourier(courier5)
                .acceptPlaces("o5-1").sortPlaces(cell5.getId(), "o5-1")
                .get();

        var cell6 = testFactory.storedCell(sortingCenter, "c-6", CellType.COURIER, CellSubType.DEFAULT,
                courier6, zoneA, 1L, 4L, 2L, 1L);

        testFactory.createForToday(order(sortingCenter, "o6").places("o6-1", "o6-2").build()).updateCourier(courier6)
                .acceptPlaces("o6-1").sortPlaces(cell6.getId(), ("o6-1"))
                .get();

        //asc
        caller.getSortableReport(sortingCenter.getPartnerId(), "sort=cellAddress,asc")
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*]", hasSize(8)))
                .andExpect(jsonPath("$.content[0].sortableBarcode", is("o5-1")))
                .andExpect(jsonPath("$.content[1].sortableBarcode", is("o6-1")))
                .andExpect(jsonPath("$.content[2].sortableBarcode", is("o4-1")))
                .andExpect(jsonPath("$.content[3].sortableBarcode", is("o2-2")))
                .andExpect(jsonPath("$.content[4].sortableBarcode", is("o3-1")))
                .andExpect(jsonPath("$.content[5].sortableBarcode", in(Set.of("o1-1", "o3-2", "o6-2"))))
                .andExpect(jsonPath("$.content[6].sortableBarcode", in(Set.of("o1-1", "o3-2", "o6-2"))))
                .andExpect(jsonPath("$.content[7].sortableBarcode", in(Set.of("o1-1", "o3-2", "o6-2"))))
                .andExpect(jsonPath("$.totalElements", is(8)));

        //desc
        caller.getSortableReport(sortingCenter.getPartnerId(), "sort=cellAddress,desc")
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*]", hasSize(8)))
                .andExpect(jsonPath("$.content[0].sortableBarcode", in(Set.of("o1-1", "o3-2", "o6-2"))))
                .andExpect(jsonPath("$.content[1].sortableBarcode", in(Set.of("o1-1", "o3-2", "o6-2"))))
                .andExpect(jsonPath("$.content[2].sortableBarcode", in(Set.of("o1-1", "o3-2", "o6-2"))))
                .andExpect(jsonPath("$.content[3].sortableBarcode", is("o3-1")))
                .andExpect(jsonPath("$.content[4].sortableBarcode", is("o2-2")))
                .andExpect(jsonPath("$.content[5].sortableBarcode", is("o4-1")))
                .andExpect(jsonPath("$.content[6].sortableBarcode", is("o6-1")))
                .andExpect(jsonPath("$.content[7].sortableBarcode", is("o5-1")))
                .andExpect(jsonPath("$.totalElements", is(8)));
    }

    @Test
    void checkSortableTypesEndpoint() {
        PartnerSortableTypeDto sortableTypeDto = new PartnerSortableTypeDto(
                List.of(SortableType.PLACE),
                List.of(SortableType.PLACE)
        );
        caller.getSortableTypes(sortingCenter.getId())
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(JacksonUtil.toString(sortableTypeDto)));
    }

    @Test
    void checkSortableStatusesEndpoint() {
        caller.getSortableStatuses(sortingCenter.getId())
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void checkSortableStatisticEndpoint() {
        caller.getSortableStageStatistic(sortingCenter.getPartnerId())
                .andDo(print())
                .andExpect(status().isOk());
    }
}
