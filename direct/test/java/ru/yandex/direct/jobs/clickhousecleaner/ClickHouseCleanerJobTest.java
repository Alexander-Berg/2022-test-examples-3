package ru.yandex.direct.jobs.clickhousecleaner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.db.config.DbConfigFactory;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.common.db.PpcPropertyNames.PPCHOUSE_CLEANER_REAL_REMOVE;
import static ru.yandex.direct.env.EnvironmentType.PRODUCTION;
import static ru.yandex.direct.env.EnvironmentType.TESTING;


/**
 * На тестах в CI для db используются конфиги из db-config.db_testing.json и db-config.db_testing.template.json
 * Поскольку clickhouse в тестах не поднимается, там стоит "password_stub" вместо пароля.
 * При локальном запуске можно использовать конфиг db-config.devtest.json, в котором лежит конфиг ридера для прода.
 * там пароль указан в файле из .direct-tokens
 * (db-config.devtest.json можно подключить через DirectConfigFactory, а не через спринг Autowired)
 */
@JobsTest
@ExtendWith(SpringExtension.class)
public class ClickHouseCleanerJobTest {
    @Autowired
    private DbConfigFactory dbConfigFactory;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Value("${clickhouse.read_hosts_url:#{null}}")
    private String readHostsUrl;

    @Value("${clickhouse_cleaner.table_store_config_path}")
    private String tableStoreConfigPath;

    private ClickHouseCleanerJob job;

    @BeforeEach
    void setUp() {
//        dbConfigFactory = new DbConfigFactory(
//                LiveResourceFactory.get(DirectConfigFactory.getConfig().getString("db_config")).getContent()
//        );

        job = new ClickHouseCleanerJob(ppcPropertiesSupport, dbConfigFactory, readHostsUrl, tableStoreConfigPath);
    }

    @Test
    @Disabled
    public void getHostsTest() {
        List<String> hostList = job.getHosts();
        assertThat(hostList).isNotEmpty();
        System.out.println(StreamEx.of(hostList).joining(", "));
    }

    @Test
    @Disabled
    public void executeTest() {
        new ClickHouseCleanerJob(mockPropSupport(false), dbConfigFactory, readHostsUrl, tableStoreConfigPath).execute();
    }

    @Test
    @Disabled
    public void testConn() throws SQLException {
        var host = "sas-r7kw5l7jy9q9i0t0.db.yandex.net";
        try (Connection connection = job.getClickHouseConnection(host)) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("show tables");

            while (resultSet.next()) {
                String row = resultSet.getString(1);
                System.out.println(row);
            }
        }
    }

    @Test
    void checkConnectionConfigTest() {
        assertThat(job.clickhouseConfig.getEngine())
                .isEqualTo(DbConfig.Engine.CLICKHOUSE);
    }

    @Test
    public void hostTimeIsBefore() throws SQLException {
        Connection conn = mock(Connection.class);
        ResultSet resultSet = mockResultSet(conn);
        when(resultSet.next()).thenReturn(true, false);
        Instant now = Instant.now();
        when(resultSet.getLong(1)).thenReturn(now.getEpochSecond() - ClickHouseCleanerJob.MAX_DB_TIME_DIFF_SEC - 1);

        assertThat(catchThrowable(() -> ClickHouseCleanerJob.checkHostTime(conn, now)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void hostTimeIsAfter() throws SQLException {
        Connection conn = mock(Connection.class);
        ResultSet resultSet = mockResultSet(conn);
        when(resultSet.next()).thenReturn(true, false);
        Instant now = Instant.now();
        when(resultSet.getLong(1)).thenReturn(now.getEpochSecond() + ClickHouseCleanerJob.MAX_DB_TIME_DIFF_SEC + 1);

        assertThat(catchThrowable(() -> ClickHouseCleanerJob.checkHostTime(conn, now)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void checkHostTimeNotThrows() throws SQLException {
        Connection conn = mock(Connection.class);
        ResultSet resultSet = mockResultSet(conn);
        when(resultSet.next()).thenReturn(true, false);
        Instant now = Instant.now();
        when(resultSet.getLong(1)).thenReturn(now.getEpochSecond() - ClickHouseCleanerJob.MAX_DB_TIME_DIFF_SEC / 2);

        assertThat(catchThrowable(() -> ClickHouseCleanerJob.checkHostTime(conn, now)))
                .isNull();
    }

    @Test
    void testGetTableStorePeriods() {
        var tableStorePeriods = job.getTableStorePeriods();
        assertThat(tableStorePeriods).isNotEmpty();
    }

    @Test
    void allTableStoreConfigsAreReadableAndContainsSameTables() {
        var tables = job.getTableStorePeriods().keySet();
        var softly = new SoftAssertions();
        for (var env : List.of(PRODUCTION, TESTING)) {
            var path = DirectConfigFactory.getConfig(env)
                    .getString("clickhouse_cleaner.table_store_config_path");
            var tablesCfg = job.readStringPeriodMap(path);
            softly.assertThat(tablesCfg)
                    .as("Tables config for " + env + " is readable")
                    .isNotNull();
            softly.assertThat(tablesCfg.keySet())
                    .as("Tables set for " + env + " equals to unit-test config")
                    .isEqualTo(tables);
        }
        softly.assertAll();
    }


    @Test
    public void clearTablesTest() throws SQLException {
        Connection conn = mock(Connection.class);
        ResultSet resultSet = mockResultSet(conn);

        when(resultSet.next())
                .thenReturn(true, true, false)
                .thenReturn(true, false);

        when(resultSet.getString(1))
                .thenReturn("t1_partition1")
                .thenReturn("t1_partition2")
                .thenReturn("t4_partition1");


        var tablePeriods = new LinkedHashMap<String, Period>() {{ // ordered
            put("table1", Period.ofYears(3));
            put("table2", Period.ofDays(-1));
            put("table3", Period.ofMonths(0));
            put("table4", Period.ofMonths(10));
        }};

        var mockJob = mock(ClickHouseCleanerJob.class);

        var now = Instant.now();
        doCallRealMethod().when(mockJob).clearTables(any(), any(), anyString(), any(), anyBoolean());
        when(mockJob.deletePartition(any(), anyString(), anyString(), anyString())).thenReturn(true);

        int failCounts = mockJob.clearTables(conn, tablePeriods, "test_host", now, true);

        assertThat(failCounts).isZero();
        verify(mockJob, times(3)).deletePartition(any(), anyString(), anyString(), anyString());
    }

    @Test
    public void clearTablesFailuresCounter() throws SQLException {
        Connection conn = mock(Connection.class);
        ResultSet resultSet = mockResultSet(conn);

        when(resultSet.next())
                .thenReturn(true, true, false)
                .thenReturn(true, false);

        when(resultSet.getString(1))
                .thenReturn("t1_partition1")
                .thenReturn("t1_partition2")
                .thenReturn("t4_partition1");

        var tablePeriods = new LinkedHashMap<String, Period>() {{ // ordered
            put("table1", Period.ofYears(3));
            put("table2", Period.ofDays(-1));
            put("table3", Period.ofMonths(0));
            put("table4", Period.ofMonths(10));
        }};

        var mockJob = mock(ClickHouseCleanerJob.class);

        var now = Instant.now();
        doCallRealMethod().when(mockJob).clearTables(any(), any(), anyString(), any(), anyBoolean());
        when(mockJob.deletePartition(any(), anyString(), anyString(), anyString()))
                .thenReturn(false, true, true);

        int failCounts = mockJob.clearTables(conn, tablePeriods, "test_host", now, true);
        // проверяем что зафейлилось, но продолжило работу.
        assertThat(failCounts).isEqualTo(1);
        verify(mockJob, times(3)).deletePartition(any(), anyString(), anyString(), anyString());
    }


    private PpcPropertiesSupport mockPropSupport(boolean realRemove) {
        var propSupport = mock(PpcPropertiesSupport.class);
        PpcProperty<Boolean> prop = mock(PpcProperty.class);
        when(prop.getOrDefault(false)).thenReturn(realRemove);
        when(propSupport.get(eq(PPCHOUSE_CLEANER_REAL_REMOVE), any())).thenReturn(prop);
        return propSupport;
    }

    private ResultSet mockResultSet(Connection mockConnection) throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(mockConnection.prepareStatement(anyString())).thenReturn(statement);
        return resultSet;
    }
}
