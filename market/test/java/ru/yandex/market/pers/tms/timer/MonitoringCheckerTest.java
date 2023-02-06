package ru.yandex.market.pers.tms.timer;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.tms.MockedPersTmsTest;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.08.2021
 */
public class MonitoringCheckerTest extends MockedPersTmsTest {
    @Autowired
    private MonitoringChecker executor;

    @Test
    public void checkMonitoringsDoesNotFail() {
        executor.checkMonitorings();
    }
}
