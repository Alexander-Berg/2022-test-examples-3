package ru.yandex.chemodan.app.djfs.core.diskinfo;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceType;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class DiskInfoDaoTest extends DjfsSingleUserTestBase {
    @Autowired
    DiskInfoManager diskInfoManager;

    @Test
    public void ensureRootExists() {
        diskInfoManager.ensureRootExists(UID);
        diskInfoManager.ensureRootExists(UID);

        Option<DiskInfo> diskInfoO = diskInfoDao.find(UID, "/");
        Assert.some(diskInfoO);
        DiskInfo diskInfo = diskInfoO.get();
        Assert.equals(DjfsResourcePath.getPgId(UID, "/"), diskInfo.getId());
        Assert.none(diskInfo.getParentId());
        Assert.equals("/", diskInfo.getPath());
        Assert.equals(DjfsResourceType.DIR, diskInfo.getType());
        Assert.equals(UID, diskInfo.getUid());
        Assert.none(diskInfo.getData());
    }

    @Test
    public void ensureLimitExists() {
        diskInfoManager.ensureRootExists(UID);
        diskInfoManager.ensureLimitExists(UID);
        diskInfoManager.ensureLimitExists(UID);

        Option<DiskInfo> diskInfoO = diskInfoDao.find(UID, "/limit");
        Assert.some(diskInfoO);
        DiskInfo diskInfo = diskInfoO.get();
        Assert.equals(DjfsResourcePath.getPgId(UID, "/limit"), diskInfo.getId());
        Assert.some(diskInfo.getParentId());
        Assert.equals(DjfsResourcePath.getPgId(UID, "/"), diskInfo.getParentId().get());
        Assert.equals("/limit", diskInfo.getPath());
        Assert.equals(DjfsResourceType.FILE, diskInfo.getType());
        Assert.equals(UID, diskInfo.getUid());
        Assert.some(diskInfo.getData());
        Assert.equals(1L, diskInfo.getData().map(DiskInfoData::getLongValue).get());
    }

    @Test
    public void limitRoundtrip() {
        diskInfoManager.ensureRootExists(UID);
        // we want to be sure that entry does not exist
        Assert.none(diskInfoDao.findLimit(UID));
        diskInfoDao.setLimit(UID, 10L);
        Assert.some(10L, diskInfoDao.findLimit(UID));
        diskInfoDao.setLimit(UID, 50L);
        Assert.some(50L, diskInfoDao.findLimit(UID));
    }

    @Test
    public void usedRoundtrip() {
        diskInfoManager.ensureRootExists(UID);
        // we want to be sure that entry does not exist
        Assert.none(diskInfoDao.findTotalUsed(UID));
        diskInfoDao.setTotalUsed(UID, 10L);
        Assert.some(10L, diskInfoDao.findTotalUsed(UID));
        diskInfoDao.setTotalUsed(UID, 50L);
        Assert.some(50L, diskInfoDao.findTotalUsed(UID));
    }

    @Test
    public void incrementTotalUsed() {
        diskInfoManager.ensureRootExists(UID);
        // we want to be sure that entry does not exist
        Assert.none(diskInfoDao.findTotalUsed(UID));
        diskInfoDao.incrementTotalUsed(UID, 10L);
        Assert.some(10L, diskInfoDao.findTotalUsed(UID));
        diskInfoDao.incrementTotalUsed(UID, 50L);
        Assert.some(60L, diskInfoDao.findTotalUsed(UID));
    }

    @Test
    public void incrementTrashUsed() {
        diskInfoManager.ensureRootExists(UID);
        // we want to be sure that entry does not exist
        Assert.none(diskInfoDao.findTrashUsed(UID));
        diskInfoDao.incrementTrashUsed(UID, 110L);
        Assert.some(110L, diskInfoDao.findTrashUsed(UID));
        diskInfoDao.incrementTrashUsed(UID, 510L);
        Assert.some(620L, diskInfoDao.findTrashUsed(UID));
    }
}
