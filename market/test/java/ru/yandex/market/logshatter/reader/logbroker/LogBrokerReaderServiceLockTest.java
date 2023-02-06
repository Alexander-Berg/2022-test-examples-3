package ru.yandex.market.logshatter.reader.logbroker;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.Session;
import ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.Session.Partition;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 23.01.2019
 */
public class LogBrokerReaderServiceLockTest {
    @Rule
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    private final LbReadingTester tester = new LbReadingTester();

    @Test
    public void startReadingNewPartition() {
        Session session = tester.givenStartedSession();
        Partition partition = session.partition(1);
        partition.lbSendsLockMessage();
        tester.verifyInteractions(partition.lbReceivedLockedMessage(0));
    }
}
