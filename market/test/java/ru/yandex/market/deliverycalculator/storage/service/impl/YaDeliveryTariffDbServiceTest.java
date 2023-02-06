package ru.yandex.market.deliverycalculator.storage.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffSource;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffUpdatedInfo;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.YaDeliveryTariffFilter;
import ru.yandex.market.deliverycalculator.storage.service.YaDeliveryTariffDbService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YaDeliveryTariffDbServiceTest extends FunctionalTest {
    @Autowired
    private YaDeliveryTariffDbService yaDeliveryTariffDbService;

    @Test
    @DbUnitDataSet(before = "yaDeliveryTariffDbService.before.csv")
    void getYaDeliveryTariffUpdatedInfoIfWasChangedTest() {
        assertTrue(yaDeliveryTariffDbService.getYaDeliveryTariffUpdatedInfoIfWasChanged(2142L, null).isPresent());
        assertTrue(yaDeliveryTariffDbService.getYaDeliveryTariffUpdatedInfoIfWasChanged(6691L,
                LocalDate.of(2019, 10, 8).atStartOfDay(ZoneId.systemDefault()).toInstant()).isPresent());
        assertFalse(yaDeliveryTariffDbService.getYaDeliveryTariffUpdatedInfoIfWasChanged(6691L,
                LocalDate.of(2019, 10, 9).atStartOfDay(ZoneId.systemDefault()).toInstant()).isPresent());

    }

    @Test
    @DbUnitDataSet(before = "yaDeliveryTariffDbService.before.csv")
    void baseFilterTest() {
        //  общее количество записей
        assertEquals(96L, yaDeliveryTariffDbService.count(YaDeliveryTariffFilter.builder().build()));
    }

    @Test
    @DbUnitDataSet(before = "yaDeliveryTariffDbService.before.csv")
    void blueFilterTest() {
        // все синие тарифы
        assertEquals(60L, yaDeliveryTariffDbService.count(YaDeliveryTariffFilter.builder()
                .programType(DeliveryTariffProgramType.MARKET_DELIVERY)
                .build()));

        // доставка в точки вывоза, синий маркет
        Set<Long> res = yaDeliveryTariffDbService.searchTariff(YaDeliveryTariffFilter.BLUE_PICKUP_FILTER)
                .stream().map(YaDeliveryTariffUpdatedInfo::getId).collect(Collectors.toSet());
        assertEquals(Sets.newSet(5993L, 4781L, 4829L, 6694L, 5459L, 6700L, 4897L, 6481L, 6229L, 5470L), res);

        // почта, синий маркет
        res = yaDeliveryTariffDbService.searchTariff(YaDeliveryTariffFilter.BLUE_POST_FILTER)
                .stream().map(YaDeliveryTariffUpdatedInfo::getId).collect(Collectors.toSet());
        assertEquals(Sets.newSet(4841L, 6490L), res);
    }

    @Test
    @DbUnitDataSet(before = "getTariffIds.before.csv")
    void getTariffIds() {
        List<Long> yadoTariffIds = yaDeliveryTariffDbService.searchTariffIds(YaDeliveryTariffFilter.builder()
                .sourceType(DeliveryTariffSource.YADO).build());
        List<Long> daasTariffIds = yaDeliveryTariffDbService.searchTariffIds(YaDeliveryTariffFilter.builder()
                .sourceType(DeliveryTariffSource.DAAS).build());

        Assertions.assertEquals(1, yadoTariffIds.size());
        Assertions.assertEquals(1, daasTariffIds.size());
        Assertions.assertEquals(1234, yadoTariffIds.get(0));
        Assertions.assertEquals(1235, daasTariffIds.get(0));
    }
}
