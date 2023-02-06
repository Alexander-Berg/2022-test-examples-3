package ru.yandex.market.sc.core.domain.warehouse.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.fulfillment.ReturnType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
class WarehouseMapperTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    ConfigurationService configurationService;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void createWarehouseWithShopType() {
        String warehouseYandexId = "my_shop";
        var order = testFactory.create(
                order(sortingCenter)
                        .warehouseReturnId(warehouseYandexId)
                        .warehouseReturnType(ReturnType.SHOP)
                        .build()).get();
        assertThat(order.getWarehouseReturn().getType()).isEqualTo(WarehouseType.SHOP);
    }

    @Test
    void createWarehouseWithWarehouseType() {
        String warehouseYandexId = "my_warehouse";
        testFactory.setWarehouseProperty(warehouseYandexId, WarehouseProperty.IS_WAREHOUSE, "true");
        var order  = testFactory.create(
                order(sortingCenter).warehouseReturnId(warehouseYandexId).build()).get();
        assertThat(order.getWarehouseReturn().getType()).isEqualTo(WarehouseType.SORTING_CENTER);
    }

    @Test
    void createWarehouseWithDropoffType() {
        var order = testFactory.create(
                order(sortingCenter).warehouseReturnId("my_dropoff").warehouseReturnType(ReturnType.DROPOFF).build()).get();
        assertThat(order.getWarehouseReturn().getType()).isEqualTo(WarehouseType.DROPOFF);
    }

}
