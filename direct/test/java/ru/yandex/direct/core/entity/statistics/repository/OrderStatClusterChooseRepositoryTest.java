package ru.yandex.direct.core.entity.statistics.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.statistics.container.ClusterToFreshnessTimestamp;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig;
import ru.yandex.direct.ytcomponents.repository.StatsDynClusterFreshnessRepository;
import ru.yandex.direct.ytcomponents.repository.YtClusterFreshnessRepository;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator;
import ru.yandex.direct.ytwrapper.model.YtOperator;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.schema.yt.Tables.ORDERSTAT_BS;
import static ru.yandex.direct.utils.DateTimeUtils.MSK;
import static ru.yandex.direct.ytwrapper.model.YtCluster.SENECA_MAN;
import static ru.yandex.direct.ytwrapper.model.YtCluster.SENECA_SAS;
import static ru.yandex.direct.ytwrapper.model.YtCluster.SENECA_VLA;

public class OrderStatClusterChooseRepositoryTest {
    private YtClusterFreshnessRepository ytClusterFreshnessRepository;
    private StatsDynClusterFreshnessRepository statsDynClusterFreshnessRepository;
    private OrderStatClusterChooseRepository orderStatClusterChooseRepository;
    private Map<YtCluster, OrderStatClusterChooseRepository.Operators> clusterOperatorsMap;
    private Instant now;

    @Before
    public void before() {
        DirectYtDynamicConfig directYtDynamicConfig = mock(DirectYtDynamicConfig.class);
        when(directYtDynamicConfig.getClusters()).thenReturn(List.of(SENECA_MAN, SENECA_SAS, SENECA_VLA));
        when(directYtDynamicConfig.getFreshnessThresholdForOrderStat()).thenReturn(Duration.ofHours(3));
        ytClusterFreshnessRepository = mock(YtClusterFreshnessRepository.class);
        statsDynClusterFreshnessRepository = mock(StatsDynClusterFreshnessRepository.class);
        YtProvider ytProvider = mock(YtProvider.class);
        orderStatClusterChooseRepository =
                Mockito.spy(new OrderStatClusterChooseRepository(ytClusterFreshnessRepository,
                        statsDynClusterFreshnessRepository, ytProvider, directYtDynamicConfig, mock(PpcPropertiesSupport.class)));
        doReturn(Collections.emptySet())
                .when(orderStatClusterChooseRepository).getDisabledClusters();
        doReturn(Collections.emptySet())
                .when(orderStatClusterChooseRepository).getShardsToSkip();
        doReturn("order_stat_skip_shards_for_unit_tests")
                .when(orderStatClusterChooseRepository).getSkipShardsPropertyName();
        clusterOperatorsMap = getClusterOperatorsMap();
        now = Instant.now();
    }

    /**
     * Если все кластера директа синхронизированны недавно(не более, чем
     * {@link DirectYtDynamicConfig#getFreshnessThresholdForOrderStat}), то они все будут возвращены в
     * результате
     */
    @Test
    public void loadSyncTablesFreshClustersAllClusterFreshTest() {
        int shard = 1;

        when(ytClusterFreshnessRepository.loadShardToTimestamp(any())).thenReturn(Map.of(
                shard, now.toEpochMilli()
        ));

        var expectedClusters = Set.of(SENECA_MAN, SENECA_VLA, SENECA_SAS);
        var gotClusters = orderStatClusterChooseRepository.loadSyncTablesFreshClusters(clusterOperatorsMap);
        assertThat(gotClusters).hasSize(3);
        assertThat(gotClusters).isEqualTo(expectedClusters);
    }

    /**
     * Если для кластера не удалось загрузить его время синхронизации(т.е. он недоступен), то он не попадет в
     * результирующее множество
     */
    @Test
    public void loadSyncTablesFreshClustersOneClusterUnavailableTest() {
        int shard = 1;

        when(ytClusterFreshnessRepository.loadShardToTimestamp(any())).thenReturn(Map.of(
                shard, now.toEpochMilli()
        ));

        when(ytClusterFreshnessRepository.loadShardToTimestamp(eq(clusterOperatorsMap.get(SENECA_SAS).ytDynamicOperator), anySet())).thenReturn(null);

        var expectedClusters = Set.of(SENECA_MAN, SENECA_VLA);
        var gotClusters = orderStatClusterChooseRepository.loadSyncTablesFreshClusters(clusterOperatorsMap);
        assertThat(gotClusters).hasSize(2);
        assertThat(gotClusters).isEqualTo(expectedClusters);
    }

    /**
     * Если для кластера шард синхронизировался очень давно (более, чем
     * {@link DirectYtDynamicConfig#getFreshnessThresholdForOrderStat}, то он не попадет в результирующее множество
     */
    @Test
    public void loadSyncTablesFreshClustersOneClusterOldTest() {
        int shard = 1;
        when(ytClusterFreshnessRepository.loadShardToTimestamp(any(), anySet())).thenReturn(Map.of(
                shard, now.toEpochMilli()
        ));

        when(ytClusterFreshnessRepository.loadShardToTimestamp(eq(clusterOperatorsMap.get(SENECA_SAS).ytDynamicOperator), anySet())).thenReturn(Map.of(
                shard, now.minus(Duration.ofDays(4)).toEpochMilli()
        ));
        var expectedClusters = Set.of(SENECA_MAN, SENECA_VLA);
        var gotClusters = orderStatClusterChooseRepository.loadSyncTablesFreshClusters(clusterOperatorsMap);
        assertThat(gotClusters).hasSize(2);
        assertThat(gotClusters).isEqualTo(expectedClusters);
    }

    /**
     * Если все кластера синхронизировались очень давно (более, чем
     * {@link DirectYtDynamicConfig#getFreshnessThresholdForOrderStat}, то результирующее множество должно состоять
     * из всех кластеров с максимальной свежестью, несмотря на ограничение
     */
    @Test
    public void loadSyncTablesFreshClustersAllClustersOldTest() {
        int shard = 1;

        when(ytClusterFreshnessRepository.loadShardToTimestamp(any())).thenReturn(Map.of(
                shard, now.minus(Duration.ofDays(5)).toEpochMilli()));
        when(ytClusterFreshnessRepository.loadShardToTimestamp(eq(clusterOperatorsMap.get(SENECA_VLA).ytDynamicOperator), anySet()))
                .thenReturn(Map.of(shard, now.minus(Duration.ofDays(4)).toEpochMilli()));
        when(ytClusterFreshnessRepository.loadShardToTimestamp(eq(clusterOperatorsMap.get(SENECA_SAS).ytDynamicOperator), anySet()))
                .thenReturn(Map.of(shard, now.minus(Duration.ofDays(4)).toEpochMilli()));

        var expectedClusters = Set.of(SENECA_VLA, SENECA_SAS);
        var gotClusters = orderStatClusterChooseRepository.loadSyncTablesFreshClusters(clusterOperatorsMap);
        assertThat(gotClusters).hasSize(2);
        assertThat(gotClusters).isEqualTo(expectedClusters);
    }


    /**
     * Если для всех кластеров userAttribute last_sync_time_bs-chevent-log таблицы
     * {@link ru.yandex.direct.grid.schema.yt.tables.OrderstatBs} больше, чем в предыдущий запуск, то в
     * результирующем списке они будут все
     */
    @Test
    public void loadBsTablesFreshClusterByPriorityAllClustersFreshTest() {
        long previousBsCheventLog = 1564499956L;

        when(statsDynClusterFreshnessRepository.getClusterFreshnessTimeForTable(eq(clusterOperatorsMap.get(SENECA_MAN).ytOperator),
                eq(ORDERSTAT_BS))).thenReturn(
                Instant.ofEpochSecond(previousBsCheventLog + 200).atZone(MSK)
        );

        when(statsDynClusterFreshnessRepository.getClusterFreshnessTimeForTable(eq(clusterOperatorsMap.get(SENECA_VLA).ytOperator),
                eq(ORDERSTAT_BS))).thenReturn(
                Instant.ofEpochSecond(previousBsCheventLog + 300).atZone(MSK)
        );

        when(statsDynClusterFreshnessRepository.getClusterFreshnessTimeForTable(eq(clusterOperatorsMap.get(SENECA_SAS).ytOperator),
                eq(ORDERSTAT_BS))).thenReturn(
                Instant.ofEpochSecond(previousBsCheventLog + 100).atZone(MSK)
        );
        var expectedClusters = List.of(
                new ClusterToFreshnessTimestamp(SENECA_VLA, previousBsCheventLog + 300),
                new ClusterToFreshnessTimestamp(SENECA_MAN, previousBsCheventLog + 200),
                new ClusterToFreshnessTimestamp(SENECA_SAS, previousBsCheventLog + 100));

        var gotClusters = orderStatClusterChooseRepository.loadBsTablesFreshClusters(previousBsCheventLog,
                clusterOperatorsMap);

        assertThat(gotClusters).hasSize(3);
        assertThat(gotClusters).containsExactlyInAnyOrder(expectedClusters.toArray(ClusterToFreshnessTimestamp[]::new));
    }

    /**
     * Если для кластера  userAttribute last_sync_time_bs-chevent-log таблицы
     * {@link ru.yandex.direct.grid.schema.yt.tables.OrderstatBs} меньше, чем в предыдущий запуск, он не попадет в
     * результирующий список
     */
    @Test
    public void loadBsTablesFreshClusterByPriorityOneClusterOldTest() {
        long previousBsCheventLog = 1564499956L;

        when(statsDynClusterFreshnessRepository.getClusterFreshnessTimeForTable(eq(clusterOperatorsMap.get(SENECA_MAN).ytOperator),
                eq(ORDERSTAT_BS))).thenReturn(
                Instant.ofEpochSecond(previousBsCheventLog + 200).atZone(MSK)
        );

        when(statsDynClusterFreshnessRepository.getClusterFreshnessTimeForTable(eq(clusterOperatorsMap.get(SENECA_VLA).ytOperator),
                eq(ORDERSTAT_BS))).thenReturn(
                Instant.ofEpochSecond(previousBsCheventLog + 300).atZone(MSK)
        );

        when(statsDynClusterFreshnessRepository.getClusterFreshnessTimeForTable(eq(clusterOperatorsMap.get(SENECA_SAS).ytOperator),
                eq(ORDERSTAT_BS))).thenReturn(
                Instant.ofEpochSecond(previousBsCheventLog - 100).atZone(MSK)
        );

        var expectedClusters = new ClusterToFreshnessTimestamp[]{
                new ClusterToFreshnessTimestamp(SENECA_VLA,
                        previousBsCheventLog + 300),
                new ClusterToFreshnessTimestamp(SENECA_MAN,
                        previousBsCheventLog + 200)};

        var gotClusters = orderStatClusterChooseRepository.loadBsTablesFreshClusters(previousBsCheventLog,
                clusterOperatorsMap);

        assertThat(gotClusters).hasSize(2);
        assertThat(gotClusters).containsExactlyInAnyOrder(expectedClusters);
    }

    /**
     * Если все кластеры не подходят из-за недоступности или несвежести, то результирующий список будет пустым
     */
    @Test
    public void loadBsTablesFreshClusterByPriorityAllClustersNotSuitableTest() {
        long previousBsCheventLog = 1564499956L;

        when(statsDynClusterFreshnessRepository.getClusterFreshnessTimeForTable(eq(clusterOperatorsMap.get(SENECA_MAN).ytOperator),
                eq(ORDERSTAT_BS))).thenReturn(
                Instant.ofEpochSecond(previousBsCheventLog - 200).atZone(MSK)
        );

        when(statsDynClusterFreshnessRepository.getClusterFreshnessTimeForTable(eq(clusterOperatorsMap.get(SENECA_VLA).ytOperator),
                eq(ORDERSTAT_BS))).thenReturn(
                Instant.ofEpochSecond(previousBsCheventLog - 300).atZone(MSK)
        );

        when(statsDynClusterFreshnessRepository.getClusterFreshnessTimeForTable(eq(clusterOperatorsMap.get(SENECA_SAS).ytOperator),
                eq(ORDERSTAT_BS))).thenReturn(
                Instant.ofEpochSecond(previousBsCheventLog - 100).atZone(MSK)
        );

        var gotClusters = orderStatClusterChooseRepository.loadBsTablesFreshClusters(previousBsCheventLog,
                clusterOperatorsMap);

        assertThat(gotClusters).hasSize(0);
    }

    /**
     * Тест проверяет, что будет будет взято пересечение всех доступных кластеров таблиц директе и таблицы OrderStat
     */
    @Test
    public void getSuitableClustersTest() {
        int shard = 1;
        long previousBsCheventLog = 1;

        Set<YtCluster> syncTableFreshClusters = Set.of(SENECA_VLA, SENECA_SAS);
        List<ClusterToFreshnessTimestamp> bsTableFreshClusters = List.of(
                new ClusterToFreshnessTimestamp(SENECA_MAN, 5),
                new ClusterToFreshnessTimestamp(SENECA_SAS, 4),
                new ClusterToFreshnessTimestamp(SENECA_VLA, 3)
        );
        doReturn(syncTableFreshClusters).when(orderStatClusterChooseRepository).loadSyncTablesFreshClusters(
                eq(clusterOperatorsMap));
        doReturn(bsTableFreshClusters).when(orderStatClusterChooseRepository).loadBsTablesFreshClusters(eq(previousBsCheventLog), eq(clusterOperatorsMap));

        var expected = new ClusterToFreshnessTimestamp[]{
                new ClusterToFreshnessTimestamp(SENECA_SAS, 4),
                new ClusterToFreshnessTimestamp(SENECA_VLA, 3)
        };

        var got = orderStatClusterChooseRepository.getSuitableClusters(previousBsCheventLog,
                clusterOperatorsMap);
        Assertions.assertThat(got).hasSize(2);
        Assertions.assertThat(got).containsExactlyInAnyOrder(expected);
    }

    /**
     * Тест проверяет, что будет если пересечение кластеров пустое, то никакой кластер не будет выбран
     */
    @Test
    public void getSuitableClustersTestNoIntersectionTest() {
        int shard = 1;
        long previousBsCheventLog = 1;

        var syncTableFreshClusters = Set.of(SENECA_VLA, SENECA_SAS);
        var bsTableFreshClusters = List.of(
                new ClusterToFreshnessTimestamp(SENECA_MAN, 3)
        );

        doReturn(syncTableFreshClusters).when(orderStatClusterChooseRepository).loadSyncTablesFreshClusters(
                eq(clusterOperatorsMap));
        doReturn(bsTableFreshClusters).when(orderStatClusterChooseRepository).loadBsTablesFreshClusters(eq(previousBsCheventLog), eq(clusterOperatorsMap));

        var got =
                orderStatClusterChooseRepository.getSuitableClusters(previousBsCheventLog,
                        clusterOperatorsMap);
        assertThat(got).isEmpty();
    }

    /**
     * Тест проверяет, что будет будет взято пересечение всех доступных кластеров таблиц директе и таблицы OrderStat
     */
    @Test
    public void getSuitableClustersTestByPriorityTest() {
        int shard = 1;
        long previousBsCheventLog = 1;
        var unorderedClusters = Arrays.asList(
                new ClusterToFreshnessTimestamp(SENECA_SAS, 2),
                new ClusterToFreshnessTimestamp(SENECA_MAN, 1),
                new ClusterToFreshnessTimestamp(SENECA_VLA, 2)
        );
        var clusterToOperatorsMap = getClusterOperatorsMap();
        doReturn(clusterToOperatorsMap).when(orderStatClusterChooseRepository).getClusterOperators();
        doReturn(unorderedClusters).when(orderStatClusterChooseRepository).getSuitableClusters(
                eq(previousBsCheventLog), eq(clusterToOperatorsMap));


        var got = orderStatClusterChooseRepository.getClustersByPriority(previousBsCheventLog);
        assertThat(got).hasSize(3);
        assertThat(got).containsExactlyInAnyOrder(unorderedClusters.toArray(ClusterToFreshnessTimestamp[]::new));
        assertThat(got).isSortedAccordingTo(comparing(ClusterToFreshnessTimestamp::getTimestamp).reversed());
    }

    private Map<YtCluster, OrderStatClusterChooseRepository.Operators> getClusterOperatorsMap() {
        OrderStatClusterChooseRepository.Operators manOperators =
                new OrderStatClusterChooseRepository.Operators(mock(YtOperator.class),
                        mock(YtDynamicOperator.class));
        OrderStatClusterChooseRepository.Operators vlaOperators =
                new OrderStatClusterChooseRepository.Operators(mock(YtOperator.class),
                        mock(YtDynamicOperator.class));
        OrderStatClusterChooseRepository.Operators sasOperators =
                new OrderStatClusterChooseRepository.Operators(mock(YtOperator.class),
                        mock(YtDynamicOperator.class));

        return Map.of(
                SENECA_MAN, manOperators,
                SENECA_VLA, vlaOperators,
                SENECA_SAS, sasOperators
        );
    }

}
