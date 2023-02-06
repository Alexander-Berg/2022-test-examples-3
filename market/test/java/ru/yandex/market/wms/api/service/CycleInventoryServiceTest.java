package ru.yandex.market.wms.api.service;

import java.time.LocalDateTime;
import java.time.Month;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.config.LostWriteOffServiceTestConfig;
import ru.yandex.market.wms.common.spring.service.CycleInventoryService;

@SpringBootTest(classes = {LostWriteOffServiceTestConfig.class})
public class CycleInventoryServiceTest extends IntegrationTest {

    @Autowired
    private CycleInventoryService cycleInventoryService;

    private static final String TYPE_1P = "1P";
    private static final String TYPE_3P = "3P";
    private static final LocalDateTime CYCLE_END_1 = LocalDateTime.of(2020, Month.JUNE, 20,
            0, 0, 0);
    private static final String EXTERN_ORDER_KEY = "outbound-fixlost-12";

    @Test
    @DatabaseSetup("/cycle-inventory/before.xml")
    @DatabaseSetup("/cycle-inventory/performance-invent-cycle.xml")
    @ExpectedDatabase(value = "/cycle-inventory/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/cycle-inventory/performance-invent-cycle.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/cycle-inventory/losts-log-1p.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testDryRunTrue() {
        cycleInventoryService.writeOffFixLost(TYPE_1P, CYCLE_END_1, true, EXTERN_ORDER_KEY);
    }

    @Disabled
    @Test
    @DatabaseSetup("/cycle-inventory/before.xml")
    @DatabaseSetup("/cycle-inventory/performance-invent-cycle.xml")
    @ExpectedDatabase(value = "/cycle-inventory/after-1P.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/cycle-inventory/performance-invent-cycle.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/cycle-inventory/losts-log-1p.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testDryRunFalse1P() {
        cycleInventoryService.writeOffFixLost(TYPE_1P, CYCLE_END_1, false, EXTERN_ORDER_KEY);
    }

    @Disabled
    @Test
    @DatabaseSetup("/cycle-inventory/before.xml")
    @DatabaseSetup("/cycle-inventory/performance-invent-cycle.xml")
    @ExpectedDatabase(value = "/cycle-inventory/after-3P.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/cycle-inventory/performance-invent-cycle.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/cycle-inventory/losts-log-3p.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testDryRunFalse3P() {
        cycleInventoryService.writeOffFixLost(TYPE_3P, CYCLE_END_1, false, EXTERN_ORDER_KEY);
    }

}
