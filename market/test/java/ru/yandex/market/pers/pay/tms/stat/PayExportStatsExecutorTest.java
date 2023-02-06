package ru.yandex.market.pers.pay.tms.stat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.pay.PersPayTest;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 28.04.2021
 */
public class PayExportStatsExecutorTest extends PersPayTest {

    @Autowired
    private PayExportStatsExecutor executor;

    @Test
    public void testExportStats() {
        executor.exportStats();
    }
}
