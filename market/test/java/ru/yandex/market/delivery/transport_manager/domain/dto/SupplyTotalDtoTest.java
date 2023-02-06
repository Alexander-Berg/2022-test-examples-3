package ru.yandex.market.delivery.transport_manager.domain.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SupplyTotalDtoTest {

    public static final IncludedSupplyDto SUPPLY_1 = new IncludedSupplyDto(
        1,
        "0001",
        "Зп-0001",
        1,
        2,
        3,
        new BigDecimal(1000),
        "12345",
        IncludedSupplyDto.SupplierType.FIRST_PARTY,
        "ИП Петров",
        BigDecimal.valueOf(3.3),
        true,
        false,
        CalendaringStatus.ACCEPTED_BY_XDOC_SERVICE
    );
    public static final IncludedSupplyDto SUPPLY_2 = new IncludedSupplyDto(
        2,
        "0002",
        "Зп-0002",
        0,
        1,
        10,
        new BigDecimal(500),
        "1",
        IncludedSupplyDto.SupplierType.THIRD_PARTY,
        "Supplier",
        BigDecimal.valueOf(2.5),
        false,
        true,
        CalendaringStatus.ACCEPTED_BY_XDOC_SERVICE
    );

    @Test
    void add() {
        SupplyTotalDto supplyTotalDto = SupplyTotalDto.xdock(CalendaringStatus.CREATED);

        Assertions.assertEquals(0, supplyTotalDto.getPalletsCount());
        Assertions.assertEquals(0, supplyTotalDto.getBoxesCount());
        Assertions.assertEquals(0, supplyTotalDto.getItemsCount());
        Assertions.assertNull(supplyTotalDto.getFfwfId());
        Assertions.assertNull(supplyTotalDto.getServiceRequestId());
        Assertions.assertEquals(new BigDecimal(0), supplyTotalDto.getApproximateVolume());
        Assertions.assertEquals(new BigDecimal("0"), supplyTotalDto.getTotalCost());
        Assertions.assertEquals(Collections.emptyList(), supplyTotalDto.getSupplies());

        supplyTotalDto.add(SUPPLY_1);

        Assertions.assertEquals(1, supplyTotalDto.getPalletsCount());
        Assertions.assertEquals(2, supplyTotalDto.getBoxesCount());
        Assertions.assertEquals(3, supplyTotalDto.getItemsCount());
        Assertions.assertNull(supplyTotalDto.getFfwfId());
        Assertions.assertNull(supplyTotalDto.getServiceRequestId());
        Assertions.assertEquals(new BigDecimal(1000), supplyTotalDto.getApproximateVolume());
        Assertions.assertEquals(new BigDecimal("3.3"), supplyTotalDto.getTotalCost());
        Assertions.assertEquals(List.of(SUPPLY_1), supplyTotalDto.getSupplies());

        supplyTotalDto.add(SUPPLY_2);

        Assertions.assertEquals(1, supplyTotalDto.getPalletsCount());
        Assertions.assertEquals(3, supplyTotalDto.getBoxesCount());
        Assertions.assertEquals(13, supplyTotalDto.getItemsCount());
        Assertions.assertNull(supplyTotalDto.getFfwfId());
        Assertions.assertNull(supplyTotalDto.getServiceRequestId());
        Assertions.assertEquals(new BigDecimal(1500), supplyTotalDto.getApproximateVolume());
        Assertions.assertEquals(new BigDecimal("5.8"), supplyTotalDto.getTotalCost());
        Assertions.assertEquals(List.of(SUPPLY_1, SUPPLY_2), supplyTotalDto.getSupplies());
    }

    @Test
    void addBbxd() {
        SupplyTotalDto supplyTotalDto = SupplyTotalDto.breakBulkXdock(CalendaringStatus.CREATED, 1L, "0001");

        Assertions.assertEquals(0, supplyTotalDto.getPalletsCount());
        Assertions.assertEquals(0, supplyTotalDto.getBoxesCount());
        Assertions.assertEquals(0, supplyTotalDto.getItemsCount());
        Assertions.assertEquals(1L, supplyTotalDto.getFfwfId());
        Assertions.assertEquals("0001", supplyTotalDto.getServiceRequestId());
        Assertions.assertEquals(new BigDecimal(0), supplyTotalDto.getApproximateVolume());
        Assertions.assertEquals(new BigDecimal("0"), supplyTotalDto.getTotalCost());
        Assertions.assertNull(supplyTotalDto.getSupplies());

        supplyTotalDto.add(SUPPLY_1);

        Assertions.assertEquals(1, supplyTotalDto.getPalletsCount());
        Assertions.assertEquals(2, supplyTotalDto.getBoxesCount());
        Assertions.assertEquals(3, supplyTotalDto.getItemsCount());
        Assertions.assertEquals(1L, supplyTotalDto.getFfwfId());
        Assertions.assertEquals("0001", supplyTotalDto.getServiceRequestId());
        Assertions.assertEquals(new BigDecimal(1000), supplyTotalDto.getApproximateVolume());
        Assertions.assertEquals(new BigDecimal("3.3"), supplyTotalDto.getTotalCost());
        Assertions.assertNull(supplyTotalDto.getSupplies());

        supplyTotalDto.add(SUPPLY_2);

        Assertions.assertEquals(1, supplyTotalDto.getPalletsCount());
        Assertions.assertEquals(3, supplyTotalDto.getBoxesCount());
        Assertions.assertEquals(13, supplyTotalDto.getItemsCount());
        Assertions.assertEquals(1L, supplyTotalDto.getFfwfId());
        Assertions.assertEquals("0001", supplyTotalDto.getServiceRequestId());
        Assertions.assertEquals(new BigDecimal(1500), supplyTotalDto.getApproximateVolume());
        Assertions.assertEquals(new BigDecimal("5.8"), supplyTotalDto.getTotalCost());
        Assertions.assertNull(supplyTotalDto.getSupplies());
    }
}
