package ru.yandex.chemodan.app.djfs.core.share;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.test.DjfsDoubleUserTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class ShareInfoManagerTest extends DjfsDoubleUserTestBase {
    @Test
    public void getParticipantSharedSubfolders() {
        String g1 = util.share.create(UID_1, "/disk/so1", UID_2, "/disk/sp1")._1.getId();
        String g2 = util.share.create(UID_1, "/disk/o1/so2", UID_2, "/disk/p1/sp2")._1.getId();
        String g3 = util.share.create(UID_1, "/disk/o1/so3", UID_2, "/disk/p1/sp3")._1.getId();
        String g4 = util.share.create(UID_1, "/disk/o2/so4", UID_2, "/disk/p2/sp4")._1.getId();
        String g5 = util.share.create(UID_1, "/disk/o2/d1/so5", UID_2, "/disk/p2/d1/sp5")._1.getId();
        String g6 = util.share.create(UID_1, "/disk/so6", UID_2, "/disk/p2/d1/sp6")._1.getId();

        Assert.isEmpty(shareInfoManager.getParticipantSharedSubfolders(DjfsResourcePath.cons(UID_1, "/disk/so1")));
        Assert.isEmpty(shareInfoManager.getParticipantSharedSubfolders(DjfsResourcePath.cons(UID_1, "/disk/o1")));
        Assert.isEmpty(shareInfoManager.getParticipantSharedSubfolders(DjfsResourcePath.cons(UID_1, "/disk/o2")));

        Assert.arraysEquals(new String[] {g1}, shareInfoManager.getParticipantSharedSubfolders(DjfsResourcePath.cons(UID_2, "/disk/sp1")).map(x -> x.getGroupId()).toArray());
        Assert.arraysEquals(new String[] {g2, g3}, shareInfoManager.getParticipantSharedSubfolders(DjfsResourcePath.cons(UID_2, "/disk/p1")).map(x -> x.getGroupId()).toArray());
        Assert.arraysEquals(new String[] {g4, g5, g6}, shareInfoManager.getParticipantSharedSubfolders(DjfsResourcePath.cons(UID_2, "/disk/p2")).map(x -> x.getGroupId()).toArray());
    }

    @Test
    public void getJoinedGroups() {
        String g1 = util.share.create(UID_1, "/disk/so1", UID_2, "/disk/sp1")._1.getId();
        String g2 = util.share.create(UID_1, "/disk/o1/so2", UID_2, "/disk/p1/sp2")._1.getId();
        String g3 = util.share.create(UID_1, "/disk/o1/so3", UID_2, "/disk/p1/sp3")._1.getId();
        String g4 = util.share.create(UID_2, "/disk/o2/so4", UID_1, "/disk/p2/sp4")._1.getId();

        ListF<Group> joinedGroups = shareInfoManager.getJoinedGroups(UID_2);
        Assert.sizeIs(3, joinedGroups);
        Assert.equals(Cf.set(g1, g2, g3), joinedGroups.map(Group::getId).unique());
    }
}
