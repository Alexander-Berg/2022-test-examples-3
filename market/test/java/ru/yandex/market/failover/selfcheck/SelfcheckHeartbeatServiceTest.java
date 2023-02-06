package ru.yandex.market.failover.selfcheck;

import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.failover.selfcheck.HeartbeatReportItem.AliveState.ALIVE;
import static ru.yandex.market.failover.selfcheck.HeartbeatReportItem.AliveState.CHECK_DISABLED;
import static ru.yandex.market.failover.selfcheck.HeartbeatReportItem.AliveState.NEVER_RUN;
import static ru.yandex.market.failover.selfcheck.HeartbeatReportItem.AliveState.NOT_ALIVE;
import static ru.yandex.market.failover.selfcheck.HeartbeatReportItem.AliveState.NOT_REGISTERED;
import static ru.yandex.market.failover.selfcheck.HeartbeatReportItem.UNDEFINED;

public class SelfcheckHeartbeatServiceTest {
    public static final String TEST_NAME = "test";
    private SelfcheckHeartbeatService selfcheckHeartbeatService;

    @Before
    public void setUp() {
        selfcheckHeartbeatService = new SelfcheckHeartbeatService();
    }

    @Test
    public void testOk() {
        selfcheckHeartbeatService.register(TEST_NAME, 1_000_000);
        selfcheckHeartbeatService.beat(TEST_NAME);
        assertTrue(selfcheckHeartbeatService.isAlive(TEST_NAME));
    }

    @Test
    public void testOverTime() throws InterruptedException {
        selfcheckHeartbeatService.register(TEST_NAME, 5);
        selfcheckHeartbeatService.beat(TEST_NAME);
        sleep(20);
        assertFalse(selfcheckHeartbeatService.isAlive(TEST_NAME));
    }

    /**
     * Мы не можем проверять незарегистрированный сервис - для него не указан
     * максимальный интервал между запусками
     */
    @Test(expected = SelfcheckException.class)
    public void testNotRegistered() {
        selfcheckHeartbeatService.isAlive(TEST_NAME);
    }

    /**
     * Мы не можем подтвердить живость сервиса, если у него никогда не было первого запуска.
     * Мы проверяем только интервал между запусками.
     */
    @Test(expected = SelfcheckException.class)
    public void testUndefinedAlive() {
        selfcheckHeartbeatService.register(TEST_NAME, 1_000_000);
        selfcheckHeartbeatService.isAlive(TEST_NAME);
    }

    @Test
    public void testAllwaysAlive() {
        selfcheckHeartbeatService.register(TEST_NAME, SelfcheckHeartbeatService.DO_NOT_CHECK_TIMEOUT);
        assertTrue(selfcheckHeartbeatService.isAlive(TEST_NAME));
    }

    @Test
    public void testReportNonRegistered() {
        selfcheckHeartbeatService.beat(TEST_NAME);
        HeartbeatReportItem reportItem = getReportItem();

        assertEquals(NOT_REGISTERED, reportItem.aliveState());
        assertNotSame(UNDEFINED, reportItem.getLastRun());
        assertEquals(UNDEFINED, reportItem.getMaxDelay());
        assertNotNull(reportItem.getNoInvocationsPeriod());
    }

    @Test
    public void testReportNeverRun() {
        selfcheckHeartbeatService.register(TEST_NAME, 1);
        HeartbeatReportItem reportItem = getReportItem();

        assertEquals(reportItem.aliveState(), NEVER_RUN);
        assertEquals(UNDEFINED, reportItem.getLastRun());
        assertNotSame(UNDEFINED, reportItem.getMaxDelay());
        assertEquals(UNDEFINED, reportItem.getNoInvocationsPeriod());
    }

    @Test
    public void testReportCheckDisabled() {
        selfcheckHeartbeatService.register(TEST_NAME, SelfcheckHeartbeatService.DO_NOT_CHECK_TIMEOUT);
        HeartbeatReportItem reportItem = getReportItem();

        assertEquals(reportItem.aliveState(), CHECK_DISABLED);
        assertEquals(UNDEFINED, reportItem.getLastRun());
        assertEquals(UNDEFINED, reportItem.getNoInvocationsPeriod());
        assertEquals(SelfcheckHeartbeatService.DO_NOT_CHECK_TIMEOUT, reportItem.getMaxDelay());
    }

    @Test
    public void testReportOk() {
        int delay = 1_000_000;
        selfcheckHeartbeatService.register(TEST_NAME, delay);
        selfcheckHeartbeatService.beat(TEST_NAME);
        HeartbeatReportItem reportItem = getReportItem();

        assertEquals(reportItem.aliveState(), ALIVE);
        assertNotSame(UNDEFINED, reportItem.getLastRun());
        assertTrue(reportItem.getNoInvocationsPeriod() <= delay);
        assertEquals(delay, reportItem.getMaxDelay());
    }

    @Test
    public void testReportFail() throws InterruptedException {
        int delay = 1;
        selfcheckHeartbeatService.register(TEST_NAME, delay);
        selfcheckHeartbeatService.beat(TEST_NAME);
        sleep(10);
        HeartbeatReportItem reportItem = getReportItem();

        assertEquals(reportItem.aliveState(), NOT_ALIVE);
        assertNotNull(reportItem.getLastRun());
        assertTrue(reportItem.getNoInvocationsPeriod() > delay);
        assertEquals(delay, reportItem.getMaxDelay());
    }

    public HeartbeatReportItem getReportItem() {
        Map<String, HeartbeatReportItem> report = selfcheckHeartbeatService.report();
        assertEquals(1, report.size());
        HeartbeatReportItem reportItem = report.get(TEST_NAME);
        assertNotNull(reportItem);
        return reportItem;
    }

    @Test
    public void getAllNamesTest() {
        selfcheckHeartbeatService.register(name(1), 10);
        selfcheckHeartbeatService.register(name(2), 10);

        selfcheckHeartbeatService.beat(name(2));
        selfcheckHeartbeatService.beat(name(3));

        Set<String> names = selfcheckHeartbeatService.allNames();

        assertNotNull(names);
        assertEquals(3, names.size());
        assertTrue(names.contains(name(1)));
        assertTrue(names.contains(name(2)));
        assertTrue(names.contains(name(3)));
    }

    @Test
    public void testGetAllRegistered() {
        var name1 = name(1);
        var name2 = name(2);

        selfcheckHeartbeatService.register(name1, 10);
        selfcheckHeartbeatService.register(name2, 10);
        assertThat(selfcheckHeartbeatService.getAllRegistered(), containsInAnyOrder(name1, name2));

        var name3 = name(3);
        selfcheckHeartbeatService.register(name3, 10);
        assertThat(selfcheckHeartbeatService.getAllRegistered(), containsInAnyOrder(name1, name2, name3));
    }

    public String name(int n) {
        return TEST_NAME + "_" + n;
    }

}
