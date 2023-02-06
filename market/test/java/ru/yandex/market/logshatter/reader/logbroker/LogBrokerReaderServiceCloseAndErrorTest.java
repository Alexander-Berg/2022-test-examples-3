package ru.yandex.market.logshatter.reader.logbroker;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.Session;
import ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.Session.Partition;

import static ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.configThatMatchesEverything;
import static ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.messageData;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 21.02.2019
 */
public class LogBrokerReaderServiceCloseAndErrorTest {
    @Rule
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    private final LbReadingTester tester = new LbReadingTester();

    @Test
    public void closeShouldAbortDataProcessing() {
        Session session = tester.givenStartedSession(configThatMatchesEverything());
        Partition partition = tester.givenStartedSessionWithLockedPartition(session);

        partition.lbSendsData(1, messageData(1, "a"));
        tester.runParsers();
        partition.lbClosesSession();
        tester.runWriters(1);
        tester.verifyInteractions(
            session.lbReceivesStreamConsumerStop()
        );
    }

    @Test
    public void errorShouldAbortDataProcessingAndRestartSession() {
        Session session = tester.givenStartedSession(configThatMatchesEverything());
        Partition partition = tester.givenStartedSessionWithLockedPartition(session);

        partition.lbSendsData(1, messageData(1, "a"));
        tester.runParsers();
        partition.lbSendsError(new RuntimeException());
        tester.runWriters(1);
        tester.verifyInteractions(
            session.lbReceivesStreamConsumerStop()
        );

        tester.runScheduledTasks();
        tester.verifyInteractions(
            session.lbReceivesStreamConsumerStart("man")
        );
    }
}
