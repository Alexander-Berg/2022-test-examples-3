package ru.yandex.market.abo.tms.gen.monitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GeneratorMonitorTest extends EmptyTest {
    @Autowired
    private CpcGeneratorMonitorExecutor cpcGeneratorMonitorExecutor;

    @Test
    public void testCpcMonitor() {
        String details = cpcGeneratorMonitorExecutor.getMonitorDetails();
        assertNotNull(details);
    }
}
