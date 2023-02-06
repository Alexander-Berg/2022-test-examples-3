package ru.yandex.market.supercontroller.mbologs.dao;

import com.google.common.collect.ImmutableSet;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.mbo.core.dashboard.DashboardClickhouseDDL;
import ru.yandex.market.mbo.core.dashboard.GenerationsDao;
import ru.yandex.market.mbo.gwt.models.dashboard.SessionsFilter;
import ru.yandex.market.mbo.gwt.models.dashboard.SuperControllerSession;
import ru.yandex.market.mbo.gwt.models.dashboard.SuperControllerSession.Status;
import ru.yandex.market.supercontroller.mbologs.Log4jAwareSpringJUnit4ClassRunner;
import ru.yandex.market.supercontroller.mbologs.conf.MboLogsIntegrationTestConfig;

import javax.annotation.Resource;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author amaslak
 */
@RunWith(Log4jAwareSpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MboLogsIntegrationTestConfig.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class GenerationLoggerTest {

    private final Logger logger = Logger.getLogger(getClass());

    private static final int INITIAL_ROW_COUNT = 2000;

    @Autowired
    @Qualifier("testMySqlGenerationManager")
    private GenerationLogger generationLogger;

    @Autowired
    private GenerationsDao generationsDao;

    @Resource(name = "dashboardClickhouseTemplate")
    private NamedParameterJdbcTemplate clickhouseTemplate;

    @Autowired
    private DashboardClickhouseDDL ddl;

    private String suffix;

    private static final String TEST_TABLE = "sc_test";

    private static final Set<String> SESSIONS = ImmutableSet.of(
            "10021101_0201", "10021101_0202", "10021101_0203", "10021101_0204", "10021101_0205"
    );

    @Before
    public void setUp() throws Exception {
        logger.info("==========================================================================================");
        suffix = '_' + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[.:-]", "_");
        ddl.checkTables(suffix);
        generationsDao.setSuffix(suffix);

        int i = INITIAL_ROW_COUNT;
        Map<String, Long> sessions = new HashMap<>();
        for (String s : SESSIONS) {
            sessions.put(s, (long) i++);
        }
        generationsDao.createNewSessions(generationLogger.getDatabase(), TEST_TABLE, TEST_TABLE,
            sessions, "??", true);

        checkStatuses(Status.NEW);
    }

    @After
    public void tearDown() throws Exception {
        ddl.dropTables(suffix);
    }

    @Test
    public void testPrepare() {
        logger.info("[prepare partitions started]");
        printGenerations();
        generationLogger.logStartProcessSessions(TEST_TABLE, SESSIONS);
        checkStatuses(Status.IN_PROCESS);
        printGenerations();
        logger.info("[prepare partitions finished]");
    }

    @Test
    public void testFail() {
        logger.info("[fail partitions started]");
        generationLogger.logStartProcessSessions(TEST_TABLE, SESSIONS);
        printGenerations();
        checkStatuses(Status.IN_PROCESS);
        generationLogger.logFailSessions(TEST_TABLE, SESSIONS);
        printGenerations();
        checkStatuses(Status.FAILED);
        logger.info("[fail partitions finished]");
    }

    @Test
    public void testPublish() {
        logger.info("[publish partitions started]");
        generationLogger.logStartProcessSessions(TEST_TABLE, SESSIONS);
        printGenerations();
        checkStatuses(Status.IN_PROCESS);
        generationLogger.logPublishSessions(TEST_TABLE, SESSIONS);
        printGenerations();
        checkStatuses(Status.PUBLISHED);
        logger.info("[publish partitions finished]");
    }

    private void printGenerations() {
        logger.debug("Generations: session_id, status, added_time, start_time, finished_time, rowcount");

        List<SuperControllerSession> sessions =
            generationsDao.getSCSessions(new SessionsFilter(generationLogger.getDatabase()),
                0, 1000, SuperControllerSession.Field.ADDED_TIME, true);
        for (SuperControllerSession session : sessions) {
            StringBuilder sb = new StringBuilder();
            sb.append(session.getSessionId()).append("\t");
            sb.append(String.format("%19s", session.getStatus())).append("\t");
            sb.append(session.getSessionId()).append("\t");
            sb.append(session.getAdded()).append("\t");
            sb.append(session.getStarted()).append("\t");
            sb.append(session.getFinished()).append("\t");
            sb.append(String.format("%19s", session.getRowCount())).append("\t");
            logger.debug(sb);
        }
        logger.debug("-------------------------------------------------------------");
    }

    private String getDate(Object o) {
        if (o == null) {
            return String.format("%19s", "null");
        }
        if (o instanceof java.sql.Timestamp) {
            Date date = new Date(((java.sql.Timestamp) o).getTime());
            return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(date);
        }
        return String.format("%19s", o.getClass());
    }

    private void checkStatuses(Status referenceStatus) {
        generationsDao.getSCSessions(new SessionsFilter(generationLogger.getDatabase()),
            0, 10000, SuperControllerSession.Field.ADDED_TIME, true).forEach(session -> {
            if (SESSIONS.contains(session.getSessionId())) {
                Assert.assertEquals(session.getStatus(), referenceStatus);
            }
        });
    }
}
