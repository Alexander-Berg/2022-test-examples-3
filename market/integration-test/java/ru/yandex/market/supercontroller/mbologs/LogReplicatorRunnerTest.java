package ru.yandex.market.supercontroller.mbologs;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.mbo.core.dashboard.DashboardClickhouseDDL;
import ru.yandex.market.mbo.core.dashboard.GenerationsDao;
import ru.yandex.market.mbo.gwt.models.dashboard.SessionsFilter;
import ru.yandex.market.mbo.gwt.models.dashboard.SuperControllerSession;
import ru.yandex.market.mbo.gwt.models.dashboard.SuperControllerSession.Database;
import ru.yandex.market.supercontroller.mbologs.cli.CommandLineParser;
import ru.yandex.market.supercontroller.mbologs.conf.MboLogsIntegrationTestConfig;
import ru.yandex.market.supercontroller.mbologs.conf.MboLogsXmlConfig;
import ru.yandex.market.supercontroller.mbologs.dao.OraclePartitionManager;
import ru.yandex.market.supercontroller.mbologs.workers.generation_data.ToOracleGenerationDataSqlldrFileRowSaverFactory;
import ru.yandex.market.supercontroller.mbologs.workers.offer_params.ToMysqlOfferParamsRowSaverFactory;
import ru.yandex.market.supercontroller.mbologs.workers.offer_params.ToOracleParamsRowSaverFactory;
import ru.yandex.utils.yt.db.YtOffersTableService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Newer version to replicate generation data to oracle.
 * Не запускается из-под идеи, т.к. не может подняться докер с кликхаусом.
 * @author amaslak
 * @noinspection SpringAutowiredFieldsWarningInspection
 */
@RunWith(Log4jAwareSpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = {MboLogsIntegrationTestConfig.class, MboLogsXmlConfig.class},
        initializers = LogReplicatorRunnerTest.Initializer.class
)
@Ignore
public class LogReplicatorRunnerTest {

    public static final String SESSIONS = String.join(":", Mocks.SESSIONS);

    public static class Initializer extends MbologsMockContextInitializer {

        @Override
        public void registerMocks(MockRegistryPostProcessor processor) {
            processor.addMockBean("oraclePartitionManager", Mockito.mock(OraclePartitionManager.class));
            processor.addMockBean("toOracleGenerationDataSqlldrFileRowSaverFactory",
                Mockito.mock(ToOracleGenerationDataSqlldrFileRowSaverFactory.class));
            processor.addMockBean("toOracleParamsRowSaverFactory",
                Mockito.mock(ToOracleParamsRowSaverFactory.class));
            processor.addMockBean("toMysqlOfferParamsRowSaverFactory",
                Mockito.mock(ToMysqlOfferParamsRowSaverFactory.class));

            processor.addMockBean("ytGenerations", Mocks.mockGenerations());
            processor.addMockBean("ytOffersTableService", Mockito.mock(YtOffersTableService.class));

            // this mock allows read on YtMock.SOURCE_TABLE only
            Yt yt = YtMock.mockYt();
            processor.addMockBean("ytHttpApi", yt);
            processor.addMockBean("ytIndexerRelatedHttpApi", yt);
        }

    }

    @Autowired
    private LogReplicatorRunner logReplicatorRunner;

    @Autowired
    private GenerationsDao generationsDao;

    @Autowired
    private DashboardClickhouseDDL ddl;

    @Autowired
    private OraclePartitionManager oraclePartitionManager;

    private String tablesSuffix;

    @Before
    public void initTables() {
        tablesSuffix = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[.:-]", "_");
        ddl.checkTables(tablesSuffix);
        generationsDao.setSuffix(tablesSuffix);
    }

    @After
    public void dropTables() {
        ddl.dropTables(tablesSuffix);
    }

    @Test
    public void empty() throws Exception {
        // test spring context initialization
    }

    private void insertNewSessions(Database database, String tableName) {
        insertNewSessions(database, tableName, Mocks.SESSIONS);
    }

    private void insertNewSessions(Database database, String tableName, List<String> sessions) {
        sessions.forEach(session ->
            generationsDao.createNewSessions(database, null,
                tableName, ImmutableMap.of(session, 0L),
                "stratocaster", false)
        );
    }

    private void assertSessionsArePublished(Database database) {
        SessionsFilter filter = new SessionsFilter(database);
        List<SuperControllerSession> sessions = generationsDao.getSCSessions(filter, 0, 100,
            SuperControllerSession.Field.ADDED_TIME, true);

        sessions.forEach(session ->
            Assert.assertEquals("Session " + session.getSessionId() + " status must be 'published'",
                SuperControllerSession.Status.PUBLISHED, session.getStatus()));
    }

    @Test
    public void testCopyLogsToOracle() throws Exception {
        insertNewSessions(Database.ORACLE, YtMock.GD_DASHBOARD_TABLE_NAME);

        CommandLineParser parser = new CommandLineParser(new String[] {
            "--tablePath", YtMock.GD_SOURCE_TABLE,
            "--dashboardTableName", YtMock.GD_DASHBOARD_TABLE_NAME,
            "--baseSession", "20121010_1010",
            "--sessions", SESSIONS,
            "--destination", "ORACLE"
        });
        logReplicatorRunner.copyLogs(parser);

        assertSessionsArePublished(Database.ORACLE);
    }

    @Test
    public void testCopyLogsToYt() throws Exception {
        insertNewSessions(Database.YT, YtMock.GD_DASHBOARD_TABLE_NAME);

        CommandLineParser parser = new CommandLineParser(new String[] {
            "--tablePath", YtMock.GD_SOURCE_TABLE,
            "--dashboardTableName", YtMock.GD_DASHBOARD_TABLE_NAME,
            "--baseSession", "20121010_1010",
            "--sessions", SESSIONS,
            "--destination", "YT"
        });

        logReplicatorRunner.copyLogs(parser);

        assertSessionsArePublished(Database.YT);
    }

    @Test
    public void testCopyDiff() throws Exception {
        insertNewSessions(Database.YT, YtMock.GD_DASHBOARD_TABLE_NAME);

        CommandLineParser parser = new CommandLineParser(new String[] {
            "--tablePath", YtMock.GD_DIFF_SOURCE_TABLE,
            "--dashboardTableName", YtMock.GD_DASHBOARD_TABLE_NAME,
            "--destination", "YT",
            "--baseSession", "20131010_1011",
            "--tableSession", Collections.max(Mocks.SESSIONS),
            "--tableName", "mbo_offers_mr",
            "--sessions", SESSIONS
        });
        logReplicatorRunner.copyLogs(parser);

        assertSessionsArePublished(Database.YT);
    }

    @Test
    public void testCopyParamsToOracle() throws Exception {
        //noinspection unchecked
        Mockito.doAnswer(i -> {
            Assert.assertEquals("sc_offer_params", i.getArgument(0));
            return null;
        }).when(oraclePartitionManager)
            .preparePartitions(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.any(Set.class), Mockito.anyBoolean(), Mockito.anyBoolean()
            );

        insertNewSessions(Database.YT, YtMock.OP_DASHBOARD_TABLE_NAME);

        CommandLineParser parser = new CommandLineParser(new String[] {
            "--tablePath", YtMock.OP_SOURCE_TABLE,
            "--tableName", "params_mr",
            "--dashboardTableName", YtMock.OP_DASHBOARD_TABLE_NAME,
            "--baseSession", "20171122_2104",
            "--sessions", SESSIONS,
            "--destination", "ORACLE"
        });
        logReplicatorRunner.copyLogs(parser);

        assertSessionsArePublished(Database.ORACLE);
        //noinspection unchecked
        Mockito.verify(oraclePartitionManager, Mockito.times(1))
            .preparePartitions(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.any(Set.class), Mockito.anyBoolean(), Mockito.anyBoolean()
            );
        Mockito.reset(oraclePartitionManager);
    }
}
