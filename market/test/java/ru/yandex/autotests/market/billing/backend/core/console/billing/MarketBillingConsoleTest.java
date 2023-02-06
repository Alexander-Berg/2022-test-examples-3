package ru.yandex.autotests.market.billing.backend.core.console.billing;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;

import ru.yandex.autotests.market.common.attacher.Attacher;

import static ru.yandex.autotests.market.billing.backend.core.console.billing.MarketBillingConsole.LOAD_DELIVERY_SERVICES_EXECUTOR;

/**
 * @author ivmelnik
 * @since 29.11.16
 */
public class MarketBillingConsoleTest {

    @Ignore
    @Test
    public void requestJobRun() {
        JUnitCore.runClasses(ParallelComputer.methods(), ParallelTest.class);
    }

    public static class ParallelTest {

        private static final String JOB_NAME = LOAD_DELIVERY_SERVICES_EXECUTOR;

        @ClassRule
        public static MarketBillingConsoleResource consoleResource = new MarketBillingConsoleResource(ConsoleConnector.BILLING);

        @Test
        public void test1() {
            Attacher.attach("test1 started");
            consoleResource.getConsole().requestJobRun(JOB_NAME);
            Attacher.attach("test1 ended");
        }

        @Test
        public void test2() {
            Attacher.attach("test2 started");
            consoleResource.getConsole().requestJobRun(JOB_NAME);
            Attacher.attach("test2 ended");
        }

        @Test
        public void test3() throws Exception {
            Thread.sleep(5000L);
            Attacher.attach("test3 started");
            consoleResource.getConsole().requestJobRun(JOB_NAME);
            Attacher.attach("test3 ended");
        }

        @Test
        public void test4() throws Exception {
            Thread.sleep(45000L);
            Attacher.attach("test4 started");
            consoleResource.getConsole().requestJobRun(JOB_NAME);
            Attacher.attach("test4 ended");
        }

    }

}
