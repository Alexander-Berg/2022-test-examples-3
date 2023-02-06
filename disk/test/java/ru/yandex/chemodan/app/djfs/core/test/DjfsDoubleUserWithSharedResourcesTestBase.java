package ru.yandex.chemodan.app.djfs.core.test;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.share.ShareManager;
import ru.yandex.chemodan.app.djfs.core.share.SharePermissions;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;

public abstract class DjfsDoubleUserWithSharedResourcesTestBase extends DjfsDoubleUserTestBase {
    protected DjfsResourcePath OWNER_PATH = DjfsResourcePath.cons(UID_1, "/disk/owner/share-owner");
    protected DjfsResourcePath PARTICIPANT_PATH = DjfsResourcePath.cons(UID_2, "/disk/participant/share-participant");

    protected DjfsUid OWNER_UID = UID_1;
    protected DjfsUid PARTICIPANT_UID = UID_2;

    protected DjfsPrincipal OWNER_PRINCIPAL = PRINCIPAL_1;
    protected DjfsPrincipal PARTICIPANT_PRINCIPAL = PRINCIPAL_2;

    protected String groupId;
    protected String groupLinkId;

    @Autowired
    protected ShareManager shareManager;

    @Override
    @Before
    public void setUp() {
        super.setUp();

        filesystem.createFolder(PRINCIPAL_1, OWNER_PATH.getParent());
        filesystem.createFolder(PRINCIPAL_1, OWNER_PATH);
        filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH.getParent());
        filesystem.createFolder(PRINCIPAL_2, PARTICIPANT_PATH);

        groupId = shareManager.createGroup(OWNER_PATH).getId();
        groupLinkId = shareManager.createLink(groupId, PARTICIPANT_PATH, SharePermissions.READ_WRITE).getId();

        changelogDao.deleteAll(UID_1);
        changelogDao.deleteAll(UID_2);
        mockCeleryTaskManager.submitted.clear();
        mockEventHistoryLogger.messageData.clear();
    }
}
