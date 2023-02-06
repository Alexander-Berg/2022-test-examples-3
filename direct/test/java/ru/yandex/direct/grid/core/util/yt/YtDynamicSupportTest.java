package ru.yandex.direct.grid.core.util.yt;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jooq.Field;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.grid.schema.yt.tables.BidstableDirect;
import ru.yandex.direct.grid.schema.yt.tables.Directphrasestatv2Bs;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfigMockFactory;
import ru.yandex.direct.ytcomponents.repository.YtClusterFreshnessRepository;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.dynamic.YtDynamicTypesafeConfig;
import ru.yandex.direct.ytwrapper.dynamic.dsl.YtDSL;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator;
import ru.yandex.direct.ytwrapper.model.YtTable;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jooq.impl.DSL.row;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.schema.yt.Tables.BIDSTABLE_DIRECT;
import static ru.yandex.direct.grid.schema.yt.Tables.DIRECTPHRASESTATV2_BS;
import static ru.yandex.direct.ytcomponents.repository.YtClusterFreshnessRepository.LAST_TIMESTAMP_FIELD;

public class YtDynamicSupportTest {
    private static final YtTable TEST_TABLE = new YtTable("//tmp/some/path");
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    @Mock
    private YtDynamicOperator senecaDynOperator;

    @Mock
    private YtProvider ytProvider;

    @Mock
    private ShardHelper shardHelper;

    private DirectYtDynamicConfig directYtDynamicConfig;
    private YtDynamicTypesafeConfig ytDynamicConfig;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        directYtDynamicConfig = DirectYtDynamicConfigMockFactory.createConfigMock();

        Config ytConfig = ConfigFactory.parseResources("ru/yandex/direct/ytwrapper/yt_test.conf").getConfig("yt");
        ytDynamicConfig = new YtDynamicTypesafeConfig(ytConfig);
        // Так как у нас цепочка вызовов mock'ов, doReturn(..).when(..) не работает
        when(directYtDynamicConfig.tables().direct().syncStatesTablePath())
                .thenReturn(TEST_TABLE.getPath());
        when(directYtDynamicConfig.getClusterRefreshPeriod())
                .thenReturn(Duration.ofMinutes(20));

        when(senecaDynOperator.selectRows(anyString()))
                .thenReturn(new UnversionedRowset(
                        new TableSchema.Builder().addKey(LAST_TIMESTAMP_FIELD.getName(), ColumnValueType.INT64)
                                .build(), emptyList()));
        doReturn(senecaDynOperator)
                .when(ytProvider).getDynamicOperator(eq(YtCluster.SENECA_MAN));

        doReturn(asList(1, 2, 3))
                .when(shardHelper).dbShards();
    }

    @Test
    public void testSelectRowsJooq() {
        when(directYtDynamicConfig.getClusters())
                .thenReturn(singletonList(YtCluster.SENECA_MAN));

        String tableOne = "//tmp/table_one_s";
        when(directYtDynamicConfig.tables().yabsStat().phrasesTablePath())
                .thenReturn(tableOne);

        String tableTwo = "//tmp/table_two_sc";
        when(directYtDynamicConfig.tables().direct().bidsTablePath())
                .thenReturn(tableTwo);

        YtClusterFreshnessRepository ytClusterFreshnessRepository =
                new YtClusterFreshnessRepository(directYtDynamicConfig);
        YtClusterFreshnessLoader freshnessLoader = new YtClusterFreshnessLoader(ytProvider, directYtDynamicConfig,
                ytClusterFreshnessRepository);
        YtDynamicSupport ytSupport = new YtDynamicSupport(shardHelper, ytProvider, directYtDynamicConfig,
                freshnessLoader, ytDynamicConfig);
        freshnessLoader.stopTimer();

        Directphrasestatv2Bs stat = DIRECTPHRASESTATV2_BS.as("S");
        BidstableDirect bids = BIDSTABLE_DIRECT.as("B");
        Field<Long> campaignId = bids.CID.as("cid");

        LocalDate today = LocalDate.parse("2018-01-10");
        Select query = YtDSL.ytContext()
                .select(campaignId, DSL.sum(YtDSL.ytIf(stat.CURRENCY_ID.eq(0L), stat.CLICKS, stat.COST)).as("some_sum"))
                .from(bids
                        .leftJoin(stat).on(row(bids.CID, bids.PHRASE_ID).eq(stat.EXPORT_ID, stat.PHRASE_ID)))
                .where(bids.CID.in(asList(1, 2, 3))
                        .and(stat.UPDATE_TIME.greaterThan(YtDSL.toEpochSecondsAtStartOfDate(today))))
                .groupBy(campaignId);

        ytSupport.selectRows(query);

        verify(senecaDynOperator).selectRows(eq(query), eq(false),
                eq(DEFAULT_TIMEOUT), isNull());
    }

    @Test
    public void testCalculateShardToCluster() {
        long ts = System.currentTimeMillis() / 1000;
        Map<Integer, List<YtCluster>> shardToCluster = YtDynamicSupport.calculateShardToClusters(
                ImmutableMap.of(
                        YtCluster.SENECA_SAS, new ClusterFreshnessInfo(ImmutableMap.of(1, ts + 1L, 2, ts + 10L, 3, ts + 100L), 0L),
                        YtCluster.SENECA_MAN, new ClusterFreshnessInfo(ImmutableMap.of(1, ts + 10L, 2, ts + 25L, 3, ts + 1L), 0L),
                        YtCluster.SENECA_VLA, new ClusterFreshnessInfo(ImmutableMap.of(1, ts + 50L, 2, ts + 5L, 3, ts + 10L), 0L)));

        assertThat(shardToCluster).isEqualTo(
                ImmutableMap.of(
                        1, List.of(YtCluster.SENECA_VLA, YtCluster.SENECA_MAN),
                        2, List.of(YtCluster.SENECA_MAN, YtCluster.SENECA_SAS),
                        3, List.of(YtCluster.SENECA_SAS, YtCluster.SENECA_VLA)
                ));
    }

    @Test
    public void testCalculateRecommendationsShardToCluster() {
        long ts = System.currentTimeMillis() / 1000;
        Map<Integer, List<YtCluster>> recommendationsShardToCluster =
                YtRecommendationsDynamicSupport.calculateRecommendationsShardToCluster(
                        ImmutableMap.of(
                                YtCluster.SENECA_SAS,
                                new ClusterFreshnessInfo(ImmutableMap.of(1, ts + 1L, 2, ts + 10L, 3, ts + 100L), ts + 1L),
                                YtCluster.SENECA_MAN,
                                new ClusterFreshnessInfo(ImmutableMap.of(1, ts + 10L, 2, ts + 25L, 3, ts + 2L), ts + 10L),
                                YtCluster.SENECA_VLA,
                                new ClusterFreshnessInfo(ImmutableMap.of(1, ts + 50L, 2, ts + 5L, 3, ts + 10L), ts + 100L)));

        assertThat(recommendationsShardToCluster).isEqualTo(
                ImmutableMap.of(
                        1, List.of(YtCluster.SENECA_VLA, YtCluster.SENECA_MAN),
                        2, List.of(YtCluster.SENECA_MAN, YtCluster.SENECA_VLA),
                        3, List.of(YtCluster.SENECA_VLA, YtCluster.SENECA_MAN)
                ));
    }

    @Test
    public void testCalculateShardToCluster_Error_NoClusters() {
        assertThatThrownBy(() -> YtDynamicSupport.calculateShardToClusters(emptyMap()))
                .isInstanceOf(IllegalArgumentException.class);

    }
}
