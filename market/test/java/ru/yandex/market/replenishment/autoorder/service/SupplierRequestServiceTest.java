package ru.yandex.market.replenishment.autoorder.service;


import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.api.dto.SupplierResponseDTO;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
public class SupplierRequestServiceTest extends FunctionalTest {

    @Autowired
    private SupplierRequestService supplierRequestService;

    @Test
    @DbUnitDataSet(before = "SupplierRequestServiceTest_updateStatuses.before.csv",
            after = "SupplierRequestServiceTest_updateStatuses.after.csv")
    public void updateStatuses() {
        setTestTime(LocalDateTime.of(2021, 4, 21, 0, 0));
        supplierRequestService.updateOutdatedStatuses();
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestServiceTest_saveSupplierResponse.before.csv",
            after = "SupplierRequestServiceTest_saveSupplierResponseToAcceptedUpdatesResponseDate.after.csv")
    public void saveSupplierResponseToAcceptedUpdatesResponseDate() {
        setTestTime(LocalDateTime.of(2021, 4, 21, 13, 34));
        SupplierResponseDTO supplierResponseDTO = new SupplierResponseDTO(true);
        supplierRequestService.saveSupplierResponse(supplierResponseDTO, 1L);
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestServiceTest_saveSupplierResponse.before.csv",
            after = "SupplierRequestServiceTest_saveSupplierResponseToDeclinedUpdatesResponseDate.after.csv")
    public void saveSupplierResponseToDeclinedUpdatesResponseDate() {
        setTestTime(LocalDateTime.of(2021, 4, 21, 13, 34));
        SupplierResponseDTO supplierResponseDTO = new SupplierResponseDTO(false);
        supplierRequestService.saveSupplierResponse(supplierResponseDTO, 1L);
    }
}
