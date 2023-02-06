package ru.yandex.chemodan.app.dataapi.test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserType;
import ru.yandex.chemodan.app.dataapi.core.dao.support.DataApiRandomValueGenerator;
import ru.yandex.chemodan.app.dataapi.core.dao.usermeta.MetaUser;
import ru.yandex.chemodan.app.dataapi.core.dao.usermeta.UserMetaManager;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public class UserInitializer {
    private final DataCleaner dataCleaner;

    private final UserMetaManager userMetaManager;

    public final int defaultShardNumber;

    public UserInitializer(DataCleaner dataCleaner, UserMetaManager userMetaManager, int defaultShardNumber) {
        this.dataCleaner = dataCleaner;
        this.userMetaManager = userMetaManager;
        this.defaultShardNumber = defaultShardNumber;
    }

    public DataApiUserId createRandomCleanUser() {
        return createRandomCleanUser(false);
    }

    public DataApiUserId createRandomCleanUserInDefaultShard() {
        return createRandomCleanUser(true);
    }

    private DataApiUserId createRandomCleanUser(boolean registerInDefaultShard) {
        return createRandomCleanUser(Option.empty(), registerInDefaultShard);
    }

    public DataApiUserId createRandomCleanUserInDefaultShard(DataApiUserType userType) {
        return createRandomCleanUser(Option.of(userType), true);
    }

    public DataApiUserId createRandomCleanUser(Option<DataApiUserType> userType, boolean registerInDefaultShard) {
        DataApiUserId uid = new DataApiRandomValueGenerator().createDataApiUserId(userType);
        initUserForTests(uid, registerInDefaultShard);
        return uid;
    }

    public void initUserForTests(DataApiUserId uid, boolean registerInDefaultShard) {
        if (registerInDefaultShard) {
            int shard = userMetaManager.findMetaUser(uid).map(MetaUser::getShardId).getOrElse(defaultShardNumber);
            dataCleaner.cleanDataFromUserPartitionOfShards(uid, Cf.list(shard));
            userMetaManager.registerIfNotExists(uid);
        } else {
            dataCleaner.cleanDataFromUserPartitionOfAllShards(uid);
        }
    }

}
