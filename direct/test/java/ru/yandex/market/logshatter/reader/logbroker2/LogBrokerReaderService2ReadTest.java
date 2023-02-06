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
 * @date 23.01.2019
 */
public class LogBrokerReaderService2ReadTest {
    @Rule
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    private final LbReadingTester tester = new LbReadingTester();

    @Test
    public void noConfigs() {
        Partition partition = tester.givenStartedSessionWithLockedPartition();
        partition.lbSendsData(1, messageData(2));
        tester.verifyInteractions(partition.lbReceivedCommit(1));
    }

    @Test
    public void skippedBecauseOfSeqNo() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(configThatMatchesEverything());

        partition.lbSendsData(1, messageData(10, "a"));

        tester.runParsers();
        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("a", 10),
            partition.lbReceivedCommit(1)
        );

        partition.lbSendsData(2, messageData(5, "a"));
        tester.verifyInteractions(
            partition.lbReceivedCommit(1)
        );
    }

    @Test
    public void oneReadBatch_twoMessages_oneConfig() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(configThatMatchesEverything());

        partition.lbSendsData(
            1,
            messageData(2, "a"),
            messageData(3, "b")
        );
        tester.verifyNoInteractions();

        tester.runParsers();
        tester.runParsers();
        tester.verifyNoInteractions();

        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData()
        );

        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("a", 2),
            partition.mongoReceivedSeqNo("b", 3),
            partition.lbReceivedCommit(1)
        );
    }

    @Test
    public void oneReadBatch_oneMessage_twoConfigs() {
        Partition partition = tester.givenStartedSessionWithLockedPartition(
            configThatMatchesEverything(), configThatMatchesEverything()
        );

        partition.lbSendsData(
            1,
            messageData(1, "a")
        );
        tester.verifyNoInteractions();

        tester.runParsers();
        tester.runParsers();
        tester.verifyNoInteractions();

        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData()
        );

        tester.runWriters(1);
        tester.verifyInteractions(
            tester.clickHouseReceivedData(),
            partition.mongoReceivedSeqNo("a", 1),
            partition.lbReceivedCommit(1)
        );
    }
}
