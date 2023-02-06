package ru.yandex.chemodan.app.docviewer.cleanup;

import java.net.MalformedURLException;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.app.docviewer.copy.StoredUriManager;
import ru.yandex.chemodan.app.docviewer.dao.results.StoredResultDao;
import ru.yandex.chemodan.app.docviewer.dao.rights.UriRightsDao;
import ru.yandex.chemodan.app.docviewer.dao.uris.StoredUri;
import ru.yandex.chemodan.app.docviewer.dao.uris.StoredUriDao;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author akirakozov
 */
public class CleanupManagerTest extends DocviewerSpringTestBase {

    @Autowired
    private CleanupManager cleanupManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private StoredUriDao storedUriDao;
    @Autowired
    private StoredUriManager storedUriManager;
    @Autowired
    private StoredResultDao storedResultDao;
    @Autowired
    private UriRightsDao uriRightsDao;

    // TODO generate image and check that it is then deleted
    @Test
    public void testCleanupByUri() throws MalformedURLException {
        ActualUri uri = new ActualUri(TestResources.Adobe_Acrobat_1_3_001p);

        testManager.makeAvailable(PassportUidOrZero.zero(), uri.getUri().toURL(), TargetType.HTML_WITH_IMAGES);

        StoredUri storedUri = storedUriDao.find(uri).getOrThrow("Stored uri should exist");
        String fileId = storedUri.getFileId().getOrThrow("File id should exist");

        Assert.some(storedResultDao.find(fileId, TargetType.HTML_WITH_IMAGES));

        cleanupManager.cleanupByActualUri(uri);

        Assert.none(storedUriDao.find(uri));
        Assert.none(storedUriManager.findByFileIdAndUidO(fileId, PassportUidOrZero.zero()));
        Assert.none(storedResultDao.find(fileId, TargetType.HTML_WITH_IMAGES));
    }

    @Test
    public void testCleanupByUid() throws MalformedURLException {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(789);
        ActualUri uri = new ActualUri(TestResources.Adobe_Acrobat_1_3_001p);

        cleanupManager.cleanupByActualUri(uri);
        testManager.makeAvailable(uid, uri.getUri().toURL(), TargetType.HTML_WITH_IMAGES);

        StoredUri storedUri = storedUriDao.find(uri).getOrThrow("Stored uri should exist");
        String fileId = storedUri.getFileId().getOrThrow("File id should exist");

        Assert.some(storedUriDao.find(uri));
        Assert.isTrue(uriRightsDao.findExistsUriRight(uri, uid));

        cleanupManager.cleanupByUid(uid);

        Assert.none(storedUriDao.find(uri));
        Assert.isFalse(uriRightsDao.findExistsUriRight(uri, uid));
        Assert.none(storedResultDao.find(fileId, TargetType.HTML_WITH_IMAGES));
    }

    @Test
    public void testCleanupByUid2() {
        PassportUidOrZero uid = PassportUidOrZero.fromUid(4002012097L);
        cleanupManager.cleanupByUid(uid);
    }

    @Test
    public void testCleanupByAge() throws MalformedURLException {
        cleanupManager.removeAll();

        PassportUidOrZero uid = PassportUidOrZero.fromUid(790);
        ActualUri uri1 = new ActualUri(TestResources.Adobe_Photoshop_CS2);
        ActualUri uri2 = new ActualUri(TestResources.Adobe_Photoshop_CS5);

        testManager.makeAvailable(uid, uri1.getUri().toURL(), TargetType.HTML_WITH_IMAGES);

        Instant after1 = TimeUtils.now();
        ThreadUtils.sleep(10);

        testManager.makeAvailable(uid, uri2.getUri().toURL(), TargetType.HTML_WITH_IMAGES);

        cleanupManager.cleanupByAgeFull(Option.of(new Duration(after1, TimeUtils.now())));

        Assert.none(storedUriDao.find(uri1));
        Assert.some(storedUriDao.find(uri2));
    }

}
