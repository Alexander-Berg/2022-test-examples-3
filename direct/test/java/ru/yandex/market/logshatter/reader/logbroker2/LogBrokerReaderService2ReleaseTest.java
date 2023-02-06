package ru.yandex.market.logshatter.reader.logbroker2;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import ru.yandex.market.logshatter.reader.logbroker2.LbReadingTester.Session.Partition;

import java.util.concurrent.TimeUnit;

import static ru.yandex.market.logshatter.reader.logbroker2.LbReadingTester.configThatMatchesEverything;
import static ru.yandex.market.logshatter.reader.logbroker2.LbReadingTester.messageData;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 21.02.2019
 */
public class LogBrokerReaderService2ReleaseTest {
    @Rule
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    private final LbReadingTester tester = new LbReadingTester();

    @Test
    public void releaseShouldNotAbortDataProcessing() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(configThatMatchesEverything());

        partition.lbSendsData(1, messageData(1, "a"));
        partition.lbSendsReleaseMessage();
        tester.verifyNoInteractions();

        tester.runParsers();
        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("a", 1),
            partition.lbReceivedCommit(1)
        );
    }
}
