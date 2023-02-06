package ru.yandex.direct.test.clickhouse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;

import ru.yandex.direct.clickhouse.ClickHouseCluster;
import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.db.config.DbConfigException;
import ru.yandex.direct.db.config.DbConfigFactory;
import ru.yandex.direct.dbutil.wrapper.DataSourceFactory;

@ParametersAreNonnullByDefault
abstract class ClickHouseClusterTestBase {
    static final String[] CLICKHOUSES = new String[]{
            "clickhouse01", "clickhouse02", "clickhouse03", "clickhouse04",
            "clickhouse05", "clickhouse06"
    };

    static ClickHouseCluster cluster;
    static String originalDbConfigJson;
    static DbConfigFactory dbConfigFactory;
    static DataSourceFactory dataSourceFactory;
    Map<String, DataSource> dataSources;
    String dbName;

    Map<String, List<String>> distributedTestPart() throws SQLException {
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            try (Connection conn = entry.getValue().getConnection()) {
                try (PreparedStatement st = conn
                        .prepareStatement("CREATE TABLE `source` ENGINE = TinyLog() AS SELECT ? AS `host`")) {
                    st.setString(1, entry.getKey());
                    st.execute();
                }
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate(String.format(
                            "CREATE TABLE `distributed` (`host` String) ENGINE = Distributed(shard, %1$s, source)",
                            dbName));
                }
            }
        }
        return fetchHosts("distributed");
    }

    <T> Map<String, T> forAllDbConfigChildrenRecursively(String source, Function<DbConfig, T> fn) {
        List<String> paths = new ArrayList<>();
        paths.add(source);
        for (int pathIdx = 0; pathIdx < paths.size(); ++pathIdx) {
            String prefix = paths.get(pathIdx);
            List<String> childNames = dbConfigFactory.getChildNames(prefix);
            for (String child : childNames) {
                paths.add(prefix + ":" + child);
            }
        }
        return paths.stream()
                .map(path -> {
                    try {
                        return dbConfigFactory.get(path);
                    } catch (DbConfigException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(DbConfig::getDbName, fn));
    }

    @SuppressWarnings("squid:S2925")
        // Thread.sleep
    Map<String, List<String>> mergeTestPart() throws InterruptedException, SQLException {
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            try (Connection conn = entry.getValue().getConnection()) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate(String.format(
                            "CREATE TABLE IF NOT EXISTS `merge` (`date` Date MATERIALIZED today(), `host` String)"
                                    + " ENGINE = ReplicatedMergeTree("
                                    + " '/clickhouse/tables/{shard}/merge_%1$s', '{replica}',"
                                    + " `date`, (`host`), 512)",
                            dbName));
                }
                try (PreparedStatement st = conn
                        .prepareStatement("INSERT INTO `merge` (`host`) VALUES (?)")) {
                    st.setString(1, entry.getKey());
                    st.execute();
                }
            }
        }
        // В ReplicatedMergeTree асинхронная репликация. Нет документированной возможности отследить, что запись была
        // синхронизирована со всеми репликами. Предполагается, что за это время реплики будут синхронизированы.
        Thread.sleep(250);
        return fetchHosts("merge");
    }

    Map<String, List<String>> fetchHosts(String fetchFromTable) throws SQLException {
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            List<String> hostResult = new ArrayList<>();
            try (Connection conn = entry.getValue().getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT `host` FROM `" + fetchFromTable + "` ORDER BY `host`")) {
                while (rs.next()) {
                    hostResult.add(rs.getString(1));
                }
            }
            result.put(entry.getKey(), hostResult);
        }
        return result;
    }
}
