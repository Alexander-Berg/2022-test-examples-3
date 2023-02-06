package ru.yandex.market.logistics.utilizer.service.cycle;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;

class SkuForUtilizationResetServiceTest extends AbstractContextualTest {
    @Autowired
    private SkuForUtilizationResetService skuForUtilizationResetService;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/cycle/reset-count/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/cycle/reset-count/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void testResetCountByActiveCycleStatuses() {
        skuForUtilizationResetService.resetCountByActiveCycleStatuses();
    }
}
