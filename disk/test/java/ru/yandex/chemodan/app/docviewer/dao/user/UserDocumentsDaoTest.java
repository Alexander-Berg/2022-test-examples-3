package ru.yandex.chemodan.app.docviewer.dao.user;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.app.docviewer.storages.mulca.MulcaFileLink;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;

public class UserDocumentsDaoTest extends DocviewerSpringTestBase {

    static final StoredUserDocument doc = StoredUserDocument.builder()
            .uri(new ActualUri("https://mpfsHost.ru/service/public_direct_url?private_hash=hash12345"))
            .previewFileLink(new MulcaFileLink("id:0001"))
            .uid(PassportUidOrZero.fromUid(101))
            .type("image/png")
            .build();

    @Autowired
    private UserDocumentsDao userDocumentsDao;

    @Before
    public void prepare() {
        userDocumentsDao.saveOrUpdateDocument(doc);
        userDocumentsDao.updateAccessTime(doc.getUid(), doc.getUri());
    }

    @After
    public void after() {
        userDocumentsDao.delete(doc);
    }

    @Test
    public void findDocument() {
        Option<StoredUserDocument> document =
                userDocumentsDao.findDocument(doc.getUid(), doc.getUri());
        Assert.some(document);
        StoredUserDocument d = document.get();
        Assert.equals(d.getId(), doc.getId());
        Assert.equals(d.getPreviewFileLink(), doc.getPreviewFileLink());
        Assert.equals(d.getUid(), doc.getUid());
        Assert.equals(d.getUri(), doc.getUri());
        Assert.notEquals(d.getLastAccess(), doc.getLastAccess());

        ListF<StoredUserDocument> documentByUrl = userDocumentsDao.findDocumentByUrl(doc.getUri());
        Assert.notEmpty(documentByUrl);
    }

    @Test
    public void findDocuments() {
        List<StoredUserDocument> documents =
                userDocumentsDao.findDocuments(doc.getUid(), 10, 0);
        Assert.notEmpty(documents);
    }
}
