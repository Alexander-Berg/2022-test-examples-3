package ru.yandex.chemodan.app.djfs.core.user;

import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class UserDaoTest extends DjfsSingleUserTestBase {
    @Test
    public void incrementVersionTo() {
        Assert.none(userDao.find(UID).get().getVersion());
        userDao.incrementVersionTo(UID, 125L);
        Assert.some(125L, userDao.find(UID).get().getVersion());
        userDao.incrementVersionTo(UID, 150L);
        Assert.some(150L, userDao.find(UID).get().getVersion());
    }

    @Test
    public void incrementVersionToDoesNotDecrementVersion() {
        Assert.none(userDao.find(UID).get().getVersion());
        userDao.incrementVersionTo(UID, 125L);
        Assert.some(125L, userDao.find(UID).get().getVersion());
        userDao.incrementVersionTo(UID, 100L);
        Assert.some(125L, userDao.find(UID).get().getVersion());
    }

    @Test
    public void incrementVersionTo_ReturnOld() {
        Assert.none(userDao.find(UID).get().getVersion());

        Assert.none(userDao.incrementVersionTo_ReturnOld(UID, 125L));
        Assert.some(125L, userDao.find(UID).get().getVersion());

        Assert.some(125L, userDao.incrementVersionTo_ReturnOld(UID, 150L));
        Assert.some(150L, userDao.find(UID).get().getVersion());
    }

    @Test
    public void incrementVersionTo_ReturnOldDoesNotThrowExceptionOnNonexistentUser() {
        DjfsUid uid = DjfsUid.cons("111111");
        sharpeiClient.createUser(uid, PG_SHARDS[0]);
        Assert.none(userDao.find(uid));
        Assert.none(userDao.incrementVersionTo_ReturnOld(uid, 125L));
    }

    @Test
    public void addCollectionDoesNotCreateDuplicates() {
        String collection = "test_collection";
        UserData startUser = userDao.find(UID).get();
        int startSize = startUser.getCollections().size();
        Assert.isFalse(startUser.getCollections().containsTs(collection));

        userDao.addCollection(UID, collection);
        userDao.addCollection(UID, collection);
        userDao.addCollection(UID, collection);

        UserData updatedUser = userDao.find(UID).get();
        Assert.isTrue(updatedUser.getCollections().containsTs(collection));
        Assert.sizeIs(startSize + 1, updatedUser.getCollections());
    }

    @Test
    public void setDeleted() {
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
        userDao.setDeleted(UID);
        Assert.equals(now, userDao.find(UID).get().getDeleted().get());

        DateTimeUtils.setCurrentMillisFixed(now.getMillis() + 1000);
        userDao.setDeleted(UID);
        Assert.equals(now, userDao.find(UID).get().getDeleted().get());
    }

    @Test
    public void blockUnblock() {
        Assert.isFalse(userDao.find(UID).get().isBlocked());
        userDao.block(UID);
        Assert.isTrue(userDao.find(UID).get().isBlocked());
        userDao.unblock(UID);
        Assert.isFalse(userDao.find(UID).get().isBlocked());
    }

    @Test
    public void notDefaultLocale() {
        JdbcTemplate3 shard = pgShardResolver.resolve(UID);
        String q = "UPDATE disk.user_index SET locale = ?::disk.locales WHERE uid = ?";
        shard.update(q, "uk", UID);

        Option<UserData> userData = userDao.find(UID);
        Assert.some(UserLocale.UK, userData.get().getLocale());
    }

    @Test
    public void getIsPaid() {
        JdbcTemplate3 shard = pgShardResolver.resolve(UID);
        String q = "UPDATE disk.user_index SET is_paid = ? WHERE uid = ?";

        shard.update(q, false, UID);
        Assert.equals(false, userDao.find(UID).get().isPaid());

        shard.update(q, true, UID);
        Assert.equals(true, userDao.find(UID).get().isPaid());

        shard.update(q, null, UID);
        Assert.equals(false, userDao.find(UID).get().isPaid());
    }
}
