package ru.yandex.chemodan.app.djfs.core.db.pg;

import java.util.HashMap;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.bolts.internal.NotImplementedException;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.util.sharpei.SharpeiClient;
import ru.yandex.chemodan.util.sharpei.SharpeiDatabaseInfo;
import ru.yandex.chemodan.util.sharpei.SharpeiShardInfo;
import ru.yandex.chemodan.util.sharpei.SharpeiUserInfo;
import ru.yandex.chemodan.util.sharpei.UserId;
import ru.yandex.misc.db.embedded.PreparedDbProvider;
import ru.yandex.misc.lang.Validate;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class InMemorySharpeiClient implements SharpeiClient {
    private ListF<SharpeiShardInfo> shards = new ArrayListF<>();
    private MapF<String, Integer> userShards = Cf.wrap(new HashMap<>());

    public void addShard(PreparedDbProvider.DbInfo dbInfo) {
        int shardId = shards.length();
        shards.add(new SharpeiShardInfo(shardId, "shard-" + shardId, Cf.arrayList(
                new SharpeiDatabaseInfo(
                        SharpeiDatabaseInfo.Role.MASTER,
                        SharpeiDatabaseInfo.Status.ALIVE,
                        new SharpeiDatabaseInfo.State(0),
                        new SharpeiDatabaseInfo.Address(dbInfo.getHost(), dbInfo.getPort(), dbInfo.getDbName(),
                                "mock-dc")))));
    }

    public void createUser(DjfsUid uid, int shard) {
        Validate.isTrue(shard > 0);
        Validate.isTrue(shard <= shards.length());
        Validate.isFalse(userShards.containsKeyTs(uid.asString()));

        userShards.put(uid.asString(), shard);
    }

    public void clear() {
        userShards.clear();
    }

    @Override
    public ListF<SharpeiShardInfo> getShards() {
        return shards;
    }

    @Override
    public void updateUser(UserId userId, Option<Tuple2<Integer, Integer>> shardUpdate,
            Option<SharpeiUserInfo.Meta> metaUpdate)
    {
        if (shardUpdate.isPresent()) {
            Assert.equals(shards.get(userShards.getTs(userId.asString()) - 1).getId(), shardUpdate.get().get1());
            userShards.put(userId.asString(), shardUpdate.get().get2() + 1);
        }
        if (metaUpdate.isPresent()) {
            throw new NotImplementedException();
        }
    }

    @Override
    public Option<SharpeiUserInfo> createUser(UserId userId) {
        throw new NotImplementedException();
    }

    @Override
    public Option<SharpeiUserInfo> findUser(UserId userId) {
        Option<Integer> shard = userShards.getO(userId.asString());
        return shard.map(x -> new SharpeiUserInfo(shards.get(x - 1), Option.empty()));
    }

    @Override
    public String getSharpeiBaseUrl() {
        return "IN_MEMORY";
    }
}
