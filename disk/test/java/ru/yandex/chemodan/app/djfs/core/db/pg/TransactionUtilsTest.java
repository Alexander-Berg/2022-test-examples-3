package ru.yandex.chemodan.app.djfs.core.db.pg;

import org.bson.types.ObjectId;
import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.changelog.Changelog;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceType;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class TransactionUtilsTest extends DjfsTestBase {
    @Test
    public void executeInTransaction() {
        DjfsUid uid = DjfsUid.cons(31337);
        initializeUser(uid, 1);
        Assert.isEmpty(changelogDao.findAll(uid));

        Assert.assertThrows(() ->
                transactionUtils.executeInNewOrCurrentTransaction(uid, () -> {
                    Changelog changelog = Changelog.builder()
                            .id(new ObjectId())
                            .path(DjfsResourcePath.cons(uid, "/disk/path"))
                            .resourceType(DjfsResourceType.FILE)
                            .operationType(Changelog.OperationType.NEW)
                            .version(1)
                            .build();
                    changelogDao.insert(changelog);
                    Assert.notEmpty(changelogDao.findAll(uid));
                    throw new RuntimeException("rollback insert please");
                }), RuntimeException.class);

        Assert.isEmpty(changelogDao.findAll(uid));
    }
}
