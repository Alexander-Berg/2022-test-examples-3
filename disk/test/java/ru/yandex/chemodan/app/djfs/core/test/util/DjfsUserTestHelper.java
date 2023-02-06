package ru.yandex.chemodan.app.djfs.core.test.util;

import java.util.function.Function;

import org.joda.time.Instant;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.db.pg.InMemorySharpeiClient;
import ru.yandex.chemodan.app.djfs.core.diskinfo.DiskInfoManager;
import ru.yandex.chemodan.app.djfs.core.filesystem.Filesystem;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.UserDao;
import ru.yandex.chemodan.app.djfs.core.user.UserData;
import ru.yandex.chemodan.app.djfs.core.user.UserLocale;
import ru.yandex.chemodan.app.djfs.core.user.UserType;

public class DjfsUserTestHelper {

    private final InMemorySharpeiClient sharpeiClient;

    private final UserDao userDao;

    private final DiskInfoManager diskInfoManager;

    private final Filesystem filesystem;

    public DjfsUserTestHelper(InMemorySharpeiClient sharpeiClient, UserDao userDao,  DiskInfoManager diskInfoManager,
                              Filesystem filesystem)
    {
        this.sharpeiClient = sharpeiClient;
        this.userDao = userDao;
        this.diskInfoManager = diskInfoManager;
        this.filesystem = filesystem;
    }

    public void initializePgUser(DjfsUid uid, int shard,
                                    Function<UserData.UserDataBuilder, UserData.UserDataBuilder> customization) {
        UserData userData = toUserData(uid, customization);
        // todo: basic collections, user_index
        sharpeiClient.createUser(uid, shard);
        userDao.insert(userData);
        diskInfoManager.ensureRootExists(uid);
        filesystem.initialize(uid);
    }

    private UserData toUserData(DjfsUid uid,
                                Function<UserData.UserDataBuilder, UserData.UserDataBuilder> customization)
    {
        UserData.UserDataBuilder builder = UserData.builder()
                .id(uid)
                .regTime(Option.of(Instant.now()))
                .locale(Option.of(UserLocale.RU))
                .shardKey(0)
                .collections(Cf.list())
                .type(Option.of(UserType.STANDARD))
                .version(Option.empty())
                .minimumDeltaVersion(Option.empty())
                .blocked(false)
                .deleted(Option.empty())
                .b2bKey(Option.empty())
                .isQuickMoveUser(false)
                .isPg(false)
                .yateamUid(Option.empty())
                .pdd(Option.empty());
        return customization.apply(builder).build();
    }
}
