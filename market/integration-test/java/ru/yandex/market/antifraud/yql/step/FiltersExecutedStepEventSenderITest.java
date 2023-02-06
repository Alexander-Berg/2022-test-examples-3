package ru.yandex.market.antifraud.yql.step;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.model.SessionStatusEnum;
import ru.yandex.market.antifraud.util.IntDateUtil;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YqlSessionType;
import ru.yandex.market.antifraud.yql.model.YtConfig;
import ru.yandex.market.antifraud.yql.validate.YtTestDataGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class FiltersExecutedStepEventSenderITest {
    @Autowired
    private FiltersExecutedStepEventSender filtersExecutedStepEventSender;

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Autowired
    private YtConfig ytConfig;

    @Value("${antifraud.yql.step.url}")
    private String stepUrl;

    @Value("${antifraud.yql.rollback.eventname}")
    private String eventName;

    @Before
    public void checkTestData() {
        testDataGenerator.initOnce();
        jdbcTemplate.exec("truncate sessions");
        jdbcTemplate.exec("truncate filters_executed_event_sent");
    }

    @Test
    public void testFiltersExecutedSessionsList() {
        Set<Long> mustBeSent = new HashSet<>();
        mustBeSent.add(createSession(SessionStatusEnum.FILTERS_EXECUTED, 20080723));
        mustBeSent.add(createSession(SessionStatusEnum.DATA_READY, 20080724));
        mustBeSent.add(createSession(SessionStatusEnum.DATA_READY, 20080724));
        mustBeSent.add(createSession(SessionStatusEnum.SUCCESSFUL, 20080725));

        // must not be sent
        createSession(SessionStatusEnum.PENDING, 20080726);
        createSession(SessionStatusEnum.FAILED, 20080727);

        // already sent
        long sentSessionId = createSession(SessionStatusEnum.SUCCESSFUL, 20080728);
        jdbcTemplate.exec("insert into filters_executed_event_sent (session_id, day, step_event_id) values " +
            "(" + sentSessionId + ", 20080728, 'teststepid1')");

        Set<Long> sent = filtersExecutedStepEventSender.getUnsentFESessions().stream()
            .map(d -> d.getSessionId())
            .collect(Collectors.toSet());

        assertEquals(mustBeSent, sent);
    }


    @Test
    public void mustSendAndGetFiltersExecutedEvent() {
        long sessionId = createSession(SessionStatusEnum.FILTERS_EXECUTED, 20080621);
        String stepId = filtersExecutedStepEventSender.send(new FiltersExecutedStepEventSender.DayAndSession(
            20080621,  sessionId, UnvalidatedDay.Scale.ARCHIVE, ytConfig.getCluster()
        ));
        log.info("eventId: " + stepId);
        DaycloseStepEventSenderITest.checkEventIdExistsInStep(stepUrl, stepId);
    }


    private long createSession(SessionStatusEnum status, int day) {
        return jdbcTemplate.query("insert into sessions " +
            "(day, scale, log, cluster, last_seen_event_id, host, status, type, seen_partitions) values " +
            "(" + day + ", 'ARCHIVE', '" + testDataGenerator.log().getLogName().getLogName() + "', '" + ytConfig.getCluster() + "', 0, 'host1'," +
            "'" + status + "'," +
            "'" + YqlSessionType.DAYCLOSING + "'," +
            "'[\"" + IntDateUtil.hyphenated(day) + "\"]') returning session_id", Long.class);
    }
}
