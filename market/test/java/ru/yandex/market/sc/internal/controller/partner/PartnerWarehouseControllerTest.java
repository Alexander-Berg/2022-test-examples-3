package ru.yandex.market.sc.internal.controller.partner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.DistributionCenterWarehouse;
import ru.yandex.market.sc.core.domain.sorting_center.repository.DistributionCenterWarehouseRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@ScIntControllerTest
class PartnerWarehouseControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    DistributionCenterWarehouseRepository distributionCenterWarehouseRepository;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void getWarehousesWithoutXdoc() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "false");
        String warehouseYandexId = "123";
        var warehouse = testFactory.storedWarehouse(warehouseYandexId);
        testFactory.create(
                order(sortingCenter).warehouseFromId(warehouseYandexId).warehouseReturnId(warehouseYandexId).build()
        ).get();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/" +
                        sortingCenter.getPartnerId() + "/warehouses"
                )
        )
                .andExpect(content().json("{\"warehouses\":[" + warehouseJson(warehouse) + "]}", true));
    }

    @Test
    void getEmptyWarehouses() throws Exception {
        testFactory.storedWarehouse();
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/" +
                        sortingCenter.getPartnerId() + "/warehouses"
                )
        )
                .andExpect(content().json("{\"warehouses\":[]}", true));
    }

    @Test
    void getWarehousesWithXdoc() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        String warehouseYandexId = "123";
        var warehouse = testFactory.storedWarehouse(warehouseYandexId);
        distributionCenterWarehouseRepository.save(new DistributionCenterWarehouse(sortingCenter.getId(), warehouse.getId()));
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/" +
                        sortingCenter.getPartnerId() + "/warehouses"
                )
        )
                .andExpect(content().json("{\"warehouses\":[" + warehouseJson(warehouse) + "]}", true));
    }

    @Test
    void getWarehousesWithXdocAndOrder() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        String warehouseYandexId = "123";
        var warehouse = testFactory.storedWarehouse(warehouseYandexId);
        var warehouse2 = testFactory.storedWarehouse(warehouseYandexId + "1");
        testFactory.create(
                order(sortingCenter).warehouseFromId(warehouseYandexId).warehouseReturnId(warehouseYandexId).build()
        ).get();
        distributionCenterWarehouseRepository.save(new DistributionCenterWarehouse(sortingCenter.getId(), warehouse2.getId()));
        mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/" +
                        sortingCenter.getPartnerId() + "/warehouses"
                )
        )
                .andExpect(content().json("{\"warehouses\":[" + warehouseJson(warehouse) + "," + warehouseJson(warehouse2) + "]}", true));
    }

    private String warehouseJson(Warehouse warehouse) {
        return "{\"id\":" + warehouse.getId() + "," +
                "\"yandexId\":\"" + warehouse.getYandexId() + "\"," +
                "\"partnerId\":\"" + warehouse.getPartnerId() + "\"," +
                "\"incorporation\":\"" + warehouse.getIncorporation() + "\"}";
    }
}
