package ru.yandex.market.wms.common.spring.service;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutboundServiceTest extends IntegrationTest {

    @Autowired
    private OutboundService outboundService;

    /** Тест на отмену отгрузок с 02 статусом */
    @Test
    @DatabaseSetup("/db/service/outbound/cancel-open/before.xml")
    @ExpectedDatabase(value = "/db/service/outbound/cancel-open/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void cancelOpen() {
        Set<String> expected = ImmutableSet.of("11", "12");
        List<String> cancelledOrderKeys = outboundService.cancelOpenOutbounds();
        assertEquals(2, cancelledOrderKeys.size());
        assertTrue(expected.containsAll(cancelledOrderKeys));
    }

    /** Тест на отработку методов завершения, когда нет отгрузок для завершения */
    @Test
    @DatabaseSetup("/db/service/outbound/nothing-to-close/db.xml")
    @ExpectedDatabase(value = "/db/service/outbound/nothing-to-close/db.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void nothingToClose() {
        List<String> cancelledOrderKeys = outboundService.cancelOpenOutbounds();
        assertTrue(cancelledOrderKeys.isEmpty());
    }
}
