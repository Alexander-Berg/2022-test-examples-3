package ru.yandex.chemodan.app.dataapi.test.stubs;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.db.ref.DatabaseRef;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.core.dao.usermeta.MetaUser;
import ru.yandex.chemodan.app.dataapi.core.dao.usermeta.UserMetaManager;

/**
 * @author Denis Bakharev
 */
public class UserMetaManagerStub implements UserMetaManager {
    private final MapF<String, MetaUser> metaUsersMap = Cf.hashMap();
    private final ListF<Integer> shardIds;
    public final static String WRONG_USER = "1001";

    public UserMetaManagerStub(ListF<Integer> shardIds) {
        this.shardIds = shardIds;
    }

    private void saveMetaUser(MetaUser user) {
        metaUsersMap.put(user.getUserId().toString(), user);
    }

    @Override
    public Option<MetaUser> findMetaUser(DataApiUserId user, boolean forRead) {
        if (WRONG_USER.equals(user.serialize()) && forRead) {
            return Option.empty();
        }
        return metaUsersMap
                .getO(user.toString())
                .orElse(() -> Option.of(new MetaUser(user, shardIds.sorted().first(), false, Cf.set())));
    }

    @Override
    public Option<MetaUser> findMetaUser(DataApiUserId user) {
        return findMetaUser(user, false);
    }

    @Override
    public void updateReadOnly(DataApiUserId user, boolean ro) {
        findMetaUser(user).forEach(
                u -> saveMetaUser(new MetaUser(u.getUserId(), u.getShardId(), ro, Cf.set())));
    }

    @Override
    public void updateMigrated(DataApiUserId user, DatabaseRef ref, boolean migrated) {
        if (migrated) {
            saveMetaUser(findMetaUser(user).get().withMigrated(ref));
        } else {
            saveMetaUser(findMetaUser(user).get().withoutMigrated(ref));
        }
    }

    @Override
    public void updateShardIdAndReadOnly(DataApiUserId user, int newShardId, boolean ro) {
        saveMetaUser(new MetaUser(user, newShardId, ro, Cf.set()));
    }

    @Override
    public ListF<MetaUser> findMetaUsers(ListF<DataApiUserId> userIds) {
        ListF<String> userIdStrings = userIds.map(DataApiUserId::toString);
        return metaUsersMap.filterKeys(userIdStrings::containsTs).values().toList();
    }

    @Override
    public MetaUser registerIfNotExists(DataApiUserId user) {
        MetaUser metaUser = new MetaUser(user, shardIds.first(), false, Cf.set());
        saveMetaUser(metaUser);
        return metaUser;
    }

    @Override
    public void registerBatch(ListF<DataApiUserId> usersIds) {
        usersIds.forEach(this::registerIfNotExists);
    }
}
