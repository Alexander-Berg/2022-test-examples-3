package ru.yandex.market.billing.imports.warehouse;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для {@link WarehouseInfoImportServiceTest}.
 */
public class WarehouseInfoImportServiceTest extends FunctionalTest {

    @Autowired
    private WarehouseInfoImportService warehouseInfoImportService;

    @DbUnitDataSet(
            before = "csv/WarehouseInfoImportServiceTest.InsertTest.before.csv",
            after = "csv/WarehouseInfoImportServiceTest.InsertTest.after.csv"
    )
    @DisplayName("Проверяем, что импортируются данные складов.")
    @Test
    void test_shouldInsertWarehouseInfo() {
        warehouseInfoImportService.persistWarehouseInfos(
                List.of(
                        WarehouseInfo.builder()
                                .setWarehouseId(172L)
                                .setWarehouseName("MARKET_MSK_SOF")
                                .setWarehouseType("FULFILLMENT")
                                .setRegionId(120013L)
                                .build(),
                        WarehouseInfo.builder()
                                .setWarehouseId(145L)
                                .setWarehouseName("MarschrouteFF")
                                .setWarehouseType("FULFILLMENT")
                                .setRegionId(21651L)
                                .build(),
                        WarehouseInfo.builder()
                                .setWarehouseId(164L)
                                .setWarehouseName("DROPSHIP_HOLODILNIK_LA_HA")
                                .setWarehouseType("DROPSHIP")
                                .setRegionId(120398L)
                                .build()
                )
        );
    }

    @DbUnitDataSet(
            before = "csv/WarehouseInfoImportServiceTest.UpdateTest.before.csv",
            after = "csv/WarehouseInfoImportServiceTest.UpdateTest.after.csv"
    )
    @DisplayName("Проверяем, что обновляется информация складов.")
    @Test
    void test_shouldUpdateWarehouseInfo() {
        warehouseInfoImportService.persistWarehouseInfos(
                List.of(
                        /* Должен измениться регион */
                        WarehouseInfo.builder()
                                .setWarehouseId(172L)
                                .setWarehouseName("MARKET_MSK_SOF")
                                .setWarehouseType("FULFILLMENT")
                                .setRegionId(120013L)
                                .build(),
                        /* Должно произойти ничего */
                        WarehouseInfo.builder()
                                .setWarehouseId(145L)
                                .setWarehouseName("MarschrouteFF")
                                .setWarehouseType("FULFILLMENT")
                                .setRegionId(21651L)
                                .build()
                        /* Третий склад просто останется неизменным */
                )
        );
    }


}
