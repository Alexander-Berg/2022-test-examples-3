package ru.yandex.market.logshatter.reader.logbroker2;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import ru.yandex.market.logshatter.reader.logbroker2.LbReadingTester.InteractionVerifier;
import ru.yandex.market.logshatter.reader.logbroker2.LbReadingTester.Session;
import ru.yandex.market.monitoring.MonitoringStatus;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Service.State.RUNNING;
import static ru.yandex.market.logshatter.reader.logbroker2.LbReadingTester.config;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 23.01.2019
 */
public class LogBrokerReaderService2CreateSessionsTest {
    @Rule
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    private final LbReadingTester tester = new LbReadingTester();

    @Test
    public void startSessions() {
        tester.givenConfigs(config("market-health-stable--other"), config("marketstat"));
        tester.givenTopicsInOldApi(
            "market-health-stable", "other",
            "rt3.man--market-health-stable--other",
            "rt3.myt--market-health-stable--other"
        );
        tester.givenTopicsInOldApi(
            "marketstat", null,
            "rt3.man--marketstat--market-dicts",
            "rt3.myt--marketstat--market-dicts",
            "rt3.man--marketstat--market-clicks-log",
            "rt3.myt--marketstat--market-clicks-log"
        );

        tester.startReaderService(Arrays.asList("man", "myt"));

        tester.clusterCriticalMonitoringShouldBe(MonitoringStatus.OK);

        List<Session> sessions = Stream.of(
            "rt3.man--market-health-stable--other",
            "rt3.myt--market-health-stable--other",
            "rt3.man--marketstat--market-dicts",
            "rt3.myt--marketstat--market-dicts",
            "rt3.man--marketstat--market-clicks-log",
            "rt3.myt--marketstat--market-clicks-log"
        )
            .map(tester::session)
            .collect(Collectors.toList());

        tester.verifyInteractions(
            sessions.stream()
                .flatMap(session -> Stream.of(
                    session.lbReceivesStreamConsumerStart("man"),
                    session.lbReceivesStreamConsumerStart("myt")
                ))
                .toArray(InteractionVerifier[]::new)
        );

        tester.readerServiceShouldBeInState(RUNNING);
    }

    @Test
    public void lbReturnedEmptyOffsetsList() {
        tester.givenConfigs(config("market-health-stable--other"));
        tester.givenTopicsInOldApi(
            "market-health-stable", "other",
            "rt3.does_not_exist--market-health-stable--other"
        );

        tester.startReaderService(Arrays.asList("man", "myt"));

        tester.clusterCriticalMonitoringShouldBe(MonitoringStatus.CRITICAL);
    }
}
