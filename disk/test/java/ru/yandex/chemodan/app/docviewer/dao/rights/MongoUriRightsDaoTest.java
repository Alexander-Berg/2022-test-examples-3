package ru.yandex.chemodan.app.docviewer.dao.rights;

import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class MongoUriRightsDaoTest extends DocviewerSpringTestBase {

    private static final PassportUid UID = new PassportUid(123);
    private static final ActualUri URI = new ActualUri("http://example.com/");

    @Autowired
    private MongoUriRightsDao dao;

    @Test
    public void test() {
        dao.saveOrUpdateUriRight(URI, UID.toUidOrZero());
        Assert.isFalse(accessedUris().isEmpty());
        dao.updateUriRights(URI, "42");
        Assert.isTrue(findUri("42").get().equals(URI));
        Assert.isTrue(dao.validate("42", UID.toUidOrZero()));
        Assert.isFalse(findUri("43").isPresent());
        Assert.isFalse(!findUris().isEmpty());
    }

    private Option<ActualUri> findUri(String fileId) {
        return dao.findUriByFileIdAndUid(fileId, UID.toUidOrZero());
    }

    private ListF<ActualUri> findUris() {
        return dao.findUrisAccessedByUid(UID.toUidOrZero());
    }

    private ListF<ActualUri> accessedUris() {
        return dao.findUrisAccessedByUid(UID.toUidOrZero());
    }

    @Before
    @After
    public void cleanup() {
        dao.deleteByTimestampLessBatch(Instant.now());
        accessedUris().forEach(dao::deleteUriRights);
        Assert.isTrue(accessedUris().isEmpty());
    }

}
