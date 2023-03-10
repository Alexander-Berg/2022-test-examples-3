package ru.yandex.direct.manualtests.tasks.slowlogs;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.cloud.mdb.mysql.api.ICloudMdbMySqlApi;
import ru.yandex.direct.cloud.mdb.mysql.api.service.CloudMdbMySqlApiService;
import ru.yandex.direct.cloud.mdb.mysql.api.transport.ClusterRawInfo;
import ru.yandex.direct.cloud.mdb.mysql.api.transport.GetClustersListRequest;
import ru.yandex.direct.cloud.mdb.mysql.api.transport.GetClustersListResponse;
import ru.yandex.direct.manualtests.app.TestTasksRunner;
import ru.yandex.direct.mysql.slowlog.writer.states.MySqlClusterSlowLogsWriterFullStateInfo;
import ru.yandex.direct.mysql.slowlog.writer.states.MySqlClusterSlowLogsWriterStateInfo;
import ru.yandex.direct.mysql.slowlog.writer.states.MySqlClusterSlowLogsWriterStateProvider;

/**
 * Дописывает ppc проперти для перекладывателя mysql slow query логов для всех кластеров в той или иной конфигурации,
 * и удаляет для тех кластеров, которых в конфигурации нет.
 */
@Component
public class MySqlSlowQueryLogsWriterStatesHelper implements Runnable {
    private static final String DIRECT_INFRA_CLOUD_FOLDER_ID = "fooa07bcrr7souccreru";
    private static final String DB_ENVIRONMENT = "testing".toLowerCase();

    @Autowired
    private MySqlClusterSlowLogsWriterStateProvider clustersStatesProvider;

    @Autowired
    private CloudMdbMySqlApiService cloudMdbMySqlApi;

    public static void main(String[] args) {
        TestTasksRunner.runTask(MySqlSlowQueryLogsWriterStatesHelper.class, SlowLogsConfiguration.class, args);
    }

    @Override
    public void run() {
        try {
            deleteClustersStates();
            String config = getClustersInitialConfig(DB_ENVIRONMENT);
            System.out.println(config);
        } catch (InterruptedException it) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    private String getClustersInitialConfig(String clusterEnvironmentFilter) throws IOException, InterruptedException {
        Predicate<ClusterRawInfo> clustersFilter = (cluster) ->
                cluster.getName().toLowerCase().startsWith("ppcd")
                        && cluster.getName().toLowerCase().contains(clusterEnvironmentFilter);
        List<ClusterRawInfo> allClusters = getMySqlClusters(
                cloudMdbMySqlApi, DIRECT_INFRA_CLOUD_FOLDER_ID, 100, clustersFilter);
        StringBuilder sbld = new StringBuilder();
        sbld.append("\tclusters: [ # generated by MySqlSlowQueryLogsWriterStatesHelper.getClustersInitialConfig()\n");
        for (ClusterRawInfo cluster : allClusters) {
            sbld.append(String.format("\t\t\"%s\"\n", cluster.getName()));
        }
        sbld.append("\t],\n");
        return sbld.toString();
    }

    private void updateClustersStates(String clusterEnvironmentFilter, boolean override)
            throws IOException, InterruptedException {
        Predicate<ClusterRawInfo> clustersFilter = (cluster) ->
                cluster.getName().toLowerCase().startsWith("ppcd")
                        && cluster.getName().toLowerCase().contains(clusterEnvironmentFilter);
        List<ClusterRawInfo> allClusters = getMySqlClusters(
                cloudMdbMySqlApi, DIRECT_INFRA_CLOUD_FOLDER_ID, 100, clustersFilter);
        Set<String> existsClusterNames = new HashSet<>(clustersStatesProvider.getClustersNames());
        Map<String, ClusterRawInfo> clustersByNameMap = allClusters.stream()
                .collect(Collectors.toMap(ClusterRawInfo::getName, Function.identity()));
        for (String existClusterName : existsClusterNames) {
            if (override || !clustersByNameMap.containsKey(existClusterName)) {
                clustersStatesProvider.remove(existClusterName);
            }
        }
        Instant time = LocalDateTime.of(2021, 11, 10, 21, 41, 35)
                .atZone(ZoneId.systemDefault()).toInstant();
        time = Instant.ofEpochSecond(time.getEpochSecond());
        for (Map.Entry<String, ClusterRawInfo> entry : clustersByNameMap.entrySet()) {
            if (!override && existsClusterNames.contains(entry.getKey())) {
                continue;
            }
            ClusterRawInfo cluster = entry.getValue();
            MySqlClusterSlowLogsWriterStateInfo clusterState = new MySqlClusterSlowLogsWriterStateInfo(
                    cluster.getId(), 1, time, -1, true);
            clustersStatesProvider.compareAndSwap(
                    cluster.getName(),
                    new MySqlClusterSlowLogsWriterFullStateInfo(null, clusterState));
        }
    }

    private void deleteClustersStates() {
        Set<String> existsClusterNames = new HashSet<>(clustersStatesProvider.getClustersNames());
        for (String existClusterName : existsClusterNames) {
            clustersStatesProvider.remove(existClusterName);
        }
    }

    private static List<ClusterRawInfo> getMySqlClusters(
            ICloudMdbMySqlApi cloudApi, String folderId, int pageSize, Predicate<ClusterRawInfo> clusterFilter)
            throws IOException, InterruptedException {
        List<ClusterRawInfo> allClusters = new ArrayList<>();
        String nextPageToken = null;
        do {
            GetClustersListResponse clustersResponse =
                    cloudApi.getClustersList(new GetClustersListRequest(folderId, pageSize, nextPageToken));
            for (ClusterRawInfo cluster : clustersResponse.getElements()) {
                if (clusterFilter != null && clusterFilter.test(cluster)) {
                    allClusters.add(cluster);
                }
            }
            nextPageToken = clustersResponse.getNextPageToken();
        } while (nextPageToken != null && !nextPageToken.isBlank());
        return allClusters;
    }
}
