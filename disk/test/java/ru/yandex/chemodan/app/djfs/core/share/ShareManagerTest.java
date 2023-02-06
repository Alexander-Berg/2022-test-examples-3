package ru.yandex.chemodan.app.djfs.core.share;

import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.NoPermissionException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.share.event.UserKickedFromGroupEvent;
import ru.yandex.chemodan.app.djfs.core.share.event.UserLeftGroupEvent;
import ru.yandex.chemodan.app.djfs.core.test.DjfsDoubleUserTestBase;
import ru.yandex.chemodan.app.djfs.core.util.InstantUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class ShareManagerTest extends DjfsDoubleUserTestBase {
    private DjfsResourcePath OWNER_PATH = DjfsResourcePath.cons(UID_1, "/disk/owner/share-owner");
    private DjfsResourcePath PARTICIPANT_PATH = DjfsResourcePath.cons(UID_2, "/disk/participant/share-participant");

    @Autowired
    private ShareManager shareManager;

    @Test
    public void kick() {
        Instant before = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(before.getMillis());

        Tuple2<Group, GroupLink> share = util.share.create(OWNER_PATH, PARTICIPANT_PATH,
                SharePermissions.READ_WRITE);
        eventInterceptor.events.clear();

        DateTimeUtils.setCurrentMillisSystem();
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        shareManager.kick(PRINCIPAL_1, share._1.getId(), UID_2);
        Assert.sizeIs(1, eventInterceptor.events);
        UserKickedFromGroupEvent event = (UserKickedFromGroupEvent) eventInterceptor.events.get(0);
        Assert.equals(UID_2, event.getUid());
        Assert.equals(now, event.getInstant());
        Assert.equals(share._1.getId(), event.getShareInfo().getGroupId());
        Assert.equals(share._2.getId(), event.getShareInfo().getGroupLinkId(UID_2).get());
        Assert.equals(PARTICIPANT_PATH, event.getUserShareRoot().get().getPath());

        Assert.some(groupDao.find(share._1.getId()));
        Assert.none(groupLinkDao.find(share._2.getId()));

        Assert.some(djfsResourceDao.find(OWNER_PATH));
        Assert.none(djfsResourceDao.find(PARTICIPANT_PATH));

        Assert.equals(InstantUtils.toVersion(before), userDao.find(UID_1).get().getVersion().get());
        Assert.equals(InstantUtils.toVersion(now), userDao.find(UID_2).get().getVersion().get());
    }

    @Test(expected = NoPermissionException.class)
    public void kickByNotOwnerThrowsException() {
        Tuple2<Group, GroupLink> share = util.share.create(OWNER_PATH, PARTICIPANT_PATH,
                SharePermissions.READ_WRITE);
        shareManager.kick(PRINCIPAL_2, share._1.getId(), UID_2);
    }

    @Test(expected = NoPermissionException.class)
    public void kickOwnerThrowsException() {
        Tuple2<Group, GroupLink> share = util.share.create(OWNER_PATH, PARTICIPANT_PATH,
                SharePermissions.READ_WRITE);
        shareManager.kick(PRINCIPAL_1, share._1.getId(), UID_1);
    }

    @Test
    public void leave() {
        Instant before = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(before.getMillis());

        Tuple2<Group, GroupLink> share = util.share.create(OWNER_PATH, PARTICIPANT_PATH,
                SharePermissions.READ_WRITE);
        eventInterceptor.events.clear();

        DateTimeUtils.setCurrentMillisSystem();
        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());

        shareManager.leave(PRINCIPAL_2, share._1.getId(), UID_2);
        Assert.sizeIs(1, eventInterceptor.events);
        UserLeftGroupEvent event = (UserLeftGroupEvent) eventInterceptor.events.get(0);
        Assert.equals(UID_2, event.getUid());
        Assert.equals(now, event.getInstant());
        Assert.equals(share._1.getId(), event.getShareInfo().getGroupId());
        Assert.equals(share._2.getId(), event.getShareInfo().getGroupLinkId(UID_2).get());
        Assert.equals(PARTICIPANT_PATH, event.getUserShareRoot().get().getPath());

        Assert.some(groupDao.find(share._1.getId()));
        Assert.none(groupLinkDao.find(share._2.getId()));

        Assert.some(djfsResourceDao.find(OWNER_PATH));
        Assert.none(djfsResourceDao.find(PARTICIPANT_PATH));

        Assert.equals(InstantUtils.toVersion(before), userDao.find(UID_1).get().getVersion().get());
        Assert.equals(InstantUtils.toVersion(now), userDao.find(UID_2).get().getVersion().get());
    }

    @Test(expected = NoPermissionException.class)
    public void leaveByNotSelfThrowsException() {
        Tuple2<Group, GroupLink> share = util.share.create(OWNER_PATH, PARTICIPANT_PATH,
                SharePermissions.READ_WRITE);
        shareManager.leave(PRINCIPAL_1, share._1.getId(), UID_2);
    }

    @Test(expected = NoPermissionException.class)
    public void leaveByOwnerThrowsException() {
        Tuple2<Group, GroupLink> share = util.share.create(OWNER_PATH, PARTICIPANT_PATH,
                SharePermissions.READ_WRITE);
        shareManager.leave(PRINCIPAL_1, share._1.getId(), UID_1);
    }
}
