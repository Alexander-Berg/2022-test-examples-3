package ru.yandex.market.ff4shops.stocks.service;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.model.StocksWarehouseGroupDto;
import ru.yandex.market.ff4shops.api.model.WarehouseDto;
import ru.yandex.market.ff4shops.api.model.WarehouseType;
import ru.yandex.market.ff4shops.api.model.WarehousesAndGroupsDto;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.model.SeekPaging;
import ru.yandex.market.ff4shops.stocks.model.StocksWarehouseGroupPaging;
import ru.yandex.market.ff4shops.stocks.model.WarehouseFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link WarehousesService}.
 */
@DbUnitDataSet(before = "WarehousesServiceTest.before.csv")
public class WarehousesServiceTest extends FunctionalTest {
    private static final WarehouseDto WAREHOUSE_10 = new WarehouseDto()
            .warehouseId(10L)
            .partnerId(110L)
            .name("test")
            .type(WarehouseType.EXPRESS)
            .address("Москва");
    private static final WarehouseDto WAREHOUSE_11 = new WarehouseDto()
            .warehouseId(11L)
            .partnerId(111L)
            .name("test")
            .type(WarehouseType.FBS)
            .address("Питер, Невский проспект");
    private static final WarehouseDto WAREHOUSE_30 = new WarehouseDto()
            .warehouseId(30L)
            .partnerId(114L)
            .name("test")
            .type(WarehouseType.EXPRESS)
            .address("Воронеж");

    @Autowired
    private WarehousesService warehousesService;

    public static Stream<Arguments> testFindWarehousesAndGroupsArguments() {
        return Stream.of(
                // ничего не находим, так как не заданы partnerIds
                Arguments.of(WarehouseFilter.builder().build(), new WarehousesAndGroupsDto(), 10, null, null, false),
                // Фильтруем только по partnerId
                Arguments.of(WarehouseFilter.builder().setPartnerIds(List.of(110L, 111L, 114L)).build(),
                        new WarehousesAndGroupsDto()
                                .addWarehousesItem(WAREHOUSE_30)
                                .addGroupsItem(new StocksWarehouseGroupDto()
                                        .id(1L)
                                        .mainWarehouseId(10L)
                                        .name("Группа 1")
                                        .warehouses(List.of(WAREHOUSE_10, WAREHOUSE_11))),
                        10, null, null, false),
                // Поиск по всем параметрам c числом в качестве подстроки
                Arguments.of(WarehouseFilter.builder().setPartnerIds(List.of(110L, 111L, 114L)).build(),
                        new WarehousesAndGroupsDto()
                                .addWarehousesItem(WAREHOUSE_30)
                                .addGroupsItem(new StocksWarehouseGroupDto()
                                        .id(1L)
                                        .mainWarehouseId(10L)
                                        .name("Группа 1")
                                        .warehouses(List.of(WAREHOUSE_10, WAREHOUSE_11))),
                        10, null, null, false),
                // Поиск по всем параметрам c числом в качестве подстроки
                Arguments.of(WarehouseFilter.builder().setPartnerIds(List.of(110L, 111L, 114L)).setSearchString("30")
                                .setRegionId(213L).setWarehouseType(WarehouseType.EXPRESS).build(),
                        new WarehousesAndGroupsDto()
                                .groups(List.of())
                                .addWarehousesItem(WAREHOUSE_30),
                        10, null, 1L, false),
                // Поиск по всем параметрам
                Arguments.of(WarehouseFilter.builder().setPartnerIds(List.of(110L, 111L, 114L)).setSearchString("test")
                                .setRegionId(213L).setWarehouseType(WarehouseType.EXPRESS).build(),
                        new WarehousesAndGroupsDto()
                                .groups(List.of())
                                .addWarehousesItem(WAREHOUSE_30),
                        1, 1L, 29L, false),
                // Поиск c полным заполнением групп
                Arguments.of(WarehouseFilter.builder().setPartnerIds(List.of(110L, 114L)).build(),
                        new WarehousesAndGroupsDto()
                                .addWarehousesItem(WAREHOUSE_30)
                                .addGroupsItem(new StocksWarehouseGroupDto()
                                        .id(1L)
                                        .mainWarehouseId(10L)
                                        .name("Группа 1")
                                        .warehouses(List.of(WAREHOUSE_10, WAREHOUSE_11))),
                        10, null, null, true)
        );
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("testFindWarehousesAndGroupsArguments")
    void testFindWarehousesAndGroups(WarehouseFilter filter, WarehousesAndGroupsDto expectedDto,
                                     int limit, Long seekGroupId, Long seekWarehouseId, boolean fetchGroupFully) {
        StocksWarehouseGroupPaging paging = seekGroupId == null && seekWarehouseId == null ? null :
                new StocksWarehouseGroupPaging(seekGroupId, seekWarehouseId);
        assertThat(warehousesService.findWarehousesAndGroups(
                filter, SeekPaging.ofKeySet(limit, paging), fetchGroupFully))
                .isEqualTo(expectedDto);
    }
}
