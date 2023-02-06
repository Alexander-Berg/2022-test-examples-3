package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.auto_processing.DemandAutoProcessingService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class DemandAutoProcessingServiceTest extends FunctionalTest {

    @Autowired
    private DemandAutoProcessingService demandAutoProcessingService;

    @Test
    @DbUnitDataSet(
        before = "DemandAutoProcessingServiceTest.testNotSplitWithoutCatman.before.csv"
    )
    public void testNotSplitWithoutCatman() {
        assertDoesNotThrow(() -> demandAutoProcessingService.prepareDemandsForExport());
    }

    @Test
    @DbUnitDataSet(
        before = "DemandAutoProcessingServiceTest.testQuotas.before.csv",
        after = "DemandAutoProcessingServiceTest.testQuotas.after.csv"
    )
    public void testQuotasValidation() {
        demandAutoProcessingService.prepareDemandsForExport();
    }
}
