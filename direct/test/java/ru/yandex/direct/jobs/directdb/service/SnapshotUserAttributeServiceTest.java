package ru.yandex.direct.jobs.directdb.service;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.bolts.collection.impl.DefaultMapF;
import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.directdb.model.SnapshotAttributes;
import ru.yandex.direct.ytwrapper.YtPathUtil;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeBooleanNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.common.db.PpcPropertyNames.YT_PROD_MYSQL_SHARDS_COUNT;

@ParametersAreNonnullByDefault
class SnapshotUserAttributeServiceTest {

    private static final String HOME_YT_PATH = "//home/direct";
    private static final String MYSQL_SYNC_PATH = "mysql-sync";

    @Mock
    private YtProvider ytProvider;

    @Mock
    private Yt yt;

    @Mock
    private Cypress cypress;

    @Mock
    private YtClusterConfig config;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private PpcProperty<Integer> ytProdMysqlShardsCountProperty;

    private SnapshotUserAttributeService service;

    @BeforeEach
    public void setUp() {
        initMocks(this);

        given(ytProvider.getClusterConfig(any())).willReturn(config);
        given(config.getHome()).willReturn(HOME_YT_PATH);
        given(ytProvider.get(any())).willReturn(yt);
        given(yt.cypress()).willReturn(cypress);
        given(shardHelper.dbShards()).willReturn(IntStream.range(1, 22).boxed().collect(Collectors.toList()));
        given(ytProdMysqlShardsCountProperty.getOrDefault(eq(0))).willReturn(0);
        given(ppcPropertiesSupport.get(eq(YT_PROD_MYSQL_SHARDS_COUNT), any())).willReturn(ytProdMysqlShardsCountProperty);
        service = new SnapshotUserAttributeService(ytProvider, shardHelper, ppcPropertiesSupport);
    }

    @Test
    public void shouldGetAndConvertUserAttributes() {

        var folders = new YTreeMapNodeImpl(new EmptyMap<>());

        var finishedTrue = new YTreeBooleanNodeImpl(true, new EmptyMap<>());
        var finishedFalse = new YTreeBooleanNodeImpl(false, new EmptyMap<>());
        var snapshot1 = new YTreeMapNodeImpl(new EmptyMap<>());

        var ppcFinished = new YTreeMapNodeImpl(DefaultMapF.wrap(Map.of("snapshot-finished", finishedTrue)));
        var ppcNotFinished = new YTreeMapNodeImpl(DefaultMapF.wrap(Map.of("snapshot-finished", finishedFalse)));

        for (int i = 1; i <= 21; i++) {
            snapshot1.put("ppc:" + i, ppcFinished);
        }

        var snapshot2 = new YTreeMapNodeImpl(new EmptyMap<>());

        for (int i = 1; i <= 20; i++) {
            snapshot2.put("ppc:" + i, ppcFinished);
        }
        snapshot2.put("ppc:21", ppcNotFinished);

        folders.put("snapshot--v.13--2019-12-26", new YTreeEntityNodeImpl(new EmptyMap<>()));
        folders.put("snapshot--v.13--2019-12-25", new YTreeEntityNodeImpl(new EmptyMap<>()));
        folders.put("current", new YTreeEntityNodeImpl(new EmptyMap<>()));
        folders.put("v.13", new YTreeMapNodeImpl(new EmptyMap<>()));

        var mysqlSyncPath = YtPathUtil.generatePath(HOME_YT_PATH, MYSQL_SYNC_PATH);

        given(
                cypress.get(
                        eq(YPath.simple(YtPathUtil.generatePath(mysqlSyncPath, "snapshot--v.13--2019-12-26"))),
                        anyCollection()
                )
        ).willReturn(snapshot1);
        given(
                cypress.get(
                        eq(YPath.simple(YtPathUtil.generatePath(mysqlSyncPath, "snapshot--v.13--2019-12-25"))),
                        anyCollection()
                )
        ).willReturn(snapshot2);

        given(cypress.get(eq(YPath.simple(YtPathUtil.generatePath(mysqlSyncPath, "current"))), anyCollection()))
                .willReturn(new YTreeEntityNodeImpl(new EmptyMap<>()));
        given(cypress.get(eq(YPath.simple(YtPathUtil.generatePath(mysqlSyncPath, "v.13"))), anyCollection()))
                .willReturn(new YTreeEntityNodeImpl(new EmptyMap<>()));

        given(cypress.get(eq(YPath.simple(mysqlSyncPath)), anyCollection())).willReturn(folders);

        var actualAttributes = service.getConvertedUserAttributes(YtCluster.HAHN);

        var expectedAttributes1 = new SnapshotAttributes(
                YtPathUtil.generatePath(HOME_YT_PATH, "mysql-sync/snapshot--v.13--2019-12-25"),
                false
        );
        var expectedAttributes2 = new SnapshotAttributes(
                YtPathUtil.generatePath(HOME_YT_PATH, "mysql-sync/snapshot--v.13--2019-12-26"),
                true
        );
        var expectedAttributes3 = new SnapshotAttributes(
                YtPathUtil.generatePath(HOME_YT_PATH, "mysql-sync/current"),
                false
        );
        var expectedAttributes4 = new SnapshotAttributes(
                YtPathUtil.generatePath(HOME_YT_PATH, "mysql-sync/v.13"),
                false
        );

        assertThat(actualAttributes)
                .containsExactlyInAnyOrder(expectedAttributes1, expectedAttributes2, expectedAttributes3,
                        expectedAttributes4);
    }
}
