package ru.yandex.market.abo.util;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.util.monitoring.JugglerMonitoring;
import ru.yandex.market.abo.util.monitoring.SharedMonitoringUnit;
import ru.yandex.market.monitoring.MonitoringStatus;
import ru.yandex.market.monitoring.MonitoringUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vasiliy Briginets (0x40@yandex-team.ru)
 */
public class MonitoringTest extends EmptyTest {
    @Autowired
    @InjectMocks
    private MonitoringUnit checkOrderMonitoring;
    @Autowired
    private JugglerMonitoring aboMonitoring;
    @Mock
    private Clock clock;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void checkOrderMonitoringTest() {
        checkOrderMonitoring.ok();
        assertEquals(MonitoringStatus.OK, checkOrderMonitoring.getStatus());

        String warnMessage = "warning";
        checkOrderMonitoring.warning(warnMessage);
        assertEquals(MonitoringStatus.WARNING, checkOrderMonitoring.getStatus());
        assertEquals(warnMessage, checkOrderMonitoring.getMessage());
    }

    @Test
    public void defaultCreatingTest() {
        MonitoringUnit monitoringUnit = aboMonitoring.getOrAddUnit("check_order");
        assertEquals(checkOrderMonitoring, monitoringUnit);

        MonitoringUnit testMonitoringUnit = aboMonitoring.getOrAddUnit("testMonitoring");
        assertNotEquals(checkOrderMonitoring, testMonitoringUnit);
        assertEquals(MonitoringStatus.OK, testMonitoringUnit.getStatus());
        assertTrue(testMonitoringUnit instanceof SharedMonitoringUnit);
    }
}
