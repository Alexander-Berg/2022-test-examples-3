package ru.yandex.market.logshatter.reader.logbroker2;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import ru.yandex.market.logshatter.reader.logbroker2.LbReadingTester.Session;
import ru.yandex.market.logshatter.reader.logbroker2.LbReadingTester.Session.Partition;

import java.util.concurrent.TimeUnit;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 23.01.2019
 */
public class LogBrokerReaderService2LockTest {
    @Rule
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    private final LbReadingTester tester = new LbReadingTester();

    @Test
    public void startReadingNewPartition_noOffsetInMongo() {
        Session session = tester.givenStartedSession();
        Partition partition = session.partition(1);
        partition.lbSendsLockMessage();
        tester.verifyInteractions(partition.lbReceivedLockedMessage(0));
    }

    /**
     * https://st.yandex-team.ru/MARKETINFRA-4539.
     * Если таска, записывающая оффсеты в Монгу, запустилась раньше чем началось чтение партиции, то она запишет в Монгу
     * оффсет -1. Новое API не принимает оффсет -1, только 0.
     */
    @Test
    public void startReadingNewPartition_invalidOffsetInMongo() {
        Session session = tester.givenStartedSession();
        Partition partition = session.partition(1);
        partition.givenOffsetInMongo(-1);
        partition.lbSendsLockMessage();
        tester.verifyInteractions(partition.lbReceivedLockedMessage(0));
    }

    @Test
    public void startReadingExistingPartition() {
        Session session = tester.givenStartedSession();
        Partition partition = session.partition(1);
        partition.givenOffsetInMongo(123);
        partition.lbSendsLockMessage();
        tester.verifyInteractions(partition.lbReceivedLockedMessage(123));
    }
}
