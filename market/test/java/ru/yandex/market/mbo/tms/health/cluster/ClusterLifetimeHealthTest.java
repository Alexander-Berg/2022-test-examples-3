package ru.yandex.market.mbo.tms.health.cluster;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageTestUtil;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelColumns;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.tms.health.cluster.life.time.LifeTimeType;
import ru.yandex.market.mbo.tms.health.cluster.life.time.YtLifeTimeMapper;
import ru.yandex.market.mbo.tms.health.cluster.life.time.YtLifeTimeReducer;
import ru.yandex.market.mbo.yt.operations.TestYtMapReduceUtils;
import ru.yandex.market.mbo.yt.utils.YieldStub;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:linelength"})
public class ClusterLifetimeHealthTest {

    private static final AtomicInteger FAKE_MODEL_ID = new AtomicInteger();

    @Test
    public void testPublishedChanging() {
        // 1. Prepare Map Reduce data
        List<YTreeMapNode> input = Arrays.asList(
            model(1, true, false, 1),
            model(1, true, false, 30),
            model(1, true, false, 180),

            model(1, true, true, 1),
            model(1, true, true, 30),
            model(1, true, true, 180),

            model(1, false, true, 1),
            model(1, false, true, 30),
            model(1, false, true, 180),

            model(1, false, false, 0),
            model(1, false, true, 0)
        );

        // 2. Emulate MapReduce
        YieldStub<YTreeMapNode> result = TestYtMapReduceUtils.run(
            new YtLifeTimeMapper("1 2", System.currentTimeMillis()),
            new YtLifeTimeReducer(),
            Cf.list("category_id", "type", "life_days"),
            Cf.list("category_id", "type", "life_days"),
            input
        );

        // Map reduced data to string key set (e.g. DELETED_NOT_EXPIRED_DAY_0_1_7) for later check
        Set<String> reducedDataKeys = result.getOut().stream().map(v ->
            String.format("%s_%d_%d_%d",
                v.getString("type"),
                v.getLong("category_id"),
                v.getInt("life_days"),
                v.getInt("count"))
        ).collect(Collectors.toSet());

        // Check reduced data
        checkResult(LifeTimeType.DELETED_NOT_EXPIRED_DAY, 0, 1, 1, reducedDataKeys);
        checkResult(LifeTimeType.DELETED_NOT_EXPIRED_MONTH, 0, 1, 2, reducedDataKeys);
        checkResult(LifeTimeType.DELETED_NOT_EXPIRED_HALFYEAR, 0, 1, 3, reducedDataKeys);

        checkResult(LifeTimeType.DELETED_EXPIRED_DAY, 0, 1, 1, reducedDataKeys);
        checkResult(LifeTimeType.DELETED_EXPIRED_MONTH, 0, 1, 2, reducedDataKeys);
        checkResult(LifeTimeType.DELETED_EXPIRED_HALFYEAR, 0, 1, 3, reducedDataKeys);

        checkResult(LifeTimeType.EXPIRED_NOT_DELETED_DAY, 0, 1, 2, reducedDataKeys);
        checkResult(LifeTimeType.EXPIRED_NOT_DELETED_MONTH, 0, 1, 3, reducedDataKeys);
        checkResult(LifeTimeType.EXPIRED_NOT_DELETED_HALFYEAR, 0, 1, 4, reducedDataKeys);

        checkResult(LifeTimeType.EXPIRED_DELETED_DAY, 0, 1, 1, reducedDataKeys);
        checkResult(LifeTimeType.EXPIRED_DELETED_MONTH, 0, 1, 2, reducedDataKeys);
        checkResult(LifeTimeType.EXPIRED_DELETED_HALFYEAR, 0, 1, 3, reducedDataKeys);

        checkResult(LifeTimeType.NOW_EXPIRED, 0, 1, 1, reducedDataKeys);
        checkResult(LifeTimeType.NOW_EXPIRED, 1, 1, 1, reducedDataKeys);
        checkResult(LifeTimeType.NOW_EXPIRED, 30, 1, 1, reducedDataKeys);
        checkResult(LifeTimeType.NOW_EXPIRED, 180, 1, 1, reducedDataKeys);

        checkResult(LifeTimeType.NOW_ACTIVE, 0, 1, 1, reducedDataKeys);
    }

    private YTreeMapNode model(long categoryId, boolean deleted, boolean expired, long lifeInDays) {
        ModelStorage.Model.Builder modelBuilder = ModelStorageTestUtil.generateModel().toBuilder()
            .setId(FAKE_MODEL_ID.getAndIncrement())
            .setCurrentType("CLUSTER")
            .setCategoryId(categoryId)
            .setDeleted(deleted)
            .clearExpiredDate()
            .clearDeletedDate()
            .clearCreatedDate()
            .clearParentId();

        modelBuilder.setCreatedDate(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(lifeInDays));

        if (deleted) {
            modelBuilder.setDeletedDate(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(lifeInDays));
        }

        if (expired) {
            modelBuilder.setExpiredDate(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(lifeInDays));
        }

        return YTree.mapBuilder()
            .key(YtModelColumns.DATA).value(modelBuilder.build().toByteArray()).buildMap();
    }

    private void checkResult(LifeTimeType type, int lifeDays, long categoryId, int count,
                             Set<String> reducedDataKeys) {
        Assert.assertTrue(reducedDataKeys.contains(
            String.format("%s_%d_%d_%d", type.name(), categoryId, lifeDays, count))
        );
    }

}
