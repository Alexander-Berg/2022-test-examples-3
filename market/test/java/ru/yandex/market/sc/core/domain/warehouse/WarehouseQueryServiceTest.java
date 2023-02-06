package ru.yandex.market.sc.core.domain.warehouse;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author: dbryndin
 * @date: 8/27/21
 */
@EmbeddedDbTest
public class WarehouseQueryServiceTest {

    @Autowired
    WarehouseQueryService warehouseQueryService;
    @Autowired
    TestFactory testFactory;

    SortingCenter sortingCenter;


    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    @DisplayName("success название дроповоф должно содержать shopId и адресс по шаблону " +
            "{<shopId> <whname> <address>} ")
    void prepareDropoffNameWhiShopIdAndAddress() {
        var expetWh0NameWithAddr = "warehouse-with-address11 ООО Ромашка-Склад (Ярославская обл, ул Ленина, 11)";
        var expetWh1NameWithAddr = "warehouse-with-address22 ООО Ромашка-Склад (Ярославская обл, ул Ленина, 11)";
        var wh0 = testFactory.storedWarehouse("warehouse-with-address11", true);
        var wh1 = testFactory.storedWarehouse("warehouse-with-address22", true);
        var wh2 = testFactory.storedWarehouse("warehouse-without-address33", false);
        var whIdToWhName = warehouseQueryService.prepareWhNameWithAddress(List.of(wh0.getId(),
                wh1.getId()));
        assertEquals(expetWh0NameWithAddr, whIdToWhName.get(wh0.getId()));
        assertEquals(expetWh1NameWithAddr, whIdToWhName.get(wh1.getId()));
        assertNull(whIdToWhName.get(wh2.getId()));
    }

    @Test
    @DisplayName("success название для дроповоф должно содержать  адресс по шаблону {<whname> <address>}")
    void prepareDropoffNameWithAddress() {
        var expetWh0NameWithAddr = "ООО Ромашка-Склад (Ярославская обл, ул Ленина, 11)";
        var expetWh1NameWithAddr = "ООО Ромашка-Склад (Ярославская обл, ул Ленина, 11)";
        var expetWh2NameWithAddr = "ООО Ромашка-Склад";
        var wh0 = testFactory.storedWarehouse("warehouse-with-address11", true);
        var wh1 = testFactory.storedWarehouse("warehouse-with-address22", true);
        var wh2 = testFactory.storedWarehouse("warehouse-without-address33", false);

        testFactory.createOrder(order(sortingCenter).warehouseReturnId(wh0.getYandexId()).externalId("o1").build());
        testFactory.createOrder(order(sortingCenter).warehouseReturnId(wh1.getYandexId()).externalId("o2").build());
        testFactory.createOrder(order(sortingCenter).warehouseReturnId(wh2.getYandexId()).externalId("o3").build());
        var warehouseDtos = warehouseQueryService.findByScId(sortingCenter.getId());
        assertEquals(expetWh0NameWithAddr,
                warehouseDtos.stream()
                        .filter(w -> wh0.getId() == w.getId())
                        .findFirst().get().getIncorporation());
        assertEquals(expetWh1NameWithAddr,
                warehouseDtos.stream()
                        .filter(w -> wh1.getId() == w.getId())
                        .findFirst().get().getIncorporation());
        assertEquals(expetWh2NameWithAddr,
                warehouseDtos.stream()
                        .filter(w -> wh2.getId() == w.getId())
                        .findFirst().get().getIncorporation());
    }

    @Test
    @DisplayName("success тест алгоритма определения мерча")
    void warehouseIsDropship() {
        var warehouse = testFactory.storedWarehouse(TestFactory.warehouse("1", "1", WarehouseType.DROPOFF,
                "sameLp"));
        var warehouseN = testFactory.storedWarehouse(TestFactory.warehouse("2", "2", WarehouseType.SORTING_CENTER,
                "otherLp"));
        assertFalse(warehouseQueryService.warehouseIsDropship(warehouse, null));
        assertFalse(warehouseQueryService.warehouseIsDropship(warehouseN, null));

        var warehouseSame = testFactory.storedWarehouse(TestFactory.warehouse("3", "3", WarehouseType.SHOP, "sameLp"));

        assertTrue(warehouseQueryService.warehouseIsDropship(warehouse, null));
        assertTrue(warehouseQueryService.warehouseIsDropship(warehouseSame, null));
        assertFalse(warehouseQueryService.warehouseIsDropship(warehouseN, null));
    }

}
