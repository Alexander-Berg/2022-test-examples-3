package ru.yandex.direct.grid.core.util.yt;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfigMockFactory;
import ru.yandex.direct.ytcomponents.repository.YtClusterFreshnessRepository;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator;
import ru.yandex.direct.ytwrapper.model.YtTable;
import ru.yandex.inside.yt.kosher.impl.common.YtException;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YsonTags;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.request.GetNode;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.core.entity.recommendation.RecommendationTablesUtils.serializeMinTimestamps;
import static ru.yandex.direct.ytcomponents.repository.YtClusterFreshnessRepository.DBNAME_FIELD;
import static ru.yandex.direct.ytcomponents.repository.YtClusterFreshnessRepository.GTID_SET_FIELD;
import static ru.yandex.direct.ytcomponents.repository.YtClusterFreshnessRepository.LAST_TIMESTAMP_FIELD;


public class YtClusterFreshnessLoaderTest {
    private static final YtTable TEST_TABLE = new YtTable("//tmp/some/path");
    private static final String RECOMMENDATION_TABLE_PATH = "//tmp/some/recommendations";
    private static final String YT_NULL_VALUE = String.valueOf(YsonTags.ENTITY);
    private static final String GTID_SET_VALUE =
            "3785dbf8-f2f3-11ea-8114-da0aa51b98ab:1-29554687,5aeb83cb-f2f3-11ea-8737-a9bebf814aec:1-11429768";

    @Mock
    private YtDynamicOperator hahnOperator;

    @Mock
    private YtDynamicOperator senecaOperator;

    private Map<YtCluster, ClusterFreshnessInfo> freshnessMap;

    private YtClusterFreshnessLoader ytClusterFreshnessLoader;
    private YtClusterFreshnessRepository ytClusterFreshnessRepository;
    private YtProvider ytProvider;
    private DirectYtDynamicConfig dynamicConfig;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        dynamicConfig = DirectYtDynamicConfigMockFactory.createConfigMock();
        when(dynamicConfig.tables().direct().syncStatesTablePath())
                .thenReturn(TEST_TABLE.getPath());
        when(dynamicConfig.tables().recommendations().recommendationsTablePath())
                .thenReturn(RECOMMENDATION_TABLE_PATH);
        when(dynamicConfig.getClusters())
                .thenReturn(Arrays.asList(YtCluster.HAHN, YtCluster.SENECA_MAN));
        when(dynamicConfig.getClusterRefreshPeriod())
                .thenReturn(Duration.ofSeconds(5));

        ytProvider = mock(YtProvider.class);
        doReturn(hahnOperator).when(ytProvider).getDynamicOperator(eq(YtCluster.HAHN));
        when(hahnOperator.getCluster()).thenReturn(YtCluster.HAHN);
        doReturn(senecaOperator).when(ytProvider).getDynamicOperator(eq(YtCluster.SENECA_MAN));
        when(senecaOperator.getCluster()).thenReturn(YtCluster.SENECA_MAN);

        ytClusterFreshnessRepository = new YtClusterFreshnessRepository(dynamicConfig);
        ytClusterFreshnessLoader =
                new YtClusterFreshnessLoader(ytProvider, dynamicConfig, ytClusterFreshnessRepository);
        ytClusterFreshnessLoader.addHandler(freshnessMap -> this.freshnessMap = freshnessMap);
        ytClusterFreshnessLoader.fillClusterToOperator();
    }

    private UnversionedRowset wrapInRowset(List<YTreeNode> nodes) {
        UnversionedRowset rowset = mock(UnversionedRowset.class);
        doReturn(nodes)
                .when(rowset).getYTreeRows();
        return rowset;
    }

    @Test
    public void testGetShardToClusterCompareNode_correctResponse() {
        long currentTimeMillis = System.currentTimeMillis();

        doReturn(wrapInRowset(
                Arrays.asList(
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 100)
                                .key(DBNAME_FIELD.getName()).value("ppc:1")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build(),
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 1000)
                                .key(DBNAME_FIELD.getName()).value("ppc:2")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build(),
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 500)
                                .key(DBNAME_FIELD.getName()).value("ppc:3")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build())))
                .when(hahnOperator).selectRows(any(String.class), any());

        Map<Integer, Long> shardToTimestamp = ytClusterFreshnessRepository.loadShardToTimestamp(hahnOperator);

        assertThat(shardToTimestamp)
                .isNotNull()
                .containsOnlyKeys(1, 2, 3);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(shardToTimestamp.get(1))
                .isEqualTo(currentTimeMillis - 100);
        soft.assertThat(shardToTimestamp.get(2))
                .isEqualTo(currentTimeMillis - 1000);
        soft.assertThat(shardToTimestamp.get(3))
                .isEqualTo(currentTimeMillis - 500);

        soft.assertAll();
    }

    @Test
    public void testGetShardToClusterCompareNodeSkipDbNames_correctResponse() {
        long currentTimeMillis = System.currentTimeMillis();

        doReturn(wrapInRowset(
                Arrays.asList(
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 100)
                                .key(DBNAME_FIELD.getName()).value("ppc:1")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build(),
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 1000)
                                .key(DBNAME_FIELD.getName()).value("ppc:2")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build(),
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 500)
                                .key(DBNAME_FIELD.getName()).value("ppc:3")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build())))
                .when(hahnOperator).selectRows(any(String.class), any());

        Map<Integer, Long> shardToTimestamp = ytClusterFreshnessRepository.loadShardToTimestamp(hahnOperator, Set.of());

        assertThat(shardToTimestamp)
                .isNotNull()
                .containsOnlyKeys(1, 2, 3);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(shardToTimestamp.get(1))
                .isEqualTo(currentTimeMillis - 100);
        soft.assertThat(shardToTimestamp.get(2))
                .isEqualTo(currentTimeMillis - 1000);
        soft.assertThat(shardToTimestamp.get(3))
                .isEqualTo(currentTimeMillis - 500);

        Map<Integer, Long> shardToTimestampWithSkippedNames = ytClusterFreshnessRepository.loadShardToTimestamp(hahnOperator, Set.of(2, 3));

        assertThat(shardToTimestampWithSkippedNames)
                .isNotNull()
                .containsOnlyKeys(1);

        soft.assertThat(shardToTimestampWithSkippedNames.get(1))
                .isEqualTo(currentTimeMillis - 100);


        soft.assertAll();
    }

    @Test
    public void testGetShardToClusterCompareNodeSkipDbNamesWithNullTimestamp_correctResponse() {
        long currentTimeMillis = System.currentTimeMillis();

        doReturn(wrapInRowset(
                Arrays.asList(
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 100)
                                .key(DBNAME_FIELD.getName()).value("ppc:1")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build(),
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 1000)
                                .key(DBNAME_FIELD.getName()).value("ppc:2")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build(),
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value((Long) null)
                                .key(DBNAME_FIELD.getName()).value("ppc:3")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build())))
                .when(hahnOperator).selectRows(any(String.class), any());

        Map<Integer, Long> shardToTimestamp = ytClusterFreshnessRepository.loadShardToTimestamp(hahnOperator, Set.of(3));

        assertThat(shardToTimestamp)
                .isNotNull()
                .containsOnlyKeys(1, 2);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(shardToTimestamp.get(1))
                .isEqualTo(currentTimeMillis - 100);
        soft.assertThat(shardToTimestamp.get(2))
                .isEqualTo(currentTimeMillis - 1000);

        soft.assertAll();
    }

    @Test
    public void testGetShardToClusterCompareNode_nullOnYtError() {
        doThrow(new YtException("Some error", new RuntimeException()))
                .when(hahnOperator).selectRows(any(String.class), any());

        assertThat(ytClusterFreshnessRepository.loadShardToTimestamp(hahnOperator))
                .isNull();
    }

    @Test
    public void testGetShardToClusterCompareNode_nullOnNullTimestamp() {
        doReturn(wrapInRowset(Collections.singletonList(
                YTree.mapBuilder()
                        .key(LAST_TIMESTAMP_FIELD.getName()).value(YT_NULL_VALUE)
                        .key(DBNAME_FIELD.getName()).value("ppc:1")
                        .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                        .endMap().build())))
                .when(hahnOperator).selectRows(any(String.class), any());

        assertThat(ytClusterFreshnessRepository.loadShardToTimestamp(hahnOperator))
                .isNull();
    }

    @Test
    public void testGetShardToClusterCompareNode_nullOnNoTimestamps() {
        doReturn(wrapInRowset(Collections.emptyList()))
                .when(hahnOperator).selectRows(any(String.class), any());

        assertThat(ytClusterFreshnessRepository.loadShardToTimestamp(hahnOperator))
                .isNull();
    }

    @Test
    public void testGetAllClustersFreshness_allNull() {
        doReturn(wrapInRowset(Collections.emptyList()))
                .when(hahnOperator).selectRows(any(String.class), any());
        doThrow(new YtException("Some error", new RuntimeException()))
                .when(senecaOperator).selectRows(any(String.class), any());

        assertThatThrownBy(() -> ytClusterFreshnessLoader.getAllClustersFreshness())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testRun() {
        long currentTimeMillis = System.currentTimeMillis();

        doReturn(wrapInRowset(
                Arrays.asList(
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 300)
                                .key(DBNAME_FIELD.getName()).value("ppc:1")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build(),
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 1000)
                                .key(DBNAME_FIELD.getName()).value("ppc:2")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build(),
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 500)
                                .key(DBNAME_FIELD.getName()).value("ppc:3")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build())))
                .when(hahnOperator).selectRows(any(String.class), any());
        doReturn(wrapInRowset(
                Arrays.asList(
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 200)
                                .key(DBNAME_FIELD.getName()).value("ppc:1")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build(),
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 21000)
                                .key(DBNAME_FIELD.getName()).value("ppc:2")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build(),
                        YTree.mapBuilder()
                                .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis - 2500)
                                .key(DBNAME_FIELD.getName()).value("ppc:3")
                                .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                                .endMap().build())))
                .when(senecaOperator).selectRows(any(String.class), any());

        YtClient hahnYtClient = mock(YtClient.class);
        when(hahnOperator.getYtClient()).thenReturn(hahnYtClient);
        when(hahnOperator.getCluster()).thenReturn(YtCluster.HAHN);
        when(hahnYtClient.getNode(any(GetNode.class))).thenReturn(
                CompletableFuture.supplyAsync(() ->
                        YTree.attributesBuilder()
                                .key("attr_min_timestamps")
                                .value(serializeMinTimestamps(
                                        // Здесь должны быть секунды
                                        ImmutableMap.of(
                                                1, (currentTimeMillis - 1000) / 1000,
                                                2, (currentTimeMillis - 2000) / 1000)))
                                .endAttributes()
                                .value("empty_value")
                                .build()
                )
        );

        YtClient senecaYtClient = mock(YtClient.class);
        when(senecaOperator.getYtClient()).thenReturn(senecaYtClient);
        when(senecaOperator.getCluster()).thenReturn(YtCluster.SENECA_MAN);
        when(senecaYtClient.getNode(any(GetNode.class))).thenReturn(
                CompletableFuture.supplyAsync(() ->
                        YTree.attributesBuilder()
                                .key("attr_min_timestamps")
                                .value(serializeMinTimestamps(
                                        // Здесь должны быть секунды
                                        ImmutableMap.of(
                                                1, (currentTimeMillis - 4000) / 1000,
                                                2, (currentTimeMillis - 3000) / 1000)))
                                .endAttributes()
                                .value("empty_value")
                                .build()
                )
        );

        ytClusterFreshnessLoader.runUpdateFreshness();

        assertThat(freshnessMap)
                .size().isEqualTo(2);

        assertThat(freshnessMap.get(YtCluster.HAHN))
                .isEqualTo(
                        new ClusterFreshnessInfo(
                                ImmutableMap.of(
                                        1, currentTimeMillis - 300,
                                        2, currentTimeMillis - 1000,
                                        3, currentTimeMillis - 500),
                                ((currentTimeMillis - 1000) / 1000) * 1000));

        assertThat(freshnessMap.get(YtCluster.SENECA_MAN))
                .isEqualTo(
                        new ClusterFreshnessInfo(
                                ImmutableMap.of(
                                        1, currentTimeMillis - 200,
                                        2, currentTimeMillis - 21000,
                                        3, currentTimeMillis - 2500),
                                ((currentTimeMillis - 3000) / 1000) * 1000));
    }

    @Test
    public void fillClusterToOperator_success_whenClusterIsNotAvailable() {
        doThrow(new RuntimeException()).when(ytProvider).getDynamicOperator(eq(YtCluster.SENECA_MAN));

        YtClusterFreshnessLoader ytClusterFreshnessLoader =
                new YtClusterFreshnessLoader(ytProvider, dynamicConfig, ytClusterFreshnessRepository);

        assertThatCode(ytClusterFreshnessLoader::fillClusterToOperator)
                .doesNotThrowAnyException();
    }

    @Test
    public void fillClusterToOperator_successSelectRows_whenClusterIsNotAvailable() {
        doThrow(new RuntimeException()).when(ytProvider).getDynamicOperator(eq(YtCluster.SENECA_MAN));
        makeValidOperatorMock(hahnOperator);

        YtClusterFreshnessLoader ytClusterFreshnessLoader =
                new YtClusterFreshnessLoader(ytProvider, dynamicConfig, ytClusterFreshnessRepository);

        ytClusterFreshnessLoader.fillClusterToOperator();

        Map<YtCluster, ClusterFreshnessInfo> clusterFreshness = ytClusterFreshnessLoader.getAllClustersFreshness();
        assertThat(clusterFreshness).containsOnlyKeys(YtCluster.HAHN);
    }

    private void makeValidOperatorMock(YtDynamicOperator operatorMock) {
        long currentTimeMillis = System.currentTimeMillis();
        long currentTimeSeconds = currentTimeMillis / 1000;
        doReturn(wrapInRowset(Collections.singletonList(
                YTree.mapBuilder()
                        .key(LAST_TIMESTAMP_FIELD.getName()).value(currentTimeMillis)
                        .key(DBNAME_FIELD.getName()).value("ppc:1")
                        .key(GTID_SET_FIELD.getName()).value(GTID_SET_VALUE)
                        .endMap().build())))
                .when(operatorMock).selectRows(any(String.class), any());

        YtClient ytClientMock = mock(YtClient.class);
        when(operatorMock.getYtClient()).thenReturn(ytClientMock);
        when(ytClientMock.getNode(any(GetNode.class))).thenReturn(
                CompletableFuture.supplyAsync(() ->
                        YTree.attributesBuilder()
                                .key("attr_min_timestamps")
                                .value(serializeMinTimestamps(
                                        ImmutableMap.of(
                                                1, currentTimeSeconds)))
                                .endAttributes()
                                .value("empty_value")
                                .build()
                )
        );
    }
}
