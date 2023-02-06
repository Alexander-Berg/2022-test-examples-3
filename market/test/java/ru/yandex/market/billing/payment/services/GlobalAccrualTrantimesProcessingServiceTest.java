package ru.yandex.market.billing.payment.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Test class for {@link GlobalAccrualTrantimesProcessingService}
 */
class GlobalAccrualTrantimesProcessingServiceTest extends FunctionalTest {

    @Autowired
    private GlobalAccrualTrantimesProcessingService globalAccrualTrantimesProcessingService;

    @Test
    @DisplayName("Правильно ли приходят из базы трантаймы")
    @DbUnitDataSet(
            before = "GlobalAccrualTrantimesProcessingService.processAccrualTrantimes.before.csv",
            after = "GlobalAccrualTrantimesProcessingService.processAccrualTrantimes.after.csv"
    )
    void testProcessAccrualTrantimes() {
        globalAccrualTrantimesProcessingService.process();
    }
}
