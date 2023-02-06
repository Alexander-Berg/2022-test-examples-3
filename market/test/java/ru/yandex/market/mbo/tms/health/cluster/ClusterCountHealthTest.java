package ru.yandex.market.mbo.tms.health.cluster;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageTestUtil;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelColumns;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.tms.health.cluster.count.ClusterState;
import ru.yandex.market.mbo.tms.health.cluster.count.YtClusterCountHealthMapper;
import ru.yandex.market.mbo.tms.health.cluster.count.YtClusterCountHealthReducer;
import ru.yandex.market.mbo.yt.operations.TestYtMapReduceUtils;
import ru.yandex.market.mbo.yt.utils.YieldStub;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:linelength"})
public class ClusterCountHealthTest {

    private static final AtomicInteger FAKE_MODEL_ID = new AtomicInteger();

    @Test
    public void testPublishedChanging() {
        // 1. Prepare Map Reduce data
        List<YTreeMapNode> input = Arrays.asList(
            model(1, false, false),
            model(1, false, false),
            model(1, false, false),
            model(2, false, false),
            model(2, true, false),
            model(2, true, false),
            model(2, true, false),
            model(2, false, true),
            model(2, false, true),
            model(2, true, true)
        );

        // 2. Emulate MapReduce
        YieldStub<YTreeMapNode> result = TestYtMapReduceUtils.run(
            new YtClusterCountHealthMapper("1 2"),
            new YtClusterCountHealthReducer(),
            Cf.list("category_id", "cluster_state"), Cf.list("category_id"),
            input
        );

        // 3. Check reduce output
        List<YTreeMapNode> reduceOutputData = result.getOut();
        Assert.assertTrue(checkResult(reduceOutputData, 1, ClusterState.DELETED_FALSE_EXPIRED_FALSE, 3));
        Assert.assertTrue(checkResult(reduceOutputData, 2, ClusterState.DELETED_FALSE_EXPIRED_FALSE, 1));
        Assert.assertTrue(checkResult(reduceOutputData, 2, ClusterState.DELETED_FALSE_EXPIRED_TRUE, 3));

        // Has to be filtered
        Assert.assertTrue(checkResult(reduceOutputData, 2, ClusterState.DELETED_TRUE_EXPIRED_FALSE, 0));
        Assert.assertTrue(checkResult(reduceOutputData, 2, ClusterState.DELETED_TRUE_EXPIRED_TRUE, 0));
    }

    private YTreeMapNode model(long categoryId, boolean expired, boolean deleted) {
        ModelStorage.Model.Builder modelBuilder = ModelStorageTestUtil.generateModel().toBuilder()
            .setId(FAKE_MODEL_ID.getAndIncrement())
            .setCurrentType("CLUSTER")
            .setCategoryId(categoryId)
            .setDeleted(deleted)
            .clearExpiredDate()
            .clearParentId();

        if (expired) {
            modelBuilder.setExpiredDate(System.currentTimeMillis());
        }

        return YTree.mapBuilder()
            .key(YtModelColumns.DATA).value(modelBuilder.build().toByteArray()).buildMap();
    }

    private boolean checkResult(List<YTreeMapNode> reducedData, long categoryId, ClusterState state, int count) {
        for (YTreeMapNode data : reducedData) {
            if (data.getLong("category_id") == categoryId
                && state.name().equals(data.getString("cluster_state"))
                && data.getInt("count") == count) {
                return true;
            }
        }

        return count == 0;
    }

}
