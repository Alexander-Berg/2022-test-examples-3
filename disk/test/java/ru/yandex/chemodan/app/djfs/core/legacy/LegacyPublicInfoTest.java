package ru.yandex.chemodan.app.djfs.core.legacy;

import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.LegacyMpfsAes;
import ru.yandex.chemodan.app.djfs.core.filesystem.exception.OverdraftUserPublicLinkException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourceId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FolderDjfsResource;
import ru.yandex.chemodan.app.djfs.core.publication.LinkData;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.chemodan.app.djfs.core.web.JsonStringResult;
import ru.yandex.misc.test.Assert;

public class LegacyPublicInfoTest extends LegacyActionsTestBase {
    @Autowired
    private LegacyMpfsAes legacyMpfsAes;

    @Test
    public void testPublicInfoForCreatedUser() {
        FolderDjfsResource folder = createFolder("/disk/sharedFolder");

        LinkData linkData = createLink(folder);

        makePublic(folder, linkData);

        JsonStringResult result = legacyFilesystemActions.public_info(
                Option.of(UID_2.asString()),
                getHash(linkData),
                Option.empty()
        );
        Assert.assertContains(result.getResult(), "sharedFolder");
    }

    @Test
    public void testPublicInfoForZeroUser() {
        FolderDjfsResource folder = createFolder("/disk/sharedFolder");

        LinkData linkData = createLink(folder);

        makePublic(folder, linkData);

        JsonStringResult result = legacyFilesystemActions.public_info(
                Option.of("0"),
                getHash(linkData),
                Option.empty()
        );
        Assert.assertContains(result.getResult(), "sharedFolder");
    }

    @Test
    public void testPublicInfoForNotInitializedUser() {
        FolderDjfsResource folder = createFolder("/disk/sharedFolder");

        LinkData linkData = createLink(folder);

        makePublic(folder, linkData);

        JsonStringResult result = legacyFilesystemActions.public_info(
                Option.of("123123123123123123"),
                getHash(linkData),
                Option.empty()
        );
        Assert.assertContains(result.getResult(), "sharedFolder");
    }

    @Test
    public void testPublicInfoWithOverdraftRestriction() {
        FolderDjfsResource folder = createFolder("/disk/sharedFolder");

        LinkData linkData = createLink(folder);

        makePublic(folder, linkData);

        long gb = 1 << 30;
        diskInfoDao.setTotalUsed(UID_1, 12 * gb);
        quotaManager.setLimit(UID_1, 10 * gb);

        Assert.assertThrows(
                () -> legacyFilesystemActions.public_info(
                        Option.of(UID_2.asString()),
                        getHash(linkData),
                        Option.empty()
                ),
                OverdraftUserPublicLinkException.class
        );
    }

    @Test
    public void testPublicInfoWithoutOverdraftRestriction() {
        FolderDjfsResource folder = createFolder("/disk/sharedFolder");

        LinkData linkData = createLink(folder);

        makePublic(folder, linkData);

        long gb = 1 << 30;
        long mb = 1 << 20;
        diskInfoDao.setTotalUsed(UID_1, 10 * gb + 512 * mb);
        quotaManager.setLimit(UID_1, 10 * gb);

        JsonStringResult result = legacyFilesystemActions.public_info(
                Option.of(UID_2.asString()),
                getHash(linkData),
                Option.empty()
        );
        Assert.assertContains(result.getResult(), "sharedFolder");
    }

    @Test
    public void testPublicListForCreatedUser() {
        FolderDjfsResource folder = createFolder("/disk/sharedFolder");

        LinkData linkData = createLink(folder);

        makePublic(folder, linkData);

        JsonStringResult result = legacyFilesystemActions.public_list(
                Option.of(UID_2.asString()),
                getHash(linkData),
                Option.empty(),
                Option.empty(),
                10
        );
        Assert.assertContains(result.getResult(), "sharedFolder");
    }

    @Test
    public void testPublicListForZeroUser() {
        FolderDjfsResource folder = createFolder("/disk/sharedFolder");

        LinkData linkData = createLink(folder);

        makePublic(folder, linkData);

        JsonStringResult result = legacyFilesystemActions.public_list(
                Option.of("0"),
                getHash(linkData),
                Option.empty(),
                Option.empty(),
                10
        );
        Assert.assertContains(result.getResult(), "sharedFolder");
    }

    @Test
    public void testPublicListForNotInitializedUser() {
        FolderDjfsResource folder = createFolder("/disk/sharedFolder");

        LinkData linkData = createLink(folder);

        makePublic(folder, linkData);

        JsonStringResult result = legacyFilesystemActions.public_list(
                Option.of("123123123123123123"),
                getHash(linkData),
                Option.empty(),
                Option.empty(),
                10
        );
        Assert.assertContains(result.getResult(), "sharedFolder");
    }

    @Test
    public void testPublicListWithOverDraftRestriction() {
        FolderDjfsResource folder = createFolder("/disk/sharedFolder");

        LinkData linkData = createLink(folder);

        makePublic(folder, linkData);

        long gb = 1 << 30;
        diskInfoDao.setTotalUsed(UID_1, 12 * gb);
        quotaManager.setLimit(UID_1, 10 * gb);

        Assert.assertThrows(
                () -> legacyFilesystemActions.public_list(
                        Option.of(UID_2.asString()),
                        getHash(linkData),
                        Option.empty(),
                        Option.empty(),
                        10
                ),
                OverdraftUserPublicLinkException.class
        );
    }

    @Test
    public void testPublicListWithoutOverDraftRestriction() {
        FolderDjfsResource folder = createFolder("/disk/sharedFolder");

        LinkData linkData = createLink(folder);

        makePublic(folder, linkData);

        long gb = 1 << 30;
        long mb = 1 << 20;
        diskInfoDao.setTotalUsed(UID_1, 10 * gb + 512 * mb);
        quotaManager.setLimit(UID_1, 10 * gb);

        JsonStringResult result = legacyFilesystemActions.public_list(
                Option.of(UID_2.asString()),
                getHash(linkData),
                Option.empty(),
                Option.empty(),
                10
        );
        Assert.assertContains(result.getResult(), "sharedFolder");
    }

    public LinkData createLink(FolderDjfsResource folder) {
        LinkData rootLinkData = LinkData.builder()
                .id(UuidUtils.randomToHexString())
                .linkDataPath("/")
                .type("dir")
                .uid(UID_1)
                .build();
        linkDataDao.insert(rootLinkData);
        LinkData linkData = LinkData.builder()
                .id(UuidUtils.randomToHexString())
                .linkDataPath("/123")
                .type("file")
                .parentId(Option.of(rootLinkData.getId()))
                .uid(UID_1)
                .targetPath(Option.of(folder.getPath()))
                .resourceId(Option.of(DjfsResourceId.cons(UID_1, folder.getFileId().get())))
                .build();
        linkDataDao.insert(linkData);
        return linkData;
    }

    private FolderDjfsResource createFolder(String path) {
        DjfsResourcePath folderPath = DjfsResourcePath.cons(UID_1, path);
        return filesystem.createFolder(PRINCIPAL_1, folderPath, x -> x.isPublic(true));
    }

    private String getHash(LinkData linkData) {
        return legacyMpfsAes.encrypt(linkData.getUid().asString() + ":" + linkData.getLinkDataPath().substring(1));
    }

    /**
     * stub for mpfs functionality, may be wrong
     */
    private void makePublic(DjfsResource djfsResource, LinkData linkData) {
        DjfsUid uid = djfsResource.getUid();
        String s = "UPDATE disk.folders SET "
                + "public = true, public_hash = :public_hash::bytea, short_url = :short_url, "
                + "symlink = :symlink, yarovaya_mark = true"
                + " WHERE uid = :uid and fid = :fid";
        String q = "UPDATE disk.files SET "
                + "public = true, public_hash = :public_hash::bytea, short_url = :short_url, "
                + "symlink = :symlink, yarovaya_mark = true"
                + " WHERE uid = :uid and fid = :fid";

        Map<String, Object> parameters = Cf.hashMap();
        parameters.put("uid", uid);
        parameters.put("fid", djfsResource.getId());
        parameters.put("symlink", linkData.getId());
        parameters.put("public_hash", getHash(linkData));
        parameters.put("short_url", "stub for: clck.generate(self.get_public_url(), mode=mode)");
        pgShardResolver.resolve(uid).update(s, parameters);
        pgShardResolver.resolve(uid).update(q, parameters);
    }

}
