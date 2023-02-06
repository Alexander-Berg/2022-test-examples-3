package ru.yandex.market.ff4shops.repository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.model.entity.WarehouseEntity;
import ru.yandex.market.ff4shops.model.enums.WarehouseShipmentType;
import ru.yandex.market.ff4shops.model.enums.WarehouseType;
import ru.yandex.market.logistics.management.entity.type.ExtendedShipmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.hamcrest.CoreMatchers.hasItems;

public class WarehouseRepositoryTest extends FunctionalTest {

    @Autowired
    private WarehouseRepository warehouseRepository;

    /**
     * Считаем объекты одинаковыми даже если у них разный timestamp
     */
    @Test
    @DbUnitDataSet(before = "WarehouseRepositoryTest.before.csv")
    public void getByIdTest() {
        var warehouseId = 1L;
        var actual = warehouseRepository.findAllById(List.of(warehouseId));
        var expected = List.of(new WarehouseEntity(1L, "test", "city", "address",
                2L, WarehouseType.DROPSHIP, WarehouseShipmentType.EXPRESS, Instant.parse("2022-02-24T05:00:00.000Z")));
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(after = "WarehouseRepositoryTest.before.csv")
    public void insertNewWarehouse() {
        var expected = new WarehouseEntity(1L, "test", "city", "address",
                2L, WarehouseType.DROPSHIP, WarehouseShipmentType.EXPRESS,
                Instant.parse("2022-02-24T05:00:00.000Z"));
        warehouseRepository.save(expected);
    }

    @Test
    @DbUnitDataSet(before = "WarehouseRepositoryTest.before.csv",
            after = "WarehouseRepositoryTest.after.csv" )
    public void updateWarehouse() {
        var expected = new WarehouseEntity(1L, "newtest", "city", "address",
                2L, WarehouseType.DROPSHIP, WarehouseShipmentType.EXPRESS,
                Instant.parse("2021-02-24T05:00:00.000Z"));
        warehouseRepository.save(expected);
    }

    /**
     * Если у логистики поменяеться контракт для нужных нам типов, то этот тест начнет падать
     */
    @Test
    public void logisticEnumTypeHaveExpectedValues() {
       var logisticValues = Arrays.stream(PartnerType.values()).collect(Collectors.toList());
       MatcherAssert.assertThat(logisticValues, hasItems(PartnerType.DROPSHIP,
               PartnerType.DROPSHIP_BY_SELLER, PartnerType.SUPPLIER));
    }

    /**
     * Если у логистики поменяеться контракт для нужных нам типов, то этот тест начнет падать
     */
    @Test
    public void logisticEnumShipmentHaveExpectedValues() {
        var logisticValues = Arrays.stream(ExtendedShipmentType.values()).collect(Collectors.toList());
        MatcherAssert.assertThat(logisticValues, hasItems(ExtendedShipmentType.EXPRESS,
                ExtendedShipmentType.IMPORT, ExtendedShipmentType.WITHDRAW));
    }
}
