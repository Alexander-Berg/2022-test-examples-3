package ru.yandex.market.pers.grade.admin.graphics;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 */
public class TestGraphics {
    //TODO remove later, fails to work in arcadia CI
    @Test
    @Ignore
    public void testCreateJPEG() throws Exception {
        long startTime = System.currentTimeMillis();
        TestJFreeChart.createTestChart();
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
    }
}
