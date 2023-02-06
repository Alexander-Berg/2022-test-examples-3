package ru.yandex.market.billing.imports.warehouse;

import java.time.LocalDate;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ExtendWith(MockitoExtension.class)
public class WarehouseInfoImportFromYtTest extends FunctionalTest {

    @Autowired
    private WarehouseInfoDao warehouseInfoDao;

    @Mock
    private WarehouseInfoYTDao warehouseInfoYTDao;

    private WarehouseInfoImportService warehouseInfoImportService;

    private static final LocalDate DATE_2022_01_01 =  LocalDate.of(2022, 01, 01);

    @BeforeEach
    public void setup() {
        Mockito.when(warehouseInfoYTDao.getWarehouseInfos()).thenReturn(
                List.of(
                        WarehouseInfo.builder()
                                .setWarehouseId(48037L)
                                .setWarehouseName("MirLen Home")
                                .setWarehouseType("SUPPLIER")
                                .setRegionId(21651L)
                                .build()
                )
        );
        warehouseInfoImportService = new WarehouseInfoImportService(warehouseInfoYTDao, warehouseInfoDao);
    }

    @Test
    @DisplayName("Тестирование импорта из YT")
    @DbUnitDataSet(
            before = "csv/WarehouseInfoImportFromYtTest.yt.before.csv",
            after = "csv/WarehouseInfoImportFromYtTest.yt.after.csv"
    )
    public void importFromYtTest() {
        warehouseInfoImportService.importWarehouseInfos(DATE_2022_01_01);
    }
}
