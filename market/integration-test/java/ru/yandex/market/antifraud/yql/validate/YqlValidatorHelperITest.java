package ru.yandex.market.antifraud.yql.validate;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.model.SessionStatusEnum;
import ru.yandex.market.antifraud.util.IntDateUtil;
import ru.yandex.market.antifraud.util.SleepUtil;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YqlSession;
import ru.yandex.market.antifraud.yql.model.YqlSessionType;
import ru.yandex.market.antifraud.yql.model.YtConfig;
import ru.yandex.market.antifraud.yql.model.YtLogConfig;
import ru.yandex.market.antifraud.yql.yt.YtTablesHelper;

import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YqlValidatorHelperITest {

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Autowired
    private YtTablesHelper ytTablesHelper;

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private YtConfig ytConfig;

    @Autowired
    private YqlValidatorHelper yqlValidatorHelper;

    @Autowired
    private YqlSessionToTxMap yqlSessionToTxMap;

    @Autowired
    private YqlValidator limitedShowsValidator;

    @Before
    public void init() {
        testDataGenerator.initOnce();
    }

    @Test
    public void testNotFailAlreadyFailedSessions() {
        YqlSession session = createSession();
        yqlValidatorHelper.sessionFailed(session, "initial error");
        yqlValidatorHelper.sessionFailed(session, "second error");
        String err = jdbcTemplate.query("select error from sessions where session_id = " + session.getId(),
            String.class);
        assertThat(err, is("initial error"));
    }

    private void setHost(String host) {
        if(host != null) {
            System.setProperty("host.name", host);
        }
    }

    @Test
    public void mustCreateTmpRollbacksTable() {
        YqlSession session = createSession();
        yqlValidatorHelper.initRollbacksTable(session);
        assertTrue(ytTablesHelper.exists(session.getTmpRollbacksFile()));
    }

    @Test
    public void testFailWhenRecentPartitionsDroped() {
        YqlSession session = createSession();
        try {
            jdbcTemplate.exec("alter table sessions disable trigger user");
            jdbcTemplate.exec("update sessions set " +
                "updated = updated - interval '1 month', " +
                "status = '" + SessionStatusEnum.FILTERS_EXECUTED + "', " +
                "scale = '" + UnvalidatedDay.Scale.RECENT + "'");
            yqlValidatorHelper.failWhenRecentPartitionsDroped();
            assertFailedSessionsContain(session);
        } finally {
            jdbcTemplate.exec("alter table sessions enable trigger user");
        }
    }

    private void assertFailedSessionsContain(YqlSession session) {
        assertTrue(yqlValidatorHelper.getSessionsInStatus(SessionStatusEnum.FAILED)
                .stream()
                .map(YqlSession::getId)
                .collect(Collectors.toSet())
                .contains(session.getId()));
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckPending() {
        YqlSession session = createSession();
        yqlValidatorHelper.sessionFailed(session, "testCheckPending_itest");
        yqlValidatorHelper.checkPending(session);
    }

    @Test
    public void testAddSuccessfulFilter() {
        YqlSession session = createSession();
        yqlValidatorHelper.addSuccessfulFilter(session, 1);
        yqlValidatorHelper.addSuccessfulFilter(session, 2);
        assertEquals(1L, getSessionLastSuccessfulFiler(session));
        assertThat(yqlValidatorHelper.getSession(yqlValidatorHelper.getLastPendingSessionId(
            session.getDay().getDay())).getSuccessfulFilters(),
            is(ImmutableSet.of(1L, 2L)));
    }

    private long getSessionLastSuccessfulFiler(YqlSession session) {
        return jdbcTemplate.query("select filter_id from session_successful_filters " +
                "where session_id = :session_id " +
                "order by order_id asc limit 1",
            "session_id", session.getId(),
            Long.class);
    }

    @Test
    public void testCleanUpFailed() {
        YqlSession session = createSession();

        ytTablesHelper.create(session.getTmpRollbacksFile());
        yqlValidatorHelper.sessionFailed(session, "testCleanUpFailed");

        String filename = session.getTmpRollbacksFile().substring(
                session.getTmpRollbacksFile().lastIndexOf("/") + 1);

        assertTrue(ytTablesHelper.list(session.getTmpRollbacksDir()).contains(filename));

        yqlValidatorHelper.cleanUpFailedOlderThan(0);

        assertFalse(ytTablesHelper.list(session.getTmpRollbacksDir()).contains(filename));
        assertThat(jdbcTemplate.query("select status from sessions where session_id = :session_id",
                "session_id", session.getId(), String.class),
                is(SessionStatusEnum.FAILED_WAS_CLEANED.toString()));
    }

    @Test
    public void testCreateSession() {
        jdbcTemplate.exec("truncate closed_days");
        YqlSession session = yqlValidatorHelper.createSession(
                new UnvalidatedDay(testDataGenerator.getRecentDay(), 0L, UnvalidatedDay.Scale.RECENT));

        NavigableSet<String> expectedPartitions = ytTablesHelper.list(ytConfig.getLogDir(
                testDataGenerator.log(), UnvalidatedDay.Scale.RECENT));

        assertThat(session.getPartitions(), is(
                expectedPartitions.stream()
                        .map((tbl) -> ytConfig
                                .getLogPath(testDataGenerator.log(), UnvalidatedDay.Scale.RECENT, tbl))
                        .filter((s) -> s.contains(IntDateUtil.hyphenated(session.getDay().getDay())))
                        .collect(Collectors.toList())
        ));
        assertThat(session.getType(), is(YqlSessionType.NORMAL));
        assertThat(session.getStatus(), is(SessionStatusEnum.PENDING));
    }

    @Test
    public void testGetSessionsInStatus() {
        jdbcTemplate.exec("truncate sessions");
        jdbcTemplate.exec("insert into sessions " +
                "(day, scale, log, cluster, last_seen_event_id, host, status, seen_partitions) values " +
                "(20101101, 'RECENT', '" + testDataGenerator.log().getLogName().getLogName() + "', 'cluster1', 0, 'host1'," +
                "'" + SessionStatusEnum.DATA_READY + "'," +
                "'[\"2017-11-10T13:00:00\", \"2017-11-10T13:30:00\"]')");
        List<YqlSession> sessions = yqlValidatorHelper.getSessionsInStatus(SessionStatusEnum.DATA_READY);
        assertThat(sessions.size(), is(1));

        YqlSession session = sessions.get(0);
        assertThat(session.getStatus(), is(SessionStatusEnum.DATA_READY));
        assertThat(session.getDay().getDay(), is(20101101));
        assertThat(session.getDay().getLastSeenEventId(), is(0L));
        assertThat(session.getDay().getScale(), is(UnvalidatedDay.Scale.RECENT));

        List<String> partitions = session.getPartitions();
        assertThat(partitions.size(), is(2));
        assertThat(partitions, is(Arrays.asList(
                ytConfig.getLogPath(testDataGenerator.log(), UnvalidatedDay.Scale.RECENT, "2017-11-10T13:00:00"),
                ytConfig.getLogPath(testDataGenerator.log(), UnvalidatedDay.Scale.RECENT, "2017-11-10T13:30:00"))));
    }

    @Test
    public void testGetSeenPartitions() {
        Set<String> seenPartitions = yqlValidatorHelper.getSeenPartitions(
                new UnvalidatedDay(testDataGenerator.getArchiveDay(), 0L, UnvalidatedDay.Scale.ARCHIVE));
        assertThat(seenPartitions, is(ImmutableSet.of(
                 IntDateUtil.hyphenated(testDataGenerator.getArchiveDay())
        )));
    }

    @Test
    public void mustReuseSession() {
        jdbcTemplate.exec("truncate sessions");
        YqlSession lastPending = createSession();
        YqlSession session = yqlValidatorHelper.createOrReuseSession(lastPending.getDay());
        assertThat(session.getId(), is(lastPending.getId()));
    }

    @Test
    public void mustCreateNewSession() {
        jdbcTemplate.exec("truncate sessions");
        YqlSession session = yqlValidatorHelper.createOrReuseSession(new UnvalidatedDay(testDataGenerator.getArchiveDay(), 0, UnvalidatedDay.Scale.ARCHIVE));
        assertNotNull(session);
        assertNotNull(session.getId());
        assertThat(session.getStatus(), is(SessionStatusEnum.PENDING));
    }

    @Test
    public void getLastPendingSession() {
        jdbcTemplate.exec("truncate sessions");
        createSession();
        YqlSession lastPending = createSession();
        assertThat(yqlValidatorHelper.getLastPendingSessionId(testDataGenerator.getArchiveDay()),
            is(lastPending.getId()));
    }

    @Test
    public void dayMustBeClosedInLF() throws Exception {
        YtLogConfig logConf = new YtLogConfig("market-new-shows-log");
        jdbcTemplate.exec("truncate step_events");
        jdbcTemplate.exec("truncate closed_days");
        jdbcTemplate.update("insert into step_events " +
                "(event_step_id, cluster, event_name, log, scale, timestamp, day) values " +
                "(:event_step_id, :cluster, " +
                ":event_name, '" + logConf.getLogName().getLogName() + "', " +
                "'1d', :timestamp, :day)",
            "event_step_id", "wstest_" + RndUtil.randomAlphabetic(10),
            "cluster", ytConfig.getCluster(),
            "event_name", logConf.getStepEventPublish().getEventName(),
            "timestamp", IntDateUtil.hyphenated(20121011),
            "day", 20121011);
//        jdbcTemplate.exec("insert into closed_days (log, day, reason) values " +
//            "('" + UnvalidatedDaysHelperITest.LOG_CONF.getLogName().getLogName() + "', 20121010, 'accelerator_itest')");
        assertTrue(yqlValidatorHelper.allPartitionsForDayExist(20121011));
    }

    @Test(timeout = 120_000)
    @Ignore("race conditions' parade")
    public void mustFailSession() {
        jdbcTemplate.exec("truncate sessions");
        new Thread(() -> limitedShowsValidator.validate(new UnvalidatedDay(
            testDataGenerator.getArchiveDay(), 1, UnvalidatedDay.Scale.ARCHIVE))
        ).start();
        while(jdbcTemplate.query("select count(*) from sessions " +
            "where day = " + testDataGenerator.getArchiveDay() + " and " +
            "status = '" + SessionStatusEnum.PENDING + "'", Long.class) == 0) {
            SleepUtil.sleep(50);
        }
        long sessionId = jdbcTemplate.query("select session_id from sessions " +
            "where day = " + testDataGenerator.getArchiveDay() + " and " +
            "status = '" + SessionStatusEnum.PENDING + "'", Long.class);

        while(yqlSessionToTxMap.get(sessionId) == null) {
            SleepUtil.sleep(10);
        }

        yqlValidatorHelper.sessionFailed(sessionId, "itest");

        assertThat(jdbcTemplate.query("select status from sessions " +
            "where session_id = " + sessionId, String.class), Matchers.is(SessionStatusEnum.FAILED.toString()));
    }

    private YqlSession createSession() {
        return yqlValidatorHelper.createSession(
                new UnvalidatedDay(testDataGenerator.getArchiveDay(), 0, UnvalidatedDay.Scale.ARCHIVE)
        );
    }
}
