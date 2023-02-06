package ru.yandex.market.logshatter.reader.logbroker;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.InteractionVerifier;
import ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.Session;
import ru.yandex.market.monitoring.MonitoringStatus;

import static com.google.common.util.concurrent.Service.State.RUNNING;
import static ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.config;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 23.01.2019
 */
public class LogBrokerReaderServiceCreateSessionsTest {
    @Rule
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    private final LbReadingTester tester = new LbReadingTester();

    @Test
    public void startSessions() {
        tester.givenConfigs(
            config("market-health-stable--other"),
            config("marketstat--market-dicts"),
            config("marketstat--market-clicks-log"),
            config("kafka-bs--marketstat@test--market-clicks-log")
        );
        tester.givenTopicsInOldApi(
            "market-health-stable", "other",
            "rt3.man--market-health-stable--other",
            "rt3.myt--market-health-stable--other"
        );
        tester.givenTopicsInOldApi(
            "marketstat", "market-dicts",
            "rt3.man--marketstat--market-dicts",
            "rt3.myt--marketstat--market-dicts"
        );
        tester.givenTopicsInOldApi(
            "marketstat", "market-clicks-log",
            "rt3.man--marketstat--market-clicks-log",
            "rt3.myt--marketstat--market-clicks-log"
        );
        tester.givenTopicsInOldApi(
            "marketstat@test", "market-clicks-log",
            "rt3.kafka-bs--marketstat@test--market-clicks-log"
        );

        tester.startReaderService(Arrays.asList("man", "myt", "kafka-bs"));

        tester.clusterCriticalMonitoringShouldBe(MonitoringStatus.OK);

        List<Session> sessions = Stream.of(
            "rt3.man--market-health-stable--other",
            "rt3.myt--market-health-stable--other",
            "rt3.man--marketstat--market-dicts",
            "rt3.myt--marketstat--market-dicts",
            "rt3.man--marketstat--market-clicks-log",
            "rt3.myt--marketstat--market-clicks-log",
            "rt3.kafka-bs--marketstat@test--market-clicks-log"
        )
            .map(tester::session)
            .collect(Collectors.toList());

        tester.verifyInteractions(
            sessions.stream()
                .flatMap(session -> Stream.of(
                    session.lbReceivesStreamConsumerStart("man"),
                    session.lbReceivesStreamConsumerStart("myt"),
                    session.lbReceivesStreamConsumerStart("kafka-bs")
                ))
                .toArray(InteractionVerifier[]::new)
        );

        tester.readerServiceShouldBeInState(RUNNING);
    }
}
