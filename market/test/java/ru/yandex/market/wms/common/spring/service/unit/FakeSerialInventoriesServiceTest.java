package ru.yandex.market.wms.common.spring.service.unit;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory;
import ru.yandex.market.wms.common.spring.service.FakeSerialInventoryService;

public class FakeSerialInventoriesServiceTest extends IntegrationTest {

    @Autowired
    private FakeSerialInventoryService service;

    @Test
    @DatabaseSetup("/db/service/serialInventory/fake/before.xml")
    public void findByEanFromIdHappy() {
        var serials = service.getRandomFakeSerialInventoriesByEan("EAN1", "LOC1", "ID01", 1);
        assertions.assertThat(serials.stream().map(SerialInventory::getSerialNumber).toList())
                .containsAnyElementsOf(List.of("001", "002", "003"));
        serials = service.getRandomFakeSerialInventoriesByEan("EAN1", "LOC1", "ID01", 3);
        assertions.assertThat(serials.stream().map(SerialInventory::getSerialNumber).toList())
                .containsExactlyInAnyOrder("001", "002", "003");
    }

    @Test
    @DatabaseSetup("/db/service/serialInventory/fake/before.xml")
    public void findByEanFromLocInconsistentTest() {
        assertions.assertThatThrownBy(() -> service.getRandomFakeSerialInventoriesByEan("EAN2", "PACK1", "ID02", 1))
                .hasMessageContaining("Разные типы уитов в одной таре. Тара 'ID02' ячейка 'PACK1'");
        assertions.assertThatThrownBy(() -> service.getRandomFakeSerialInventoriesByEan("EAN4", "PACK1", "", 1))
                .hasMessageContaining("Для EAN EAN4 найдено более 1 SkuId");
        assertions.assertThatThrownBy(() -> service.getRandomFakeSerialInventoriesByEan("EAN1", "LOC1", "ID01", 10))
                .hasMessageContaining("Недостаточно товара. " +
                        "Запросили 10 шт. есть 3 шт. тара: ID01, ячейка: LOC1, sku: SkuId(storerKey=STORER, sku=SKU1)");
    }

    @Test
    @DatabaseSetup("/db/service/serialInventory/fake/before.xml")
    public void findByEanFromLocEmptyIdHappy() {
        var serials = service.getRandomFakeSerialInventoriesByEan("EAN2", "PACK", "", 1);
        assertions.assertThat(serials.stream().map(SerialInventory::getSerialNumber).toList())
                .containsAnyElementsOf(List.of("004", "005"));
    }
}
