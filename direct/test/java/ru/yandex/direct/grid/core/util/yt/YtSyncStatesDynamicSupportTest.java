package ru.yandex.direct.grid.core.util.yt;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfigMockFactory;
import ru.yandex.direct.ytcomponents.model.MysqlSyncStates;
import ru.yandex.direct.ytwrapper.model.YtCluster;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class YtSyncStatesDynamicSupportTest {

    private static final String GTID_SET_ZENO_PPC1 =
            "3785dbf8-f2f3-11ea-8114-da0aa51b98ab:1-29554687,5aeb83cb-f2f3-11ea-8737-a9bebf814aec:1-11484182";
    private static final String GTID_SET_ZENO_PPC2 =
            "1c29de72-f309-11ea-9904-1ed7dabf31b3:1-23813046,3c697407-f309-11ea-aa2b-21d0affde78c:1-10565922";
    private static final String GTID_SET_SENECA_PPC1 =
            "3785dbf8-f2f3-11ea-8114-da0aa51b98ab:1-29554687,5aeb83cb-f2f3-11ea-8737-a9bebf814aec:1-11486037";

    private YtSyncStatesLoader ytSyncStatesLoader;
    private YtSyncStatesDynamicSupport ytSyncStatesDynamicSupport;

    @Before
    public void before() {
        DirectYtDynamicConfig dynamicConfig = DirectYtDynamicConfigMockFactory.createConfigMock();
        when(dynamicConfig.getClusters()).thenReturn(Arrays.asList(YtCluster.ZENO, YtCluster.SENECA_MAN));

        ytSyncStatesLoader = mock(YtSyncStatesLoader.class);
        ytSyncStatesDynamicSupport = new YtSyncStatesDynamicSupport(ytSyncStatesLoader, dynamicConfig);
    }

    @Test
    public void getGtidSetForShard_whenOneAvailableCluster() {
        long currentTimeMillis = System.currentTimeMillis();

        List<MysqlSyncStates> syncStates = List.of(
                new MysqlSyncStates()
                        .withDbName("ppc:1")
                        .withLastTimestamp(currentTimeMillis - 1000)
                        .withGtidSet(GTID_SET_ZENO_PPC1)
        );
        doReturn(syncStates).when(ytSyncStatesLoader).getSyncStatesForCluster(eq(YtCluster.ZENO));
        doReturn(null).when(ytSyncStatesLoader).getSyncStatesForCluster(eq(YtCluster.SENECA_MAN));

        Optional<String> gtidSet = ytSyncStatesDynamicSupport.getGtidSetForShard(1);
        assertThat(gtidSet).isEqualTo(Optional.of(GTID_SET_ZENO_PPC1));
    }

    @Test
    public void getGtidSetForShard_whenNoAvailableCluster() {
        doReturn(emptyList()).when(ytSyncStatesLoader).getSyncStatesForCluster(eq(YtCluster.ZENO));
        doReturn(null).when(ytSyncStatesLoader).getSyncStatesForCluster(eq(YtCluster.SENECA_MAN));

        Optional<String> gtidSet = ytSyncStatesDynamicSupport.getGtidSetForShard(1);
        assertThat(gtidSet).isEqualTo(Optional.empty());
    }

    @Test
    public void getGtidSetForShard_whenTwoAvailableClusters() {
        long currentTimeMillis = System.currentTimeMillis();

        List<MysqlSyncStates> zenoSyncStates = List.of(
                new MysqlSyncStates()
                        .withDbName("ppc:1")
                        .withLastTimestamp(currentTimeMillis - 1000)
                        .withGtidSet(GTID_SET_ZENO_PPC1),
                new MysqlSyncStates()
                        .withDbName("ppc:2")
                        .withLastTimestamp(currentTimeMillis - 500)
                        .withGtidSet(GTID_SET_ZENO_PPC2)
        );
        List<MysqlSyncStates> senecaSyncStates = List.of(
                new MysqlSyncStates()
                        .withDbName("ppc:1")
                        .withLastTimestamp(currentTimeMillis - 200)
                        .withGtidSet(GTID_SET_SENECA_PPC1)
        );
        doReturn(zenoSyncStates).when(ytSyncStatesLoader).getSyncStatesForCluster(eq(YtCluster.ZENO));
        doReturn(senecaSyncStates).when(ytSyncStatesLoader).getSyncStatesForCluster(eq(YtCluster.SENECA_MAN));

        Map<Integer, Optional<String>> expectedGtidSetByShard = Map.of(
                1, Optional.of(GTID_SET_SENECA_PPC1),
                2, Optional.of(GTID_SET_ZENO_PPC2),
                3, Optional.empty()
        );
        SoftAssertions softAssertions = new SoftAssertions();

        for (Integer shard : expectedGtidSetByShard.keySet()) {
            Optional<String> expected = expectedGtidSetByShard.get(shard);
            Optional<String> actual = ytSyncStatesDynamicSupport.getGtidSetForShard(shard);
            softAssertions.assertThat(actual).isEqualTo(expected);
        }
        softAssertions.assertAll();
    }
}
