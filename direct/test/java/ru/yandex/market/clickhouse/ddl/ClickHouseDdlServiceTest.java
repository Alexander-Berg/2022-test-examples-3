package ru.yandex.market.clickhouse.ddl;

import com.google.common.collect.Maps;
import com.mockrunner.jdbc.JDBCTestModule;
import com.mockrunner.jdbc.PreparedStatementResultSetHandler;
import com.mockrunner.jdbc.StatementResultSetHandler;
import com.mockrunner.mock.jdbc.JDBCMockObjectFactory;
import com.mockrunner.mock.jdbc.MockResultSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.clickhouse.settings.ClickHouseProperties;
import ru.yandex.market.clickhouse.ddl.engine.MergeTree;
import ru.yandex.market.monitoring.MonitoringStatus;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 04/04/2018
 */
@RunWith(MockitoJUnitRunner.class)
public class ClickHouseDdlServiceTest {

    private static final Logger log = LogManager.getLogger();

    @Test
    public void testCreateCluster() {
        TestCluster cl1 = createCl1();

        MockJdbcFactory mockJdbcFactory = new MockJdbcFactory(cl1);
        ClickHouseDdlService service = ClickHouseDdlService.create(
            Collections.singletonList("seedHost"), new ClickHouseProperties(), "cl1", mockJdbcFactory
        );
        ClickHouseCluster cluster = service.getCluster();
        Assert.assertEquals("cl1", cluster.getName());
        Assert.assertEquals(4, cluster.getServers().size());
        Assert.assertEquals(new ClickHouseCluster.Server("test01h", 1, 1), cluster.getServer("test01h"));
        Assert.assertEquals(new ClickHouseCluster.Server("test02h", 2, 1), cluster.getServer("test02h"));
        Assert.assertEquals(new ClickHouseCluster.Server("test01e", 1, 2), cluster.getServer("test01e"));
        Assert.assertEquals(new ClickHouseCluster.Server("test02e", 2, 2), cluster.getServer("test02e"));

        mockJdbcFactory.checkQueriesExecuted();
    }

    @Test
    public void testCreateTable() {

        ClickHouseTableDefinition tableDefinition = new ClickHouseTableDefinitionImpl(
            "db1", "table1",
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("string", ColumnType.String),
                new Column("uInt8", ColumnType.UInt8),
                new Column("int64", ColumnType.Int64),
                new Column("other_string.new", ColumnType.String)
            ),
            MergeTree.fromOldDefinition("date", Arrays.asList("uInt8", "string"), 8192)
        );


        TestCluster cluster = createCl1(
            tableExistsQuery("db1.table1", false),
            new ExpectedQuery("CREATE DATABASE IF NOT EXISTS db1"),
            new ExpectedQuery("CREATE TABLE IF NOT EXISTS db1.table1 ( `date` Date, `string` String, `uInt8` UInt8, `int64` Int64, `other_string.new` String ) ENGINE = MergeTree() PARTITION BY toYYYYMM(date) ORDER BY (uInt8, string) SETTINGS index_granularity = 8192")
        );

        MockJdbcFactory mockJdbcFactory = new MockJdbcFactory(cluster);

        ClickHouseDdlService service = ClickHouseDdlService.create(
            Collections.singletonList("seedHost"), new ClickHouseProperties(), "cl1", mockJdbcFactory
        );

        TableDdlState ddlState = service.applyDdl(tableDefinition);
        Assert.assertEquals(MonitoringStatus.OK, ddlState.getGlobalStatus());
        Assert.assertEquals(2, ddlState.getShardStates().size());
        for (TableDdlState.ShardState shardState : ddlState.getShardStates()) {
            Assert.assertEquals(2, shardState.getServerStates().size());
            Assert.assertEquals(MonitoringStatus.OK, shardState.getShardStatus());
            for (TableDdlState.ServerState serverState : shardState.getServerStates()) {
                Assert.assertEquals(MonitoringStatus.OK, serverState.getReplicaStatus());
                DDL ddl = serverState.getDdl();
                Assert.assertFalse(ddl.requireAttention());
                Assert.assertTrue(ddl.getErrors().isEmpty());
                Assert.assertTrue(ddl.getManualUpdates().isEmpty());
                Assert.assertTrue(ddl.getUpdates().isEmpty());
            }
        }

        mockJdbcFactory.checkQueriesExecuted();
    }

    private static ExpectedQuery tableExistsQuery(String fullTableName, boolean exists) {
        MockResultSet rs = new MockResultSet("exists " + fullTableName);
        rs.addColumn("result", Collections.singletonList(exists ? 1 : 0));
        return new ExpectedQuery("EXISTS TABLE " + fullTableName, rs);
    }


    private static TestCluster createCl1(ExpectedQuery... allHostQueries) {

        TestServer test01h = new TestServer("test01h", 1, 1, allHostQueries);
        TestServer test01e = new TestServer("test01e", 1, 2, allHostQueries);
        TestServer test02h = new TestServer("test02h", 2, 1, allHostQueries);
        TestServer test02e = new TestServer("test02e", 2, 2, allHostQueries);

        return new TestCluster("cl1", test01h, test02h, test01e, test02e);
    }

    @Test
    public void checkPartitionQuotes() {
        ClickHouseDdlService service = Mockito.mock(ClickHouseDdlService.class);
        Mockito.when(service.checkPartitionQuotes(Mockito.any())).thenCallRealMethod();

        List<String> partitions = Arrays.asList(
            "2019-01-01", "'2019-01-02'", "201901", "201902", "tuple()", "201901_020202"
        );
        List<String> expectedPartitions = Arrays.asList(
            "'2019-01-01'", "'2019-01-02'", "201901", "201902", "tuple()", "201901_020202"
        );
        Assert.assertEquals(expectedPartitions, service.checkPartitionQuotes(partitions));
    }

    private static class TestCluster {
        private final String clusterName;
        private final List<TestServer> servers;
        private final Map<String, TestServer> hostToServer;

        private TestCluster(String clusterName, TestServer... servers) {
            this.clusterName = clusterName;
            this.servers = Arrays.asList(servers);
            hostToServer = Maps.uniqueIndex(this.servers, TestServer::getHost);
        }

        public List<ExpectedQuery> getClusterExpectedQueries() {
            MockResultSet rs = new MockResultSet("cluster");
            rs.addColumn("host_name", servers.stream().map(TestServer::getHost).collect(Collectors.toList()));
            rs.addColumn("shard_num", servers.stream().map(TestServer::getShardNumber).collect(Collectors.toList()));
            rs.addColumn("replica_num", servers.stream().map(TestServer::getReplicaNumber).collect(Collectors.toList()));
            return Arrays.asList(new ExpectedQuery("SELECT * FROM system.clusters WHERE cluster = ?", rs, clusterName));
        }

        public TestServer getServer(String host) {
            return hostToServer.get(host);
        }
    }

    private static class TestServer {
        private final String host;
        private final int shardNumber;
        private final int replicaNumber;
        private final List<ExpectedQuery> expectedQueries;

        public TestServer(String host, int shardNumber, int replicaNumber, ExpectedQuery... expectedQueries) {
            this.host = host;
            this.shardNumber = shardNumber;
            this.replicaNumber = replicaNumber;
            this.expectedQueries = Arrays.asList(expectedQueries);
        }

        public int getReplicaNumber() {
            return replicaNumber;
        }

        public int getShardNumber() {
            return shardNumber;
        }

        public String getHost() {
            return host;
        }

        public List<ExpectedQuery> getExpectedQueries() {
            return expectedQueries;
        }
    }

    private static class ExpectedQuery {
        private final String query;
        private final List<Object> params;
        private final MockResultSet result;
        private final SQLException exception;

        public ExpectedQuery(String query) {
            this.query = query;
            params = Collections.emptyList();
            result = null; // new MockResultSet(String.valueOf(Math.random()));
            exception = null;
        }

        public ExpectedQuery(String query, MockResultSet result, Object... params) {
            this.query = query;
            this.result = result;
            this.exception = null;
            this.params = Arrays.asList(params);
        }

        public ExpectedQuery(String query, SQLException exception) {
            this.query = query;
            this.exception = exception;
            this.result = null;
            this.params = null;
        }

        public String getQuery() {
            return query;
        }

        public List<Object> getParams() {
            return params;
        }

        public MockResultSet getResult() {
            return result;
        }

        public SQLException getException() {
            return exception;
        }
    }

    private class MockJdbcFactory implements ClickHouseDdlService.JdbcTemplateFactory {

        private TestCluster testCluster;
        private JDBCTestModule clusterTestModule;
        private Map<String, JDBCTestModule> hostTestModules = new HashMap<>();

        public MockJdbcFactory(TestCluster testCluster) {
            this.testCluster = testCluster;
        }

        @Override
        public JdbcTemplate create(String host, DataSource dataSource) {
            if (host == null) {
                JDBCMockObjectFactory clusterFactory = createJdbcMock(testCluster.getClusterExpectedQueries());
                clusterTestModule = new JDBCTestModule(clusterFactory);
                return new JdbcTemplate(clusterFactory.getMockDataSource());
            } else {
                JDBCMockObjectFactory hostFactory = createJdbcMock(testCluster.getServer(host).getExpectedQueries());
                hostTestModules.put(host, new JDBCTestModule(hostFactory));
                return new JdbcTemplate(hostFactory.getMockDataSource());
            }
        }

        public void checkQueriesExecuted() {
            checkExecutedQueries(testCluster.getClusterExpectedQueries(), clusterTestModule);
            for (TestServer server : testCluster.servers) {
                checkExecutedQueries(server.getExpectedQueries(), hostTestModules.get(server.host));
            }
        }
    }

    private void checkExecutedQueries(List<ExpectedQuery> expectedQueries, JDBCTestModule testModule) {
        Assert.assertEquals(
            expectedQueries.stream().map(ExpectedQuery::getQuery).collect(Collectors.toList()),
            testModule.getExecutedSQLStatements()
        );
    }

    private JDBCMockObjectFactory createJdbcMock(List<ExpectedQuery> expectedQueries) {

        JDBCMockObjectFactory factory = new JDBCMockObjectFactory();
        PreparedStatementResultSetHandler psRsHandler = factory.getMockConnection().getPreparedStatementResultSetHandler();
        StatementResultSetHandler rsHandler = factory.getMockConnection().getStatementResultSetHandler();

        for (ExpectedQuery expectedQuery : expectedQueries) {
            if (expectedQuery.exception != null) {
                psRsHandler.prepareThrowsSQLException(expectedQuery.query, expectedQuery.exception);
            } else {
                if (expectedQuery.params.isEmpty()) {
                    rsHandler.prepareResultSet(expectedQuery.query, expectedQuery.result);
                    psRsHandler.prepareResultSet(expectedQuery.query, expectedQuery.result);
                } else {
                    psRsHandler.prepareResultSet(expectedQuery.query, expectedQuery.result, expectedQuery.params);
                }
            }
        }
        return factory;
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteReplicatedQuery() {
        String query = "Replicated Query";
        TestCluster cl1 = createCl1();
        MockJdbcFactory mockJdbcFactory = new MockJdbcFactory(cl1);
        ClickHouseDdlService service = ClickHouseDdlService.create(
            Collections.singletonList("seedHost"), new ClickHouseProperties(), "cl1", mockJdbcFactory
        );

        /* Waiting success results only on one of replicas per shard */
        Set<ClickHouseCluster.Server> executedQueryOnServer = new HashSet<>();

        service.executeReplicatedQuery((jdbcTemplate, server) -> {
            executedQueryOnServer.add(server);
            return Collections.singletonList(query);
        });

        long shardCount = cl1.servers.stream()
            .map(s -> s.shardNumber)
            .distinct()
            .count();

        Assert.assertEquals(shardCount, executedQueryOnServer.size());

        /* Waiting success results only on 2nd replica per shard */

        String query2 = "2nd replica's query";

        service.executeReplicatedQuery((jdbcTemplate, server) -> {
            if (server.getReplicaNumber() == 1) {
                throw new RuntimeException("Query failed in test, It's okay");
            }

            return Collections.singletonList(query2);
        });

        List<Integer> replicaNumbers = mockJdbcFactory.hostTestModules.entrySet().stream()
            .filter(e -> e.getValue().getExecutedSQLStatements().contains(query2))
            .map(e -> cl1.hostToServer.get(e.getKey()).replicaNumber)
            .collect(Collectors.toList());

        Assert.assertEquals(replicaNumbers.size(), 2);
        replicaNumbers.forEach(num -> Assert.assertEquals(shardCount, (long) num));


        /* Waiting exception because of all queries will be failed */
        boolean throwException = true;
        service.executeReplicatedQuery((jdbcTemplate, server) -> {
            if (throwException) {
                throw new RuntimeException("Query failed in test, It's okay");
            }
            return Collections.singletonList(query2);
        });
    }
}
