package ru.yandex.market.pers.notify.executor;

import org.junit.jupiter.api.Test;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         25.03.16
 */
public class ThreadDumpMonitorExecutorTest extends MarketMailerMockedDbTest {
	@Test
	public void testPrintDump() throws Exception {
		System.out.println(ThreadDumpMonitorExecutor.logThreadDump());
	}
}
