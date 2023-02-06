package ru.yandex.chemodan.app.docviewer;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.dao.rights.UriRightsDao;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class RightsTest extends DocviewerSpringTestBase {

    @Autowired
    private TestManager testManager;

    @Autowired
    private UriRightsDao uriRightsDao;

    // scenario from https://jira.yandex-team.ru/browse/DOCVIEWER-758
    @Test
    public void newComerRightsExistAndUsed() {
        PassportUidOrZero uid1 = PassportUidOrZero.fromUid(101L);
        PassportUidOrZero uid2 = PassportUidOrZero.fromUid(1012);

        String fileId = testManager.makeAvailable(uid1, TestResources.Adobe_Acrobat_1_3_001p, TargetType.PDF);
        testManager.waitUriToCompleteNoCleanup(uid2, TestResources.Adobe_Acrobat_1_3_001p, TargetType.PDF);

        Assert.some(uriRightsDao.findUriByFileIdAndUid(fileId, uid2));
    }

}
