package ru.yandex.chemodan.app.dataapi.test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.yandex.ydb.core.Result;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.core.dao.ShardPartitionDataSource;
import ru.yandex.chemodan.app.dataapi.core.dao.ShardPartitionLocator;
import ru.yandex.chemodan.app.dataapi.core.dao.support.DataApiShardPartitionDaoSupport;
import ru.yandex.chemodan.ydb.dao.ThreadLocalYdbTransactionManager;
import ru.yandex.commune.db.partition.rewrite.PartitionLocator;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.misc.ExceptionUtils;

/**
 * @author Denis Bakharev
 */
@YaIgnore
public class DataCleaner extends DataApiShardPartitionDaoSupport {

    private final Optional<ThreadLocalYdbTransactionManager> ydbTransactionManager;

    protected DataCleaner(ShardPartitionDataSource dataSource, Optional<ThreadLocalYdbTransactionManager> ydbTransactionManager) {
        super(dataSource);
        this.ydbTransactionManager = ydbTransactionManager;
    }

    public void cleanDataFromUserPartitionOfAllShards(DataApiUserId uid) {
        cleanDataFromUserPartitionOfShards(uid, shardManager2.shards().map(s -> s.getShardInfo().getId()));
    }

    public void cleanDataFromUserPartitionOfShards(DataApiUserId uid, ListF<Integer> shardIds) {
        PartitionLocator part = PartitionLocator.byDiscriminant(uid.discriminant());

        ListF<String> handles = shardIds.flatMap(shard -> getJdbcTemplate(new ShardPartitionLocator(shard, part))
                .queryForList("SELECT handle from databases_% WHERE user_id = '" + uid.toString() + "';", String.class));

        shardIds.forEach(shard -> getJdbcTemplate(new ShardPartitionLocator(shard, part)).update(""
                + "DELETE FROM databases_% WHERE user_id = '" + uid.toString() + "';\n"
                + "DELETE FROM p_data_% WHERE user_id = '" + uid.toString() + "';\n"
                + "DELETE FROM deleted_databases_% WHERE user_id = '" + uid.toString() + "';\n"
                + "DELETE FROM database_snapshots_references WHERE user_id = '" + uid.toString() + "';\n")
        );

        if (handles.isNotEmpty()) {
            shardIds.forEach(shard -> getJdbcTemplate(new ShardPartitionLocator(shard, part)).update(""
                    + "DELETE FROM deltas_% WHERE handle in (" + handles.mkString("'", "', '", "'") + ");"
            ));
        }

        if (ydbTransactionManager.isPresent()) {
            ydbTransactionManager.get().executeInTmpSession((s, tx) -> {
                String sql = ""
                        + "DELETE FROM databases WHERE user_id = '" + uid.toString() + "';\n"
                        + "DELETE FROM data WHERE user_id = '" + uid.toString() + "';\n"
                        + "DELETE FROM deleted_databases WHERE user_id = '" + uid.toString() + "';\n";

                try {
                    s.executeDataQuery(sql, tx).get().expect("Failed to clear data");
                } catch (Exception e) {
                    throw ExceptionUtils.translate(e);
                }
                return CompletableFuture.completedFuture(Result.success(null));
            });
        }
    }

    public void deleteAllSnapshotReferences(DataApiUserId uid) {
        shardManager2.shards().forEach(shard ->
                getJdbcTemplate(new ShardPartitionLocator(shard.getShardInfo().getId(), PartitionLocator.noRewrite()))
                        .update("DELETE FROM database_snapshots_references WHERE user_id = '" + uid.toString() + "';"));
    }
}
