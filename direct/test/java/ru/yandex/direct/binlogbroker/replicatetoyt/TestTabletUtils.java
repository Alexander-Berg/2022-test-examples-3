package ru.yandex.direct.binlogbroker.replicatetoyt;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;

/**
 * Тест предназначен для ручного запуска, т.к. ходит в настоящий продакшновый YT.
 * Для CI в таком виде тест непригоден.
 */
@Ignore
@ParametersAreNonnullByDefault
public class TestTabletUtils {
    private static final Duration TABLET_TIMEOUT = Duration.ofSeconds(30);
    private static final Logger logger = LoggerFactory.getLogger(TestTabletUtils.class);

    @ClassRule
    public static JunitRealYt realYt = new JunitRealYt(YtCluster.HAHN);

    @Test
    public void test() throws TimeoutException, InterruptedException {
        Yt yt = realYt.getYt();
        YtClient ytClient = realYt.getYtClient();
        YPath basePath = YPath.simple(realYt.getBasePath());
        YPath tablePath = basePath.child("dynamic_table");

        TestTable table = new TestTable(
                tablePath.toString(),
                new TableSchema.Builder()
                        .addKey("x", ColumnValueType.INT64)
                        .addValue("y", ColumnValueType.INT64)
                        .addValue("z", ColumnValueType.STRING)
                        .build());
        table.createYtTable(ytClient);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (long x = 0; x < 100; ++x) {
            rows.add(ImmutableMap.of("x", x, "y", x % 50, "z", String.valueOf(x)));
        }
        table.addRows(rows);
        table.insertYtRows(ytClient);

        TabletUtils.waitForTablets(yt, tablePath, TabletUtils.TabletState.MOUNTED, TABLET_TIMEOUT);

        Assert.assertTrue(TabletUtils.areAllTabletsInState(yt, tablePath, TabletUtils.TabletState.MOUNTED));
        Assert.assertEquals(TabletUtils.TabletState.MOUNTED, TabletUtils.getTableState(yt, tablePath));

        logger.info("freezing mounted table");
        TabletUtils.syncFreezeTable(yt, tablePath, TABLET_TIMEOUT);
        Assert.assertTrue(TabletUtils.areAllTabletsInState(yt, tablePath, TabletUtils.TabletState.FROZEN));
        Assert.assertEquals(TabletUtils.TabletState.FROZEN, TabletUtils.getTableState(yt, tablePath));

        logger.info("unmounting frozen table");
        TabletUtils.syncUnmountTable(yt, tablePath, TABLET_TIMEOUT);
        Assert.assertTrue(TabletUtils.areAllTabletsInState(yt, tablePath, TabletUtils.TabletState.UNMOUNTED));
        Assert.assertEquals(TabletUtils.TabletState.UNMOUNTED, TabletUtils.getTableState(yt, tablePath));

        logger.info("unmounting unmounted table");
        TabletUtils.syncUnmountTable(yt, tablePath, TABLET_TIMEOUT);
        Assert.assertTrue(TabletUtils.areAllTabletsInState(yt, tablePath, TabletUtils.TabletState.UNMOUNTED));
        Assert.assertEquals(TabletUtils.TabletState.UNMOUNTED, TabletUtils.getTableState(yt, tablePath));

        logger.info("mounting unmounted table");
        TabletUtils.syncMountTable(yt, tablePath, TABLET_TIMEOUT);
        Assert.assertTrue(TabletUtils.areAllTabletsInState(yt, tablePath, TabletUtils.TabletState.MOUNTED));
        Assert.assertEquals(TabletUtils.TabletState.MOUNTED, TabletUtils.getTableState(yt, tablePath));

        logger.info("mounting mounted table");
        TabletUtils.syncMountTable(yt, tablePath, TABLET_TIMEOUT);
        Assert.assertTrue(TabletUtils.areAllTabletsInState(yt, tablePath, TabletUtils.TabletState.MOUNTED));
        Assert.assertEquals(TabletUtils.TabletState.MOUNTED, TabletUtils.getTableState(yt, tablePath));

        logger.info("unmounting mounted table");
        TabletUtils.syncUnmountTable(yt, tablePath, TABLET_TIMEOUT);
        Assert.assertTrue(TabletUtils.areAllTabletsInState(yt, tablePath, TabletUtils.TabletState.UNMOUNTED));
        Assert.assertEquals(TabletUtils.TabletState.UNMOUNTED, TabletUtils.getTableState(yt, tablePath));
    }
}
